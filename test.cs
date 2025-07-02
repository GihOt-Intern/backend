using System;
using System.Text;
using System.Threading.Tasks;
using UnityEngine;
using NativeWebSocket;

public class WebSocketClient : MonoBehaviour
{
    private WebSocket websocket;

    async void Start()
    {
        websocket = new WebSocket("ws://localhost:8080/ws"); // thay bằng URL server của bạn

        websocket.OnOpen += () =>
        {
            Debug.Log("WebSocket opened!");
            SendBinaryData();
        };

        websocket.OnError += (e) =>
        {
            Debug.Log("WebSocket error: " + e);
        };

        websocket.OnClose += (e) =>
        {
            Debug.Log("WebSocket closed!");
        };

        websocket.OnMessage += (bytes) =>
        {
            Debug.Log("Received binary data! Length: " + bytes.Length);

            // Ví dụ: đọc 2 số nguyên từ bytes
            int x = BitConverter.ToInt32(bytes, 0);
            int y = BitConverter.ToInt32(bytes, 4);

            Debug.Log($"Received x: {x}, y: {y}");
        };

        await websocket.Connect();
    }

    void Update()
    {
#if !UNITY_WEBGL || UNITY_EDITOR
        websocket?.DispatchMessageQueue(); // rất quan trọng để nhận message
#endif
    }

    async void SendBinaryData()
    {
        // Ví dụ: gửi 2 số nguyên 32-bit (x = 100, y = 200)
        byte[] data = new byte[8];
        Buffer.BlockCopy(BitConverter.GetBytes(100), 0, data, 0, 4);
        Buffer.BlockCopy(BitConverter.GetBytes(200), 0, data, 4, 4);

        await websocket.Send(data);
        Debug.Log("Sent binary data!");
    }

    private async void OnApplicationQuit()
    {
        await websocket.Close();
    }
}




async void SendBinaryData() {
    short type = 1;
    double x1 = 0.0;
    double y1 = 1.2;
    double x2 = 4.6;
    double y2 = -3.5;

    byte[] value = new byte[32];
    Buffer.BlockCopy(BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(BitConverter.DoubleToInt64Bits(x1))), 0, value, 0, 8);
    Buffer.BlockCopy(BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(BitConverter.DoubleToInt64Bits(y1))), 0, value, 8, 8);
    Buffer.BlockCopy(BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(BitConverter.DoubleToInt64Bits(x2))), 0, value, 16, 8);
    Buffer.BlockCopy(BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(BitConverter.DoubleToInt64Bits(y2))), 0, value, 24, 8);

    byte[] buffer = new byte[2 + 4 + value.Length];

    int length = value.Length;

    Buffer.BlockCopy(BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(type)), 0, buffer, 0, 2);
    Buffer.BlockCopy(BitConverter.GetBytes(System.Net.IPAddress.HostToNetworkOrder(length)), 0, buffer, 2, 4);
    Buffer.BlockCopy(value, 0, buffer, 6, value.Length);

    Debug.Log("Sending binary data: " + BitConverter.ToString(buffer));
    await websocket.Send(buffer);
}



void HandleServerMessage(byte[] payload) {
    if (payload.Length < 6) {
        Debug.LogWarning("Invalid message: too short.");
        return;
    }

    // Read two first bytes as short type (big endian) to determine message type
    short type = System.Net.IPAddress.NetworkToHostOrder(BitConverter.ToInt16(payload, 0));

    // Read next four bytes as int length (big endian)
    int length = System.Net.IPAddress.NetworkToHostOrder(BitConverter.ToInt32(payload, 2));

    if (payload.Length < 6 + length) {
        Debug.LogWarning("Invalid message: length mismatch.");
        return;
    }

    // Read the value bytes
    byte[] value = new byte[length];
    Buffer.BlockCopy(payload, 6, value, 0, length);

    // Handle the message based on its type
    if (type == 2) {
        if (value.Length != 8) {
            Debug.LogWarning("Invalid DistanceResponse: value length must be 8.");
            return;
        }

        long rawLong = System.Net.IPAddress.NetworkToHostOrder(BitConverter.ToInt64(value, 0));
        double distance = BitConverter.Int64BitsToDouble(rawLong);

        Debug.Log("Distance from server: " + distance);
    }
    else  {
        // TODO: Handle other message types
    }
}
