package com.example.serialport;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.example.serialport.model.MessageCommand;
import com.example.serialport.utils.SerialPortUtil;
import com.excellence.basetoolslibrary.databinding.BaseRecyclerBindingAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android_serialport_api.SerialPortFinder;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Spinner mSerialSpinner;

    private EditText mBaudrateEditText,mSendEditText;

    private Button mOpenButton,mCloseButton,mSendButton;

    private RecyclerView mRecyclerView;


//    private ActivityMainBinding mBinding;

    private SerialPortUtil mSerialUtil;

    private String[] mSerialPath;

    private String mPath;

    private HashMap<String, SerialPortUtil> mSerialMap = new HashMap<>();

    private ArrayList<MessageCommand> mMessageList = new ArrayList<>();

    private BaseRecyclerBindingAdapter<MessageCommand> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        initView();
    }

    private void initView() {
        mSerialSpinner = (Spinner) findViewById(R.id.sp_serial);
        mBaudrateEditText = (EditText) findViewById(R.id.et_baudrate);
        mOpenButton = (Button) findViewById(R.id.btn_open);
        mCloseButton = (Button) findViewById(R.id.btn_close);
        mSendButton = (Button) findViewById(R.id.btn_send);
        mSendEditText = (EditText) findViewById(R.id.et_send);
        mRecyclerView = (RecyclerView) findViewById(R.id.rv_show);


        SerialPortFinder finder = new SerialPortFinder();
        //串口号
        String[] mSerialNames = finder.getAllDevices();
        mSerialPath = finder.getAllDevicesPath();
        if (mSerialPath.length > 0) {
            mPath = mSerialPath[0];
        }
        //设置串口下拉列表
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, mSerialNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSerialSpinner.setAdapter(adapter);
        mSerialSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPath = mSerialPath[position];
                if (mSerialMap.containsKey(mPath)) {
                    mSerialUtil = mSerialMap.get(mPath);
                    mOpenButton.setEnabled(false);
                    mCloseButton.setEnabled(true);
                } else {
                    mCloseButton.setEnabled(false);
                    mOpenButton.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        //信息打印
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new BaseRecyclerBindingAdapter<>(mMessageList, R.layout.command_recyclerveiw_item, BR.message);
        mRecyclerView.setAdapter(mAdapter);

        mOpenButton.setOnClickListener(this);
        mCloseButton.setOnClickListener(this);
        mSendButton.setOnClickListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageReceived(MessageCommand message) {
        mMessageList.add(message);
        mAdapter.notifyItemInserted(mMessageList.size() - 1);
        mRecyclerView.scrollToPosition(mMessageList.size() - 1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open:
                openSerialPort();
                break;
            case R.id.btn_close:
                closeSerialPort();
                break;
            case R.id.btn_send:
                mSerialUtil.mAutoSend = false;
                sendData();
                break;
        }
    }

    //打开串口
    private void openSerialPort() {

        if (!mSerialMap.containsKey(mPath)) {
            mSerialUtil = new SerialPortUtil();
            mSerialUtil.openSerialPort(mPath, Integer.valueOf(mBaudrateEditText.getText().toString()));
            mSerialMap.put(mPath, mSerialUtil);
            mCloseButton.setEnabled(true);
            mOpenButton.setEnabled(false);

        } else {
            mSerialUtil = mSerialMap.get(mPath);
        }
    }

    //发送数据
    private void sendData() {
        String data = mSendEditText.getText().toString();
        if (!TextUtils.isEmpty(data) && mSerialUtil.mSerialPortStatus) {
            mSerialUtil.sendSerialPort(data);
            EventBus.getDefault().post(new MessageCommand(mPath, data));
//            mBinding.etSend.setText("");

            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    //关闭串口
    private void closeSerialPort() {
        if (mSerialUtil.mSerialPortStatus) {
            mSerialUtil.closeSerialPort();
            mSerialUtil.mSerialPortStatus = false;
            mSerialMap.remove(mPath);
            mCloseButton.setEnabled(false);
            mOpenButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (mSerialUtil != null && mSerialUtil.mSerialPortStatus) {
            mSerialUtil.mSerialPortStatus = false;
            try {
                mSerialUtil.mOutputStream.close();
                mSerialUtil.mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

}
