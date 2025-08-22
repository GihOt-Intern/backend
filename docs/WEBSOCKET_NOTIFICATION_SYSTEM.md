# WebSocket Notification System

## Overview

The WebSocket notification system provides real-time communication between the game server and connected clients. It automatically notifies players when matches are found (matchmaking) or when games start (room-based), sending them the WebSocket URL to connect to the game server.

## How It Works

### 1. Player Connection
Players connect to the main WebSocket server at `ws://localhost:8386/ws?token=<jwt_token>` for real-time notifications.

### 2. Notification Types

#### Match Found Notification (Matchmaking)
When enough players are found in the matchmaking queue, all matched players receive a notification:

```json
{
  "type": "MATCH_FOUND",
  "matchId": "match-uuid-123",
  "websocketUrl": "ws://localhost:8386/game/match-uuid-123",
  "message": "Match found! Connecting to game server..."
}
```

#### Game Started Notification (Room-based)
When a host starts a game in a room, all players in that room receive a notification:

```json
{
  "type": "GAME_STARTED",
  "roomId": "room-uuid-456",
  "websocketUrl": "ws://localhost:8386/game/room-uuid-456",
  "message": "Game started! Connecting to game server..."
}
```

## Implementation Details

### NotificationService
The `NotificationService` handles sending WebSocket messages to players:

```java
@Service
public class NotificationService {
    // Send match found notification to all players in a match
    public void notifyMatchFound(List<String> playerIds, String matchId, String websocketUrl)
    
    // Send game started notification to all players in a room
    public void notifyGameStarted(List<String> playerIds, String roomId, String websocketUrl)
    
    // Send a message to a single player
    public void sendToPlayer(String playerId, String message)
}
```

### Integration Points

#### MatchmakingService
- **Trigger**: When `matchPlayers()` scheduled task finds enough players
- **Action**: Calls `notificationService.notifyMatchFound()`
- **URL Format**: `ws://localhost:8386/game/{matchId}`

#### RoomService
- **Trigger**: When host calls `startGame()` endpoint
- **Action**: Calls `notificationService.notifyGameStarted()`
- **URL Format**: `ws://localhost:8386/game/{roomId}`

### UserChannelRegistry
The `UserChannelRegistry` maintains a mapping between user IDs and their WebSocket channels:

```java
public class UserChannelRegistry {
    // Register a user's WebSocket channel
    public static void register(String userId, Channel channel)
    
    // Get a user's WebSocket channel
    public static Channel getChannel(String userId)
    
    // Unregister a user's WebSocket channel
    public static void unregister(Channel channel)
}
```

## Client Integration

### 1. Unity (C#) Client Integration

You can use a WebSocket library such as [NativeWebSocket](https://github.com/endel/NativeWebSocket) or [BestHTTP/2](https://assetstore.unity.com/packages/tools/network/best-http-2-155981) for Unity. Below is an example using [NativeWebSocket](https://github.com/endel/NativeWebSocket), which is free and works on most Unity platforms.

#### **Install NativeWebSocket**
- Download and import the NativeWebSocket package into your Unity project.

#### **Connect to Notification WebSocket**
```csharp
using NativeWebSocket;
using UnityEngine;
using System;

public class NotificationClient : MonoBehaviour
{
    private WebSocket websocket;
    public string jwtToken; // Set this with your JWT token

    async void Start()
    {
        string notificationUrl = $"ws://localhost:8386/ws?token={jwtToken}";
        websocket = new WebSocket(notificationUrl);

        websocket.OnOpen += () =>
        {
            Debug.Log("Connection open!");
        };

        websocket.OnError += (e) =>
        {
            Debug.LogError($"WebSocket Error: {e}");
        };

        websocket.OnClose += (e) =>
        {
            Debug.Log("Connection closed!");
        };

        websocket.OnMessage += (bytes) =>
        {
            string message = System.Text.Encoding.UTF8.GetString(bytes);
            Debug.Log($"Notification: {message}");
            HandleNotification(message);
        };

        await websocket.Connect();
    }

    private void HandleNotification(string message)
    {
        // Parse the JSON and handle MATCH_FOUND or GAME_STARTED
        var notification = JsonUtility.FromJson<NotificationMessage>(message);
        if (notification.type == "MATCH_FOUND" || notification.type == "GAME_STARTED")
        {
            ConnectToGameServer(notification.websocketUrl);
        }
    }

    private async void ConnectToGameServer(string websocketUrl)
    {
        var gameWebSocket = new WebSocket(websocketUrl);
        gameWebSocket.OnOpen += () => Debug.Log("Connected to game server!");
        gameWebSocket.OnMessage += (bytes) => Debug.Log($"Game message: {System.Text.Encoding.UTF8.GetString(bytes)}");
        await gameWebSocket.Connect();
    }

    [Serializable]
    public class NotificationMessage
    {
        public string type;
        public string matchId;
        public string roomId;
        public string websocketUrl;
        public string message;
    }

    private async void OnApplicationQuit()
    {
        if (websocket != null)
        {
            await websocket.Close();
        }
    }
}
```

**Notes:**
- Replace `localhost` with your server's IP or domain in production.
- Always use `wss://` (secure WebSocket) in production.
- Make sure your JWT token is valid and not expired.
- You can expand the `NotificationMessage` class to match your server's notification structure.

---

## WebSocket URL Patterns

### Matchmaking
- **Notification URL**: `ws://localhost:8386/ws?token=<jwt_token>`
- **Game Server URL**: `ws://localhost:8386/game/match-{uuid}`

### Room-based
- **Notification URL**: `ws://localhost:8386/ws?token=<jwt_token>`
- **Game Server URL**: `ws://localhost:8386/game/{roomId}`

## Error Handling

### Connection Issues
- If a player is not connected via WebSocket, a warning is logged
- The notification is not sent, but the match/game still proceeds
- Players can still get the WebSocket URL via REST API endpoints

### Message Delivery
- Messages are sent asynchronously
- Failed message deliveries are logged but don't block the process
- Each player's connection status is checked before sending

## Configuration

### WebSocket Server
- **Port**: 8386 (configurable)
- **Path**: `/ws` for notifications, `/game/{id}` for game servers
- **Authentication**: JWT token required

### Notification Format
- **Encoding**: UTF-8 JSON
- **Structure**: Type, ID, WebSocket URL, and message
- **Delivery**: Broadcast to all relevant players

## Security Considerations

### Authentication
- All WebSocket connections require valid JWT tokens
- Tokens are validated on connection establishment
- Invalid tokens result in immediate connection closure

### Authorization
- Players can only receive notifications for matches/rooms they're part of
- WebSocket URLs are specific to each match/room
- No cross-contamination between different games

## Monitoring and Logging

### Log Levels
- **INFO**: Successful notifications sent
- **DEBUG**: Individual message delivery status
- **WARN**: Players not connected via WebSocket
- **ERROR**: Failed message delivery attempts

### Metrics
- Number of notifications sent per match/room
- Success/failure rates for message delivery
- Connection status of players

## Example Flow

### Matchmaking Flow
1. Player joins matchmaking queue via REST API
2. Player connects to notification WebSocket
3. When match is found, all players receive `MATCH_FOUND` notification
4. Players connect to game server using provided WebSocket URL
5. Game begins

### Room-based Flow
1. Players join room via REST API
2. Players connect to notification WebSocket
3. Host starts game via REST API
4. All players receive `GAME_STARTED` notification
5. Players connect to game server using provided WebSocket URL
6. Game begins

## Troubleshooting

### Common Issues

#### Player Not Receiving Notifications
- Check if player is connected to notification WebSocket
- Verify JWT token is valid
- Check server logs for connection errors

#### WebSocket URL Issues
- Ensure game server is running on correct port
- Verify URL format matches expected pattern
- Check if match/room ID is valid

#### Connection Failures
- Verify network connectivity
- Check firewall settings
- Ensure WebSocket server is running

### Debug Steps
1. Check server logs for notification attempts
2. Verify player WebSocket connection status
3. Test WebSocket URL connectivity
4. Validate JWT token authentication 