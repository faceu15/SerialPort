package com.example.serialport.model;

import android.arch.lifecycle.MutableLiveData;

/**
 * @Author:
 * @Date: Created in 10:36 2019/3/7
 * @Description:
 */
public class MessageCommand {

    private String mSerialPath;

    private String mCommand;

    public MessageCommand() {
    }

    public MessageCommand(String serialPath, String command) {
        this.mSerialPath = serialPath;
        mCommand = command;
    }

    public String getSerialPath() {
        return mSerialPath;
    }

    public void setSerialPath(String mSerialPath) {
        this.mSerialPath = mSerialPath;
    }

    public String getCommand() {
        return mCommand;
    }

    public void setCommand(String command) {
        mCommand = command;
    }
}
