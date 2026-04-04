package com.bluetooth.touchpad;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothTouchpad";
    private static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String SERVICE_NAME = "BluetoothTouchpad";
    
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int BLUETOOTH_ENABLE_CODE = 2;

    private BluetoothAdapter bluetoothAdapter;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    
    private View touchView;
    private TextView statusText;
    private Button connectButton;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        touchView = findViewById(R.id.touchView);
        statusText = findViewById(R.id.statusText);
        connectButton = findViewById(R.id.connectButton);
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (bluetoothAdapter == null) {
            showToast("设备不支持蓝牙");
            statusText.setText("错误：不支持蓝牙");
            return;
        }
        
        checkPermissions();
        
        connectButton.setOnClickListener(v -> {
            if (connectedThread != null && connectedThread.isConnected()) {
                disconnect();
            } else {
                startAcceptThread();
            }
        });
        
        setupTouchListeners();
    }
    
    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            };
            
            boolean needRequest = false;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                        != PackageManager.PERMISSION_GRANTED) {
                    needRequest = true;
                    break;
                }
            }
            
            if (needRequest) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            } else {
                checkBluetoothEnabled();
            }
        } else {
            // For older Android versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    PERMISSION_REQUEST_CODE);
            } else {
                checkBluetoothEnabled();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                checkBluetoothEnabled();
            } else {
                showToast("需要权限才能使用蓝牙功能");
                statusText.setText("错误：权限被拒绝");
            }
        }
    }
    
    private void checkBluetoothEnabled() {
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "请启用蓝牙", Toast.LENGTH_LONG).show();
            // Note: enable() is deprecated in API 33+, user needs to enable manually
            statusText.setText("请先在系统设置中启用蓝牙");
        } else {
            statusText.setText("就绪，点击'等待连接'开始");
        }
    }
    
    private void startAcceptThread() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        
        acceptThread = new AcceptThread();
        acceptThread.start();
        statusText.setText("正在等待连接...");
        connectButton.setText("取消等待");
        showToast("等待蓝牙设备连接");
    }
    
    public synchronized void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        statusText.setText("已断开连接");
        connectButton.setText("等待连接");
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListeners() {
        touchView.setOnTouchListener((v, event) -> {
            if (connectedThread == null || !connectedThread.isConnected()) {
                return true;
            }
            
            int action = event.getActionMasked();
            float x = event.getX();
            float y = event.getY();
            
            // Get screen dimensions for normalization
            float width = v.getWidth();
            float height = v.getHeight();
            
            // Normalize coordinates to 0-1 range
            float normalizedX = x / width;
            float normalizedY = y / height;
            
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    sendTouchEvent('D', normalizedX, normalizedY, event.getPointerCount());
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    sendTouchEvent('M', normalizedX, normalizedY, event.getPointerCount());
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    sendTouchEvent('U', normalizedX, normalizedY, event.getPointerCount());
                    break;
                    
                case MotionEvent.ACTION_CANCEL:
                    sendTouchEvent('C', normalizedX, normalizedY, event.getPointerCount());
                    break;
            }
            
            return true;
        });
    }
    
    private void sendTouchEvent(char type, float x, float y, int pointerCount) {
        if (connectedThread == null) return;
        
        // Format: T(type)(x:0.000)(y:0.000)(pointers:00)
        // Example: TD0.50000.75001
        String message = String.format("T%c%.6f%.6f%02d", type, x, y, pointerCount);
        connectedThread.write(message.getBytes());
    }
    
    private void updateStatus(String status) {
        mainHandler.post(() -> {
            statusText.setText(status);
            if (status.contains("已连接")) {
                connectButton.setText("断开连接");
            }
        });
    }
    
    private void showToast(String message) {
        mainHandler.post(() -> 
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }
    
    // Server socket thread to accept connections
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;
        private boolean cancelled = false;
        
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME, SERVICE_UUID);
            } catch (IOException e) {
                e.printStackTrace();
                updateStatus("错误：无法创建服务器套接字");
            }
            serverSocket = tmp;
        }
        
        @Override
        public void run() {
            BluetoothSocket socket = null;
            
            while (!cancelled && serverSocket != null) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    if (!cancelled) {
                        e.printStackTrace();
                    }
                    break;
                }
                
                if (socket != null) {
                    synchronized (MainActivity.this) {
                        if (connectedThread != null) {
                            connectedThread.cancel();
                        }
                        connectedThread = new ConnectedThread(socket);
                        connectedThread.start();
                        
                        BluetoothDevice device = socket.getRemoteDevice();
                        updateStatus("已连接：" + device.getName());
                        showToast("设备已连接！");
                    }
                    
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        
        public void cancel() {
            cancelled = true;
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Thread for managing connected socket
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final OutputStream outputStream;
        private volatile boolean connected = true;
        
        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            OutputStream tmpOut = null;
            
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            outputStream = tmpOut;
        }
        
        @Override
        public void run() {
            // Keep connection alive, data is only sent from phone to computer
            try {
                while (connected) {
                    sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            connected = false;
            updateStatus("连接已断开");
        }
        
        public void write(byte[] bytes) {
            if (outputStream != null && connected) {
                try {
                    outputStream.write(bytes);
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    connected = false;
                    updateStatus("发送失败，连接可能已断开");
                }
            }
        }
        
        public boolean isConnected() {
            return connected && socket != null && socket.isConnected();
        }
        
        public void cancel() {
            connected = false;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}
