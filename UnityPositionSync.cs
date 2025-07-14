using UnityEngine;
using System;
using System.Collections.Generic;
using System.IO;
using System.Net.WebSockets;
using System.Threading;
using System.Threading.Tasks;

public class UnityPositionSync : MonoBehaviour
{
    [Header("Network Settings")]
    public string serverUrl = "ws://localhost:8386";
    public string gameId = "test_game";
    public string authToken = "your_auth_token_here";
    
    [Header("Position Settings")]
    public float updateRate = 10f; // Server updates per second
    public float interpolationDelay = 0.1f; // 100ms delay for interpolation
    public float extrapolationTime = 0.05f; // 50ms extrapolation for smooth movement
    
    [Header("Interpolation Settings")]
    public bool useInterpolation = true;
    public bool useExtrapolation = true;
    public float maxExtrapolationTime = 0.2f; // Maximum extrapolation time
    
    private ClientWebSocket webSocket;
    private CancellationTokenSource cancellationTokenSource;
    private bool isConnected = false;
    private short mySlot = -1;
    
    // Position tracking
    private Vector3 myPosition;
    private Dictionary<short, PlayerData> otherPlayers = new Dictionary<short, PlayerData>();
    
    // Advanced interpolation
    private Dictionary<short, InterpolationData> interpolationData = new Dictionary<short, InterpolationData>();
    private Dictionary<short, ExtrapolationData> extrapolationData = new Dictionary<short, ExtrapolationData>();
    
    [System.Serializable]
    public class PlayerData
    {
        public Vector3 position;
        public Vector3 velocity;
        public long timestamp;
        public Vector3 targetPosition;
        public float interpolationTime;
        public bool isMoving;
    }
    
    [System.Serializable]
    public class InterpolationData
    {
        public Vector3 startPosition;
        public Vector3 endPosition;
        public Vector3 startVelocity;
        public Vector3 endVelocity;
        public float startTime;
        public float duration;
        public bool isActive;
    }
    
    [System.Serializable]
    public class ExtrapolationData
    {
        public Vector3 position;
        public Vector3 velocity;
        public float startTime;
        public bool isActive;
    }
    
    void Start()
    {
        ConnectToServer();
    }
    
    void Update()
    {
        // Update interpolation and extrapolation for other players
        if (useInterpolation)
            UpdateInterpolation();
        
        if (useExtrapolation)
            UpdateExtrapolation();
        
        // Send position updates
        if (isConnected && mySlot != -1)
        {
            SendPositionUpdate();
        }
    }
    
    void OnDestroy()
    {
        DisconnectFromServer();
    }
    
    async void ConnectToServer()
    {
        try
        {
            webSocket = new ClientWebSocket();
            cancellationTokenSource = new CancellationTokenSource();
            
            var uri = new Uri($"{serverUrl}?token={authToken}&gameId={gameId}");
            await webSocket.ConnectAsync(uri, cancellationTokenSource.Token);
            
            isConnected = true;
            Debug.Log("Connected to game server");
            
            // Start receiving messages
            _ = ReceiveMessages();
            
            // Start sending position updates
            InvokeRepeating(nameof(SendPositionUpdate), 0f, 1f / updateRate);
            
        }
        catch (Exception e)
        {
            Debug.LogError($"Failed to connect: {e.Message}");
        }
    }
    
    async void DisconnectFromServer()
    {
        if (webSocket != null)
        {
            isConnected = false;
            cancellationTokenSource?.Cancel();
            
            try
            {
                await webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Disconnecting", CancellationToken.None);
            }
            catch (Exception e)
            {
                Debug.LogError($"Error during disconnect: {e.Message}");
            }
        }
    }
    
    async Task ReceiveMessages()
    {
        var buffer = new byte[4096];
        
        while (isConnected && webSocket.State == WebSocketState.Open)
        {
            try
            {
                var result = await webSocket.ReceiveAsync(new ArraySegment<byte>(buffer), cancellationTokenSource.Token);
                
                if (result.MessageType == WebSocketMessageType.Binary)
                {
                    ProcessMessage(buffer, result.Count);
                }
                else if (result.MessageType == WebSocketMessageType.Close)
                {
                    Debug.Log("Server closed connection");
                    break;
                }
            }
            catch (Exception e)
            {
                Debug.LogError($"Error receiving message: {e.Message}");
                break;
            }
        }
        
        isConnected = false;
    }
    
    void ProcessMessage(byte[] data, int count)
    {
        try
        {
            using (var stream = new MemoryStream(data, 0, count))
            using (var reader = new BinaryReader(stream))
            {
                // Read TLV header
                short messageType = reader.ReadInt16();
                int messageLength = reader.ReadInt32();
                
                Debug.Log($"Received message type: {messageType}, length: {messageLength}");
                
                switch (messageType)
                {
                    case 2: // AUTHENTICATION_SEND
                        ProcessAuthenticationResponse(reader);
                        break;
                        
                    case 8: // POSITION_UPDATE_SEND
                        ProcessPositionUpdate(reader);
                        break;
                        
                    case 12: // INFO_PLAYERS_IN_ROOM_SEND
                        ProcessPlayerInfo(reader);
                        break;
                        
                    default:
                        Debug.LogWarning($"Unknown message type: {messageType}");
                        break;
                }
            }
        }
        catch (Exception e)
        {
            Debug.LogError($"Error processing message: {e.Message}");
        }
    }
    
    void ProcessAuthenticationResponse(BinaryReader reader)
    {
        int statusCode = reader.ReadInt32();
        int messageLength = reader.ReadInt32();
        string message = System.Text.Encoding.UTF8.GetString(reader.ReadBytes(messageLength));
        
        Debug.Log($"Authentication response: {statusCode} - {message}");
        
        if (statusCode == 8386) // SUCCESS
        {
            Debug.Log("Authentication successful!");
        }
        else
        {
            Debug.LogError("Authentication failed!");
        }
    }
    
    void ProcessPlayerInfo(BinaryReader reader)
    {
        short playerCount = reader.ReadInt16();
        Debug.Log($"Received player info for {playerCount} players");
        
        for (int i = 0; i < playerCount; i++)
        {
            int usernameLength = reader.ReadInt32();
            string username = System.Text.Encoding.UTF8.GetString(reader.ReadBytes(usernameLength));
            short slot = reader.ReadInt16();
            
            Debug.Log($"Player {username} assigned to slot {slot}");
            
            // If this is our username, remember our slot
            if (username == "YourUsername") // Replace with actual username logic
            {
                mySlot = slot;
                Debug.Log($"I am assigned to slot {mySlot}");
            }
        }
    }
    
    void ProcessPositionUpdate(BinaryReader reader)
    {
        short playerCount = reader.ReadInt16();
        
        for (int i = 0; i < playerCount; i++)
        {
            short slot = reader.ReadInt16();
            float x = reader.ReadSingle();
            float y = reader.ReadSingle();
            
            // Skip our own position updates
            if (slot == mySlot)
                continue;
            
            Vector3 newPosition = new Vector3(x, 0, y); // Assuming Y is up in Unity
            
            // Update player data
            if (!otherPlayers.ContainsKey(slot))
            {
                otherPlayers[slot] = new PlayerData();
            }
            
            var playerData = otherPlayers[slot];
            Vector3 oldPosition = playerData.position;
            
            // Calculate velocity based on position change
            float deltaTime = (Time.time - playerData.interpolationTime);
            if (deltaTime > 0)
            {
                playerData.velocity = (newPosition - oldPosition) / deltaTime;
            }
            
            playerData.targetPosition = newPosition;
            playerData.timestamp = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            playerData.interpolationTime = Time.time;
            playerData.isMoving = Vector3.Distance(oldPosition, newPosition) > 0.01f;
            
            // Setup interpolation
            if (useInterpolation)
            {
                SetupInterpolation(slot, oldPosition, newPosition, playerData.velocity);
            }
            
            // Setup extrapolation
            if (useExtrapolation && playerData.isMoving)
            {
                SetupExtrapolation(slot, newPosition, playerData.velocity);
            }
            
            Debug.Log($"Received position for slot {slot}: ({x}, {y})");
        }
        
        // Read timestamp
        long timestamp = reader.ReadInt64();
    }
    
    void SetupInterpolation(short slot, Vector3 startPos, Vector3 endPos, Vector3 velocity)
    {
        if (!interpolationData.ContainsKey(slot))
        {
            interpolationData[slot] = new InterpolationData();
        }
        
        var interp = interpolationData[slot];
        interp.startPosition = startPos;
        interp.endPosition = endPos;
        interp.startVelocity = velocity;
        interp.endVelocity = velocity; // Assume constant velocity for simplicity
        interp.startTime = Time.time + interpolationDelay; // Delay interpolation
        interp.duration = interpolationDelay;
        interp.isActive = true;
    }
    
    void SetupExtrapolation(short slot, Vector3 position, Vector3 velocity)
    {
        if (!extrapolationData.ContainsKey(slot))
        {
            extrapolationData[slot] = new ExtrapolationData();
        }
        
        var extrap = extrapolationData[slot];
        extrap.position = position;
        extrap.velocity = velocity;
        extrap.startTime = Time.time;
        extrap.isActive = true;
    }
    
    void UpdateInterpolation()
    {
        float currentTime = Time.time;
        
        foreach (var kvp in interpolationData)
        {
            short slot = kvp.Key;
            var interp = kvp.Value;
            
            if (!interp.isActive || !otherPlayers.ContainsKey(slot))
                continue;
            
            var playerData = otherPlayers[slot];
            
            // Wait for interpolation delay
            if (currentTime < interp.startTime)
                continue;
            
            float elapsed = currentTime - interp.startTime;
            float t = Mathf.Clamp01(elapsed / interp.duration);
            
            // Smooth interpolation with easing
            float smoothT = SmoothStep(t);
            
            // Interpolate position
            playerData.position = Vector3.Lerp(interp.startPosition, interp.endPosition, smoothT);
            
            // Interpolate velocity (optional)
            playerData.velocity = Vector3.Lerp(interp.startVelocity, interp.endVelocity, smoothT);
            
            // Remove interpolation data when complete
            if (t >= 1f)
            {
                interp.isActive = false;
            }
        }
    }
    
    void UpdateExtrapolation()
    {
        float currentTime = Time.time;
        
        foreach (var kvp in extrapolationData)
        {
            short slot = kvp.Key;
            var extrap = kvp.Value;
            
            if (!extrap.isActive || !otherPlayers.ContainsKey(slot))
                continue;
            
            var playerData = otherPlayers[slot];
            
            float elapsed = currentTime - extrap.startTime;
            
            // Limit extrapolation time
            if (elapsed > maxExtrapolationTime)
            {
                extrap.isActive = false;
                continue;
            }
            
            // Only extrapolate if no active interpolation
            if (!interpolationData.ContainsKey(slot) || !interpolationData[slot].isActive)
            {
                // Extrapolate position based on velocity
                Vector3 extrapolatedPos = extrap.position + extrap.velocity * elapsed;
                playerData.position = extrapolatedPos;
            }
        }
    }
    
    float SmoothStep(float t)
    {
        // Smooth step function for better interpolation
        return t * t * (3f - 2f * t);
    }
    
    void SendPositionUpdate()
    {
        if (!isConnected || mySlot == -1)
            return;
        
        try
        {
            // Get current position (you can modify this based on your character controller)
            Vector3 currentPos = transform.position;
            
            // Create position update message
            using (var stream = new MemoryStream())
            using (var writer = new BinaryWriter(stream))
            {
                // TLV header
                writer.Write((short)7); // POSITION_UPDATE_RECEIVE
                
                // Calculate message length (slot + x + y + timestamp)
                int messageLength = 2 + 4 + 4 + 8; // short + float + float + long
                writer.Write(messageLength);
                
                // Message content
                writer.Write(mySlot);
                writer.Write(currentPos.x);
                writer.Write(currentPos.z); // Use Z as Y for 2D game
                writer.Write(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());
                
                // Send message
                var messageBytes = stream.ToArray();
                webSocket.SendAsync(new ArraySegment<byte>(messageBytes), WebSocketMessageType.Binary, true, cancellationTokenSource.Token);
            }
        }
        catch (Exception e)
        {
            Debug.LogError($"Error sending position update: {e.Message}");
        }
    }
    
    // Public method to get other player positions (for rendering)
    public Dictionary<short, Vector3> GetOtherPlayerPositions()
    {
        var positions = new Dictionary<short, Vector3>();
        foreach (var kvp in otherPlayers)
        {
            positions[kvp.Key] = kvp.Value.position;
        }
        return positions;
    }
    
    // Public method to get other player velocities (for rendering)
    public Dictionary<short, Vector3> GetOtherPlayerVelocities()
    {
        var velocities = new Dictionary<short, Vector3>();
        foreach (var kvp in otherPlayers)
        {
            velocities[kvp.Key] = kvp.Value.velocity;
        }
        return velocities;
    }
    
    // Public method to get our slot
    public short GetMySlot()
    {
        return mySlot;
    }
    
    // Public method to check connection status
    public bool IsConnected()
    {
        return isConnected;
    }
    
    // Public method to get interpolation status for debugging
    public Dictionary<short, bool> GetInterpolationStatus()
    {
        var status = new Dictionary<short, bool>();
        foreach (var kvp in interpolationData)
        {
            status[kvp.Key] = kvp.Value.isActive;
        }
        return status;
    }
} 