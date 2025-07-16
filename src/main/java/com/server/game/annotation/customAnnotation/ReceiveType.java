package com.server.game.annotation.customAnnotation;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.server.game.netty.tlv.typeDefine.ReceiveMessageType;

import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ReceiveType {
    ReceiveMessageType value();
}
