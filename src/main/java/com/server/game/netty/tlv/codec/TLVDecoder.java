package com.server.game.netty.tlv.codec;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


import com.server.game.netty.tlv.interf4ce.TLVDecodable;


public class TLVDecoder {

    private static final Map<Short, Class<? extends TLVDecodable>> registry = new HashMap<>();

    public static void register(short type, Class<? extends TLVDecodable> clazz) {
        if (registry.containsKey(type)) {
            throw new IllegalArgumentException("Type already registered: " + type);
        }
        registry.put(type, clazz);
        System.out.println(">>> Registered TLVDecodable: <" + clazz.getSimpleName() + "> for type=<" + type + ">");
    }


    public static TLVDecodable bytes2Object(short type, byte[] value) {
        // OOP abstraction: use interface (abstraction) to create an concrete instance base on [type]
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
