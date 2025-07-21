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
public class HealthUpdateSend implements TLVEncodable {
    short targetSlot; // Champion slot that received damage (-1 if it's a target/NPC)
    String targetId; // Target/NPC ID that received damage (null if it's a champion)
    int currentHealth;
    int maxHealth;
    int damage;
    long timestamp;

    // Constructor for champion health update
    public HealthUpdateSend(short targetSlot, int currentHealth, int maxHealth, int damage, long timestamp) {
        this.targetSlot = targetSlot;
        this.targetId = null;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.timestamp = timestamp;
    }

    // Constructor for target/NPC health update
    public HealthUpdateSend(String targetId, int currentHealth, int maxHealth, int damage, long timestamp) {
        this.targetSlot = -1;
        this.targetId = targetId;
        this.currentHealth = currentHealth;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.timestamp = timestamp;
    }

    @Override
    public SendMessageType getType() {
        return SendMessageType.HEALTH_UPDATE_SEND;
    }

    @Override
    public byte[] encode() {
        byte[] targetIdBytes = targetId != null ? Util.stringToBytes(targetId) : new byte[0];
        int targetIdLength = targetIdBytes.length;
        
        ByteBuffer buf = Util.allocateByteBuffer(
            Util.SHORT_SIZE + // targetSlot
            Util.SHORT_SIZE + targetIdLength + // targetId length + targetId
            Util.INT_SIZE + // currentHealth
            Util.INT_SIZE + // maxHealth
            Util.INT_SIZE + // damage
            8 // timestamp (long)
        );
        
        buf.putShort(targetSlot);
        buf.putShort((short) targetIdLength);
        if (targetIdLength > 0) {
            buf.put(targetIdBytes);
        }
        buf.putInt(currentHealth);
        buf.putInt(maxHealth);
        buf.putInt(damage);
        buf.putLong(timestamp);
        
        return buf.array();
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new UnicastTarget(channel);
    }
}
