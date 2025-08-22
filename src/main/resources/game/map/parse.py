import json
from pathlib import Path

def convert_map_to_grid(input_file: str, output_file: str, map_id: int = 2, map_name: str = "map_2"):
    """
    Converts 'Map 2.json' with a flat 'data' array into 'map_2_grid.json' with a reshaped 2D 'matrix' array.
    
    :param input_file: Path to the source JSON file.
    :param output_file: Path to save the converted JSON.
    :param map_id: ID for the output map.
    :param map_name: Map name for the output JSON.
    """
    # Load input JSON
    with open(input_file, 'r', encoding='utf-8') as f:
        data = json.load(f)

    corner_a = data["CornerA"]
    corner_b = data["CornerB"]
    cell_size = data["cellsize"]
    n_cols = data["width"]
    n_rows = data["height"]
    flat_data = data["data"]

    expected_length = n_cols * n_rows
    if len(flat_data) != expected_length:
        raise ValueError(f"Data length {len(flat_data)} does not match expected {expected_length} for {n_cols}x{n_rows}.")

    # Reshape into 2D matrix
    matrix_2d = [
        flat_data[i * n_cols : (i + 1) * n_cols]
        for i in range(n_rows)
    ]

    # Construct output structure
    output_data = {
        "id": map_id,
        "mapName": map_name,
        "cornerA": corner_a,
        "cornerB": corner_b,
        "cellSize": cell_size,
        "nCols": n_cols,
        "nRows": n_rows,
        "matrix": matrix_2d
    }

    # Save to output file with compact row arrays
    json_string = json.dumps(output_data, indent=4, ensure_ascii=False)

    # Compact the matrix rows
    import re
    json_string = re.sub(
        r'(\[\s*(?:\d+,\s*)*\d+\s*\])',
        lambda m: m.group(0).replace('\n', '').replace(' ', ''),
        json_string
    )

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(json_string)

    print(f"Conversion successful: '{input_file}' ➔ '{output_file}'")

if __name__ == "__main__":
    convert_map_to_grid("Map 2.json", "map_2_grid.json")
