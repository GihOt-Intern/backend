package com.server.game.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.server.game.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.messageObject.sendObject.PositionSend;
import com.server.game.service.MoveService.PositionData;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PositionBroadcastService {
    
    @Autowired
    private PositionService positionService;

    @Autowired
    private MoveService moveService;
    
    /**
     * Hủy đăng ký game - được gọi từ GameScheduler
     */
    public void unregisterGame(String gameId) {
        positionService.clearGamePositions(gameId);
        moveService.clearMoveTargets(gameId);
        ChannelManager.clearGameSlotMappings(gameId);
        log.info("Unregistered game from position broadcasting: {}", gameId);
    }
    
    /**
     * Broadcast vị trí cho một game cụ thể - được gọi từ GameScheduler
     */
    public void broadcastGamePositions(String gameId) {
        // Lấy pending positions (vị trí mới từ client)
        Map<Short, PositionData> pendingPositions = positionService.getPendingPositions(gameId);
        
        if (pendingPositions.isEmpty()) {
            return; // Không có vị trí mới để broadcast
        }
        
        // Lấy vị trí cũ từ main cache
        Map<Short, PositionData> oldPositions = positionService.getGamePositions(gameId);
        
        // Tạo danh sách player data chỉ cho những player đã thay đổi vị trí
        List<PositionSend.PlayerPositionData> playerDataList = new ArrayList<>();
        
        for (Map.Entry<Short, PositionData> entry : pendingPositions.entrySet()) {
            short playerSlot = entry.getKey();
            PositionData newPosition = entry.getValue();
            PositionData oldPosition = oldPositions.get(playerSlot);
            
            // Kiểm tra xem player này có thay đổi vị trí không
            if (hasPositionChanged(oldPosition, newPosition)) {
                playerDataList.add(new PositionSend.PlayerPositionData(
                    playerSlot,
                    newPosition.getPosition(),
                    newPosition.getSpeed()
                ));
                System.out.println(">>> Player slot " + playerSlot + 
                    " position changed to (" + newPosition.getPosition().x() + ", " + newPosition.getPosition().y() + ")"
                );
            }
        }
        
        // Nếu có player thay đổi vị trí, broadcast
        if (!playerDataList.isEmpty()) {
            // Tạo message để broadcast
            long currentTime = System.currentTimeMillis();
            PositionSend positionSend = new PositionSend(playerDataList, currentTime);

            // Lấy tất cả channel trong game
            Set<Channel> channels = ChannelManager.getChannelsByGameId(gameId);
            
            if (channels != null && !channels.isEmpty()) {
                // Broadcast cho tất cả player trong game, chỉ cần writeAndFlush cho channel đầu tiên
                Channel firstChannel = channels.iterator().next();
                firstChannel.writeAndFlush(positionSend);

                System.out.println(">>> Broadcasted positions for gameId: " + gameId + 
                    ", players updated: " + playerDataList.size()
                );
            } else {
                // Không có player nào trong game, không cần broadcast, xoá game khỏi activeGames
                unregisterGame(gameId);
            }
            
            // Cập nhật main cache với vị trí mới
            for (Map.Entry<Short, PositionData> entry : pendingPositions.entrySet()) {
                short playerSlot = entry.getKey();
                PositionData position = entry.getValue();
                positionService.updatePosition(gameId, playerSlot, 
                    position.getPosition(), position.getSpeed(), position.getTimestamp());
            }
            
            // Xóa pending positions sau khi đã broadcast
            positionService.clearPendingPositions(gameId);
            long elapsedTime = System.currentTimeMillis() - currentTime;
            log.info("Broadcast positions for game {} completed in {} ms", gameId, elapsedTime);
        }
    }
    
    /**
     * Kiểm tra xem player có thay đổi vị trí so với lần cập nhật trước không
     */
    private boolean hasPositionChanged(PositionData oldPosition, PositionData newPosition) {
        if (oldPosition == null) {
            return true; // Lần đầu, coi như thay đổi
        }
        
        
        // So sánh vị trí với ngưỡng thay đổi nhỏ
        float threshold = 0.01f; // Ngưỡng thay đổi tối thiểu
        
        
        Vector2 oldPos = oldPosition.getPosition();
        Vector2 newPos = newPosition.getPosition();
        Vector2 delta = newPos.subtract(oldPos);
        float dPos = delta.length();
        return dPos > threshold;
    }
} 