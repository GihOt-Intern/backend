package com.server.game.netty.receiveObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;
import com.server.game.util.ChampionEnum;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.CHOOSE_CHAMPION_RECEIVE) // Custom annotation to define the type of this message
@Component // Register this class as a Spring component to be scanned in ReceiveTypeScanner when the application starts
public class ChooseChampionReceive implements TLVDecodable {
    ChampionEnum championEnum;

    @Override
    public void decode(byte[] value) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(value);
            DataInputStream dis = new DataInputStream(bais);

            this.championEnum = ChampionEnum.fromShort(dis.readShort());

        } catch (Exception e) {
            throw new  RuntimeException("Cannot decode " + this.getClass().getSimpleName(), e);
        }
    }
}
