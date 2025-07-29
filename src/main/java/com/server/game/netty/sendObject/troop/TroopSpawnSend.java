package com.server.game.netty.sendObject.troop;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TroopSpawnSend implements TLVEncodable {
    String troopId; // Unique identifier for the troop
    short troopType;
    short ownerSlot;
    float x, y;
    long timestamp;

    @Override
    public SendMessageType getType() {
        return SendMessageType.TROOP_SPAWN_SEND;
    }

    @Override
    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            byte[] troopIdBytes = troopId != null ? troopId.getBytes("UTF-8") : new byte[0];
            int troopIdLength = troopIdBytes.length;
            dos.writeInt(troopIdLength);
            if (troopIdLength > 0) {
                dos.write(troopIdBytes);
            }
            dos.writeShort(troopType);
            dos.writeShort(ownerSlot);
            dos.writeFloat(x);
            dos.writeFloat(y);
            dos.writeLong(timestamp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
