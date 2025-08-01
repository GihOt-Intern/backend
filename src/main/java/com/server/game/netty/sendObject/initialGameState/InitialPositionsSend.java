package com.server.game.netty.sendObject.initialGameState;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import io.netty.channel.Channel;

import com.server.game.model.game.GameState;
import com.server.game.model.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.resource.model.SlotInfo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitialPositionsSend implements TLVEncodable {
    short mapId;
    List<InitialPositionData> championPositionsData;

    
    public InitialPositionsSend(GameState gameState) {
        this.mapId = gameState.getGameMapId();
        List<SlotInfo> slotInfos = gameState.getSlotInfos();
        this.championPositionsData = slotInfos.stream()
            .map(slotInfo -> new InitialPositionData(slotInfo, 
                gameState.getChampionBySlot(slotInfo.getSlot()).getStringId()))
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
        Vector2 position;
        float rotate;

        public InitialPositionData(SlotInfo slotInfo, String championStringId) {
            this.slot = slotInfo.getSlot();
            this.position = slotInfo.getSpawn().getPosition();
            this.rotate = slotInfo.getSpawn().getRotate();
            this.championStringId = championStringId;
        }

        
        public byte[] encode() { // encode local data to byte array
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(slot);
                dos.writeUTF(championStringId); // This method already add first two bytes for length
                dos.writeFloat((float) position.x());
                dos.writeFloat((float) position.y());
                dos.writeFloat(rotate);

                System.out.println(">>> [Log in InitialPositionData.encode] Champion " + championStringId + " position: " + position + ", rotate: " + rotate);

                return baos.toByteArray();

            } catch (IOException e) {
                throw new RuntimeException("Cannot encoding InitialPositionData", e);
            }
        }
    }
    //************* End of InitialPositionData class *************//
}
