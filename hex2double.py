import struct

# Chuỗi hex dạng string
hex_str = "40 1A 96 53 B8 6F 19 77"

# Bước 1: Chuyển từ hex string sang bytes
hex_bytes = bytes.fromhex(hex_str)

# Bước 2: Giải mã double big-endian (">d")
value = struct.unpack(">d", hex_bytes)[0]

print("Double value:", value)
