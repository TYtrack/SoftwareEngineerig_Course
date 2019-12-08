package com.example.dell.software;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.example.dell.software.bean.MessageEvent;
import com.example.dell.software.utils.BitmapUtil;
import com.example.dell.software.utils.CameraUtil;
import com.example.dell.software.utils.Constant;

public class MainActivity extends Activity {

    private String TAG = "MainActivity";
    private int srcFrameWidth  = 1200 * 2;
    private int srcFrameHeight = 1200 * 2;
    private Camera camera = null;
    private boolean isOpen = false;
    private SurfaceView surfaceView;
    private ServiceBroadcastReceiver receiver = new ServiceBroadcastReceiver();
    private ServerSocket serverSocket = null;
    final int SERVER_PORT = 21897;
    private int times = 0;
    private CameraUtil cameraUtil;
    private ThreadReadWriterIOSocket threadSocket;
    // 摄像头前置/后置
    public static final int CAMERA_BACK = 0;
    public static final int CAMERA_FRONT = 1;
    private int curCameraIndex = CAMERA_BACK;
    private SurfaceHolder surfaceHolder;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        EventBus.getDefault().register(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction("NotifyServiceStart");
        filter.addAction("NotifyServiceStop");
        registerReceiver(receiver, filter);

        initSurfaceView();

        Log.e(TAG, "onCreate");
    }

    private void initSurfaceView() {
        surfaceView = (SurfaceView) findViewById(R.id.surview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder arg0) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder arg0) {
                // 开启摄像头
                startCamera(curCameraIndex);
            }

            @Override
            public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
                                       int arg3) {

            }
        });
        surfaceView.setFocusable(true);
        surfaceView.setBackgroundColor(TRIM_MEMORY_BACKGROUND);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.message) {
            case Constant.DISCONNECT:

                Toast.makeText(MainActivity.this, "客户端断开", Toast.LENGTH_LONG)
                        .show();
                threadSocket.cancel();

            case Constant.START:

                isOpen = true;
                Toast.makeText(MainActivity.this, "客户端连接上", Toast.LENGTH_LONG)
                        .show();
                startCamera(curCameraIndex);

                break;
            case Constant.STOP:

                stopCamera();
                isOpen = false;

                break;
            case Constant.TAKEPHOTO:

                if (isOpen) {
                    camera.takePicture(new ShutterCallback() {

                        @Override
                        public void onShutter() {

                        }
                    }, new Camera.PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] arg0, Camera arg1) {

                        }
                    }, new Camera.PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {

                            // 向电脑端发送数据
                            int datalen = data.length;
                            if (isOpen) {
                                // 写入头
                                sendData(new byte[] { (byte) 0xA1 });
                                // 写入数组长度
                                sendData(intToByteArray(datalen));
                                // 写入数据值
                                sendData(data);
                            }

                            // 重新浏览
                            camera.stopPreview();
                            camera.startPreview();
                        }
                    });

                }

        }
    }

    // 根据索引初始化摄像头
    @SuppressLint("NewApi")
    public void startCamera(int cameraIndex) {

        // 先停止摄像头
        stopCamera();
        // 再初始化并打开摄像头
        if (camera == null) {
            camera = Camera.open(cameraIndex);
            cameraUtil = new CameraUtil(camera, callback);
            cameraUtil.initCamera(srcFrameHeight, srcFrameWidth, surfaceHolder);
            Log.e(TAG, "打开相机");
        }
    }

    // 停止并释放摄像头
    public void stopCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    //摄像头打开成功后采集到的数据回调
    PreviewCallback callback = new PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Size size = camera.getParameters().getPreviewSize();
            try {
                if (times == 0) {
                    YuvImage image = new YuvImage(data, ImageFormat.NV21,
                            size.width, size.height, null);
                    if (image != null) {
                        // 将YuvImage对象转为字节数组
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(0, 0, size.width,
                                size.height), 100, outputStream);
                        byte[] srcData = outputStream.toByteArray();
                        int len = srcData.length;
                        // 字节数组转为Bitmap
                        Bitmap src = BitmapFactory.decodeByteArray(srcData, 0,
                                len);

                        // Log.e(TAG, "旋转角度  = " + degree);
                        src = BitmapUtil.rotate(src, 90);
                        // 压缩Bitmap，并获取压缩后的字节数组
                        byte[] outdata = BitmapUtil.transImage(src,
                                srcFrameWidth / 4, srcFrameHeight / 4);
                        int datalen = outdata.length;

                        if (isOpen) {
                            // 写入头
                            sendData(new byte[] { (byte) 0xA0 });
                            // 写入数组长度
                            sendData(intToByteArray(datalen));
                            // 写入数据值
                            sendData(outdata);
                        }

                        // 回收Bitmap
                        if (!src.isRecycled()) {
                            src.recycle();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 监听返回按键处理
     */
    private long exitTime = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN) {

            if (System.currentTimeMillis() - exitTime > 2000) {
                // 再按一次程序退出部分
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                stopCamera();
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void sendData(final byte[] data) {
        threadSocket.writeData(data);
    }

    public class ServiceBroadcastReceiver extends BroadcastReceiver {

        private static final String START_ACTION = "NotifyServiceStart";
        private static final String STOP_ACTION = "NotifyServiceStop";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (START_ACTION.equalsIgnoreCase(action)) {
                // 启动服务
                Log.e(TAG, "收到广播信息启动监听");
                new Thread() {
                    public void run() {

                        if (serverSocket != null) {
                            try {
                                serverSocket.close();
                            } catch (IOException e) {

                                e.printStackTrace();
                            }
                        }

                        doListen();
                    };
                }.start();

            } else if (STOP_ACTION.equalsIgnoreCase(action)) {

            }
        }
    }

    // 启动服务器端监听
    private void doListen() {
        serverSocket = null;
        try {
            Log.e(TAG, "NMSL1");
            serverSocket = new ServerSocket(3242);
            Log.e(TAG, "NMSL2");
            while (true) {
                Socket socket = serverSocket.accept();
                Log.e(TAG, "监听到设备连接，启动通信线程");
                threadSocket = new ThreadReadWriterIOSocket(socket);
                new Thread(threadSocket).start();
            }
        } catch (IOException e) {
            Log.e(TAG, "服务端监听失败");
            e.printStackTrace();
        }
    }

    //int转byte数组
    public static byte[] intToByteArray(int a) {
        return new byte[] { (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF), (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF) };
    }

    @Override
    protected void onStop() {
        stopCamera();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
