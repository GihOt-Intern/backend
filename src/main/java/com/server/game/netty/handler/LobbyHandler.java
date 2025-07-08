package com.server.game.netty.handler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.messageObject.receiveObject.ChooseChampionReceive;
import com.server.game.netty.messageObject.sendObject.ChooseChampionSend;


@Component
public class LobbyHandler {


    @MessageMapping(ChooseChampionReceive.class)
    public ChooseChampionSend handleChooseChampion(ChooseChampionReceive receiveObject, String userId) {
        int championId = receiveObject.getChampionEnum().getChampionId();
        System.out.println("Chosen Champion ID: " + championId);
        return new ChooseChampionSend(userId, championId);
    }
}
