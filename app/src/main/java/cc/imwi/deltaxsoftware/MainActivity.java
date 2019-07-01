package cc.imwi.deltaxsoftware;

import android.app.Activity;
import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.greenrobot.eventbus.EventBus;

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

    ArrayAdapter<Integer> baudrateAdapter;
    Integer[] Baudrates = {9600, 115200};
    int baudrate = 9600;

    HandleDataChangeInterface handleDataChangeInterface;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //handleDataChangeInterface = (HandleDataChangeInterface)getApplicationContext();

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
                    }
                    hideKeyboard(v);
                }
            }
        });
    }

    void InitData()
    {
        baudrateAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_spinner_dropdown_item, Baudrates);
        spinnerBaudrate.setAdapter(baudrateAdapter);
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
}

