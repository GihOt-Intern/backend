package com.server.game.netty.receiveObject.troop;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

import lombok.Data;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ReceiveType(ReceiveMessageType.TROOP_POSITION_RECEIVE)
@Component
public class TroopPositionReceive implements TLVDecodable{
    private List<String> troopIds;
    float x, y;
    private long timestamp;

    @Override
    public void decode(byte[] value) {
        try {
            this.troopIds = new ArrayList<>(); // Initialize the list
            ByteArrayInputStream bais = new ByteArrayInputStream(value);
            DataInputStream dis = new DataInputStream(bais);
            short size = dis.readShort();
            for(int i=0; i<size; i++) {
                short troopIdLength = dis.readShort();
                byte[] troopIdBytes = new byte[troopIdLength];
                dis.readFully(troopIdBytes);
                String troopId = new String(troopIdBytes);
                this.troopIds.add(troopId); // Add troopId to the list
            }
            this.x = dis.readFloat();
            this.y = dis.readFloat();
            // Read the timestamp
            this.timestamp = dis.readLong();
        } catch (Exception e) {
            throw new RuntimeException("Cannot decode troop position dataa", e);
        }
    }
}
