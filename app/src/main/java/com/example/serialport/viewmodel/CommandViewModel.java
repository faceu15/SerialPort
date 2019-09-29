package com.example.serialport.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.List;

/**
 * @Author:
 * @Date: Created in 15:22 2019/3/6
 * @Description:
 */
public class CommandViewModel extends ViewModel {

    private MutableLiveData<List<String>> mCommandList = new MutableLiveData<List<String>>();

    public void addCommand(String command){
        mCommandList.getValue().add(command);
    }

}
