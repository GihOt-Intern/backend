package com.server.game.netty.receiveObject;

import java.nio.ByteBuffer;

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

    @Override // must override this method of TLVDecodable interface
    public void decode(ByteBuffer buffer) { // buffer only contains the [value] part of the TLV message
        short championId = buffer.getShort();
        this.championEnum = ChampionEnum.fromShort(championId);
    }
}
