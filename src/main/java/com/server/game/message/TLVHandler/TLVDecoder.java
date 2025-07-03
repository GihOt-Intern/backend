package com.server.game.message.TLVHandler;

import java.util.HashMap;
import java.util.Map;

import com.server.game.message.TLVInterface.TLVDecodable;

public class TLVDecoder {

    private static final Map<Short, Class<? extends TLVDecodable>> registry = new HashMap<>();

    public static void register(short type, Class<? extends TLVDecodable> clazz) {
        if (registry.containsKey(type)) {
            throw new IllegalArgumentException("Type already registered: " + type);
        }
        registry.put(type, clazz);
    }


    public static TLVDecodable byte2Object(short type, byte[] value) {
        Class<? extends TLVDecodable> clazz = registry.get(type);
        if (clazz == null) {
            System.out.println(">>> No class registered for type: " + type);
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
