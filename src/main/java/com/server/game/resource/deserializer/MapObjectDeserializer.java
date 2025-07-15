package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.server.game.resource.model.GameMap.MapObject;
import com.server.game.map.component.Vector2;
import java.io.IOException;

public class MapObjectDeserializer extends JsonDeserializer<MapObject> {

    @Override
    public MapObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        String id = node.get("id").asText();
        String type = node.get("type").asText();

        // Deserialize position
        JsonNode posNode = node.get("position");
        float x = (float) posNode.get("x").asDouble();
        float y = (float) posNode.get("y").asDouble();
        Vector2 position = new Vector2(x, y);

        // Deserialize size
        JsonNode sizeNode = node.get("size");
        float width = (float) sizeNode.get("width").asDouble();
        float length = (float) sizeNode.get("length").asDouble();

        return new MapObject(id, type, position, width, length);
    }
}
