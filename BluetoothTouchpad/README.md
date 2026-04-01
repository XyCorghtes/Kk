# 蓝牙触控板 - Android 应用

将您的手机变成电脑的无线触控板！通过蓝牙连接，支持多点触控。

## 📱 功能特点

- **蓝牙连接**：使用经典蓝牙 (SPP) 连接电脑
- **触摸控制**：将手机屏幕作为触控板使用
- **多点触控**：支持多指操作
- **实时传输**：低延迟的触摸数据传输
- **跨平台**：电脑端支持 Windows、Linux、macOS

## 🔧 构建 APK

### 方法一：使用 Android Studio（推荐）

1. 打开 Android Studio
2. 选择 `File` -> `Open`
3. 选择 `BluetoothTouchpad` 文件夹
4. 等待 Gradle 同步完成
5. 选择 `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`
6. APK 将生成在：`app/build/outputs/apk/debug/app-debug.apk`

### 方法二：使用命令行

如果您已安装 Android SDK：

```bash
cd BluetoothTouchpad
./gradlew assembleDebug
```

APK 位置：`app/build/outputs/apk/debug/app-debug.apk`

## 📲 使用方法

### 手机端
1. 安装 APK 到 Android 手机
2. 打开应用，授予蓝牙权限
3. 点击"等待连接"按钮
4. 手机会进入配对模式

### 电脑端
1. 确保电脑有蓝牙功能
2. 运行电脑接收程序：`python computer_receiver.py`
3. 在电脑上搜索并配对手机（设备名：BluetoothTouchpad）
4. 配对成功后，手机会显示"已连接"

### 使用触控板
- 单指滑动：移动鼠标光标
- 单指点击：鼠标左键
- 双指滑动：滚动页面
- 双指点击：鼠标右键

## 🖥️ 电脑端程序

电脑端需要运行 Python 接收程序来接收蓝牙数据并模拟鼠标操作。

依赖安装：
```bash
pip install bleak pynput
```

运行：
```bash
python computer_receiver.py
```

## ⚙️ 技术细节

- **蓝牙协议**：RFCOMM (SPP)
- **Service UUID**：00001101-0000-1000-8000-00805F9B34FB
- **数据格式**：`T{type}{x}{y}{pointers}`
  - type: D(按下), M(移动), U(抬起), C(取消)
  - x, y: 归一化坐标 (0.0-1.0)
  - pointers: 触摸点数量

## 📝 注意事项

1. 首次使用需要在系统设置中启用蓝牙
2. Android 12+ 需要授予蓝牙扫描和连接权限
3. 确保手机和电脑距离较近以获得稳定连接
4. 如果连接失败，尝试重启蓝牙或重新配对

## 🛠️ 故障排除

**问题：无法找到设备**
- 确保手机蓝牙已开启
- 确保电脑蓝牙已开启且可被发现
- 检查是否已授予应用所有必要权限

**问题：连接后立即断开**
- 删除已有的配对记录，重新配对
- 重启电脑端的接收程序
- 检查防火墙设置

**问题：触摸不灵敏**
- 确保手指完全接触屏幕
- 尝试在设置中调整触摸灵敏度（如需要）

## 📄 许可证

MIT License
