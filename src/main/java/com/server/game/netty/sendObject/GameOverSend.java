package com.server.game.netty.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GameOverSend implements TLVEncodable {
    short winnerSlot; // Slot of the winning player

    @Override
    public SendMessageType getType() {
        return SendMessageType.GAME_OVER;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            
            // Ghi slot người thắng
            dos.writeShort(winnerSlot);
            
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode GameOverSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
