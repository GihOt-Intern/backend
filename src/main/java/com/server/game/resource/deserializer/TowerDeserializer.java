package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.server.game.resource.model.SlotInfo.Tower;

import java.io.IOException;

public class TowerDeserializer extends AbstractSizedObjectDeserializer<Tower> {

    @Override
    public Tower deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        float rotate = (float) node.get("rotate").asDouble();

        return parse(node, sized -> new Tower(sized.id(), sized.position(), sized.width(), sized.length(), rotate));
    }
}