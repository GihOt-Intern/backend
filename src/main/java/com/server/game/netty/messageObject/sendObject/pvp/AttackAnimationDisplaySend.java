package com.server.game.netty.messageObject.sendObject.pvp;

import java.nio.ByteBuffer;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;
import com.server.game.util.Util;

import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttackAnimationDisplaySend implements TLVEncodable {
    short attackerSlot; // Champion slot that's attacking (-1 if it's a target/NPC)
    String attackerId; // Target/NPC ID that's attacking (null if it's a champion)
    short targetSlot;
    String targetId;
    String animationType; // Type of attack animation (e.g., "sword_slash", "magic_missile", "arrow_shot")
    long timestamp;

    @Override
    public SendMessageType getType() {
        return SendMessageType.ATTACK_ANIMATION_DISPLAY_SEND;
    }

    @Override
    public byte[] encode() {
        byte[] attackerIdBytes = attackerId != null ? Util.stringToBytes(attackerId) : new byte[0];
        byte[] animationTypeBytes = Util.stringToBytes(animationType);
        int attackerIdLength = attackerIdBytes.length;
        int animationTypeLength = animationTypeBytes.length;
        
        ByteBuffer buf = Util.allocateByteBuffer(
            Util.SHORT_SIZE + // attackerSlot
            Util.SHORT_SIZE + attackerIdLength + // attackerId length + attackerId
            Util.SHORT_SIZE + animationTypeLength + // animationType length + animationType
            8 // timestamp (long)
        );
        
        buf.putShort(attackerSlot);
        buf.putShort((short) attackerIdLength);
        if (attackerIdLength > 0) {
            buf.put(attackerIdBytes);
        }
        buf.putShort((short) animationTypeLength);
        buf.put(animationTypeBytes);
        buf.putLong(timestamp);
        
        return buf.array();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
