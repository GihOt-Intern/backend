# Room Notification Workflow Documentation

## Overview

This document describes the workflow for how clients connect to the server and receive real-time notifications about room changes using the unified Netty WebSocket system running on port 8386.

## Architecture

The system uses a single Netty WebSocket server (port 8386) for both:
1. **Gameplay communication** - Real-time game events, player movements, etc.
2. **Room notifications** - Room changes, player joins/leaves, host changes, etc.

## Message Types

### Client Message Types (Sent to Server)
- `AUTHENTICATION_RECEIVE` (Type: 1) - Initial authentication
- `ROOM_REGISTRATION_RECEIVE` (Type: 19) - Register for room notifications
- `MESSAGE_RECEIVE` (Type: 3) - General messages
- `CHOOSE_CHAMPION_RECEIVE` (Type: 5) - Champion selection
- `DISTANCE_RECEIVE` (Type: 17) - Distance calculations

### Server Message Types (Sent to Client)
- `AUTHENTICATION_SEND` (Type: 2) - Authentication response
- `ROOM_REGISTRATION_SEND` (Type: 20) - Room registration response
- `MESSAGE_SEND` (Type: 4) - General messages (including room notifications)
- `CHOOSE_CHAMPION_SEND` (Type: 6) - Champion selection broadcast
- `DISTANCE_SEND` (Type: 18) - Distance calculation response

## Connection Workflow

### 1. Initial Connection
```
Client → Server: WebSocket connection to ws://localhost:8386
```

### 2. Authentication
```
Client → Server: AUTHENTICATION_RECEIVE
{
  "token": "jwt_token_here",
  "gameId": "game_session_id"
}

Server → Client: AUTHENTICATION_SEND
{
  "statusCode": 8386, // SUCCESS
  "message": "Authenticated successfully!"
}
```

### 3. Room Registration (for notifications)
```
Client → Server: ROOM_REGISTRATION_RECEIVE
{
  "token": "jwt_token_here",
  "roomId": "room_123"
}

Server → Client: ROOM_REGISTRATION_SEND
{
  "statusCode": 8386, // SUCCESS
  "message": "Registered to room notifications successfully!"
}
```

## Room Notification Types

All room notifications are sent as `MESSAGE_SEND` with JSON payloads:

### 1. Player Joined Room
```json
{
  "type": "PLAYER_JOINED_ROOM",
  "roomId": "room_123",
  "playerId": "user_456",
  "playerName": "Alice",
  "message": "Alice joined the room"
}
```

### 2. Player Left Room
```json
{
  "type": "PLAYER_LEFT_ROOM",
  "roomId": "room_123",
  "playerId": "user_456",
  "playerName": "Alice",
  "message": "Alice left the room"
}
```

### 3. Host Changed
```json
{
  "type": "HOST_CHANGED",
  "roomId": "room_123",
  "newHostId": "user_789",
  "newHostName": "Bob",
  "message": "Bob is now the host"
}
```

### 4. Room Deleted
```json
{
  "type": "ROOM_DELETED",
  "roomId": "room_123",
  "message": "Room has been deleted"
}
```

### 5. Game Started
```json
{
  "type": "GAME_STARTED",
  "roomId": "room_123",
  "websocketUrl": "ws://localhost:8386/game/room_123",
  "message": "Game started! Connecting to game server..."
}
```

### 6. Match Found (Matchmaking)
```json
{
  "type": "MATCH_FOUND",
  "matchId": "match_456",
  "websocketUrl": "ws://localhost:8386/game/match_456",
  "message": "Match found! Connecting to game server..."
}
```

## Client Implementation Guide

### JavaScript/TypeScript Example

```typescript
class GameWebSocket {
    private ws: WebSocket;
    private messageHandlers: Map<string, Function> = new Map();

    constructor() {
        this.ws = new WebSocket('ws://localhost:8386');
        this.setupEventHandlers();
    }

    private setupEventHandlers() {
        this.ws.onopen = () => {
            console.log('Connected to game server');
            this.authenticate();
        };

        this.ws.onmessage = (event) => {
            this.handleMessage(event.data);
        };
    }

    private authenticate() {
        const authMessage = {
            type: 1, // AUTHENTICATION_RECEIVE
            token: localStorage.getItem('jwt_token'),
            gameId: 'game_session_' + Date.now()
        };
        this.sendMessage(authMessage);
    }

    public registerForRoomNotifications(roomId: string) {
        const roomRegMessage = {
            type: 19, // ROOM_REGISTRATION_RECEIVE
            token: localStorage.getItem('jwt_token'),
            roomId: roomId
        };
        this.sendMessage(roomRegMessage);
    }

    private handleMessage(data: any) {
        // Parse TLV message and extract JSON payload
        const message = JSON.parse(data);
        
        switch (message.type) {
            case 'PLAYER_JOINED_ROOM':
                this.handlePlayerJoined(message);
                break;
            case 'PLAYER_LEFT_ROOM':
                this.handlePlayerLeft(message);
                break;
            case 'HOST_CHANGED':
                this.handleHostChanged(message);
                break;
            case 'ROOM_DELETED':
                this.handleRoomDeleted(message);
                break;
            case 'GAME_STARTED':
                this.handleGameStarted(message);
                break;
            case 'MATCH_FOUND':
                this.handleMatchFound(message);
                break;
        }
    }

    private handlePlayerJoined(message: any) {
        console.log(`${message.playerName} joined the room`);
        // Update UI to show new player
        this.updateRoomUI();
    }

    private handlePlayerLeft(message: any) {
        console.log(`${message.playerName} left the room`);
        // Update UI to remove player
        this.updateRoomUI();
    }

    private handleHostChanged(message: any) {
        console.log(`${message.newHostName} is now the host`);
        // Update UI to show new host
        this.updateHostUI(message.newHostId);
    }

    private handleRoomDeleted(message: any) {
        console.log('Room has been deleted');
        // Navigate away from room or show room deleted message
        this.navigateToLobby();
    }

    private handleGameStarted(message: any) {
        console.log('Game is starting!');
        // Connect to game server or show game starting message
        this.connectToGameServer(message.websocketUrl);
    }

    private handleMatchFound(message: any) {
        console.log('Match found!');
        // Connect to game server for the match
        this.connectToGameServer(message.websocketUrl);
    }

    private sendMessage(message: any) {
        // Implement TLV encoding for sending messages
        const encodedMessage = this.encodeTLVMessage(message);
        this.ws.send(encodedMessage);
    }

    private encodeTLVMessage(message: any): ArrayBuffer {
        // TLV encoding implementation
        // Type (2 bytes) + Length (4 bytes) + Value (variable)
        const jsonString = JSON.stringify(message);
        const encoder = new TextEncoder();
        const valueBytes = encoder.encode(jsonString);
        
        const buffer = new ArrayBuffer(6 + valueBytes.length);
        const view = new DataView(buffer);
        
        view.setInt16(0, message.type, false); // Big endian
        view.setInt32(2, valueBytes.length, false);
        
        const uint8Array = new Uint8Array(buffer, 6);
        uint8Array.set(valueBytes);
        
        return buffer;
    }
}

// Usage
const gameSocket = new GameWebSocket();

// When user joins a room
gameSocket.registerForRoomNotifications('room_123');
```

## Server-Side Implementation

### Channel Registry
The server maintains three types of channel mappings:
1. **User Channels** - Maps user ID to their WebSocket channel
2. **Game Channels** - Maps game ID to set of player channels
3. **Room Channels** - Maps room ID to set of player channels

### Notification Service
The `NotificationService` handles sending notifications to:
- Individual players via `sendToPlayer()`
- Multiple players via `sendToPlayers()`
- All players in a room via `sendToRoom()`

### Room Service Integration
The `RoomService` automatically sends notifications when:
- Player joins a room
- Player leaves a room
- Host changes
- Room is deleted
- Game starts

## Error Handling

### Connection Errors
- If authentication fails, the server sends `AUTHENTICATION_SEND` with failure status
- If room registration fails, the server sends `ROOM_REGISTRATION_SEND` with failure status

### Disconnection Handling
- When a client disconnects, the server automatically unregisters their channels
- Room notifications will no longer be sent to disconnected clients

## Security Considerations

1. **JWT Authentication** - All messages require valid JWT tokens
2. **Room Access Control** - Only authenticated users can register for room notifications
3. **Channel Validation** - Server validates that users are actually in the rooms they're registering for

## Performance Considerations

1. **Connection Pooling** - Single WebSocket connection handles both game and room events
2. **Efficient Broadcasting** - Room notifications are sent only to registered room members
3. **Memory Management** - Channels are automatically cleaned up on disconnection

## Testing

The system includes comprehensive tests for:
- Room service operations
- Notification service functionality
- Channel registry operations
- Message handling

Run tests with: `mvn test` 