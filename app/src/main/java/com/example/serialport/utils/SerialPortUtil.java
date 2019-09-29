package com.example.serialport.utils;

import android.util.Log;

import com.example.serialport.model.MessageCommand;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import android_serialport_api.SerialPort;

/**
 * @Author:
 * @Date: Created in 10:26 2019/2/28
 * @Description:
 */
public class SerialPortUtil {

    private final String TAG = "SerialPortUtil";

    private String mPath;
    private int mBaudRate;
    public boolean mAutoSend = false;


    public SerialPort mSerialPort;
    public InputStream mInputStream;
    public OutputStream mOutputStream;

    private ReadThread mReadThread;
    //串口状态 true为打开
    public boolean mSerialPortStatus = false;

    public SerialPort openSerialPort(String path, int baudRate) {
        mPath = path;
        mBaudRate = baudRate;
        try {
            mSerialPort = new SerialPort(new File(path), baudRate, 0);
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
            mSerialPortStatus = true;
            mReadThread = new ReadThread();
            mReadThread.start();
            EventBus.getDefault().post(new MessageCommand(mPath, "开启串口"));

        } catch (IOException e) {
            e.printStackTrace();
            EventBus.getDefault().post(new MessageCommand(mPath, "串口打开异常"));
        }
        return mSerialPort;
    }

    public void closeSerialPort() {
        try {
            if (mSerialPortStatus) {
                mInputStream.close();
                mOutputStream.close();
                mSerialPort.close();
                mReadThread.interrupt();
                this.mSerialPortStatus = false;
                Log.i("DDDD", mPath + "串口已关闭");
            }
        } catch (IOException e) {
            Log.e(TAG, "closeSerialPort: 关闭串口异常：" + e.toString());
        }
    }

    public void sendSerialPort(String data) {
        try {
            byte[] sendData = data.getBytes();
            if (sendData.length > 0) {
                mOutputStream.write(sendData);
                mOutputStream.write('\n');
                mOutputStream.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, "发送数据错误:" + e.toString());
        }
    }

    public class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                if (mInputStream == null) {
                    return;
                }
                try {
                    if (mInputStream.available() > 0) {
                        byte[] recData = new byte[64];
                        int size = mInputStream.read(recData);
                        Log.d("DDDD", "Received[]：" + Arrays.toString(recData));
                        if (size > 0) {
                            StringBuffer sb = new StringBuffer();
                            for (byte b : recData) {
                                sb.append((char) b);
                            }
                            EventBus.getDefault().post(new MessageCommand(mPath, sb.toString()));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ReadThread1 extends Thread {

        @Override
        public void run() {
            super.run();
            // 定义一个包的最大长度
            int maxLength = 64;
            byte[] buffer = new byte[maxLength];
            // 每次收到实际长度
            int available = 0;
            // 当前已经收到包的总长度
            int currentLength = 0;
            // 协议头长度4个字节（开始符1，类型1，长度2）
            int headerLength = 4;

            while (!isInterrupted()) {
                try {
                    Log.i("DDDD", "RUN HERE 1");
                    available = mInputStream.available();
                    if (available > 0) {
                        // 防止超出数组最大长度导致溢出
                        Log.i("DDDD", "RUN HERE 2");
                        if (available > maxLength - currentLength) {
                            available = maxLength - currentLength;
                        }
                        mInputStream.read(buffer, currentLength, available);
                        currentLength += available;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                int cursor = 0;
                // 如果当前收到包大于头的长度，则解析当前包
                while (currentLength >= headerLength) {
                    Log.i("DDDD", "RUN HERE 3");
                    // 取到头部第一个字节
                    if (buffer[cursor] != 0x0F) {
                        --currentLength;
                        ++cursor;
                        Log.i("DDDD", "RUN HERE 4");
                        continue;
                    }

                    int contentLenght = parseLen(buffer, cursor, headerLength);
                    // 如果内容包的长度大于最大内容长度或者小于等于0，则说明这个包有问题，丢弃
                    if (contentLenght <= 0 || contentLenght > maxLength - 5) {
                        Log.i("DDDD", "RUN HERE 5");
                        currentLength = 0;
                        break;
                    }
                    // 如果当前获取到长度小于整个包的长度，则跳出循环等待继续接收数据
                    int factPackLen = contentLenght + 5;
                    if (currentLength < contentLenght + 5) {
                        Log.i("DDDD", "RUN HERE 6");
                        break;
                    }

                    // 一个完整包即产生
                    // proceOnePacket(buffer,i,factPackLen);
                    onDataReceived(buffer, cursor, factPackLen);
                    currentLength -= factPackLen;
                    cursor += factPackLen;
                }
                // 残留字节移到缓冲区首
                if (currentLength > 0 && cursor > 0) {
                    System.arraycopy(buffer, cursor, buffer, 0, currentLength);
                }
            }
        }
    }

    public int parseLen(byte buffer[], int index, int headerLength) {

//      if (buffer.length - index < headerLength) { return 0; }
        byte a = buffer[index + 2];
        byte b = buffer[index + 3];
        int rlt = 0;
        if (((a >> 7) & 0x1) == 0x1) {
            rlt = (((a & 0x7f) << 8) | b);
        } else {
            char[] tmp = new char[2];
            tmp[0] = (char) a;
            tmp[1] = (char) b;
            String s = new String(tmp, 0, 2);
            rlt = Integer.parseInt(s, 16);
        }

        return rlt;
    }

    protected void onDataReceived(final byte[] buffer, final int index, final int packlen) {
        System.out.println("收到信息");
        Log.i("DDDD", "收到信息" + buffer.toString());
        byte[] buf = new byte[packlen];
        System.arraycopy(buffer, index, buf, 0, packlen);
        EventBus.getDefault().post(new MessageCommand(mPath, Arrays.toString(buf)));
    }

}
