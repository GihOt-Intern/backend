using UnityEngine;
using System.Collections.Generic;
using System.Collections;

public class GameManager : MonoBehaviour
{
    [Header("Player Management")]
    public GameObject playerPrefab;
    public Transform playerSpawnParent;
    public Vector3[] spawnPositions = new Vector3[4];
    
    [Header("Game Settings")]
    public int maxPlayers = 4;
    public float gameStartDelay = 3f;
    
    private UnityPositionSync positionSync;
    private Dictionary<short, CharacterController> players = new Dictionary<short, CharacterController>();
    private short localPlayerSlot = -1;
    private bool gameStarted = false;
    
    void Start()
    {
        // Get position sync component
        positionSync = FindObjectOfType<UnityPositionSync>();
        if (positionSync == null)
        {
            Debug.LogError("UnityPositionSync not found!");
            return;
        }
        
        // Start monitoring for player assignments
        StartCoroutine(MonitorPlayerAssignments());
    }
    
    void Update()
    {
        if (!gameStarted || positionSync == null) return;
        
        // Update all players
        UpdatePlayers();
        
        // Handle player disconnections
        HandlePlayerDisconnections();
    }
    
    System.Collections.IEnumerator MonitorPlayerAssignments()
    {
        // Wait for connection
        while (!positionSync.IsConnected())
        {
            yield return new WaitForSeconds(0.1f);
        }
        
        Debug.Log("Connected to server, waiting for slot assignment...");
        
        // Wait for slot assignment
        while (positionSync.GetMySlot() == -1)
        {
            yield return new WaitForSeconds(0.1f);
        }
        
        localPlayerSlot = positionSync.GetMySlot();
        Debug.Log($"Local player assigned to slot: {localPlayerSlot}");
        
        // Create local player
        CreatePlayer(localPlayerSlot, true);
        
        // Start game
        StartCoroutine(StartGame());
    }
    
    System.Collections.IEnumerator StartGame()
    {
        Debug.Log("Game starting in " + gameStartDelay + " seconds...");
        yield return new WaitForSeconds(gameStartDelay);
        
        gameStarted = true;
        Debug.Log("Game started!");
        
        // Start monitoring for other players
        StartCoroutine(MonitorOtherPlayers());
    }
    
    System.Collections.IEnumerator MonitorOtherPlayers()
    {
        while (gameStarted)
        {
            // Check for new players
            Dictionary<short, Vector3> positions = positionSync.GetOtherPlayerPositions();
            
            foreach (var kvp in positions)
            {
                short slot = kvp.Key;
                
                // Skip local player
                if (slot == localPlayerSlot)
                    continue;
                
                // Create player if not exists
                if (!players.ContainsKey(slot))
                {
                    CreatePlayer(slot, false);
                    Debug.Log($"Created remote player for slot: {slot}");
                }
            }
            
            yield return new WaitForSeconds(0.5f); // Check every 500ms
        }
    }
    
    void CreatePlayer(short slot, bool isLocalPlayer)
    {
        if (playerPrefab == null)
        {
            Debug.LogError("Player prefab not assigned!");
            return;
        }
        
        // Calculate spawn position
        Vector3 spawnPos = GetSpawnPosition(slot);
        
        // Create player GameObject
        GameObject playerObj = Instantiate(playerPrefab, spawnPos, Quaternion.identity, playerSpawnParent);
        playerObj.name = $"Player_{slot}";
        
        // Setup character controller
        CharacterController characterController = playerObj.GetComponent<CharacterController>();
        if (characterController == null)
        {
            characterController = playerObj.AddComponent<CharacterController>();
        }
        
        characterController.slot = slot;
        characterController.isLocalPlayer = isLocalPlayer;
        
        // Setup visual elements
        SetupPlayerVisuals(playerObj, slot, isLocalPlayer);
        
        // Add to players dictionary
        players[slot] = characterController;
        
        Debug.Log($"Created player {slot} (Local: {isLocalPlayer}) at position {spawnPos}");
    }
    
    Vector3 GetSpawnPosition(short slot)
    {
        int index = (slot - 1) % spawnPositions.Length;
        return spawnPositions[index];
    }
    
    void SetupPlayerVisuals(GameObject playerObj, short slot, bool isLocalPlayer)
    {
        // Setup character model
        GameObject characterModel = playerObj.transform.Find("CharacterModel")?.gameObject;
        if (characterModel != null)
        {
            Renderer renderer = characterModel.GetComponent<Renderer>();
            if (renderer != null)
            {
                Material material = new Material(renderer.material);
                
                if (isLocalPlayer)
                {
                    material.color = Color.cyan;
                }
                else
                {
                    // Different colors for different slots
                    Color[] colors = { Color.red, Color.blue, Color.yellow, Color.green };
                    int colorIndex = (slot - 1) % colors.Length;
                    material.color = colors[colorIndex];
                }
                
                renderer.material = material;
            }
        }
        
        // Setup name tag
        GameObject nameTag = playerObj.transform.Find("NameTag")?.gameObject;
        if (nameTag != null)
        {
            TextMesh nameText = nameTag.GetComponent<TextMesh>();
            if (nameText != null)
            {
                nameText.text = $"Player {slot}";
                nameText.color = isLocalPlayer ? Color.green : Color.white;
            }
        }
        
        // Setup health bar (if exists)
        GameObject healthBar = playerObj.transform.Find("HealthBar")?.gameObject;
        if (healthBar != null)
        {
            // Setup health bar logic here
        }
    }
    
    void UpdatePlayers()
    {
        // Update all players
        foreach (var kvp in players)
        {
            short slot = kvp.Key;
            CharacterController player = kvp.Value;
            
            if (player != null)
            {
                // Player controller handles its own updates
                // This is just for game manager logic
            }
        }
    }
    
    void HandlePlayerDisconnections()
    {
        List<short> disconnectedSlots = new List<short>();
        
        // Check for disconnected players
        foreach (var kvp in players)
        {
            short slot = kvp.Key;
            CharacterController player = kvp.Value;
            
            // Skip local player
            if (slot == localPlayerSlot)
                continue;
            
            // Check if player still exists in position sync
            Dictionary<short, Vector3> positions = positionSync.GetOtherPlayerPositions();
            if (!positions.ContainsKey(slot))
            {
                disconnectedSlots.Add(slot);
            }
        }
        
        // Remove disconnected players
        foreach (short slot in disconnectedSlots)
        {
            RemovePlayer(slot);
        }
    }
    
    void RemovePlayer(short slot)
    {
        if (players.ContainsKey(slot))
        {
            CharacterController player = players[slot];
            if (player != null)
            {
                // Add disconnect effect
                AddDisconnectEffect(player.transform.position);
                
                // Destroy player GameObject
                Destroy(player.gameObject);
            }
            
            players.Remove(slot);
            Debug.Log($"Removed player {slot}");
        }
    }
    
    void AddDisconnectEffect(Vector3 position)
    {
        // Create disconnect particle effect
        GameObject disconnectEffect = new GameObject("DisconnectEffect");
        disconnectEffect.transform.position = position;
        
        ParticleSystem particles = disconnectEffect.AddComponent<ParticleSystem>();
        var main = particles.main;
        main.startLifetime = 2f;
        main.startSpeed = 3f;
        main.startSize = 0.5f;
        main.startColor = Color.red;
        
        var emission = particles.emission;
        emission.rateOverTime = 20;
        
        var shape = particles.shape;
        shape.shapeType = ParticleSystemShapeType.Sphere;
        shape.radius = 1f;
        
        // Destroy effect after 3 seconds
        Destroy(disconnectEffect, 3f);
    }
    
    // Public methods
    public CharacterController GetPlayer(short slot)
    {
        return players.ContainsKey(slot) ? players[slot] : null;
    }
    
    public Dictionary<short, CharacterController> GetAllPlayers()
    {
        return new Dictionary<short, CharacterController>(players);
    }
    
    public short GetLocalPlayerSlot()
    {
        return localPlayerSlot;
    }
    
    public bool IsGameStarted()
    {
        return gameStarted;
    }
    
    public int GetPlayerCount()
    {
        return players.Count;
    }
    
    // Debug methods
    void OnGUI()
    {
        if (!gameStarted) return;
        
        GUILayout.BeginArea(new Rect(10, 10, 300, 200));
        GUILayout.Label($"Game Status: {(gameStarted ? "Running" : "Waiting")}");
        GUILayout.Label($"Local Player Slot: {localPlayerSlot}");
        GUILayout.Label($"Total Players: {players.Count}");
        GUILayout.Label($"Connection: {(positionSync.IsConnected() ? "Connected" : "Disconnected")}");
        
        // Show interpolation status
        Dictionary<short, bool> interpStatus = positionSync.GetInterpolationStatus();
        GUILayout.Label("Interpolation Status:");
        foreach (var kvp in interpStatus)
        {
            GUILayout.Label($"  Slot {kvp.Key}: {(kvp.Value ? "Active" : "Inactive")}");
        }
        
        GUILayout.EndArea();
    }
} 