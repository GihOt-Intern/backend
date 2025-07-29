package com.server.game.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;



@Component
public class Util {

    public static final int SHORT_SIZE = 2;
    public static final int INT_SIZE = 4;
    public static final int FLOAT_SIZE = 4;
    public static final int DOUBLE_SIZE = 8;
    // public static final int CHAR_UTF32_SIZE = 4;

    private static int nettyServerPort;
    private static String nettyServerAddress;

    private static final String STRING_ENCODING = "UTF-8";

    private static Integer gameTickIntervalMs;

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

    @Value("${netty.server.port}")
    public void setNettyServerPort(int port) {
        Util.nettyServerPort = port;
    }

    @Value("${netty.server.address}")
    public void setNettyServerAddress(String address) {
        Util.nettyServerAddress = address;
    }

    @Value("${game.tick-interval-ms}")
    public void setGameTickIntervalMs(int interval) {
        Util.gameTickIntervalMs = interval;
    }

    public static int getNettyServerPort() {
        return nettyServerPort;
    }

    public static String getNettyServerAddress() {
        return nettyServerAddress;
    }

    public static int getGameTickIntervalMs() {
        return gameTickIntervalMs;
    }

    public static long seconds2GameTick(float seconds) {
        return (long)(seconds * 1000 / gameTickIntervalMs);
    }

    // public static float tick2seconds(long ticks) {
    //     return ticks * gameTickIntervalMs / 1000.0f;
    // }

    public static ByteBuffer allocateByteBuffer(Integer size) {
        return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN);
    }


    public static String compressBooleanArray(boolean[] array) {
        int byteLength = (int) Math.ceil(array.length / 8.0);
        byte[] packed = new byte[byteLength];

        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                packed[i / 8] |= 1 << (7 - (i % 8));
            }
        }

        return Base64.getEncoder().encodeToString(packed);
    }


    public static boolean[] decompressBooleanArray(String base64, int arrayLength) {
        byte[] packed = Base64.getDecoder().decode(base64);
        boolean[] array = new boolean[arrayLength];

        for (int i = 0; i < arrayLength; i++) {
            array[i] = (packed[i / 8] & (1 << (7 - (i % 8)))) != 0;
        }

        return array;
    }
}
