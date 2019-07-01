package cc.imwi.deltaxsoftware;

import android.app.Activity;
import android.content.*;
import android.inputmethodservice.Keyboard;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends FragmentActivity implements OnPositionDataPass{

    final static String IMG_TAG =  "IMG_TAG";
    final static int MAX_PROGRESS_SEEK_BAR = 100;

    public int CurrentX = 0;
    public int CurrentY = 0;
    public int CurrentZ = 0;
    public int CurrentW = 0;

    TextView tvConnectionStatus;
    Spinner spinnerBaudrate;
    TextView tvTerminal;
    EditText etInputGcode;
    Button btSendData;

    EditText etXPos;
    EditText etYPos;
    EditText etZPos;
    EditText etWPos;

    VerticalSeekBar sbZPosition;
    PositionFrag positionFrag;

    Button btGo;
    Button btHome;
    Button btAdd;
    Switch swLoop;

    EditText etGcode;

    HandleDataChangeInterface handleDataChangeInterface;

    ArrayAdapter<Integer> baudrateAdapter;
    Integer[] Baudrates = {9600, 115200};
    int baudrate = 9600;

    String Gcode = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitWidget();
        InitActionFromWidget();
        InitData();
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
        setCurrentXYPosition(x,y);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
    }

    void InitWidget()
    {
        tvConnectionStatus = (TextView) findViewById(R.id.tv_connection_status);
        spinnerBaudrate = (Spinner) findViewById(R.id.spinner_baudrate);
        tvTerminal = (TextView) findViewById(R.id.tv_terminate);
        etInputGcode = (EditText)findViewById(R.id.et_input_gcode);
        btSendData = (Button) findViewById(R.id.bt_send_data);

        etXPos = (EditText) findViewById(R.id.et_x_pos);
        etYPos = (EditText) findViewById(R.id.et_y_pos);
        etZPos = (EditText) findViewById(R.id.et_z_pos);
        etWPos = (EditText) findViewById(R.id.et_w_pos);

        sbZPosition = (VerticalSeekBar) findViewById(R.id.sb_z_pos);

        btGo = (Button) findViewById(R.id.bt_go);
        btHome = (Button) findViewById(R.id.bt_home);
        btAdd = (Button) findViewById(R.id.bt_add_pos);
        swLoop = (Switch) findViewById(R.id.sw_loop);

        etGcode = (EditText) findViewById(R.id.et_gcode);

        if (findViewById(R.id.fragment_container) != null) {
            positionFrag = new PositionFrag();
            positionFrag.setArguments(getIntent().getExtras());

            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, positionFrag).commit();
        }
    }

    void InitActionFromWidget()
    {
        tvTerminal.setMovementMethod(new ScrollingMovementMethod());
        spinnerBaudrate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                {
                    baudrate = 9600;
                    SerialReconnect(baudrate);
                }
                if(position == 1)
                {
                    baudrate = 115200;
                    SerialReconnect(baudrate);
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
                CurrentZ = sbZPosition.progress;
                //Toast.makeText(getApplicationContext(), "seek bar progress:" + progress,Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateZPos();
            }
        });

        etXPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                {
                    if(v.getId() == R.id.et_x_pos)
                    {
                        CurrentX = Integer.parseInt(etXPos.getText().toString());
                        updateFragment(CurrentX,CurrentY);
                        Gcode = "G01 X" + Integer.toString(CurrentX) + "\n";
                        //if(Gcode.charAt(Gcode.length() - 1) == )
                        if(usbService != null)
                        {
                            usbService.write(Gcode.getBytes());
                        }
                        tvTerminal.append(Gcode);

                    }
                    hideKeyboard(v);
                }
            }
        });

        etYPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                {
                    if(v.getId() == R.id.et_y_pos)
                    {
                        CurrentY = Integer.parseInt(etYPos.getText().toString());
                        updateFragment(CurrentX, CurrentY);
                    }
                    hideKeyboard(v);
                }
            }
        });

        etWPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                {
                    if(v.getId() == R.id.et_w_pos)
                    {
                        CurrentW = Integer.parseInt(etWPos.getText().toString());
                    }
                    hideKeyboard(v);
                }
            }
        });

        etZPos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                {
                    if(v.getId() == R.id.et_z_pos)
                    {
                        CurrentZ = Integer.parseInt(etZPos.getText().toString());
                        sbZPosition.setProgress(CurrentZ);

                        if(usbService != null)
                        {
                            int z = -229 - CurrentZ;
                            Gcode = "G01 Z" + z + "\n";
                            usbService.write(Gcode.getBytes());
                        }
                    }
                    hideKeyboard(v);
                }
            }
        });
        btSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etInputGcode.getText().toString() + "\n";
                tvTerminal.append(text);
                if(usbService != null)
                {
                    usbService.write(text.getBytes());
                }
            }
        });
        btHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String home = "G28\n";
                tvTerminal.append(home);
                if(usbService != null)
                {
                    usbService.write(home.getBytes());
                }
            }
        });
        btGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Gcode = "G01 X" + CurrentX + " Y" + CurrentY + "\n";
                tvTerminal.append(Gcode);
                if(usbService != null)
                {
                    usbService.write(Gcode.getBytes());
                }
            }
        });
    }

    void InitData()
    {
        baudrateAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_dropdown_item, Baudrates);
        spinnerBaudrate.setAdapter(baudrateAdapter);
        mHandler = new MyHandler(this);
    }

    public void setCurrentXYPosition(int x, int y)
    {
        CurrentX = x;
        CurrentY = y;

        etXPos.setText(Integer.toString(CurrentX));
        etYPos.setText(Integer.toString(CurrentY));
    }

    void SerialReconnect(int baudrate)
    {

    }

    public void updateZPos()
    {
        etZPos.setText(Integer.toString(CurrentZ));
    }

    void updateFragment(int x, int y)
    {
        handleDataChangeInterface.handleDataChange(x,y);
    }

    public void setHandleDataChangeInterface(HandleDataChangeInterface handleDataChangeInterface) {
        this.handleDataChangeInterface = handleDataChangeInterface;
    }

    void hideKeyboard(View view)
    {
        InputMethodManager imm = (InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
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

    private UsbService usbService;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
            if(usbService!=null)
            {
                tvConnectionStatus.setText("Connected");
                usbService.write("Position\n".getBytes());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
            tvConnectionStatus.setText("Disconnected");
        }
    };

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

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().tvTerminal.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}

