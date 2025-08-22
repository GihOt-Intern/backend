# Room Notifications System

## Overview
The Room Notifications System broadcasts real-time updates to all players in a room when certain events occur. This document explains how these notifications work and how to handle them in your client application.

## Types of Notifications

### Player Joined Room
Sent when a new player joins the room.

```json
{
  "type": "PLAYER_JOINED_ROOM",
  "roomId": "room123",
  "playerId": "user456",
  "playerName": "PlayerName",
  "message": "PlayerName joined the room"
}
```

### Player Left Room
Sent when a player leaves the room.

```json
{
  "type": "PLAYER_LEFT_ROOM",
  "roomId": "room123",
  "playerId": "user456",
  "playerName": "PlayerName",
  "message": "PlayerName left the room"
}
```

### Host Changed
Sent when the host role is transferred to another player.

```json
{
  "type": "HOST_CHANGED",
  "roomId": "room123",
  "newHostId": "user789",
  "newHostName": "NewHostName",
  "message": "NewHostName is now the host"
}
```

### Room Deleted
Sent when a room is deleted, typically when the host leaves or all players have left.

```json
{
  "type": "ROOM_DELETED",
  "roomId": "room123",
  "message": "Room has been deleted"
}
```

### Game Started
Sent when the host starts the game.

```json
{
  "type": "GAME_STARTED",
  "roomId": "room123",
  "websocketUrl": "ws://localhost:8386",
  "message": "Game started! Connecting to game server..."
}
```

## Handling Notifications

When your client receives a notification, you should:

1. Parse the JSON message to determine the notification type
2. Update your UI to reflect the changes
3. Take appropriate actions based on the notification

### Example (React.js):

```javascript
function handleRoomNotification(notification) {
  switch (notification.type) {
    case "PLAYER_JOINED_ROOM":
      console.log(`${notification.playerName} joined the room`);
      // Add player to the list
      setPlayers(prevPlayers => [...prevPlayers, {
        id: notification.playerId,
        name: notification.playerName
      }]);
      break;
      
    case "PLAYER_LEFT_ROOM":
      console.log(`${notification.playerName} left the room`);
      // Remove player from the list
      setPlayers(prevPlayers => 
        prevPlayers.filter(player => player.id !== notification.playerId)
      );
      break;
      
    case "HOST_CHANGED":
      console.log(`${notification.newHostName} is now the host`);
      // Update the host information
      setHost({
        id: notification.newHostId,
        name: notification.newHostName
      });
      break;
      
    case "ROOM_DELETED":
      console.log("Room has been deleted");
      // Navigate back to room selection screen
      navigate("/rooms");
      break;
      
    case "GAME_STARTED":
      console.log("Game is starting!");
      // Connect to the game WebSocket
      connectToGameServer(notification.websocketUrl);
      break;
      
    default:
      console.log("Unknown notification type:", notification.type);
  }
}

// In your WebSocket message handler
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  if (data.type && data.type.includes("_ROOM") || data.type === "GAME_STARTED") {
    handleRoomNotification(data);
  }
};
```

## Implementation Notes

1. All notifications are automatically sent to all connected players in the room
2. The server handles player connections tracking through the `ChannelManager`
3. Notifications are sent as JSON over the WebSocket connection
4. Your client should maintain a single WebSocket connection while in a room
