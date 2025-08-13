package com.server.game.netty.sendObject.initialGameState;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.channel.Channel;

import com.server.game.model.game.Champion;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.UnicastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;
import com.server.game.resource.model.TroopDB;

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
    Integer goldMineDamage;
    Float skillCooldown;
    Integer initGold;

    Set<TroopCostData> troopCosts;


    public ChampionInitialStatsSend(Champion champion, Integer initGold, Set<TroopDB> troopDBs) {
        this.defense = champion.getDefense();
        this.attack = champion.getDamage();
        this.moveSpeed = champion.getMoveSpeed();
        this.attackSpeed = champion.getAttackSpeed();
        this.attackRange = champion.getAttackRange();
        this.goldMineDamage = champion.getGoldMineDamage();
        this.skillCooldown = champion.getCooldown();
        this.initGold = initGold;

        this.troopCosts = troopDBs.stream()
            .map(troopDB -> new TroopCostData(
                troopDB.getId(),
                troopDB.getStats().getCost()))
            .collect(Collectors.toSet());
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
            dos.writeInt(goldMineDamage);
            dos.writeFloat(skillCooldown);
            dos.writeInt(initGold);

            
            dos.writeShort((short) troopCosts.size());
            for (TroopCostData troopCostData : troopCosts) {
                byte[] encodedData = troopCostData.encode();
                dos.write(encodedData);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode ChampionInitialStatsSend", e);
        }
    }

    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TroopCostData {
        short troopType;
        Integer cost;

        public byte[] encode() {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(troopType);
                dos.writeInt(cost);

                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Cannot encoding " + this.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
    
}
