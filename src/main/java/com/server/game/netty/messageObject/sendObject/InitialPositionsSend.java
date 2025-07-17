package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import io.netty.channel.Channel;

import com.server.game.map.component.Vector2;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;
import com.server.game.resource.model.SlotInfo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InitialPositionsSend implements TLVEncodable {
    short mapId;
    List<InitialPositionData> championPositionsData;

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
        Vector2 position;
        float rotate;

        public InitialPositionData(SlotInfo slotInfo) {
            this.slot = slotInfo.getSlot();
            this.position = slotInfo.getSpawn().getPosition();
            this.rotate = slotInfo.getSpawn().getRotate();
        }

        
        public byte[] encode() { // encode local data to byte array
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(slot);
                dos.writeFloat(position.x());
                dos.writeFloat(position.y());
                dos.writeFloat(rotate);

                return baos.toByteArray();

            } catch (IOException e) {
                throw new RuntimeException("Cannot encoding InitialPositionData", e);
            }
        }
    }
    //************* End of InitialPositionData class *************//
}
