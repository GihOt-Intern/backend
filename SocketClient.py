import socket
import struct
import io
import time

# Cấu hình thông tin kết nối
game_id = "room123"
token = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJteWFwcC5leGFtcGxlLmNvbSIsInN1YiI6IjY4NmNjN2Q5YjQ4YjJiNmE0NzhlZDNiYyIsImV4cCI6Mjc1MjA1ODYyNiwiaWF0IjoxNzUyMDU4NjI2LCJqdGkiOiIyMDdmNTYzMy0zOGU3LTQ1MjYtYmU0My0yY2FhYjk2YmExNGMiLCJzY29wZSI6IlVTRVIifQ.gvNRwc3K5KEf757DPtc6X5_xcKr2RyVgVeDt6X1FBXw"

host = "localhost"
port = 8386


# These type are reversed with server
AUTHENTICATION_SEND = 1
AUTHENTICATION_RECEIVE = 2

MESSAGE_SEND = 3
MESSAGE_RECEIVE = 4


def build_tlv_message(type_id: int, **kwagrs) -> bytes:
    length: int = 0
    value: bytes = b""

    if type_id == AUTHENTICATION_SEND:
        token: str = kwagrs.get("token", "")
        match_id: str = kwagrs.get("match_id", "")

        tokenBytes: bytes = token.encode("utf-8")
        matchBytes: bytes = match_id.encode("utf-8")

        tokenByteLength: int = len(tokenBytes)
        matchByteLength: int = len(matchBytes)

        length = 4 + tokenByteLength + 4 + matchByteLength

        value: bytes = struct.pack(">i", tokenByteLength) + tokenBytes + \
            struct.pack(">i", matchByteLength) + matchBytes
        

        # Print TLV message in hex format
        # print(f"[Client -> Server] TLV message: {tlv_message.hex()}")

    
    elif type_id == MESSAGE_SEND:
        message: str = kwagrs.get("message", "")
        message_bytes: bytes = message.encode("utf-8")
        message_byte_length: int = len(message_bytes)
        length: int = 4 + message_byte_length
        value: bytes = struct.pack(">i", message_byte_length) + message_bytes





    tlv_message: bytes = struct.pack(">h", type_id) + struct.pack(">i", length) + value
    return tlv_message



# ===== PARSE RESPONSE =====
def parse_tlv_message(type_id:int, buffer: io.BytesIO) -> tuple:
    '''buffer only the [value] part of TLV message'''

    if type_id == AUTHENTICATION_RECEIVE:
        status_code = struct.unpack(">i", buffer.read(4))[0]
        message_length = struct.unpack(">i", buffer.read(4))[0]
        message = buffer.read(message_length).decode("utf-8")

        print(f"[Server -> Client] Status Code: {status_code}, Message: {message}")
        return status_code, message


    if type_id == MESSAGE_RECEIVE:
        message_byte_length = struct.unpack(">i", buffer.read(4))[0]
        message = buffer.read(message_byte_length).decode("utf-8")

        print(f"[Server -> Client] Message: {message}")
        return message


    return None, None
    


# ===== CLIENT TCP RAW =====
def connect():
    try:
        with socket.create_connection((host, port)) as sock:
            print(f"Connected to server {host}:{port}")

            # Gửi login TLV
            authentication_TLV_msg = build_tlv_message(
                type_id=AUTHENTICATION_SEND,
                token=token,
                match_id=game_id
            )


            # Test first TLV message is not authentication message
            other_TLV_msg = build_tlv_message(
                type_id=MESSAGE_SEND,
                message="Hello, this is a test message."
            )


            TLV_msg = other_TLV_msg
            # TLV_msg = authentication_TLV_msg

            print(f"[Client -> Server] Send TLV message: {TLV_msg.hex()}")
            sock.sendall(TLV_msg)

            # print("Sleep for 10 seconds before reading response...")
            # # Delay
            # time.sleep(10)

            # # Gửi TLV khác (type=5, value=2)
            # msg = build_tlv_message(type_id=5, value=2)
            # sock.sendall(msg)
            # print(f"[Client -> Server] Gửi TLV: {msg.hex()}")

            # Đọc phản hồi
            while True:
                header = sock.recv(6)
                if not header:
                    print("Server đã đóng kết nối.")
                    break

                print(f"[Server -> Client] Nhận header: {header.hex()}")

                buffer = io.BytesIO(header)
                type_id = struct.unpack(">h", buffer.read(2))[0]
                length = struct.unpack(">i", buffer.read(4))[0]
                
                value_bytes = b""
                while len(value_bytes) < length:
                    chunk = sock.recv(length - len(value_bytes))
                    if not chunk:
                        print("Server đóng kết nối giữa chừng khi nhận Value.")
                        break
                    value_bytes += chunk


                print(f"[Server -> Client] Received full message: {header.hex()}{value_bytes.hex()}")
                buffer = io.BytesIO(value_bytes)
                if type_id == AUTHENTICATION_RECEIVE:
                    statusCode, message = parse_tlv_message(type_id, buffer)
                elif type_id == MESSAGE_RECEIVE:
                    message = parse_tlv_message(type_id, buffer)

                # type_id = struct.unpack(">h", header[:2])[0]
                # length = struct.unpack(">i", header[2:6])[0]

                # payload = b""
                # while len(payload) < length:
                #     chunk = sock.recv(length - len(payload))
                #     if not chunk:
                #         break
                #     payload += chunk

                # full_message = header + payload
                # parse_tlv_message(full_message)

    except Exception as e:
        print(f"Lỗi kết nối/giao tiếp: {e}")


if __name__ == "__main__":
    connect()
