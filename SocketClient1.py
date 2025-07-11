import socket
import struct
import io
import time

# Cấu hình thông tin kết nối
game_id = "room123"
token = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJteWFwcC5leGFtcGxlLmNvbSIsInN1YiI6IjY4NmIzMTA0YWQ4YWNjNTRjMDhhZjQxZiIsImV4cCI6Mjc1MjIwODU2MCwiaWF0IjoxNzUyMjA4NTYwLCJqdGkiOiJiMzhkYWY0ZS00MjFjLTQxN2QtOTMwOC0zZjMxODhkOWE1MDgiLCJzY29wZSI6IlVTRVIifQ.LNICbsB1gMwth4_wV4nAovXD0XJbFZ0DnSPz6TywLXE"

host = "localhost"
port = 8386


# These type are reversed with server
AUTHENTICATION_SEND = 1
AUTHENTICATION_RECEIVE = 2

MESSAGE_SEND = 3
MESSAGE_RECEIVE = 4

# INFO_PLAYERS_IN_ROOM_SEND = 5
INFO_PLAYERS_IN_ROOM_RECEIVE = 12

isAuthenticated = False


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
        global isAuthenticated
        isAuthenticated = True
        # return status_code, message


    if type_id == MESSAGE_RECEIVE:
        message_byte_length = struct.unpack(">i", buffer.read(4))[0]
        message = buffer.read(message_byte_length).decode("utf-8")

        print(f"[Server -> Client] Message: {message}")
        # return message

    if type_id == INFO_PLAYERS_IN_ROOM_RECEIVE:
        d = dict()
        num_players: int = struct.unpack(">h", buffer.read(2))[0]
        for i in range(num_players):
            usernameByteLength = struct.unpack(">i", buffer.read(4))[0]
            usernameBytes = buffer.read(usernameByteLength)
            username = usernameBytes.decode("utf-8")
            slot: int = struct.unpack(">h", buffer.read(2))[0]
            d[slot] = username
        print(f"[Server -> Client] Players in room: {d}")

    # return None, None
    


# ===== CLIENT TCP RAW =====
def connect():
    try:
        with socket.create_connection((host, port)) as sock:
            print(f"Connected to server {host}:{port}")

            # Create TLV message for authentication
            authentication_TLV_msg = build_tlv_message(
                type_id=AUTHENTICATION_SEND,
                token=token,
                match_id=game_id
            )


            # Create TLV message for sending a message
            other_TLV_msg = build_tlv_message(
                type_id=MESSAGE_SEND,
                message="Hello, this is a test message."
            )


            # TLV_msg = other_TLV_msg
            TLV_msg = authentication_TLV_msg


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
                parse_tlv_message(type_id, buffer)
                # if type_id == AUTHENTICATION_RECEIVE:
                #     statusCode, message = parse_tlv_message(type_id, buffer)
                # elif type_id == MESSAGE_RECEIVE:
                #     message = parse_tlv_message(type_id, buffer)
                # elif type_id == INFO_PLAYERS_IN_ROOM_RECEIVE:
                #     parse_tlv_message(type_id, buffer)

                # if isAuthenticated:
                #     print("Authenticated, sending another TLV message...")
                #     TLV_msg = other_TLV_msg
                #     print(f"[Client -> Server] Send TLV message: {TLV_msg.hex()}")
                #     sock.sendall(TLV_msg)
                #     break


                # break



    except Exception as e:
        print(f"Lỗi kết nối/giao tiếp: {e}")


if __name__ == "__main__":
    connect()
