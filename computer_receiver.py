import bluetooth
import struct
from pynput.mouse import Controller, Button
import time

# 初始化鼠标控制器
mouse = Controller()

# 定义蓝牙服务 UUID (可以随意生成一个)
SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB" # 标准串口服务

def handle_touch_event(x, y, action):
    """
    将收到的数据转换为系统操作
    :param x: 0.0 - 1.0 (归一化坐标)
    :param y: 0.0 - 1.0
    :param action: 0=移动, 1=按下, 2=抬起
    """
    # 获取当前屏幕分辨率 (pynput 不需要显式获取，它是相对移动或绝对位置取决于后端)
    # 注意：pynput 的 mouse.position 通常是绝对像素坐标，我们需要映射
    
    # 这里为了演示，我们假设接收的是相对移动或者简单的绝对映射
    # 实际项目中需要获取屏幕宽高进行映射: real_x = x * screen_width
    
    if action == 0: # 移动
        # pynput 设置绝对位置
        # 注意：不同操作系统对绝对坐标的支持不同，Windows 下 pynput 主要支持相对移动
        # 为了通用性，这里演示简单的逻辑，实际可能需要根据上一帧计算差值
        print(f"移动到: {x}, {y}")
        # 模拟：这里仅作打印，实际需结合屏幕分辨率转换
        # mouse.position = (int(x * screen_w), int(y * screen_h)) 
        
    elif action == 1: # 按下
        print("触摸按下")
        mouse.press(Button.left)
        
    elif action == 2: # 抬起
        print("触摸抬起")
        mouse.release(Button.left)

def start_server():
    server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    server_sock.bind(("", bluetooth.PORT_ANY))
    server_sock.listen(1)
    
    port = server_sock.getsockname()[1]
    
    # 广播服务，让手机能搜到
    bluetooth.advertise_service(server_sock, "PhoneTouchPad", 
                                service_id=SERVICE_UUID,
                                service_classes=[SERVICE_UUID, bluetooth.SERIAL_PORT_CLASS],
                                profiles=[bluetooth.SERIAL_PROFILE])
    
    print(f"等待连接... 端口: {port}")
    
    while True:
        client_sock, address = server_sock.accept()
        print(f"已连接设备: {address}")
        
        try:
            while True:
                # 约定数据格式: 4字节浮点X, 4字节浮点Y, 1字节动作
                # 总共 9 字节
                data = client_sock.recv(9)
                if not data:
                    break
                
                if len(data) == 9:
                    x, y = struct.unpack('ff', data[:8])
                    action = data[8]
                    handle_touch_event(x, y, action)
                else:
                    print(f"收到无效数据包长度: {len(data)}")
                    
        except bluetooth.btcommon.BluetoothError as e:
            print(f"蓝牙错误: {e}")
        finally:
            client_sock.close()
            print("连接断开")

if __name__ == "__main__":
    try:
        start_server()
    except KeyboardInterrupt:
        print("程序退出")
    except Exception as e:
        print(f"发生错误: {e}")
        print("提示：如果在 Windows 上运行 pybluez 困难，请考虑使用 WiFi Socket 替代蓝牙传输，代码逻辑几乎相同。")
