package com.server.game.netty.sendObject.initialGameState;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.channel.Channel;

import com.server.game.model.game.Champion;
import com.server.game.model.game.GameState;
import com.server.game.model.game.SlotState;
import com.server.game.model.game.Tower;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.ChannelManager;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitialPositionsSend implements TLVEncodable {
    short mapId;
    Integer towerHP;
    Integer burgHP;
    List<InitialPositionData> championPositionsData;

    
    public InitialPositionsSend(GameState gameState) {
        this.mapId = gameState.getGameMapId();
        this.towerHP = gameState.getTowersInitHP();
        this.burgHP = gameState.getBurgsInitHP();
        Set<SlotState> slotStates = gameState.getSlotStates()
            .entrySet().stream()
            .map(entry -> entry.getValue())
            .collect(Collectors.toSet());
        this.championPositionsData = slotStates.stream()
            .map(slotState -> new InitialPositionData(gameState, slotState))
            .collect(Collectors.toList());
    }


    @Override
    public SendMessageType getType() {
        return SendMessageType.INITIAL_POSITIONS_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write map ID
            dos.writeShort(mapId);

            dos.writeInt(towerHP);
            dos.writeInt(burgHP);

            // Write number of champion positions
            dos.writeShort(championPositionsData.size());

            // Write each champion's position data
            for (InitialPositionData positionData : championPositionsData) {
                byte[] encodedData = positionData.encode();
                dos.write(encodedData);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode ChampionPositionsSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
    



    //************* InitialPositionData class *************//
    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InitialPositionData {
        short slot;
        String championStringId;
        ChampionEnum championEnum; 
        String username;
        Vector2 position;
        float rotate;
        int maxHP;

        Set<TowerData> towerDataList; 

        BurgData burgData; 

        public InitialPositionData(GameState gameState, SlotState slotState) {
            this.slot = slotState.getSlot();
            Champion champion = slotState.getChampion();
            this.championEnum = champion.getChampionEnum();
            this.championStringId = champion.getStringId();
            this.username = ChannelManager.getUsernameBySlot(gameState.getGameId(), this.slot);
            this.position = gameState.getGameMap().getSpawnPosition(this.slot);
            this.rotate = gameState.getGameMap().getInitialRotate(this.slot);
            this.maxHP = champion.getMaxHP();

            this.towerDataList = slotState.getTowers().stream()
                .map(tower -> new TowerData(gameState, slotState, tower))
                .collect(Collectors.toSet());

            this.burgData = new BurgData(gameState, slotState);
        }

        
        public byte[] encode() { // encode local data to byte array
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(slot);
                dos.writeUTF(championStringId); // This method already add first two bytes for length
                dos.writeShort(championEnum.getChampionId());
                dos.writeUTF(username); // This method already add first two bytes for length
                dos.writeFloat(position.x());
                dos.writeFloat(position.y());
                dos.writeFloat(rotate);
                dos.writeInt(maxHP);

                // Write tower data
                dos.writeShort((short) towerDataList.size());
                for (TowerData towerData : towerDataList) {
                    byte[] towerDataBytes = towerData.encode();
                    dos.write(towerDataBytes);
                }

                // Write burg data
                byte[] burgDataBytes = burgData.encode();
                dos.write(burgDataBytes);


                // System.out.println(">>> [Log in InitialPositionData.encode] Champion " + championStringId + " position: " + position + ", rotate: " + rotate);

                return baos.toByteArray();

            } catch (IOException e) {
                throw new RuntimeException("Cannot encoding " + this.getClass().getSimpleName(), e);
            }
        }


        @AllArgsConstructor
        public static class TowerData {
            String stringId;
            Vector2 position;
            float rotate;

            public TowerData(GameState gameState, SlotState slotState, Tower tower) {
                this.stringId = tower.getStringId();
                this.position = tower.getPosition();
                this.rotate = gameState.getGameMap()
                        .getTowerDB(slotState.getSlot(), tower.getDbId())
                        .getRotate();
            }

            public byte[] encode() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    dos.writeUTF(stringId);
                    dos.writeFloat(position.x());
                    dos.writeFloat(position.y());
                    dos.writeFloat(rotate);

                    return baos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Cannot encoding " + this.getClass().getSimpleName(), e);
                }
            }
        }

        @AllArgsConstructor
        public static class BurgData {
            String stringId;
            Vector2 position;
            float rotate;

            public BurgData(GameState gameState, SlotState slotState) {
                this.stringId = slotState.getBurg().getStringId();
                this.position = slotState.getBurg().getPosition();
                this.rotate = gameState.getGameMap()
                        .getBurgDB(slotState.getSlot())
                        .getRotate();
            }

            public byte[] encode() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);

                    dos.writeUTF(stringId);
                    dos.writeFloat((float) position.x());
                    dos.writeFloat((float) position.y());
                    dos.writeFloat(rotate);

                    return baos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Cannot encoding " + this.getClass().getSimpleName(), e);
                }
            }
        }
    }
    //************* End of InitialPositionData class *************//
}
