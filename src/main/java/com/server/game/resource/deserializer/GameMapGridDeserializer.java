package com.server.game.resource.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.server.game.map.component.Vector2;
import com.server.game.resource.model.GameMapGrid;

import java.io.IOException;



public class GameMapGridDeserializer extends JsonDeserializer<GameMapGrid> {

    @Override
    public GameMapGrid deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        short id = (short) node.get("id").asInt();
        String name = node.get("mapName").asText();

        JsonNode cornerANode = node.get("cornerA");
        Vector2 cornerA = new Vector2(
                (float) cornerANode.get("x").asDouble(),
                (float) cornerANode.get("z").asDouble()
        );

        JsonNode cornerBNode = node.get("cornerB");
        Vector2 cornerB = new Vector2(
                (float) cornerBNode.get("x").asDouble(),
                (float) cornerBNode.get("z").asDouble()
        );

        int nRows = node.get("nRows").asInt();
        int nCols = node.get("nCols").asInt();


        float cellSize = (float) node.get("cellSize").asDouble();
        boolean[][] grid = new boolean[nRows][nCols];

        JsonNode matrixNode = node.get("matrix");

        for (int row = 0; row < nRows; row++) {
            JsonNode rowNode = matrixNode.get(row);
            for (int col = 0; col < nCols; col++) {
                grid[row][col] = rowNode.get(col).asInt() != 0;
            }
        }

        return new GameMapGrid(id, name, cornerA, cornerB, nRows, nCols, cellSize, grid);
    }
}
