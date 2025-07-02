package com.server.game.handler;

import org.springframework.stereotype.Component;

import com.server.game.message.receive.DistanceReceive;
import com.server.game.message.send.DistanceSend;
import com.server.game.ws.messageMapping.MessageMapping;

// import com.server.game.ws.messageMapping.MessageMapping;

@Component
public class MoveHandler {

    // @MessageMapping(MoveRequest.class)
    // public void handleMove(MoveRequest msg) {
    //     // xử lý di chuyển
    // }

    @MessageMapping(DistanceReceive.class)
    public DistanceSend handleDistance(DistanceReceive request) {
        Double x1 = request.getX1();
        Double y1 = request.getY1();
        Double x2 = request.getX2();
        Double y2 = request.getY2();
        System.out.println("x1= " + x1 + ", y1= " + y1 + ", x2= " + x2 + ", y2= " + y2);
        Double d = Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2));
        System.out.println("Distance: " + d);
        return new DistanceSend(d);
    }
}
