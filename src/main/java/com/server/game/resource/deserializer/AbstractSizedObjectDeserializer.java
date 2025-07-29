package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.server.game.model.map.component.Vector2;

import java.io.IOException;
import java.util.function.Function;



public abstract class AbstractSizedObjectDeserializer<T> extends JsonDeserializer<T> {

    protected T parse(JsonParser p, Function<SizedObject, T> builder) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        String id = node.get("id").asText();
        Vector2 position = new Vector2(
                (float) node.get("position").get("x").asDouble(),
                (float) node.get("position").get("y").asDouble()
        );
        float width = (float) node.get("size").get("width").asDouble();
        float length = (float) node.get("size").get("length").asDouble();

        return builder.apply(new SizedObject(id, position, width, length));
    }

    protected record SizedObject(String id, Vector2 position, float width, float length) {
    }
}
