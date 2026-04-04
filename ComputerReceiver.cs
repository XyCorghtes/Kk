using System;
using System.IO;
using System.Net.Sockets;
using System.Runtime.InteropServices;
using System.Threading;
using InTheHand.Net;
using InTheHand.Net.Bluetooth;
using InTheHand.Net.Sockets;

namespace BluetoothTouchpad
{
    class Program
    {
        private static readonly Guid ServiceUuid = new Guid("00001101-0000-1000-8000-00805F9B34FB");
        private const string ServiceName = "PhoneTouchPad";
        
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
            Console.WriteLine("手机蓝牙触控板 - 电脑端接收程序");
            Console.WriteLine($"屏幕分辨率: {screenWidth}x{screenHeight}");
            Console.WriteLine("按 Ctrl+C 退出程序\n");
            
            var listener = new BluetoothListener(ServiceUuid);
            listener.ServiceName = ServiceName;
            listener.Start();
            
            Console.WriteLine($"等待蓝牙连接... 服务: {ServiceName}");
            
            try
            {
                while (true)
                {
                    var client = listener.AcceptBluetoothClient();
                    Console.WriteLine($"\n已连接设备: {client.RemoteEndPoint}");
                    
                    Thread clientThread = new Thread(HandleClient);
                    clientThread.Start(client);
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"\n错误: {ex.Message}");
            }
            finally
            {
                listener.Stop();
            }
        }
        
        static void HandleClient(object obj)
        {
            var client = (BluetoothClient)obj;
            
            try
            {
                using (var stream = client.GetStream())
                {
                    byte[] buffer = new byte[9];
                    int bytesRead;
                    
                    while ((bytesRead = stream.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        if (bytesRead == 9)
                        {
                            float x = BitConverter.ToSingle(buffer, 0);
                            float y = BitConverter.ToSingle(buffer, 4);
                            byte action = buffer[8];
                            
                            HandleTouchEvent(x, y, action);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"连接错误: {ex.Message}");
            }
            finally
            {
                client.Close();
                Console.WriteLine("连接已断开");
            }
        }
        
        static void HandleTouchEvent(float x, float y, byte action)
        {
            int realX = (int)(x * screenWidth);
            int realY = (int)(y * screenHeight);
            
            switch (action)
            {
                case 0:
                    Console.WriteLine($"移动到: ({x:F3}, {y:F3}) -> ({realX}, {realY})");
                    SetCursorPos(realX, realY);
                    break;
                    
                case 1:
                    Console.WriteLine("触摸按下");
                    mouse_event(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0);
                    break;
                    
                case 2:
                    Console.WriteLine("触摸抬起");
                    mouse_event(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0);
                    break;
            }
        }
    }
}
