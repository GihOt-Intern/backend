package com.server.game.netty.sendObject.pvp;

import java.nio.ByteBuffer;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
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
        byte[] targetIdBytes = targetId != null ? Util.stringToBytes(targetId) : new byte[0];
        byte[] animationTypeBytes = Util.stringToBytes(animationType);
        int attackerIdLength = attackerIdBytes.length;
        int targetIdLength = targetIdBytes.length;
        int animationTypeLength = animationTypeBytes.length;
        
        System.out.println(">>> [AttackAnimationDisplaySend] Lengths - attackerId: " + attackerIdLength + ", targetId: " + targetIdLength + ", animationType: " + animationTypeLength);
        
        int totalSize = Util.SHORT_SIZE + // attackerSlot
            Util.SHORT_SIZE + attackerIdLength + // attackerId length + attackerId
            Util.SHORT_SIZE + // targetSlot
            Util.SHORT_SIZE + targetIdLength + // targetId length + targetId
            Util.SHORT_SIZE + animationTypeLength + // animationType length + animationType
            8; // timestamp (long)
            
        System.out.println(">>> [AttackAnimationDisplaySend] Total buffer size: " + totalSize);
        
        ByteBuffer buf = Util.allocateByteBuffer(totalSize);
        
        buf.putShort(attackerSlot);
        buf.putShort((short) attackerIdLength);
        if (attackerIdLength > 0) {
            buf.put(attackerIdBytes);
        }
        buf.putShort(targetSlot);
        buf.putShort((short) targetIdLength);
        if (targetIdLength > 0) {
            buf.put(targetIdBytes);
        }
        buf.putShort((short) animationTypeLength);
        buf.put(animationTypeBytes);
        buf.putLong(timestamp);
        
        System.out.println(">>> [AttackAnimationDisplaySend] Encoding completed successfully");
        return buf.array();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
