// package com.server.game.service;

// import com.server.game.dto.request.CreateRoomRequest;
// import com.server.game.dto.response.RoomResponse;
// import com.server.game.exception.http.DataNotFoundException;
// import com.server.game.mapper.RoomMapper;
// import com.server.game.model.Room;
// import com.server.game.model.RoomStatus;
// import com.server.game.model.RoomVisibility;
// import com.server.game.model.User;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.quality.Strictness;
// import org.mockito.junit.jupiter.MockitoSettings;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContext;
// import org.springframework.security.core.context.SecurityContextHolder;

// import java.util.*;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.LENIENT)
// class RoomServiceTest {

//     @Mock
//     private RoomRedisService roomRedisService;

//     @Mock
//     private UserService userService;

//     @Mock
//     private RoomMapper roomMapper;

//     @Mock
//     private NotificationService notificationService;

//     @Mock
//     private SecurityContext securityContext;

//     @Mock
//     private Authentication authentication;

//     @InjectMocks
//     private RoomService roomService;

//     private User hostUser;
//     private User player1;
//     private User player2;
//     private Room testRoom;

//     @BeforeEach
//     void setUp() {
//         hostUser = new User("host-id", "Alice", "password");
//         player1 = new User("player1-id", "Bob", "password");
//         player2 = new User("player2-id", "Charlie", "password");

//         testRoom = new Room();
//         testRoom.setId("room-123");
//         testRoom.setName("Test Room");
//         testRoom.setHost(hostUser);
//         testRoom.setStatus(RoomStatus.WAITING);
//         testRoom.setMaxPlayers(4);
//         testRoom.setVisibility(RoomVisibility.PUBLIC);
//         testRoom.setPlayers(new HashSet<>(Arrays.asList(hostUser, player1, player2)));

//         lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
//         SecurityContextHolder.setContext(securityContext);
//     }

//     @Test
//     void testHostLeavesRoom() {
//         when(authentication.getName()).thenReturn("host-id");
//         when(userService.getUserInfo()).thenReturn(hostUser);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);

//         roomService.leaveRoom("room-123");

//         verify(roomRedisService).delete(testRoom);
//         verify(notificationService).notifyRoomDeleted("room-123");
//     }

//     @Test
//     void testRegularPlayerLeaves() {
//         when(authentication.getName()).thenReturn("player1-id");
//         when(userService.getUserInfo()).thenReturn(player1);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);
//         when(roomRedisService.save(any(Room.class))).thenReturn(testRoom);

//         roomService.leaveRoom("room-123");

//         verify(roomRedisService).save(any(Room.class));
//         verify(notificationService).notifyPlayerLeftRoom("room-123", "player1-id", "Bob");
//         verify(roomRedisService).save(argThat(room -> 
//             room.getHost().getUsername().equals("Alice") &&
//             room.getPlayers().size() == 2 &&
//             room.getPlayers().stream().anyMatch(p -> p.getUsername().equals("Alice")) &&
//             room.getPlayers().stream().anyMatch(p -> p.getUsername().equals("Charlie")) &&
//             room.getPlayers().stream().noneMatch(p -> p.getUsername().equals("Bob"))
//         ));
//     }

//     @Test
//     void testRoomDeletedWhenLastPlayerLeaves() {
//         // Only player1 in the room
//         testRoom.setPlayers(new HashSet<>(Arrays.asList(player1)));
//         testRoom.setHost(player1);
//         when(authentication.getName()).thenReturn("player1-id");
//         when(userService.getUserInfo()).thenReturn(player1);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);

//         roomService.leaveRoom("room-123");

//         verify(roomRedisService).delete(testRoom);
//         verify(notificationService).notifyRoomDeleted("room-123");
//     }

//     @Test
//     void testUserNotInRoomThrowsException() {
//         User nonExistentUser = new User("non-existent-id", "NonExistent", "password");
//         when(authentication.getName()).thenReturn("non-existent-id");
//         when(userService.getUserInfo()).thenReturn(nonExistentUser);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);

//         assertThrows(IllegalArgumentException.class, () -> {
//             roomService.leaveRoom("room-123");
//         });
//     }

//     @Test
//     void testRoomNotFoundThrowsException() {
//         when(roomRedisService.findById("non-existent-room")).thenThrow(new DataNotFoundException("Room not found"));

//         assertThrows(DataNotFoundException.class, () -> {
//             roomService.leaveRoom("non-existent-room");
//         });
//     }

//     @Test
//     void testCreateRoom() {
//         CreateRoomRequest request = new CreateRoomRequest();
//         request.setName("Test Room");
//         request.setMaxPlayers(4);
//         request.setVisibility(RoomVisibility.PUBLIC);
        
//         when(userService.getUserInfo()).thenReturn(hostUser);
//         when(roomRedisService.save(any(Room.class))).thenReturn(testRoom);
//         when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(new RoomResponse());

//         RoomResponse result = roomService.createRoom(request);

//         assertNotNull(result);
//         verify(roomRedisService).save(any(Room.class));
//     }

//     @Test
//     void testJoinRoom() {
//         // Remove player1 from the room before joining
//         testRoom.getPlayers().remove(player1);
//         when(userService.getUserInfo()).thenReturn(player1);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);
//         when(roomRedisService.save(any(Room.class))).thenReturn(testRoom);
//         when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(new RoomResponse());

//         RoomResponse result = roomService.joinRoom("room-123", null);

//         assertNotNull(result);
//         verify(roomRedisService).save(any(Room.class));
//         verify(notificationService).notifyPlayerJoinedRoom("room-123", "player1-id", "Bob");
//     }

//     @Test
//     void testStartGame() {
//         when(userService.getUserInfo()).thenReturn(hostUser);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);
//         when(roomRedisService.save(any(Room.class))).thenReturn(testRoom);
//         // when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(new RoomResponse());

//         // RoomResponse result = roomService.startGameSocket("room-123");

//         // assertNotNull(result);
//         verify(roomRedisService).save(any(Room.class));
//         verify(notificationService).notifyGameStarted(anyList(), eq("room-123"), anyString());
//     }

//     @Test
//     void testChangeHost() {
//         when(userService.getUserInfo()).thenReturn(hostUser);
//         when(userService.getUserByIdInternal("player1-id")).thenReturn(player1);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);
//         when(roomRedisService.save(any(Room.class))).thenReturn(testRoom);
//         when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(new RoomResponse());

//         RoomResponse result = roomService.changeHost("room-123", "player1-id");

//         assertNotNull(result);
//         verify(roomRedisService).save(any(Room.class));
//         verify(notificationService).notifyHostChanged("room-123", "player1-id", "Bob");
//     }

//     @Test
//     void testInviteUser() {
//         User invitedUser = new User("invited-id", "David", "password");
//         when(userService.getUserInfo()).thenReturn(hostUser);
//         when(userService.getUserByIdInternal("invited-id")).thenReturn(invitedUser);
//         when(roomRedisService.findById("room-123")).thenReturn(testRoom);
//         when(roomRedisService.save(any(Room.class))).thenReturn(testRoom);
//         when(roomMapper.toRoomResponse(any(Room.class))).thenReturn(new RoomResponse());

//         RoomResponse result = roomService.inviteUser("room-123", "invited-id");

//         assertNotNull(result);
//         verify(roomRedisService).save(any(Room.class));
//         verify(notificationService).notifyPlayerJoinedRoom("room-123", "invited-id", "David");
//     }
// } 