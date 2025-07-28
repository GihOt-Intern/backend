package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.server.game.resource.model.GameMap.PlayGround;

import java.io.IOException;

public class PlayGroundDeserializer extends AbstractSizedObjectDeserializer<PlayGround> {

    @Override
    public PlayGround deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return parse(p, sized -> new PlayGround(sized.id(), sized.position(), sized.width(), sized.length()));
    }
}