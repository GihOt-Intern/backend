# Hướng dẫn giao tiếp Client với Server Netty TCP Socket

## Tổng quan

Server sử dụng **Netty TCP Socket** (không phải WebSocket) với định dạng message **TLV (Type-Length-Value)** để giao tiếp với client. Hệ thống hỗ trợ game MOBA với đồng bộ hóa vị trí real-time.

## 1. Kết nối và Xác thực

### 1.1 Kết nối TCP
```csharp
// Unity C# Example
using System.Net.Sockets;

TcpClient client = new TcpClient();
client.Connect("localhost", 8386); // Port từ application.properties
NetworkStream stream = client.GetStream();
```

### 1.2 Xác thực (Bắt buộc - Message đầu tiên)
Client **PHẢI** gửi message xác thực đầu tiên khi kết nối.

**Message Type:** `AUTHENTICATION_RECEIVE` (Type = 1)

**Format TLV:**
```
[Type: 2 bytes][Length: 4 bytes][Value: TokenLength + Token + GameIdLength + GameId]
```

**Cấu trúc Value:**
```csharp
// Gửi authentication
public void SendAuthentication(string token, string gameId)
{
    byte[] tokenBytes = Encoding.UTF8.GetBytes(token);
    byte[] gameIdBytes = Encoding.UTF8.GetBytes(gameId);
    
    // Tạo TLV message
    using (MemoryStream ms = new MemoryStream())
    using (BinaryWriter writer = new BinaryWriter(ms))
    {
        // Type = 1 (AUTHENTICATION_RECEIVE)
        writer.Write((short)1);
        
        // Length = 4 + tokenLength + 4 + gameIdLength
        int valueLength = 4 + tokenBytes.Length + 4 + gameIdBytes.Length;
        writer.Write(valueLength);
        
        // Value
        writer.Write(tokenBytes.Length);  // Token length
        writer.Write(tokenBytes);         // Token
        writer.Write(gameIdBytes.Length); // GameId length  
        writer.Write(gameIdBytes);        // GameId
        
        byte[] message = ms.ToArray();
        stream.Write(message, 0, message.Length);
    }
}
```

**Response từ Server:**
- `AUTHENTICATION_SEND` (Type = 2) với status SUCCESS/FAILURE
- Nếu thành công: Server sẽ xử lý các message tiếp theo
- Nếu thất bại: Kết nối sẽ bị đóng

## 2. Giao tiếp vị trí Player

### 2.1 Gửi vị trí từ Client

**Message Type:** `POSITION_UPDATE_RECEIVE` (Type = 7)

**Format TLV:**
```
[Type: 2 bytes][Length: 4 bytes][Value: Slot + X + Y + Timestamp]
```

**Cấu trúc Value:**
```csharp
public void SendPosition(short slot, float x, float y)
{
    using (MemoryStream ms = new MemoryStream())
    using (BinaryWriter writer = new BinaryWriter(ms))
    {
        // Type = 7 (POSITION_UPDATE_RECEIVE)
        writer.Write((short)7);
        
        // Length = 2 + 4 + 4 + 8 = 18 bytes
        writer.Write(18);
        
        // Value
        writer.Write(slot);           // 2 bytes
        writer.Write(x);              // 4 bytes (float)
        writer.Write(y);              // 4 bytes (float)
        writer.Write(DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()); // 8 bytes (long)
        
        byte[] message = ms.ToArray();
        stream.Write(message, 0, message.Length);
    }
}
```

### 2.2 Nhận vị trí từ Server

**Message Type:** `POSITION_UPDATE_SEND` (Type = 8)

**Format TLV:**
```
[Type: 2 bytes][Length: 4 bytes][Value: PlayerCount + Players + Timestamp]
```

**Cấu trúc Value:**
```csharp
public class PositionUpdate
{
    public List<PlayerPosition> Players { get; set; }
    public long Timestamp { get; set; }
}

public class PlayerPosition
{
    public short Slot { get; set; }
    public float X { get; set; }
    public float Y { get; set; }
}

// Đọc position update từ server
public PositionUpdate ReadPositionUpdate(NetworkStream stream)
{
    // Đọc TLV header
    byte[] typeBytes = new byte[2];
    stream.Read(typeBytes, 0, 2);
    short type = BitConverter.ToInt16(typeBytes, 0);
    
    if (type != 8) // POSITION_UPDATE_SEND
        throw new Exception("Unexpected message type: " + type);
    
    byte[] lengthBytes = new byte[4];
    stream.Read(lengthBytes, 0, 4);
    int length = BitConverter.ToInt32(lengthBytes, 0);
    
    // Đọc value
    byte[] valueBytes = new byte[length];
    stream.Read(valueBytes, 0, length);
    
    using (MemoryStream ms = new MemoryStream(valueBytes))
    using (BinaryReader reader = new BinaryReader(ms))
    {
        short playerCount = reader.ReadInt16();
        var players = new List<PlayerPosition>();
        
        for (int i = 0; i < playerCount; i++)
        {
            players.Add(new PlayerPosition
            {
                Slot = reader.ReadInt16(),
                X = reader.ReadSingle(),
                Y = reader.ReadSingle()
            });
        }
        
        long timestamp = reader.ReadInt64();
        
        return new PositionUpdate
        {
            Players = players,
            Timestamp = timestamp
        };
    }
}
```

## 3. Chiến lược gửi vị trí tối ưu

### 3.1 Khi nào gửi vị trí

**Khuyến nghị sử dụng Hybrid Approach:**

```csharp
public class PositionSender : MonoBehaviour
{
    [Header("Position Update Settings")]
    public float minUpdateInterval = 0.016f; // 60fps minimum
    public float maxUpdateInterval = 0.1f;   // 10fps maximum
    public float movementThreshold = 0.1f;   // Minimum movement to trigger update
    
    private Vector3 lastSentPosition;
    private float lastUpdateTime;
    private float currentUpdateInterval;
    
    void Update()
    {
        Vector3 currentPosition = transform.position;
        float timeSinceLastUpdate = Time.time - lastUpdateTime;
        
        // Kiểm tra có di chuyển đủ để gửi update không
        bool hasMoved = Vector3.Distance(currentPosition, lastSentPosition) > movementThreshold;
        
        // Adaptive rate: tăng tần suất khi di chuyển nhanh
        if (hasMoved)
        {
            float speed = Vector3.Distance(currentPosition, lastSentPosition) / timeSinceLastUpdate;
            currentUpdateInterval = Mathf.Lerp(minUpdateInterval, maxUpdateInterval, 
                Mathf.Clamp01(speed / 10f)); // Normalize speed
        }
        
        // Gửi update nếu đã đến lúc hoặc di chuyển đủ
        if (timeSinceLastUpdate >= currentUpdateInterval || hasMoved)
        {
            SendPositionUpdate(currentPosition);
            lastSentPosition = currentPosition;
            lastUpdateTime = Time.time;
        }
    }
}
```

### 3.2 Các chiến lược khác

1. **Fixed Rate (10fps):** Gửi cố định mỗi 100ms
2. **Movement-based:** Chỉ gửi khi di chuyển > threshold
3. **Input-based:** Gửi khi có input từ user
4. **Server-controlled:** Server yêu cầu client gửi

## 4. Xử lý vị trí nhận từ Server

### 4.1 Interpolation để đạt 60fps

Server gửi vị trí 10fps, client cần interpolate để đạt 60fps:

```csharp
public class PlayerInterpolator : MonoBehaviour
{
    [Header("Interpolation Settings")]
    public float interpolationDelay = 0.1f; // 100ms delay
    public float smoothingTime = 0.1f;
    
    private Queue<PositionSnapshot> positionHistory = new Queue<PositionSnapshot>();
    private Vector3 targetPosition;
    private Vector3 velocity;
    
    public void OnPositionUpdate(PositionUpdate update)
    {
        // Tìm vị trí của player này
        var myPosition = update.Players.Find(p => p.Slot == mySlot);
        if (myPosition != null)
        {
            // Thêm vào history với timestamp
            positionHistory.Enqueue(new PositionSnapshot
            {
                Position = new Vector3(myPosition.X, myPosition.Y, 0),
                Timestamp = update.Timestamp
            });
            
            // Giữ history trong khoảng delay
            while (positionHistory.Count > 0)
            {
                var oldest = positionHistory.Peek();
                if (Time.time - oldest.Timestamp > interpolationDelay)
                    positionHistory.Dequeue();
                else
                    break;
            }
        }
    }
    
    void Update()
    {
        if (positionHistory.Count >= 2)
        {
            // Interpolate giữa 2 position gần nhất
            var positions = positionHistory.ToArray();
            float alpha = (Time.time - positions[0].Timestamp) / 
                         (positions[1].Timestamp - positions[0].Timestamp);
            
            targetPosition = Vector3.Lerp(positions[0].Position, positions[1].Position, alpha);
        }
        
        // Smooth movement
        transform.position = Vector3.SmoothDamp(transform.position, targetPosition, 
            ref velocity, smoothingTime);
    }
}

public struct PositionSnapshot
{
    public Vector3 Position;
    public float Timestamp;
}
```

### 4.2 Extrapolation cho dự đoán

```csharp
public class PositionPredictor : MonoBehaviour
{
    private Vector3 lastPosition;
    private Vector3 velocity;
    private float lastUpdateTime;
    
    public void OnPositionUpdate(PositionUpdate update)
    {
        var myPosition = update.Players.Find(p => p.Slot == mySlot);
        if (myPosition != null)
        {
            Vector3 newPosition = new Vector3(myPosition.X, myPosition.Y, 0);
            
            // Tính velocity
            float deltaTime = Time.time - lastUpdateTime;
            if (deltaTime > 0)
            {
                velocity = (newPosition - lastPosition) / deltaTime;
            }
            
            lastPosition = newPosition;
            lastUpdateTime = Time.time;
        }
    }
    
    public Vector3 GetPredictedPosition()
    {
        float predictionTime = Time.time - lastUpdateTime;
        return lastPosition + velocity * predictionTime;
    }
}
```

## 5. Các Message khác

### 5.1 Choose Champion
```csharp
// Gửi
public void SendChooseChampion(int championId)
{
    // Type = 5, Value = championId (4 bytes)
}

// Nhận
// Type = 6, Value = slot (2 bytes) + championId (4 bytes)
```

### 5.2 Player Ready
```csharp
// Gửi
public void SendPlayerReady()
{
    // Type = 13, Value = empty
}

// Nhận
// Type = 14, Value = slot (2 bytes) + isAllReady (1 byte)
```

### 5.3 Info Players in Room
```csharp
// Nhận (khi game start)
// Type = 12, Value = playerCount + [usernameLength + username + slot] * playerCount
```

## 6. Best Practices

### 6.1 Error Handling
```csharp
public class NetworkManager : MonoBehaviour
{
    private TcpClient client;
    private NetworkStream stream;
    private bool isConnected = false;
    
    void Start()
    {
        ConnectToServer();
    }
    
    void ConnectToServer()
    {
        try
        {
            client = new TcpClient();
            client.Connect("localhost", 8386);
            stream = client.GetStream();
            isConnected = true;
            
            // Gửi authentication ngay
            SendAuthentication(token, gameId);
        }
        catch (Exception e)
        {
            Debug.LogError("Connection failed: " + e.Message);
            // Retry logic
            Invoke(nameof(ConnectToServer), 5f);
        }
    }
    
    void OnDestroy()
    {
        if (client != null)
        {
            client.Close();
        }
    }
}
```

### 6.2 Thread Safety
```csharp
public class ThreadSafeNetworkManager : MonoBehaviour
{
    private Queue<Action> mainThreadActions = new Queue<Action>();
    private readonly object lockObject = new object();
    
    // Gọi từ network thread
    public void QueueMainThreadAction(Action action)
    {
        lock (lockObject)
        {
            mainThreadActions.Enqueue(action);
        }
    }
    
    // Thực thi trong main thread
    void Update()
    {
        lock (lockObject)
        {
            while (mainThreadActions.Count > 0)
            {
                mainThreadActions.Dequeue()?.Invoke();
            }
        }
    }
}
```

### 6.3 Performance Optimization

1. **Batch Updates:** Gom nhóm nhiều update thành 1 message
2. **Compression:** Nén dữ liệu vị trí nếu cần
3. **Priority Queue:** Ưu tiên gửi vị trí quan trọng
4. **Connection Pooling:** Tái sử dụng connection

## 7. Debug và Monitoring

### 7.1 Logging
```csharp
public static class NetworkLogger
{
    public static void LogMessage(string message, bool isOutgoing = false)
    {
        string direction = isOutgoing ? "OUT" : "IN";
        Debug.Log($"[{direction}] {DateTime.Now:HH:mm:ss.fff} - {message}");
    }
}
```

### 7.2 Performance Metrics
```csharp
public class NetworkMetrics
{
    public int messagesSent;
    public int messagesReceived;
    public float averageLatency;
    public float packetLoss;
    
    public void LogMetrics()
    {
        Debug.Log($"Network Stats - Sent: {messagesSent}, Received: {messagesReceived}, " +
                  $"Latency: {averageLatency:F2}ms, Loss: {packetLoss:F2}%");
    }
}
```

## 8. Kết luận

- Server sử dụng **Netty TCP Socket** với **TLV format**
- **Authentication bắt buộc** là message đầu tiên
- **Position updates** được gửi 10fps từ server, client interpolate để 60fps
- Sử dụng **Hybrid approach** để tối ưu bandwidth và responsiveness
- **Slot-based** thay vì userId để giảm overhead
- Implement **error handling** và **retry logic** cho production

Hệ thống này đảm bảo game MOBA có độ trễ thấp và trải nghiệm mượt mà cho người chơi. 