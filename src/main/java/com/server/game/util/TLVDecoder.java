package com.server.game.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import com.server.game.message.receive.DistanceReceive;
import com.server.game.message.receive.TLVDecodable;

public class TLVDecoder {

    private static final Map<Short, Class<? extends TLVDecodable>> typeMap = new HashMap<>();

    static {
        typeMap.put((short) 0x0001, DistanceReceive.class);
        // typeMap.put((short) 0x0002, AnotherDTO.class);
    }

    public static Object decode(short type, byte[] value) {
        Class<? extends TLVDecodable> clazz = typeMap.get(type);
        if (clazz == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        try {
            
            TLVDecodable instance = clazz.getDeclaredConstructor().newInstance();
            instance.decode(value);
            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to decode TLV message for type: " + type, e);
        }
    }
}
