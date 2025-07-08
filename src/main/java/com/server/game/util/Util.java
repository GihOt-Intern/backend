package com.server.game.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.server.game.exception.DataNotFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.QueryStringDecoder;

@Component
public class Util {

    public static final int SHORT_SIZE = 2;
    public static final int INT_SIZE = 4;
    public static final int DOUBLE_SIZE = 8;
    public static final int CHAR_UTF32_SIZE = 4;

    private static int nettyServerPort;
    private static String nettyServerAddress;


    public static int STRING_BYTE_SIZE(int stringLength) {
        // number of characters * 4 bytes (UTF-32-BE uses 4 bytes per character)
        return stringLength * CHAR_UTF32_SIZE;
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
            return new String(bytes, "UTF-32BE");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding: UTF-32BE");
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

    public static Map<String, String> handleQueryString(String query) {
        return Arrays.stream(query.split("&"))
            .map(pair -> pair.split("=", 2)) // split by '=' but limit to 2 parts
            .filter(keyValue -> keyValue.length == 2) // filter out pairs that do not have both key and value
            .collect(Collectors.toMap(   // convert to Map
                keyValue -> keyValue[0],
                keyValue -> keyValue[1],
                (v1, v2) -> v2,               // if duplicate key, get the last value
                HashMap::new
            ));
    }

}
