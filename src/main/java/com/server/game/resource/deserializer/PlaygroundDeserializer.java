package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.server.game.resource.model.GameMap.Playground;

import java.io.IOException;

public class PlaygroundDeserializer extends AbstractSizedObjectDeserializer<Playground> {

    @Override
    public Playground deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p.getCodec().readTree(p), 
        sized -> new Playground(sized.id(), sized.position(), sized.width(), sized.length()));
    }
}