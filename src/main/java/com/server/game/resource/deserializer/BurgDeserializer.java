package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.server.game.model.map.component.Vector2;
import com.server.game.resource.model.SlotInfo.Burg;

import java.io.IOException;

public class BurgDeserializer extends AbstractSizedObjectDeserializer<Burg> {

    @Override
    public Burg deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        float rotate = (float) node.get("rotate").asDouble();

        return parse(node, sized -> new Burg(sized.id(), sized.position(), sized.width(), sized.length(), rotate));
    }
}