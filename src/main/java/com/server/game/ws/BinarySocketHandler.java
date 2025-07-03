// package com.server.game.ws;

// import com.server.game.messageMapping.MessageDispatcher;
// import com.server.game.tlv.codec.TLVDecoder;
// import com.server.game.tlv.codec.TLVEncoder;
// import com.server.game.tlv.serializationable.TLVDecodable;
// import com.server.game.tlv.serializationable.TLVEncodable;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Component;
// import org.springframework.web.socket.BinaryMessage;
// import org.springframework.web.socket.WebSocketSession;
// import org.springframework.web.socket.handler.BinaryWebSocketHandler;


// import java.nio.ByteBuffer;
// import java.nio.ByteOrder;


// @Component
// public class BinarySocketHandler extends BinaryWebSocketHandler {

//     @Autowired
//     private MessageDispatcher dispatcher;

    
//     @Override
//     public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
//         System.out.println("Client connected: " + session.getId());
//     }

//     @Override
//     protected void handleBinaryMessage(@NonNull WebSocketSession session, @NonNull BinaryMessage message) throws Exception {
        
        
//         System.out.println("Received binary message: " + message);
        
//         ByteBuffer buffer = ByteBuffer.wrap(message.getPayload().array()).order(ByteOrder.BIG_ENDIAN);

//         short type = buffer.getShort();
//         int length = buffer.getInt();

//         System.out.println("Received TLV type: " + type + ", length: " + length);
//         System.out.println("Remaining in buffer: " + buffer.remaining());

//         byte[] value = new byte[length];
//         buffer.get(value);

//         TLVDecodable receiveObj = TLVDecoder.byte2Object(type, value);
//         // Object messageSend = dispatcher.dispatch(messageReceive);
//         TLVEncodable sendObj = (TLVEncodable) dispatcher.dispatch(receiveObj);

//         if (sendObj != null) {
//             byte[] response = TLVEncoder.object2Byte(sendObj);

//             StringBuilder sb = new StringBuilder();
//             for (byte b : response) {
//                 sb.append(String.format("0x%02X ", b)); // In mỗi byte dưới dạng hex 2 chữ số, có tiền tố 0x
//             }
//             System.out.println("Sending binary message: " + sb.toString().trim());

//             session.sendMessage(new BinaryMessage(response));
//         }
//     }
// }
