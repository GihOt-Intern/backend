package com.server.game.netty.sendObject.attack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.server.game.model.game.context.AttackContext;
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
public class AttackAnimationSend implements TLVEncodable {
    String attackerId; 
    String targetId; 

    float attackSpeed; // Attack speed of the attacker
    long timestamp;


    public AttackAnimationSend(AttackContext ctx) {
        this.attackerId = ctx.getAttacker().getStringId();
        this.targetId = ctx.getTarget().getStringId();
        this.attackSpeed = ctx.getAttacker().getAttackSpeed();
        this.timestamp = ctx.getTimestamp();
    }

    @Override
    public SendMessageType getType() {
        return SendMessageType.ATTACK_ANIMATION_SEND;
    }

    @Override
    public byte[] encode() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeUTF(attackerId);
            dos.writeUTF(targetId);


            dos.writeFloat(attackSpeed); 
            dos.writeLong(timestamp);

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode " + this.getClass().getSimpleName(), e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
}
