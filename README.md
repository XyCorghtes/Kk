# 手机蓝牙触控板项目

这个项目包含两部分：
1. **电脑端接收程序** (Python) - 运行在电脑上，通过蓝牙接收手机发送的触摸数据并转换为系统级鼠标操作
2. **Android 手机端应用** (Java) - 运行在手机上，采集触摸事件并通过蓝牙发送给电脑

## 项目结构

```
/workspace
├── computer_receiver.py    # 电脑端 Python 接收程序
└── android_app/            # Android 手机端应用源码
    ├── MainActivity.java
    └── activity_main.xml
```

## 功能说明

- 手机作为触摸板使用
- 通过蓝牙 RFCOMM 协议传输数据
- 电脑端将收到的坐标数据转换为鼠标移动和点击操作
- 支持单指滑动（移动）、点击（按下/抬起）

## 数据协议

通信协议非常简单，每次发送 9 字节：
- 4 字节：float 类型的 X 坐标 (归一化 0.0-1.0)
- 4 字节：float 类型的 Y 坐标 (归一化 0.0-1.0)
- 1 字节：动作类型 (0=移动，1=按下，2=抬起)

## 使用方法

### 1. 电脑端设置

#### Windows 系统
由于 PyBlueZ 在 Windows 上支持有限，建议使用 WiFi 方案或以下替代方法：

**方案 A: 使用 WiFi Socket (推荐)**
修改 `computer_receiver.py` 使用 socket 代替 bluetooth:

```python
import socket
# 替换蓝牙部分为 TCP Server
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(('0.0.0.0', 5000))
server.listen(1)
```

**方案 B: 使用 Linux/Mac**
在 Linux 或 Mac 上可以直接运行原代码：
```bash
pip install pybluez pynput
sudo python computer_receiver.py
```

### 2. Android 手机端设置

1. 使用 Android Studio 打开 `android_app` 目录
2. 创建新项目，将 `MainActivity.java` 和 `activity_main.xml` 复制到对应位置
3. 在 `AndroidManifest.xml` 添加权限：

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

4. 编译安装到手机

### 3. 配对连接

1. 在手机蓝牙设置中与电脑配对
2. 运行电脑端程序
3. 打开手机 App，点击"连接电脑"
4. 连接成功后，在手机灰色区域滑动即可控制电脑鼠标

## 注意事项

1. **Windows 蓝牙限制**: PyBlueZ 在 Windows 上不支持 RFCOMM 服务器模式，建议使用 WiFi 方案
2. **权限问题**: Android 12+ 需要动态申请蓝牙权限
3. **屏幕分辨率**: 当前实现使用归一化坐标，电脑端需要根据实际屏幕分辨率转换
4. **延迟优化**: 如需更低延迟，可以考虑增加数据压缩或使用 UDP 协议

## 扩展建议

- [ ] 添加多指手势支持（缩放、滚动）
- [ ] 添加键盘输入功能
- [ ] 改进连接界面，显示可用设备列表
- [ ] 添加 WiFi 直连模式（无需路由器）
- [ ] 优化坐标映射算法，支持不同 DPI 设置