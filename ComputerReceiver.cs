using System;
using System.IO;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using InTheHand.Net.Bluetooth;
using InTheHand.Net.Sockets;

namespace BluetoothTouchpad
{
    class Program
    {
        private static readonly Guid ServiceUuid = new Guid("00001101-0000-1000-8000-00805F9B34FB");
        private const string ServiceName = "BluetoothTouchpad";
        
        [DllImport("user32.dll")]
        private static extern bool SetCursorPos(int x, int y);
        
        [DllImport("user32.dll")]
        private static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint cButtons, uint dwExtraInfo);
        
        private const uint MOUSEEVENTF_LEFTDOWN = 0x02;
        private const uint MOUSEEVENTF_LEFTUP = 0x04;
        
        private static int screenWidth = GetSystemMetrics(0);
        private static int screenHeight = GetSystemMetrics(1);
        
        [DllImport("user32.dll")]
        private static extern int GetSystemMetrics(int nIndex);
        
        static void Main(string[] args)
        {
            Console.WriteLine("============================================");
            Console.WriteLine("  手机蓝牙触控板 - 电脑端接收程序");
            Console.WriteLine("============================================");
            Console.WriteLine($"屏幕分辨率: {screenWidth}x{screenHeight}");
            Console.WriteLine();
            
            StartServer();
        }
        
        static void StartServer()
        {
            BluetoothListener listener = null;
            
            try
            {
                listener = new BluetoothListener(ServiceUuid);
                listener.ServiceName = ServiceName;
                listener.Start();
                
                Console.WriteLine("============================================");
                Console.WriteLine("  电脑已就绪，等待手机连接...");
                Console.WriteLine("============================================");
                Console.WriteLine();
                Console.WriteLine("请在手机上操作：");
                Console.WriteLine("1. 打开蓝牙触控板APP");
                Console.WriteLine("2. 点击'等待连接'按钮");
                Console.WriteLine("3. 在手机蓝牙列表中找到此电脑并点击连接");
                Console.WriteLine();
                Console.WriteLine("按 Ctrl+C 退出程序");
                Console.WriteLine();
                
                while (true)
                {
                    Console.WriteLine("等待手机连接...");
                    
                    BluetoothClient client = listener.AcceptBluetoothClient();
                    
                    string deviceName = "手机";
                    try
                    {
                        deviceName = client.RemoteMachineName ?? "手机";
                    }
                    catch { }
                    
                    Console.WriteLine();
                    Console.WriteLine($"*** 手机已连接: {deviceName} ***");
                    Console.WriteLine("现在可以移动手机来控制电脑了！");
                    Console.WriteLine();
                    
                    HandleClient(client);
                    
                    Console.WriteLine();
                    Console.WriteLine("手机已断开连接。");
                    Console.WriteLine();
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"\n错误: {ex.Message}");
                Console.WriteLine("\n按任意键退出...");
                Console.ReadKey();
            }
            finally
            {
                listener?.Stop();
            }
        }
        
        static void HandleClient(BluetoothClient client)
        {
            try
            {
                using (var stream = client.GetStream())
                {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    
                    while (client.Connected && (bytesRead = stream.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        string message = System.Text.Encoding.UTF8.GetString(buffer, 0, bytesRead);
                        Console.WriteLine($"收到数据: {message}");
                        
                        if (message.StartsWith("T"))
                        {
                            ParseAndExecuteTouch(message);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"连接错误: {ex.Message}");
            }
        }
        
        static void ParseAndExecuteTouch(string message)
        {
            try
            {
                string[] parts = message.Split('|');
                
                if (parts.Length >= 5 && parts[0] == "T")
                {
                    char type = parts[1][0];
                    float x = float.Parse(parts[2]);
                    float y = float.Parse(parts[3]);
                    
                    int realX = (int)(x * screenWidth);
                    int realY = (int)(y * screenHeight);
                    
                    switch (type)
                    {
                        case 'D':
                            Console.WriteLine($"触摸按下: ({x:F3}, {y:F3})");
                            mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);
                            break;
                            
                        case 'M':
                            Console.WriteLine($"移动到: ({x:F3}, {y:F3}) -> ({realX}, {realY})");
                            SetCursorPos(realX, realY);
                            break;
                            
                        case 'U':
                            Console.WriteLine($"触摸抬起");
                            mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
                            break;
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"解析错误: {ex.Message}");
            }
        }
    }
}
