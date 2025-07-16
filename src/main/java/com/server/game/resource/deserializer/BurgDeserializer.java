package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.server.game.resource.model.GameMap.SlotInfo.Burg;

import java.io.IOException;

public class BurgDeserializer extends AbstractSizedObjectDeserializer<Burg> {

    @Override
    public Burg deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p, sized -> new Burg(sized.id(), sized.position(), sized.width(), sized.length()));
    }
}