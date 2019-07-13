package cc.imwi.deltaxsoftware;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends FragmentActivity implements OnPositionDataPass {

    final static String IMG_TAG = "IMG_TAG";
    final static int MAX_PROGRESS_SEEK_BAR = 100;

    public int CurrentX = 0;
    public int CurrentY = 0;
    public int CurrentZ = 0;
    public int CurrentW = 0;
    public int Feedrate = 0;

    TextView tvConnectionStatus;
    TextView tvTerminal;
    EditText etInputGcode;
    Button btSendData;

    EditText etXPos;
    EditText etYPos;
    EditText etZPos;
    EditText etWPos;
    EditText etFeedrate;

    TextView tvX;
    TextView tvY;
    TextView tvZ;
    TextView tvW;
    TextView tvFeedrate;

    VerticalSeekBar sbZPosition;
    PositionFrag positionFrag;

    Button btGo;
    Button btHome;
    Button btAdd;
    Switch swLoop;

    EditText etGcode;
    Spinner spinnerGcode;
    ImageButton btPlay;
    ToggleButton tbProtocolSwitch;

    HandleDataChangeInterface handleDataChangeInterface;
    UsbService usbService;
    MyHandler mHandler;
    BluetoothSPP bluetooth;

    ArrayAdapter<String> gcodeCommandAdapter;
    String[] CommandArray = {"G01", "G02", "G03", "G28", "M03", "M04", "M05", "M204"};
    String CurrentCommand = "G01";
    String LastCommand = "G01";
    int IndexOfEnter = 0;
    int OldIndexOfEnter = 0;
    int LastIndexOfEnter = 0;
    String rawCode;
    String Gcode = "";
    boolean isPlaying = false;
    boolean isEnableEditTextChangeListener = true;
    boolean loop = false;

    boolean isEnableBluetooth = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBluetooth();
        InitWidget();
        InitActionFromWidget();
        InitData();
    }

    void initBluetooth() {

        bluetooth = new BluetoothSPP(this);

        bluetooth.setAutoConnectionListener(new BluetoothSPP.AutoConnectionListener() {
            @Override
            public void onAutoConnectionStarted() {
                Log.i("Check", "Auto menu_connection started");
            }

            @Override
            public void onNewConnection(String name, String address) {
                Log.i("Check", "New Connection - " + name + " - " + address);
            }
        });
        bluetooth.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {
                handleSerialData(message);

            }
        });

        bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            @Override
            public void onDeviceConnected(String name, String address) {

                tvConnectionStatus.setText("Connected to: " + name);

                bluetooth.send("IsDelta", true);
            }

            @Override
            public void onDeviceDisconnected() {
                tvConnectionStatus.setText("connection failed");
            }

            @Override
            public void onDeviceConnectionFailed() {
                tvConnectionStatus.setText("Status: not connect");
            }
        });
    }

    void turnOnBluetooth() {
        if (!bluetooth.isBluetoothEnabled()) {
            bluetooth.enable();
        } else {
            if (!bluetooth.isServiceAvailable()) {
                bluetooth.setDeviceTarget(BluetoothState.DEVICE_OTHER);
                bluetooth.setupService();
                bluetooth.startService(BluetoothState.DEVICE_OTHER);
            }
        }

        if (!bluetooth.isBluetoothAvailable()) {
            tvConnectionStatus.setText("Bluetooth is not available");
        }

        if (bluetooth.getServiceState() == BluetoothState.STATE_CONNECTED) {
            bluetooth.disconnect();
        } else {
            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void handlePositionChange(int x, int y) {
        setCurrentXYPosition(x, y);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }

    void InitWidget() {
        tvConnectionStatus = (TextView) findViewById(R.id.tv_connection_status);
        tvTerminal = (TextView) findViewById(R.id.tv_terminate);
        etInputGcode = (EditText) findViewById(R.id.et_input_gcode);
        btSendData = (Button) findViewById(R.id.bt_send_data);

        etXPos = (EditText) findViewById(R.id.et_x_pos);
        etYPos = (EditText) findViewById(R.id.et_y_pos);
        etZPos = (EditText) findViewById(R.id.et_z_pos);
        etWPos = (EditText) findViewById(R.id.et_w_pos);
        etFeedrate = (EditText) findViewById(R.id.et_feedrate);

        tvX = findViewById(R.id.tv_x_pos);
        tvY = findViewById(R.id.tv_y_pos);
        tvZ = findViewById(R.id.tv_z_pos);
        tvW = findViewById(R.id.tv_w_pos);
        tvFeedrate = findViewById(R.id.tv_feedrate);

        sbZPosition = (VerticalSeekBar) findViewById(R.id.sb_z_pos);

        btGo = (Button) findViewById(R.id.bt_go);
        btHome = (Button) findViewById(R.id.bt_home);
        btAdd = (Button) findViewById(R.id.bt_add_pos);
        swLoop = (Switch) findViewById(R.id.sw_loop);

        etGcode = (EditText) findViewById(R.id.et_gcode);
        spinnerGcode = (Spinner) findViewById(R.id.spinner_gcode);
        btPlay = (ImageButton) findViewById(R.id.img_bt_play);
        tbProtocolSwitch = findViewById(R.id.tb_bluetooth);

        if (findViewById(R.id.fragment_container) != null) {
            positionFrag = new PositionFrag();
            positionFrag.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, positionFrag).commit();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    void InitActionFromWidget() {

        tvTerminal.setMovementMethod(new ScrollingMovementMethod());

        tbProtocolSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    turnOnBluetooth();
                    isEnableBluetooth = true;
                } else {
                    bluetooth.stopService();
                    isEnableBluetooth = false;
                }
            }
        });
        spinnerGcode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LastCommand = CurrentCommand;
                CurrentCommand = CommandArray[position];
                ((TextView) parent.getChildAt(0)).setTextSize(14);
                switch (CurrentCommand) {
                    case "G01":
                        G01(true);
                        isEnableEditTextChangeListener = true;
                        break;
                    case "G02":
                        G02(true);
                        isEnableEditTextChangeListener = false;
                        break;
                    case "G03":
                        G03(true);
                        isEnableEditTextChangeListener = false;
                        break;
                    case "G28":
                        G28(true);
                        isEnableEditTextChangeListener = false;
                        break;
                    case "M03":
                        M03(true);
                        isEnableEditTextChangeListener = false;
                        break;
                    case "M04":
                        M04(true);
                        isEnableEditTextChangeListener = false;
                        break;
                    case "M05":
                        M05(true);
                        isEnableEditTextChangeListener = false;
                        break;
                    case "M204":
                        M204(true);
                        isEnableEditTextChangeListener = false;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sbZPosition.setMax(MAX_PROGRESS_SEEK_BAR);
        sbZPosition.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                CurrentZ = -229 - progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateZPos();
            }
        });

        sbZPosition.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Gcode = "G01 Z" + CurrentZ + "\n";
                    SendGcode(Gcode);
                    if (!CurrentCommand.equals("G01")) {
                        G01(true);
                        spinnerGcode.setSelection(0, true);
                    }
                    return true;
                }
                return false;
            }
        });

        swLoop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                loop = isChecked;
            }
        });

        etXPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (v.getId() == R.id.et_x_pos) {
                        if (isEnableEditTextChangeListener) {
                            CurrentX = Integer.parseInt(etXPos.getText().toString());
                            updateFragment(CurrentX, CurrentY);
                            Gcode = "G01 X" + Integer.toString(CurrentX) + "\n";
                            SendGcode(Gcode);
                        }

                    }
                    hideKeyboard(v);
                }
            }
        });

        etYPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (v.getId() == R.id.et_y_pos) {
                        if (isEnableEditTextChangeListener) {
                            CurrentY = Integer.parseInt(etYPos.getText().toString());
                            updateFragment(CurrentX, CurrentY);
                            Gcode = "G01 Y" + Integer.toString(CurrentY) + "\n";
                            SendGcode(Gcode);
                        }
                    }
                    hideKeyboard(v);
                }
            }
        });

        etZPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (v.getId() == R.id.et_z_pos) {
                        if (isEnableEditTextChangeListener) {
                            CurrentZ = Integer.parseInt(etZPos.getText().toString());
                            sbZPosition.setProgress(-229 - CurrentZ);

                            if (usbService != null) {
                                //int z = -229 - CurrentZ;
                                Gcode = "G01 Z" + CurrentZ + "\n";
                                SendGcode(Gcode);
                            }
                        }
                    }
                    hideKeyboard(v);
                }
            }
        });

        etWPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (v.getId() == R.id.et_w_pos) {
                        if (isEnableEditTextChangeListener) {
                            CurrentW = Integer.parseInt(etWPos.getText().toString());
                            Gcode = "G01 W" + CurrentW + "\n";
                            SendGcode(Gcode);
                        }
                    }
                    hideKeyboard(v);
                }
            }
        });

        etFeedrate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (isEnableEditTextChangeListener) {
                        Feedrate = Integer.parseInt(etFeedrate.getText().toString());
                        Gcode = "G01 F" + Feedrate + "\n";
                        SendGcode(Gcode);
                    }
                    hideKeyboard(v);
                }
            }
        });

        etInputGcode.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (v.getId() == R.id.et_input_gcode) {
                        hideKeyboard(v);
                    }
                }
            }
        });

        etGcode.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                etGcode.setText("");
                return true;
            }
        });

        btSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etInputGcode.getText().toString() + "\n";
                SendGcode(text);
            }
        });

        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CurrentX = 0;
                CurrentY = 0;
                CurrentZ = -229;

                updateFragment(0, 0);
                etXPos.setText("0");
                etYPos.setText("0");
                etZPos.setText("-229");
                sbZPosition.setProgress(0);

                String home = "G28\n";
                SendGcode(home);
            }
        });

        btGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gcode = "G01 X" + CurrentX + " Y" + CurrentY + " Z" + CurrentZ + "\n";
                SendGcode(Gcode);
            }
        });

        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (CurrentCommand) {
                    case "G01":
                        G01(false);
                        etGcode.append(Gcode);
                        break;
                    case "G02":
                        G02(false);
                        etGcode.append(Gcode);
                        break;
                    case "G03":
                        G03(false);
                        etGcode.append(Gcode);
                        break;
                    case "G28":
                        G28(false);
                        etGcode.append(Gcode);
                        break;
                    case "M03":
                        M03(false);
                        etGcode.append(Gcode);
                        break;
                    case "M04":
                        M04(false);
                        etGcode.append(Gcode);
                        break;
                    case "M05":
                        M05(false);
                        etGcode.append(Gcode);
                        break;
                    case "M204":
                        M204(false);
                        etGcode.append(Gcode);
                        break;
                }
            }
        });

        btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isPlaying) {
                    isPlaying = true;
                    btPlay.setImageResource(android.R.drawable.ic_media_pause);
                    OldIndexOfEnter = 0;
                    LastIndexOfEnter = 0;
                    rawCode = etGcode.getText().toString();
                    IndexOfEnter = rawCode.indexOf('\n');
                    LastIndexOfEnter = rawCode.lastIndexOf('\n');
                    sendGcodeLine(rawCode);
                } else {
                    isPlaying = false;
                    btPlay.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        });
    }

    void InitData() {
        gcodeCommandAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, CommandArray);
        spinnerGcode.setAdapter(gcodeCommandAdapter);
        CurrentCommand = "G01";
        mHandler = new MyHandler(this);

    }

    public void setCurrentXYPosition(int x, int y) {

        CurrentX = x;
        CurrentY = y;

        etXPos.setText(Integer.toString(CurrentX));
        etYPos.setText(Integer.toString(CurrentY));

        Gcode = "G01 X" + CurrentX + " Y" + CurrentY + "\n";
        SendGcode(Gcode);

        if (!CurrentCommand.equals("G01")) {
            G01(true);
            spinnerGcode.setSelection(0, true);
        }

    }

    public void updateZPos() {
        etZPos.setText(Integer.toString(CurrentZ));
    }

    void updateFragment(int x, int y) {
        handleDataChangeInterface.handleDataChange(x, y);
    }

    void SendGcode(String gcode) {
        String editedGcode;
        if (!gcode.endsWith("\n")) {
            editedGcode = gcode + '\n';
        } else {
            editedGcode = gcode;
        }

        if (!isEnableBluetooth) {
            if (usbService != null) {
                usbService.write(editedGcode.getBytes());
                tvTerminal.append(editedGcode);
            }
        } else {
            bluetooth.send(editedGcode, true);
        }
    }

    public void setHandleDataChangeInterface(HandleDataChangeInterface handleDataChangeInterface) {
        this.handleDataChangeInterface = handleDataChangeInterface;
    }

    void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
            if (usbService != null) {
                String connected = "Connected";
                tvConnectionStatus.setText(connected);
                usbService.write("Position\n".getBytes());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;

            String disconnect = "Disconnected";
            tvConnectionStatus.setText(disconnect);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bluetooth.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetooth.setupService();
                bluetooth.startService(BluetoothState.DEVICE_ANDROID);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth was not enabled.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    mActivity.get().handleSerialData(msg.toString());
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    public void handleSerialData(String _msg) {
        String data = _msg;
        if (data.indexOf('k') > 0) {
            if (isPlaying) {
                this.sendGcodeLine(rawCode);
            }
        }
        if (data.charAt(data.length() - 1) != '\n') {
            data += "\r\n";
        }

        tvTerminal.append(data);
    }

    public void sendGcodeLine(String rawCode) {
        if (IndexOfEnter == LastIndexOfEnter) {
            Gcode = rawCode.substring(OldIndexOfEnter, IndexOfEnter + 1);

            SendGcode(Gcode);

            if (loop) {
                IndexOfEnter = rawCode.indexOf('\n', 0);
                OldIndexOfEnter = 0;
            } else {
                isPlaying = false;
                btPlay.setImageResource(android.R.drawable.ic_media_play);
                OldIndexOfEnter = 0;
            }
            return;
        }
        Gcode = rawCode.substring(OldIndexOfEnter, IndexOfEnter + 1);
        SendGcode(Gcode);
        OldIndexOfEnter = IndexOfEnter + 1;
        IndexOfEnter = rawCode.indexOf('\n', OldIndexOfEnter);

    }

    void isLoop(boolean l) {
        if (l) {
            IndexOfEnter = 0;
            LastIndexOfEnter = 0;
        } else {
            isPlaying = false;
            btPlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    void G01(boolean init) {
        if (init) {
            tvX.setText("X");
            tvY.setText("Y");
            tvZ.setText("Z");
            tvW.setText("W");
            tvFeedrate.setText("F");

            etXPos.setText(Integer.toString(CurrentX));
            etYPos.setText(Integer.toString(CurrentY));
            etZPos.setText(Integer.toString(CurrentZ));
            etWPos.setText(Integer.toString(CurrentW));
            etFeedrate.setText(Integer.toString(Feedrate));

        } else {
            Gcode = "G01 X";
            if (Feedrate != Integer.parseInt(etFeedrate.getText().toString())) {
                Feedrate = Integer.parseInt(etFeedrate.getText().toString());
                String appendText = CurrentX + " Y" + CurrentY + " Z" + CurrentZ + " F" + Feedrate + "\n";
                Gcode += appendText;
                SendGcode(Gcode);
            } else {
                String appendText = CurrentX + " Y" + CurrentY + " Z" + CurrentZ + "\n";
                Gcode += appendText;
                SendGcode(Gcode);
            }
        }
    }

    void G02(boolean init) {
        if (init) {
            tvX.setText("F");
            tvY.setText("I");
            tvZ.setText("J");
            tvW.setText("X");
            tvFeedrate.setText("Y");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");
        } else {
            Gcode = "G02";
            if (!etXPos.getText().toString().equals("")) {
                Gcode += " F";
                Gcode += etXPos.getText().toString();
            }
            if (!etYPos.getText().toString().equals("")) {
                Gcode += " I";
                Gcode += etYPos.getText().toString();
            }
            if (!etZPos.getText().toString().equals("")) {
                Gcode += " J";
                Gcode += etZPos.getText().toString();
            }
            if (!etWPos.getText().toString().equals("")) {
                Gcode += " X";
                Gcode += etWPos.getText().toString();
            }
            if (!etFeedrate.getText().toString().equals("")) {
                Gcode += " Y";
                Gcode += etFeedrate.getText().toString();
            }
            Gcode += "\n";
            SendGcode(Gcode);
        }
    }

    void G03(boolean init) {
        if (init) {
            tvX.setText("F");
            tvY.setText("I");
            tvZ.setText("J");
            tvW.setText("X");
            tvFeedrate.setText("Y");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");
        } else {
            Gcode = "G03";
            if (!etXPos.getText().toString().equals("")) {
                Gcode += " F";
                Gcode += etXPos.getText().toString();
            }
            if (!etYPos.getText().toString().equals("")) {
                Gcode += " I";
                Gcode += etYPos.getText().toString();
            }
            if (!etZPos.getText().toString().equals("")) {
                Gcode += " J";
                Gcode += etZPos.getText().toString();
            }
            if (!etWPos.getText().toString().equals("")) {
                Gcode += " X";
                Gcode += etWPos.getText().toString();
            }
            if (!etFeedrate.getText().toString().equals("")) {
                Gcode += " Y";
                Gcode += etFeedrate.getText().toString();
            }
            Gcode += "\n";
            SendGcode(Gcode);
        }
    }

    void G28(boolean init) {
        if (init) {
            tvX.setText("");
            tvY.setText("");
            tvZ.setText("");
            tvW.setText("");
            tvFeedrate.setText("");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");
        } else {
            Gcode = "G28\n";
            SendGcode(Gcode);
        }
    }

    void M03(boolean init) {
        if (init) {
            tvX.setText("S");
            tvY.setText("");
            tvZ.setText("");
            tvW.setText("");
            tvFeedrate.setText("");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");

        } else {
            Gcode = "M03 S";
            Gcode += etXPos.getText().toString();
            Gcode += "\n";
            SendGcode(Gcode);
        }
    }

    void M04(boolean init) {
        if (init) {
            tvX.setText("S");
            tvY.setText("");
            tvZ.setText("");
            tvW.setText("");
            tvFeedrate.setText("");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");

        } else {
            Gcode = "M04 S";
            Gcode += etXPos.getText().toString();
            Gcode += "\n";
            SendGcode(Gcode);
        }
    }

    void M05(boolean init) {
        if (init) {
            tvX.setText("");
            tvY.setText("");
            tvZ.setText("");
            tvW.setText("");
            tvFeedrate.setText("");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");
        } else {
            Gcode = "M05\n";
            SendGcode(Gcode);
        }
    }

    void M204(boolean init) {
        if (init) {
            tvX.setText("A");
            tvY.setText("");
            tvZ.setText("");
            tvW.setText("");
            tvFeedrate.setText("");

            etXPos.setText("");
            etYPos.setText("");
            etZPos.setText("");
            etWPos.setText("");
            etFeedrate.setText("");
        } else {
            Gcode = "M204 A";
            Gcode += etXPos.getText().toString();
            Gcode += "\n";
            SendGcode(Gcode);
        }
    }
}

