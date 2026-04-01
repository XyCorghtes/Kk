package com.example.phonetouchpad;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PhoneTouchPad";
    // 必须与电脑端保持一致的 UUID
    private static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    
    private View touchSurface;
    private TextView statusText;
    private Button connectButton;

    // 权限请求码
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        touchSurface = findViewById(R.id.touch_surface);
        statusText = findViewById(R.id.status_text);
        connectButton = findViewById(R.id.btn_connect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkPermissions();
        setupListeners();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "需要蓝牙权限才能运行", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupListeners() {
        connectButton.setOnClickListener(v -> connectToDevice());

        // 设置触摸监听，将触摸事件转换为数据发送
        touchSurface.setOnTouchListener((v, event) -> {
            if (outputStream == null || socket == null || !socket.isConnected()) {
                statusText.setText("状态：未连接");
                return true;
            }

            float x = event.getX() / v.getWidth(); // 归一化 0.0 - 1.0
            float y = event.getY() / v.getHeight();
            int action = event.getActionMasked();

            sendTouchData(x, y, action);
            return true;
        });
    }

    private void connectToDevice() {
        // 简单示例：连接已配对的第一个设备
        // 实际应用中应该做一个列表让用户选择
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "没有配对的设备，请先在系统设置中配对电脑", Toast.LENGTH_LONG).show();
            return;
        }

        // 这里假设用户已经知道电脑的蓝牙名称，或者我们取第一个
        // 为了演示，我们取第一个配对的设备，实际应增加选择界面
        BluetoothDevice device = pairedDevices.iterator().next(); 
        
        new Thread(() -> {
            try {
                runOnUiThread(() -> statusText.setText("状态：正在连接..."));
                socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID);
                socket.connect();
                outputStream = socket.getOutputStream();
                
                runOnUiThread(() -> {
                    statusText.setText("状态：已连接到 " + device.getName());
                    connectButton.setEnabled(false);
                    Toast.makeText(MainActivity.this, "连接成功！现在可以使用触摸板了。", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    statusText.setText("状态：连接失败");
                    Toast.makeText(MainActivity.this, "连接失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void sendTouchData(float x, float y, int action) {
        if (outputStream == null) return;

        try {
            // 协议：4字节 float x, 4字节 float y, 1字节 action
            // 总共 9 字节
            ByteBuffer buffer = ByteBuffer.allocate(9);
            buffer.putFloat(x);
            buffer.putFloat(y);
            buffer.put((byte) action);
            
            outputStream.write(buffer.array());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            // 连接可能已断开
            runOnUiThread(() -> {
                statusText.setText("状态：发送失败，可能已断开");
                connectButton.setEnabled(true);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
