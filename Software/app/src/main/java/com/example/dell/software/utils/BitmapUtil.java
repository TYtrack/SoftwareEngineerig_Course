package com.example.dell.software.utils;

import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;

public class BitmapUtil {

	// Bitmap按照一定大小转为字节数组，以便写入socket进行发送
	public static byte[] transImage(Bitmap bitmap, int width, int height) {
		// bitmap = adjustPhotoRotation(bitmap, 90);
		try {
			int bitmapWidth = bitmap.getWidth();
			int bitmapHeight = bitmap.getHeight();
			float scaleWidth = (float) width / bitmapWidth;
			float scaleHeight = (float) height / bitmapHeight;
			Matrix matrix = new Matrix();
			matrix.postScale(scaleWidth, scaleHeight);
			// 创建压缩后的Bitmap
			Bitmap resizeBitemp = Bitmap.createBitmap(bitmap, 0, 0,
					bitmapWidth, bitmapHeight, matrix, false);
			
			// 压缩图片质量
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			resizeBitemp.compress(CompressFormat.JPEG, 80, outputStream);
			// 转为字节数组
			byte[] byteArray = outputStream.toByteArray();
			outputStream.close();

			// 回收资源
			if (!bitmap.isRecycled()) {
				bitmap.recycle();
			}
			if (!resizeBitemp.isRecycled()) {
				resizeBitemp.recycle();
			}
			return byteArray;

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	
	public static Bitmap rotate(Bitmap bitmap, float degree) {
		Matrix matrix = new Matrix();
       // matrix.setScale(0.5f, 0.5f);// 缩小为原来的一半
        matrix.postRotate(degree);// 旋转45度 == matrix.setSinCos(0.5f, 0.5f);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        return bitmap;
	}
	

}
