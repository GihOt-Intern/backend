// package com.server.game.netty.pipelineComponent;


// import io.netty.buffer.ByteBuf;
// import io.netty.channel.ChannelHandlerContext;
// import io.netty.channel.SimpleChannelInboundHandler;
// import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;


// public class Reader extends  SimpleChannelInboundHandler<BinaryWebSocketFrame> {

//       @Override
//       public void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) throws Exception {     
//             System.out.println(">>> ======= Server received a BinaryWebSocketFrame ======");
//             ByteBuf buf = frame.content();
//             // ByteBuffer byteBuffer = buf.nioBuffer(); // Convert ByteBuf to ByteBuffer if needed
//             // Util.printHex(byteBuffer); // Print the hex representation of the ByteBuffer
//             System.out.println(">>> Server converted BinaryWebSocketFrame to ByteBuf and passed it to the next handler in the pipeline");
//             ctx.fireChannelRead(buf.retain()); // Pass the ByteBuf to the next handler in the pipeline
//       }

//       @Override
//       public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//             System.out.println(">>> Server Exception: " + cause.getMessage());
//             cause.printStackTrace();
//             ctx.close(); // Close the channel on exception
//       }
// }
