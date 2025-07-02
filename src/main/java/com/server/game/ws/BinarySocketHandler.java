package com.server.game.ws;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.server.game.util.TLVEncoder;
import com.server.game.util.TLVDecoder;
import com.server.game.ws.messageMapping.MessageDispatcher;

@Component
public class BinarySocketHandler extends BinaryWebSocketHandler {

    @Autowired
    private MessageDispatcher dispatcher;

    
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage message) throws Exception {
        
        
        System.out.println("Received binary message: " + message);
        
        ByteBuffer buffer = ByteBuffer.wrap(message.getPayload().array()).order(ByteOrder.BIG_ENDIAN);

        short type = buffer.getShort();
        int length = buffer.getInt();

        System.out.println("Received TLV type: " + type + ", length: " + length);
        System.out.println("Remaining in buffer: " + buffer.remaining());

        byte[] value = new byte[length];
        buffer.get(value);

        Object messageReceive = TLVDecoder.decode(type, value);
        // Object messageSend = dispatcher.dispatch(messageReceive);
        Object messageSend = dispatcher.dispatch(messageReceive);

        if (messageSend != null) {
            byte[] response = TLVEncoder.encode((short) 2, messageSend);

            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append(String.format("0x%02X ", b)); // In mỗi byte dưới dạng hex 2 chữ số, có tiền tố 0x
            }
            System.out.println("Sending binary message: " + sb.toString().trim());


            
            session.sendMessage(new BinaryMessage(response));




        }
    }
}
