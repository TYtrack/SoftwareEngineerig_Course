package com.example.dell.software.utils;

import java.io.IOException;
import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.view.SurfaceHolder;

public class CameraUtil {
	Camera camera;
	int cameraIndex;
	int srcFrameWidth;
	int srcFrameHeight;
	SurfaceHolder surfaceHolder;
	PreviewCallback callback;

	public CameraUtil(Camera camera, PreviewCallback callback) {
		this.camera = camera;
		this.callback = callback;
	}

	
	public void initCamera(final int srcFrameWidth, final int srcFrameHeight, final SurfaceHolder surfaceHolder) {
		
		this.srcFrameHeight = srcFrameHeight;
		this.srcFrameWidth = srcFrameWidth;
		this.surfaceHolder = surfaceHolder;
		Camera.Parameters params = camera.getParameters();
		//params.setPreviewSize(srcFrameWidth / 4, srcFrameHeight / 4);
		params.setPreviewFormat(ImageFormat.NV21);
		params.setPreviewFrameRate(30);
		params.setJpegQuality(100);
		params.setPictureFormat(ImageFormat.JPEG);
		params.set("orientation", "portrait");
		params.set("rotation", 90);
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 1连续对焦
		camera.setParameters(params);
		camera.setDisplayOrientation(90);
		// 设置显示图像的SurfaceView
		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		camera.setPreviewCallback(callback);
		camera.startPreview();
		camera.autoFocus(new AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean result, Camera camera) {
				
				// 自动对焦完成时回调
				if (result) {
					initCamera(srcFrameWidth, srcFrameHeight, surfaceHolder);
					camera.cancelAutoFocus();
				}
			}
		});
	}

	@SuppressLint("NewApi") 
	public void startCamera(int cameraIndex) {
		
		this.cameraIndex = cameraIndex;
		// 先停止摄像头
		stopCamera();
		// 再初始化并打开摄像头
		if (camera == null) {
			camera = Camera.open(cameraIndex);
			initCamera(srcFrameWidth, srcFrameHeight, surfaceHolder);
			
		}
	}

	public void stopCamera() {
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}
}
