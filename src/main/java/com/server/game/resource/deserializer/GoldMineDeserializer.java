package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.server.game.resource.model.GameMap.GoldMine;

import java.io.IOException;

public class GoldMineDeserializer extends AbstractSizedObjectDeserializer<GoldMine> {

    @Override
    public GoldMine deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p, sized -> new GoldMine(sized.id(), sized.position(), sized.width(), sized.length()));
    }
}