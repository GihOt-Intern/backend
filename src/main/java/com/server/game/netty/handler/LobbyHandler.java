package com.server.game.netty.handler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.ChannelRegistry;
import com.server.game.netty.messageObject.receiveObject.ChooseChampionReceive;
import com.server.game.netty.messageObject.sendObject.ChooseChampionSend;

import io.netty.channel.Channel;


@Component
public class LobbyHandler {


    @MessageMapping(ChooseChampionReceive.class)
    public ChooseChampionSend handleChooseChampion(ChooseChampionReceive receiveObject, Channel channel) {
        int championId = receiveObject.getChampionEnum().getChampionId();
        short slot = ChannelRegistry.getSlotByChannel(channel);
        System.out.println("Chosen Champion ID: " + championId);
        return new ChooseChampionSend(slot, championId);
    }
}
