import asyncio
import struct
import websockets
import random

async def send_binary_data(websocket, type):
    tlv = None
    if type == 1:
        x1, y1 = random.uniform(-500.0, 500.0), random.uniform(-500.0, 500.0)
        x2, y2 = random.uniform(-500.0, 500.0), random.uniform(-500.0, 500.0)
        print(f"Sending coordinates: ({x1}, {y1}), ({x2}, {y2})")
        # Encode 4 double values (big-endian)
        value = struct.pack(">dddd", x1, y1, x2, y2)

        # Encode TLV: type (short), length (int), value (32 bytes)
        tlv = struct.pack(">hI", type, len(value)) + value

    elif type == 3:
        message = "Hello server. This is a test message."
        messageLength = len(message)
        # Encode message as bytes
        messageBytes = message.encode("utf-32")
        # Create TLV
        tlv = struct.pack(">hII", type, 4+messageLength, messageLength) + messageBytes
    
    print("Sending binary data:", tlv.hex())        
    await websocket.send(tlv)


async def receive_response(websocket):
    response = await websocket.recv()
    print(f"Received {len(response)} bytes")
    print("Hex:", response.hex())
    # Giải mã tùy theo TLV response nếu cần ở đây

async def main():
    uri = "ws://localhost:8386/ws"
    uri += "?token=eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJteWFwcC5leGFtcGxlLmNvbSIsInN1YiI6IjY4NmIzMTA0YWQ4YWNjNTRjMDhhZjQxZiIsImV4cCI6Mjc1MTg1NTM4NywiaWF0IjoxNzUxODU1Mzg3LCJqdGkiOiIwNTdlYzg3ZC05MjI0LTQzNDItOTE1ZC0wOGZjYWY5ZmE0OTMiLCJzY29wZSI6IlVTRVIifQ.L2i68ThIs9bAGUOaaCuX9YwZpSTNK3lm3_XIeZEwuiE"
    
    try:
        async with websockets.connect(uri) as websocket:
            print("Connected to WebSocket server.")

            # for i in range(1000):
            #     print(f"Sending message {i + 1}")
            await send_binary_data(websocket, type=3)
            #     await receive_response(websocket)
            print("Closing connection.")
    except Exception as e:
        print("Exception:", str(e))

if __name__ == "__main__":
    asyncio.run(main())
