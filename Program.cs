using System;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

class WebSocketTest
{
    static async Task Main(string[] args)
    {
        using (var ws = new ClientWebSocket())
        {
            // ✅ URL WebSocket cần kết nối (thay đổi nếu cần)
            var uri = new Uri("ws://localhost:8080/ws"); // hoặc "ws://localhost:8080/ws"

            Console.WriteLine("👉 Đang kết nối đến: " + uri);
            await ws.ConnectAsync(uri, CancellationToken.None);
            Console.WriteLine("✅ Đã kết nối WebSocket!");

            // Gửi message
            string message = "Hello from C# client";
            var buffer = Encoding.UTF8.GetBytes(message);
            await ws.SendAsync(new ArraySegment<byte>(buffer), WebSocketMessageType.Text, true, CancellationToken.None);
            Console.WriteLine($"📤 Đã gửi: {message}");

            // Nhận message
            var receiveBuffer = new byte[1024];
            var result = await ws.ReceiveAsync(new ArraySegment<byte>(receiveBuffer), CancellationToken.None);
            var response = Encoding.UTF8.GetString(receiveBuffer, 0, result.Count);
            Console.WriteLine($"📥 Đã nhận: {response}");

            // Đóng kết nối
            await ws.CloseAsync(WebSocketCloseStatus.NormalClosure, "Bye", CancellationToken.None);
            Console.WriteLine("👋 Kết thúc kết nối");
        }
    }
}
