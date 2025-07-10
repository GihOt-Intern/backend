package com.server.game.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class Util {

    public static final int SHORT_SIZE = 2;
    public static final int INT_SIZE = 4;
    public static final int DOUBLE_SIZE = 8;
    // public static final int CHAR_UTF32_SIZE = 4;

    private static int nettyServerPort;
    private static String nettyServerAddress;

    private static final String STRING_ENCODING = "UTF-8";


    // public static int STRING_BYTE_SIZE(int stringLength) {
    //     // number of characters * 4 bytes (UTF-32-BE uses 4 bytes per character)
    //     return stringLength * CHAR_UTF32_SIZE;
    // }

    public static void printHex(ByteBuffer buffer, boolean isFromBeginning) {
        ByteBuffer readOnly = buffer.asReadOnlyBuffer();
        if (isFromBeginning) {
            readOnly.position(0);
        }
        while (readOnly.hasRemaining()) {
            byte b = readOnly.get();
            System.out.printf("%02X", b);
        }
        System.out.println();
    }   

    public static String bytesToString(byte[] bytes) {
        try {
            return new String(bytes, Util.STRING_ENCODING);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding: " + Util.STRING_ENCODING);
            return null;
        }
    }

    public static byte[] stringToBytes(String str) {
        try {
            return str.getBytes(Util.STRING_ENCODING);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding: " + Util.STRING_ENCODING);
            return null;
        }
    }

    // public static String getTokenFromUri(String uri) throws DataNotFoundException{
    //     QueryStringDecoder decoder = new QueryStringDecoder(uri);
    //     String token = decoder.parameters().getOrDefault("token", null).get(0);
    //     if (token == null) {
    //         throw new DataNotFoundException("Token not found in URI");
    //     }
    //     return token;
    // }

    @Value("${netty.server.port}")
    public void setNettyServerPort(int port) {
        Util.nettyServerPort = port;
    }

    @Value("${netty.server.address}")
    public void setNettyServerAddress(String address) {
        Util.nettyServerAddress = address;
    }

    public static int getNettyServerPort() {
        return nettyServerPort;
    }

    public static String getNettyServerAddress() {
        return nettyServerAddress;
    }

    public static ByteBuffer allocateByteBuffer(Integer size) {
        return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
    }

    // public static Map<String, String> handleQueryString(String query) {
    //     return Arrays.stream(query.split("&"))
    //         .map(pair -> pair.split("=", 2)) // split by '=' but limit to 2 parts
    //         .filter(keyValue -> keyValue.length == 2) // filter out pairs that do not have both key and value
    //         .collect(Collectors.toMap(   // convert to Map
    //             keyValue -> keyValue[0],
    //             keyValue -> keyValue[1],
    //             (v1, v2) -> v2,               // if duplicate key, get the last value
    //             HashMap::new
    //         ));
    // }

}
