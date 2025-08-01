package com.server.game.netty.sender;


import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.receiveObject.DistanceReceive;
import com.server.game.netty.sendObject.DistanceSend;


@Component
public class MoveSender {

    // @MessageMapping(MoveRequest.class)
    // public void handleMove(MoveRequest msg) {
    //     // xử lý di chuyển
    // }

    @MessageMapping(DistanceReceive.class)
    public DistanceSend handleDistance(DistanceReceive receiveObject) {
        float x1 = receiveObject.getX1();
        float y1 = receiveObject.getY1();
        float x2 = receiveObject.getX2();
        float y2 = receiveObject.getY2();
        System.out.println("x1= " + x1 + ", y1= " + y1 + ", x2= " + x2 + ", y2= " + y2);
        float dx = x2 - x1;
        float dy = y2 - y1;
        float d = (float) Math.sqrt(dx*dx + dy*dy);
        System.out.println("Distance: " + d);
        return new DistanceSend(d); // return a sendObject
    }

    // @MessageMapping(AnnotherReceive.class)
    // public AnnotherSend handleAnnother(AnnotherReceive receiveObject) {
    //     // Handle the another receive object
    // }
}
