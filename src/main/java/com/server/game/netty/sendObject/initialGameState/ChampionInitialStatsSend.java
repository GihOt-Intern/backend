package com.server.game.netty.sendObject.initialGameState;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.netty.channel.Channel;

import com.server.game.model.game.Champion;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

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
    Integer initGold;


    public ChampionInitialStatsSend(Champion champion, Integer initGold) {
        this.defense = champion.getDefense();
        this.attack = champion.getDamage();
        this.moveSpeed = champion.getMoveSpeed();
        this.attackSpeed = champion.getAttackSpeed();
        this.attackRange = champion.getAttackRange();
        this.resourceClaimingSpeed = champion.getResourceClaimingSpeed();
        this.skillCooldown = champion.getCooldown();
        this.initGold = initGold;
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
            dos.writeFloat(skillCooldown);
            dos.writeInt(initGold);


            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode ChampionInitialStatsSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
    
}
