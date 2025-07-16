import socket
import struct
import io
import time

# Cấu hình thông tin kết nối
game_id = "D7HLB"
token = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJteWFwcC5leGFtcGxlLmNvbSIsInN1YiI6IjY4NmNjN2Q5YjQ4YjJiNmE0NzhlZDNiYyIsImV4cCI6Mjc1MjU5NjgzOSwiaWF0IjoxNzUyNTk2ODM5LCJqdGkiOiIyNjc5MzNhNC0yZTM3LTQ2MmEtODlhZS1hNDc0Y2M5OTliMzEiLCJzY29wZSI6IlVTRVIifQ.8wOCJ9iOdCvzBVpmSgnopA0ll4aMgC8w59HKCvwShj4"

# host = "34.63.179.208"
host = "localhost"
port = 8386

USERNAME = "user2"
my_slot = None
MY_CHAMPION_ID = 2


# These type are reversed with server
AUTHENTICATION_SEND = 1
AUTHENTICATION_RECEIVE = 2

MESSAGE_SEND = 3
MESSAGE_RECEIVE = 4

CHOOSE_CHAMPION_SEND = 5
CHOOSE_CHAMPION_RECEIVE = 6

INFO_PLAYERS_IN_ROOM_RECEIVE = 12

PLAYER_READY_SEND = 13
PLAYER_READY_RECEIVE = 14

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

    elif type_id == PLAYER_READY_SEND:
        length: int = 0
        value: bytes = b""

    elif type_id == CHOOSE_CHAMPION_SEND:
        champion_id: int = kwagrs.get("champion_id", MY_CHAMPION_ID)
        value: bytes = struct.pack(">h", champion_id)
        length = 2


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
        return status_code == 8386


    elif type_id == MESSAGE_RECEIVE:
        message_byte_length = struct.unpack(">i", buffer.read(4))[0]
        message = buffer.read(message_byte_length).decode("utf-8")

        print(f"[Server -> Client] Message: {message}")
        # return message

    elif type_id == INFO_PLAYERS_IN_ROOM_RECEIVE:
        d = dict()
        num_players: int = struct.unpack(">h", buffer.read(2))[0]
        for _ in range(num_players):
            usernameByteLength = struct.unpack(">i", buffer.read(4))[0]
            usernameBytes = buffer.read(usernameByteLength)
            username = usernameBytes.decode("utf-8")
            slot: int = struct.unpack(">h", buffer.read(2))[0]
            d[slot] = username
        print(f"[Server -> Client] Players in room: {d}")
        return d


    elif type_id == PLAYER_READY_RECEIVE:
        slot: int = struct.unpack(">h", buffer.read(2))[0]
        isAllPlayersReady: bool = True if struct.unpack(">h", buffer.read(1))[0] == 1 else False
        print(f"[Server -> Client] Player {slot} is ready; all players are " + ("" if isAllPlayersReady else "not") + " ready.")
        return slot, isAllPlayersReady
    
    elif type_id == CHOOSE_CHAMPION_RECEIVE:
        slot: int = struct.unpack(">h", buffer.read(2))[0]
        champion_id: int = struct.unpack(">h", buffer.read(2))[0]
        print(f"[Server -> Client] Player {slot} chose champion ID: {champion_id}")
        return slot, champion_id

    # return None, None


def getResponse(sock: socket.socket):
    try:
        header = sock.recv(6)
        if not header:
            print("Server đóng kết nối.")
            return None, None

        buffer = io.BytesIO(header)
        type_id = struct.unpack(">h", buffer.read(2))[0]
        length = struct.unpack(">i", buffer.read(4))[0]

        value_bytes = b""
        while len(value_bytes) < length:
            chunk = sock.recv(length - len(value_bytes))
            if not chunk:
                break
            value_bytes += chunk

        buffer = io.BytesIO(value_bytes)
        return type_id, buffer

    except Exception as e:
        print(f"Lỗi khi nhận TLV: {e}")
        return None, None


def hearStartGame(sock: socket.socket):
    print("Listening for server messages...")
    try:
        while True:
            type_id, buffer = getResponse(sock)
            if type_id is None:
                break
            parse_tlv_message(type_id, buffer)
            if type_id == INFO_PLAYERS_IN_ROOM_RECEIVE: 
                break
    except KeyboardInterrupt:
        print("Client stopped.")


def hearChooseChampion(sock: socket.socket):
    print("Listening for server messages...")
    try:
        while True:
            type_id, buffer = getResponse(sock)
            if type_id is None:
                break
            parse_tlv_message(type_id, buffer)
            # if type_id == INFO_PLAYERS_IN_ROOM_RECEIVE: 
            #     break
    except KeyboardInterrupt:
        print("Client stopped.")



def authenticate(sock: socket.socket) -> bool:
    authentication_TLV_msg = build_tlv_message(
        type_id=AUTHENTICATION_SEND,
        token=token,
        match_id=game_id
    )

    print(f"[Client -> Server] Send TLV message: {authentication_TLV_msg.hex()}")
    sock.sendall(authentication_TLV_msg)
    
    time.sleep(1)  # Đợi server xử lý
    type_id, buffer = getResponse(sock)

    if buffer is None:
        print("Không nhận được phản hồi từ server.")
        return False

    return parse_tlv_message(type_id, buffer)




def sendReady(sock: socket.socket):
    ready_TLV_msg = build_tlv_message(
        type_id=PLAYER_READY_SEND
    )

    print(f"[Client -> Server] Send TLV message: {ready_TLV_msg.hex()}")
    sock.sendall(ready_TLV_msg)

    type_id, buffer = getResponse(sock)
    if buffer is None:
        print("Không nhận được phản hồi từ server.")
        return False
    

def sendChooseChampion(sock: socket.socket, champion_id: int = MY_CHAMPION_ID):
    choose_champion_TLV_msg = build_tlv_message(
        type_id=CHOOSE_CHAMPION_SEND,
        champion_id=champion_id
    )

    print(f"[Client -> Server] Send Choose Champion message: {choose_champion_TLV_msg.hex()}")
    sock.sendall(choose_champion_TLV_msg)


# ===== CLIENT TCP RAW =====
def connect():
    try:
        sock = socket.create_connection((host, port))
        print(f"Connected to server {host}:{port}")
        isAuthenticated = authenticate(sock)
        if isAuthenticated:
            print("Authenticated")

        try:
            hearStartGame(sock)
            time.sleep(2)
            sendChooseChampion(sock)
            hearChooseChampion(sock)
        except KeyboardInterrupt:
            print("Client stopped.")


    except Exception as e:
        print(f"Lỗi kết nối/giao tiếp: {e}")


if __name__ == "__main__":
    connect()
