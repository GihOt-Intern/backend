using UnityEngine;
using System.Collections.Generic;

public class CharacterController : MonoBehaviour
{
    [Header("Character Settings")]
    public short slot;
    public bool isLocalPlayer = false;
    
    [Header("Movement Settings")]
    public float moveSpeed = 5f;
    public float rotationSpeed = 10f;
    
    [Header("Visual Settings")]
    public GameObject characterModel;
    public GameObject nameTag;
    public TextMesh nameText;
    
    private UnityPositionSync positionSync;
    private Vector3 targetPosition;
    private Vector3 currentVelocity;
    private bool isMoving = false;
    
    // For local player
    private CharacterControllerInput inputController;
    
    // For remote players
    private Vector3 lastInterpolatedPosition;
    private float lastUpdateTime;
    
    void Start()
    {
        // Get position sync component
        positionSync = FindObjectOfType<UnityPositionSync>();
        if (positionSync == null)
        {
            Debug.LogError("UnityPositionSync not found in scene!");
            return;
        }
        
        // Setup for local player
        if (isLocalPlayer)
        {
            SetupLocalPlayer();
        }
        else
        {
            SetupRemotePlayer();
        }
        
        // Setup visual elements
        SetupVisuals();
    }
    
    void Update()
    {
        if (positionSync == null) return;
        
        if (isLocalPlayer)
        {
            UpdateLocalPlayer();
        }
        else
        {
            UpdateRemotePlayer();
        }
    }
    
    void SetupLocalPlayer()
    {
        // Add input controller for local player
        inputController = gameObject.AddComponent<CharacterControllerInput>();
        inputController.moveSpeed = moveSpeed;
        
        // Set slot when assigned
        StartCoroutine(WaitForSlotAssignment());
    }
    
    void SetupRemotePlayer()
    {
        // Remote players don't need input
        if (inputController != null)
        {
            Destroy(inputController);
        }
    }
    
    System.Collections.IEnumerator WaitForSlotAssignment()
    {
        while (positionSync.GetMySlot() == -1)
        {
            yield return null;
        }
        
        slot = positionSync.GetMySlot();
        Debug.Log($"Local player assigned to slot: {slot}");
    }
    
    void SetupVisuals()
    {
        // Setup name tag
        if (nameTag != null && nameText != null)
        {
            nameText.text = $"Player {slot}";
            
            // Different color for local player
            if (isLocalPlayer)
            {
                nameText.color = Color.green;
            }
            else
            {
                nameText.color = Color.white;
            }
        }
        
        // Setup character model
        if (characterModel != null)
        {
            // Different material for local player
            Renderer renderer = characterModel.GetComponent<Renderer>();
            if (renderer != null && isLocalPlayer)
            {
                Material localPlayerMaterial = new Material(renderer.material);
                localPlayerMaterial.color = Color.cyan;
                renderer.material = localPlayerMaterial;
            }
        }
    }
    
    void UpdateLocalPlayer()
    {
        // Local player movement is handled by CharacterControllerInput
        // Position sync will automatically send updates
        
        // Update visual feedback
        UpdateMovementVisuals();
    }
    
    void UpdateRemotePlayer()
    {
        // Get interpolated position from position sync
        Dictionary<short, Vector3> positions = positionSync.GetOtherPlayerPositions();
        Dictionary<short, Vector3> velocities = positionSync.GetOtherPlayerVelocities();
        
        if (positions.ContainsKey(slot))
        {
            Vector3 interpolatedPosition = positions[slot];
            Vector3 interpolatedVelocity = velocities.ContainsKey(slot) ? velocities[slot] : Vector3.zero;
            
            // Smooth movement to interpolated position
            float smoothTime = 0.1f; // Adjust for responsiveness
            transform.position = Vector3.SmoothDamp(transform.position, interpolatedPosition, ref currentVelocity, smoothTime);
            
            // Update movement state
            isMoving = interpolatedVelocity.magnitude > 0.1f;
            
            // Rotate character towards movement direction
            if (isMoving)
            {
                Vector3 moveDirection = interpolatedVelocity.normalized;
                if (moveDirection != Vector3.zero)
                {
                    Quaternion targetRotation = Quaternion.LookRotation(moveDirection);
                    transform.rotation = Quaternion.Slerp(transform.rotation, targetRotation, rotationSpeed * Time.deltaTime);
                }
            }
            
            // Update visual feedback
            UpdateMovementVisuals();
            
            lastInterpolatedPosition = interpolatedPosition;
            lastUpdateTime = Time.time;
        }
        else
        {
            // Player not found, maybe disconnected
            isMoving = false;
            UpdateMovementVisuals();
        }
    }
    
    void UpdateMovementVisuals()
    {
        // Update character model animations
        Animator animator = GetComponent<Animator>();
        if (animator != null)
        {
            animator.SetBool("IsMoving", isMoving);
            animator.SetFloat("MoveSpeed", currentVelocity.magnitude);
        }
        
        // Update particle effects
        ParticleSystem moveParticles = GetComponentInChildren<ParticleSystem>();
        if (moveParticles != null)
        {
            if (isMoving && !moveParticles.isPlaying)
            {
                moveParticles.Play();
            }
            else if (!isMoving && moveParticles.isPlaying)
            {
                moveParticles.Stop();
            }
        }
        
        // Update name tag position
        if (nameTag != null)
        {
            nameTag.transform.position = transform.position + Vector3.up * 2f;
            nameTag.transform.LookAt(Camera.main.transform);
        }
    }
    
    // Public methods for external access
    public short GetSlot()
    {
        return slot;
    }
    
    public bool IsMoving()
    {
        return isMoving;
    }
    
    public Vector3 GetCurrentVelocity()
    {
        return currentVelocity;
    }
    
    public bool IsLocalPlayer()
    {
        return isLocalPlayer;
    }
}

// Input controller for local player
public class CharacterControllerInput : MonoBehaviour
{
    public float moveSpeed = 5f;
    private CharacterController characterController;
    private UnityPositionSync positionSync;
    
    void Start()
    {
        characterController = GetComponent<CharacterController>();
        positionSync = FindObjectOfType<UnityPositionSync>();
    }
    
    void Update()
    {
        if (characterController == null || positionSync == null) return;
        
        // Handle input
        float horizontal = Input.GetAxis("Horizontal");
        float vertical = Input.GetAxis("Vertical");
        
        Vector3 movement = new Vector3(horizontal, 0, vertical).normalized;
        
        if (movement.magnitude > 0.1f)
        {
            // Move character
            transform.Translate(movement * moveSpeed * Time.deltaTime, Space.World);
            
            // Rotate character towards movement direction
            Quaternion targetRotation = Quaternion.LookRotation(movement);
            transform.rotation = Quaternion.Slerp(transform.rotation, targetRotation, 10f * Time.deltaTime);
        }
        
        // Handle sprint
        if (Input.GetKey(KeyCode.LeftShift))
        {
            moveSpeed = 8f;
        }
        else
        {
            moveSpeed = 5f;
        }
    }
} 