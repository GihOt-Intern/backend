package com.server.game.netty.sendObject.initialGameState;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.netty.channel.Channel;

import com.server.game.model.game.Champion;
import com.server.game.netty.pipelineComponent.outboundSendMessage.SendTarget;
import com.server.game.netty.pipelineComponent.outboundSendMessage.sendTargetType.AMatchBroadcastTarget;
import com.server.game.netty.tlv.interf4ce.TLVEncodable;
import com.server.game.netty.tlv.messageEnum.SendMessageType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;


@Deprecated
@SuppressWarnings("unused")
@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChampionInitialHPsSend implements TLVEncodable {
    List<ChampionInitialHPData> championInitialHPsData;

    public ChampionInitialHPsSend(Map<Short, Champion> slot2Champion) {
        this.championInitialHPsData = new ArrayList<>();
        for (Map.Entry<Short, Champion> entry : slot2Champion.entrySet()) {
            Short slot = entry.getKey();
            Champion champion = entry.getValue();
            ChampionInitialHPData initialHPData = new ChampionInitialHPData(slot, champion.getInitialHP());
            this.championInitialHPsData.add(initialHPData);
        }
    }


    @Override
    public SendMessageType getType() {
        return SendMessageType.CHAMPION_INITIAL_HPS_SEND;
    }

    @Override
    public byte[] encode() { // only return the [value] part of the TLV message
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            // Write number of champion initial HPs
            dos.writeShort(championInitialHPsData.size());

            // Write each champion's initial HP data
            for (ChampionInitialHPData hpData : championInitialHPsData) {
                byte[] encodedData = hpData.encode();
                dos.write(encodedData);
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Cannot encode ChampionInitialHPsSend", e);
        }
    }

    @Override
    public SendTarget getSendTarget(Channel channel) {
        return new AMatchBroadcastTarget(channel);
    }
    


    //************* ChampionInitialHPData class *************//
    @Data
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ChampionInitialHPData {
        short slot;
        int initialHP;

        
        public byte[] encode() { // encode local data to byte array
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);

                dos.writeShort(slot);
                dos.writeInt(initialHP);

                return baos.toByteArray();

            } catch (IOException e) {
                throw new RuntimeException("Cannot encoding ChampionInitialHPData", e);
            }
        }
    }
    //************* End of ChampionInitialHPData class *************//
}
