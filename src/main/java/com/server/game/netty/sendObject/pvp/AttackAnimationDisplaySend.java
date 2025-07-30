package com.server.game.netty.sendObject.pvp;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.util.AnimationEnum;

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
    
    short targetSlot; // Champion slot that's being attacked (-1 if it's a target/NPC)
    String targetId; // Target/NPC ID that's being attacked (null if it's a champion)

    AnimationEnum animationEnum; // ATTACK_ANIMATION (=0) or SKILL_ANIMATION (=1)
    
    float attackSpeed; // Attack speed of the attacker

    long timestamp;

    @Override
    public SendMessageType getType() {
        return SendMessageType.ATTACK_ANIMATION_DISPLAY_SEND;
    }

    @Override
    public byte[] encode() {
        // byte[] attackerIdBytes = attackerId != null ? Util.stringToBytes(attackerId) : new byte[0];
        // byte[] targetIdBytes = targetId != null ? Util.stringToBytes(targetId) : new byte[0];
        // byte[] animationTypeBytes = Util.stringToBytes(animationType);
        // int attackerIdLength = attackerIdBytes.length;
        // int targetIdLength = targetIdBytes.length;
        // int animationTypeLength = animationTypeBytes.length;
        
        // System.out.println(">>> [AttackAnimationDisplaySend] Lengths - attackerId: " + attackerIdLength + ", targetId: " + targetIdLength + ", animationType: " + animationTypeLength);
        
        // int totalSize = Util.SHORT_SIZE + // attackerSlot
        //     Util.SHORT_SIZE + attackerIdLength + // attackerId length + attackerId
        //     Util.SHORT_SIZE + // targetSlot
        //     Util.SHORT_SIZE + targetIdLength + // targetId length + targetId
        //     Util.SHORT_SIZE + animationTypeLength + // animationType length + animationType
        //     8; // timestamp (long)
            
        // System.out.println(">>> [AttackAnimationDisplaySend] Total buffer size: " + totalSize);
        
        // ByteBuffer buf = Util.allocateByteBuffer(totalSize);
        
        // buf.putShort(attackerSlot);
        // buf.putShort((short) attackerIdLength);
        // if (attackerIdLength > 0) {
        //     buf.put(attackerIdBytes);
        // }
        // buf.putShort(targetSlot);
        // buf.putShort((short) targetIdLength);
        // if (targetIdLength > 0) {
        //     buf.put(targetIdBytes);
        // }
        // buf.putShort((short) animationTypeLength);
        // buf.put(animationTypeBytes);
        // buf.putLong(timestamp);
        
        // System.out.println(">>> [AttackAnimationDisplaySend] Encoding completed successfully");
        // return buf.array();


        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeShort(attackerSlot);

            if (attackerId == null) { attackerId = ""; }
            dos.writeUTF(attackerId); // this method already writes first 2 bytes for the length of the byte string
            
            dos.writeShort(targetSlot);

            if (targetId == null) { targetId = ""; }
            dos.writeUTF(targetId); // this method already writes first 2 bytes for the length of the byte string

            dos.writeShort(animationEnum.getAttackTypeId());

            dos.writeFloat(attackSpeed); 
            dos.writeLong(timestamp);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode AttackAnimationDisplaySend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
