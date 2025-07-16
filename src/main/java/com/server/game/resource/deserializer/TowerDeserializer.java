package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.server.game.resource.model.GameMap.SlotInfo.Tower;

import java.io.IOException;

public class TowerDeserializer extends AbstractSizedObjectDeserializer<Tower> {

    @Override
    public Tower deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p, sized -> new Tower(sized.id(), sized.position(), sized.width(), sized.length()));
    }
}