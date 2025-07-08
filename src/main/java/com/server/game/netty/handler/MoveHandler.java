package com.server.game.netty.handler;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.MessageMapping;
import com.server.game.netty.messageObject.receiveObject.DistanceReceive;
import com.server.game.netty.messageObject.sendObject.DistanceSend;


@Component
public class MoveHandler {

    // @MessageMapping(MoveRequest.class)
    // public void handleMove(MoveRequest msg) {
    //     // xử lý di chuyển
    // }

    @MessageMapping(DistanceReceive.class)
    public DistanceSend handleDistance(DistanceReceive receiveObject) {
        Double x1 = receiveObject.getX1();
        Double y1 = receiveObject.getY1();
        Double x2 = receiveObject.getX2();
        Double y2 = receiveObject.getY2();
        System.out.println("x1= " + x1 + ", y1= " + y1 + ", x2= " + x2 + ", y2= " + y2);
        Double d = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
        System.out.println("Distance: " + d);
        return new DistanceSend(d); // return a sendObject
    }

    // @MessageMapping(AnnotherReceive.class)
    // public AnnotherSend handleAnnother(AnnotherReceive receiveObject) {
    //     // Handle the another receive object
    // }
}
