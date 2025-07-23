package com.server.game.netty.receiveMessageHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.server.game.netty.ChannelManager;
import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.map.component.Vector2;
import com.server.game.netty.messageObject.receiveObject.TestGameStartAnnounceReceive;
import com.server.game.netty.messageObject.sendObject.ErrorSend;
import com.server.game.netty.messageObject.sendObject.TestGameStartResponseSend;
import com.server.game.netty.messageObject.sendObject.TestGameStartResponseSend.PlayerInfo;
import com.server.game.service.GameCoordinator;
import com.server.game.util.ChampionEnum;
import com.server.game.resource.service.ChampionService;
import com.server.game.resource.service.GameMapService;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestGameHandler {

    private final GameCoordinator gameCoordinator;
    private final GameMapService gameMapService; 
    private final ChampionService championService;
    private final Random random = new Random();
    
    // Test game uses map ID 2 (map_2.json)
    private static final short TEST_GAME_MAP_ID = 2;

    @MessageMapping(TestGameStartAnnounceReceive.class)
    public void handleTestGameStart(TestGameStartAnnounceReceive receiveObject, ChannelHandlerContext ctx) {
        String gameId = receiveObject.getGameId();
        Channel channel = ctx.channel();

        //Verify this is the test game
        if (!"Testgame123".equalsIgnoreCase(gameId)) {
            System.out.println(">>> Invalid test game Id: " + gameId);
            channel.writeAndFlush(new ErrorSend(
                "Invalid test game Id. Only 'Testgame123' is accepted"
            ));
            return;
        }

        //Register game for position broadcasting if not already
        gameCoordinator.registerGame(gameId);

        //Get all channels in this game
        Set<Channel> channels = ChannelManager.getChannelsByGameId(gameId);

        //Create player info list
        List<PlayerInfo> playerInfoList = new ArrayList<>();
        short slot = 0;

        for (Channel ch : channels) {
            //Assign a slot to each channel
            ChannelManager.setSlot2Channel(slot, ch);
        
            //Generate random champion ID (0-3)
            short championId = (short)random.nextInt(4);

            //Add to player info list
            playerInfoList.add(new PlayerInfo(slot, championId));

            // Get initial spawn position from map data
            Vector2 spawnPosition = gameMapService.getSpawnPosition(TEST_GAME_MAP_ID, slot);

            float spawnX = -70; // Default fallback
            float spawnY = 1.3f; // Default fallback
            
            if (spawnPosition != null) {
                spawnX = (float) spawnPosition.x();
                spawnY = (float) spawnPosition.y();
                System.out.println(">>> Using spawn position from map for slot " + slot + 
                    ": (" + spawnX + ", " + spawnY + ")");
            } else {
                System.out.println(">>> Warning: Could not get spawn position for slot " + slot + 
                    ", using default position (" + spawnX + ", " + spawnY + ")");
            }


            //Set initial position for the player based on map data
            gameCoordinator.updatePosition(
                gameId,
                slot,
                spawnPosition,
                championService.getChampionById(ChampionEnum.fromShort(championId)).getMoveSpeed(),
                System.currentTimeMillis()
            );

            //Increment slot
            slot++;

            System.out.println(">>> Assigned slot " + (slot - 1) + " with champion " + championId + 
                " to user " + ChannelManager.getUserIdByChannel(ch)
            );
        }

        //Create a response
        TestGameStartResponseSend response = new TestGameStartResponseSend(playerInfoList);

        //Send the response
        channel.writeAndFlush(response);

        System.out.println(">>> Test game started with " + slot + " players for game ID: " + gameId);
    }
}
