package com.server.game.netty.receiveObject.troop;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.server.game.annotation.customAnnotation.ReceiveType;
import com.server.game.netty.tlv.interf4ce.TLVDecodable;
import com.server.game.netty.tlv.messageEnum.ReceiveMessageType;

import lombok.AllArgsConstructor;
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
    private List<PositionData> positions;
    private long timestamp;

    @Override
    public void decode(byte[] value) {
        try {
            this.positions = new ArrayList<>(); // Initialize the list
            ByteArrayInputStream bais = new ByteArrayInputStream(value);
            DataInputStream dis = new DataInputStream(bais);
            short size = dis.readShort();
            for(int i=0; i<size; i++) {
                short troopIdLength = dis.readShort();
                byte[] troopIdBytes = new byte[troopIdLength];
                dis.readFully(troopIdBytes);
                String troopId = new String(troopIdBytes);

                float x = dis.readFloat();
                float y = dis.readFloat();

                positions.add(new PositionData(troopId, x, y));
            }
            this.timestamp = dis.readLong();
        } catch (Exception e) {
            throw new RuntimeException("Cannot decode troop position dataa", e);
        }
    }

    @Data
    @AllArgsConstructor
    public static class PositionData {
        private String troopId;
        private float x;
        private float y;
    }
}
