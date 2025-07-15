package com.server.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.game.dto.request.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FullGameFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token1, token2, roomId;

    @Test
    public void testFullGameFlow() throws Exception {
        // 1. Register player1
        String player1Id = register("player1", "password123");
        // 2. Register player2
        String player2Id = register("player2", "password456");

        // 3. Login player1
        token1 = login("player1", "password123");
        // 4. Login player2
        token2 = login("player2", "password456");

        // 5. player1 creates a room
        roomId = createRoom(token1);

        // 6. player2 joins the room
        joinRoom(token2, roomId);

        // 7. player1 starts the game
        startGame(token1, roomId);

        // 8. Both players connect to Netty and choose champions
        String nettyHost = "localhost";
        int nettyPort = 8386; // adjust if needed

        // player1 chooses champion 1
        try (NettyTestClient client1 = new NettyTestClient(nettyHost, nettyPort, token1, roomId, (short)1)) {
            client1.sendChooseChampion(1);
        }
        // player2 chooses champion 2
        try (NettyTestClient client2 = new NettyTestClient(nettyHost, nettyPort, token2, roomId, (short)2)) {
            client2.sendChooseChampion(2);
        }
    }

    private String register(String username, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<?,?> map = objectMapper.readValue(response, Map.class);
        Map<?,?> data = (Map<?,?>) map.get("data");
        return (String) data.get("id");
    }

    private String login(String username, String password) throws Exception {
        AuthenticationRequest req = new AuthenticationRequest();
        req.setUsername(username);
        req.setPassword(password);
        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<?,?> map = objectMapper.readValue(response, Map.class);
        Map<?,?> data = (Map<?,?>) map.get("data");
        return (String) data.get("token");
    }

    private String createRoom(String token) throws Exception {
        CreateRoomRequest req = new CreateRoomRequest();
        req.setName("Test Room");
        req.setMaxPlayers(2);
        String response = mockMvc.perform(post("/api/rooms")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Map<?,?> map = objectMapper.readValue(response, Map.class);
        Map<?,?> data = (Map<?,?>) map.get("data");
        return (String) data.get("id");
    }

    private void joinRoom(String token, String roomId) throws Exception {
        JoinRoomRequest req = new JoinRoomRequest();
        req.setPassword(null);
        mockMvc.perform(post("/api/rooms/" + roomId + "/join")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private void startGame(String token, String roomId) throws Exception {
        mockMvc.perform(post("/api/rooms/" + roomId + "/start")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // --- Netty Test Client (simplified for TLV handshake and champion selection) ---
    static class NettyTestClient implements AutoCloseable {
        private final Socket socket;
        private final DataOutputStream out;
        private final DataInputStream in;
        private final short slot;

        public NettyTestClient(String host, int port, String token, String gameId, short slot) throws IOException {
            this.socket = new Socket(host, port);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
            this.slot = slot;
            sendAuthentication(token, gameId);
        }

        private void sendAuthentication(String token, String gameId) throws IOException {
            byte[] tokenBytes = token.getBytes("UTF-8");
            byte[] gameIdBytes = gameId.getBytes("UTF-8");
            ByteBuffer buf = ByteBuffer.allocate(2 + 4 + 4 + tokenBytes.length + 4 + gameIdBytes.length);
            buf.putShort((short)1); // AUTHENTICATION_RECEIVE type
            buf.putInt(4 + tokenBytes.length + 4 + gameIdBytes.length);
            buf.putInt(tokenBytes.length);
            buf.put(tokenBytes);
            buf.putInt(gameIdBytes.length);
            buf.put(gameIdBytes);
            out.write(buf.array());
            out.flush();
            // Optionally, read server response here
        }

        public void sendChooseChampion(int championId) throws IOException {
            ByteBuffer buf = ByteBuffer.allocate(2 + 4 + 2 + 4);
            buf.putShort((short)5); // CHOOSE_CHAMPION_RECEIVE type
            buf.putInt(2 + 4);
            buf.putShort(slot);
            buf.putInt(championId);
            out.write(buf.array());
            out.flush();
            // Optionally, read server response here
        }

        @Override
        public void close() throws IOException {
            in.close();
            out.close();
            socket.close();
        }
    }
} 