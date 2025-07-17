package com.server.game.netty.messageObject.sendObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import io.netty.channel.Channel;

import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.typeDefine.SendMessageType;
import com.server.game.resource.model.Champion;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChampionInitialStatsSend implements TLVEncodable {
    Integer defense;
    Integer attack;
    Float moveSpeed;
    Float attackSpeed;
    Float attackRange;
    Float resourceClaimingSpeed;
    Float skillCooldown;


    public ChampionInitialStatsSend(Champion champion) {
        this.defense = champion.getDefense();
        this.attack = champion.getAttack();
        this.moveSpeed = champion.getMoveSpeed();
        this.attackSpeed = champion.getAttackSpeed();
        this.attackRange = champion.getAttackRange();
        this.resourceClaimingSpeed = champion.getResourceClaimingSpeed();
        this.skillCooldown = champion.getCooldown();
    }


    @Override
    public SendMessageType getType() {
        return SendMessageType.CHAMPION_INITIAL_STATS_SEND;
    }


    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(defense);
            dos.writeInt(attack);
            dos.writeFloat(moveSpeed);
            dos.writeFloat(attackSpeed);
            dos.writeFloat(attackRange);
            dos.writeFloat(resourceClaimingSpeed);


            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode ChampionInitialHPsSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
    
}
