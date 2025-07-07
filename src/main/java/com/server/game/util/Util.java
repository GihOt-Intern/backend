package com.server.game.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.server.game.exception.DataNotFoundException;

import io.netty.handler.codec.http.QueryStringDecoder;

public class Util {

    public static final int SHORT_SIZE = 2;
    public static final int INT_SIZE = 4;
    public static final int DOUBLE_SIZE = 8;
    public static final int CHAR_UTF32_SIZE = 4;


    public static int STRING_BYTE_SIZE(int stringLength) {
        // 4 first bytes for BOM, then number of characters * 4 bytes (UTF-32 uses 4 bytes per character)
        return 4 + stringLength * CHAR_UTF32_SIZE;
    }

    public static void printHex(ByteBuffer buffer) {
        ByteBuffer readOnly = buffer.asReadOnlyBuffer();
        while (readOnly.hasRemaining()) {
            byte b = readOnly.get();
            System.out.printf("%02X", b);
        }
        System.out.println();
    }   

    public static String bytesToString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-32");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding: UTF-32");
            return null;
        }
    }

    public static String getTokenFromUri(String uri) throws DataNotFoundException{
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        String token = decoder.parameters().getOrDefault("token", null).get(0);
        if (token == null) {
            throw new DataNotFoundException("Token not found in URI");
        }
        return token;
    }
}
