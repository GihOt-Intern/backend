package com.server.game.netty.sendObject.troop;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
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
public class TroopDeathSend implements TLVEncodable{
    String troopId;
    short slot;

    @Override
    public SendMessageType getType() {
        return SendMessageType.TROOP_DEATH_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            byte[] troopIdBytes = troopId != null ? troopId.getBytes("UTF-8") : new byte[0];
            int troopIdLength = troopIdBytes.length;
            dos.writeInt(troopIdLength);
            if (troopIdLength > 0) {
                dos.write(troopIdBytes);
            }
            dos.writeShort(slot);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error encoding TroopDeathSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
