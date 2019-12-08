package com.example.dell.software;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import org.greenrobot.eventbus.EventBus;
import android.util.Log;

import com.example.dell.software.bean.MessageEvent;
import com.example.dell.software.utils.Constant;

/**
 * 数据读写线程
 * 
 * @author Administrator
 * 
 */
public class ThreadReadWriterIOSocket implements Runnable {

	private static String TAG = "ThreadReadWriterIOSocket";
	private Socket client;
	private BufferedOutputStream out;
	private BufferedInputStream in;
	boolean isConnecting = false;
	private String cmd = "";

	public ThreadReadWriterIOSocket(Socket client) {
		this.client = client;
	}

	@Override
	public void run() {
		Log.e(TAG, "有客户端连接上");
		isConnecting = true;
		try {
			// 获取输入输出流
			out = new BufferedOutputStream(client.getOutputStream());
			in  = new BufferedInputStream(client.getInputStream());
	
			// 循环等待，接受PC端的命令
			while (isConnecting) {
				try {
					if (!client.isConnected()) {
						break;
					}
					// 读取命令
					cmd = readCMDFromSocket(in);	
					Log.e(TAG, "读取到PC发送的命令" + cmd);
					/* 根据命令分别处理数据 */
					if (cmd.equals(Constant.CONNECT)) {// 收到连接命令
						EventBus.getDefault().post(new MessageEvent(Constant.START));
						out.flush();
					} else if (cmd.equalsIgnoreCase(Constant.DISCONNECT)) {// 断开命令
						EventBus.getDefault().post(new MessageEvent(Constant.STOP));
						out.flush();
					}else if (cmd.equals(Constant.TAKEPHOTO)) {
						EventBus.getDefault().post(new MessageEvent(Constant.TAKEPHOTO));
						out.flush();
					}
					in.reset();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			out.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void cancel() {
		isConnecting = false;
	}

	public void writeData(byte[] data) {
		if (out != null) {
			try {
				out.write((data));
			} catch (IOException e) {
				Log.e(TAG, "输入输出异常");
				e.printStackTrace();
			}
		}
	}

	/* 读取命令 */
	public String readCMDFromSocket(InputStream in) {

		int MAX_BUFFER_BYTES = 2048;
		String msg = "";
		byte[] tempbuffer = new byte[MAX_BUFFER_BYTES];
		try {
			int numReadedBytes = in.read(tempbuffer, 0, tempbuffer.length);
			msg = new String(tempbuffer, 0, numReadedBytes, "utf-8");
			tempbuffer = null;
		} catch (Exception e) {
			Log.e(TAG, "readCMDFromSocket读数异常" + e.toString());
			EventBus.getDefault().post(new MessageEvent(Constant.DISCONNECT));
			e.printStackTrace();
		}
		return msg;
	}
}