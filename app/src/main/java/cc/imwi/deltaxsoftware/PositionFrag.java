package cc.imwi.deltaxsoftware;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;

public class PositionFrag extends Fragment implements HandleDataChangeInterface{

    final static int HALF_IMAGE_WIDTH = 40;
    final static  int HALF_IMAGE_HEIGHT = 40;

    final static int HALF_MAX_WIDTH = 220;
    final static int HALF_MAX_HEIGHT = 220;

    public int local_x = 0;
    public int local_y = 0;
    float dX;
    float dY;
    int x_cord;
    int y_cord;
    int CurrentX = 0;
    int CurrentY = 0;
    int lastAction;
    String msg = "TAG";

    ImageView imageView;
    ConstraintLayout.LayoutParams constraintLayoutParams;

    OnPositionDataPass onPositionDataPass;

    public static PositionFrag newInstance() {
        return new PositionFrag();
    }

    @Override
    public void handleDataChange(int x, int y) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onPositionDataPass = (OnPositionDataPass) context;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.position_fragment, container, false);

        imageView = (ImageView) view.findViewById(R.id.img);

        imageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipData.Item item = new ClipData.Item((CharSequence)v.getTag());
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};

                ClipData dragData = new ClipData(v.getTag().toString(),mimeTypes, item);
                View.DragShadowBuilder myShadow = new View.DragShadowBuilder(imageView);

                v.startDrag(dragData,myShadow,null,0);
                return true;
            }
        });

        imageView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                switch(event.getAction())
                {
                    case DragEvent.ACTION_DRAG_STARTED:
                        constraintLayoutParams = (ConstraintLayout.LayoutParams)v.getLayoutParams();
                        Log.i(msg, "Action is DragEvent.ACTION_DRAG_STARTED");
                        x_cord = (int) event.getX();
                        y_cord = (int) event.getY();
                        local_x = x_cord - v.getWidth()/2;
                        local_y = y_cord -  v.getHeight()/2;
                        constraintLayoutParams.leftMargin = x_cord;
                        constraintLayoutParams.topMargin = y_cord;
                        v.setLayoutParams(constraintLayoutParams);
                        break;

                    case DragEvent.ACTION_DRAG_ENTERED:
                        //((MainActivity)getActivity()).setCurrentXYPosition(local_x, local_y);
                        break;

                    case DragEvent.ACTION_DRAG_EXITED:
                        x_cord = (int) event.getX();
                        y_cord = (int) event.getY() - 76;
                        constraintLayoutParams.leftMargin = x_cord;
                        constraintLayoutParams.topMargin = y_cord;
                        v.setLayoutParams(constraintLayoutParams);
                        break;

                    case DragEvent.ACTION_DRAG_LOCATION:
                        x_cord = (int) event.getX() - HALF_IMAGE_WIDTH;
                        y_cord = (int) event.getY() - HALF_IMAGE_HEIGHT;
                        constraintLayoutParams.leftMargin = (int) v.getX() + (x_cord);
                        constraintLayoutParams.topMargin = (int) v.getY() + (y_cord);
                        v.setLayoutParams(constraintLayoutParams);
                        break;

                    case DragEvent.ACTION_DRAG_ENDED:
                        break;

                    case DragEvent.ACTION_DROP:
                        x_cord = (int)event.getX();
                        y_cord = (int)event.getY();
                        local_x = x_cord - HALF_IMAGE_WIDTH;
                        local_y = y_cord - HALF_IMAGE_HEIGHT;
                        v.setX(local_x);
                        v.setY(local_y);
                        Log.i(msg, "Action is DragEvent.ACTION_DROP" + local_x + "-" + local_y);
                        break;

                    default:
                        break;
                }
                return true;
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        lastAction = MotionEvent.ACTION_DOWN;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        view.setY(event.getRawY() + dY);
                        view.setX(event.getRawX() + dX);
                        lastAction = MotionEvent.ACTION_MOVE;
                        break;

                    case MotionEvent.ACTION_UP:
                        int x_pos = (int)(event.getRawX() + dX);
                        int y_pos = (int)(event.getRawY() + dY);
                        int _currentX = x_pos + HALF_IMAGE_WIDTH;
                        int _currentY = y_pos + HALF_IMAGE_HEIGHT;
                        ConvertToDeltaCoordinates(_currentX, _currentY);
                        onPositionDataPass.handlePositionChange(CurrentX, CurrentY);
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });

        ((MainActivity)getActivity()).setHandleDataChangeInterface(new HandleDataChangeInterface() {
            @Override
            public void handleDataChange(int x, int y) {
                ConvertToFragmentCoordinates(x,y);
                x_cord = local_x;
                y_cord = local_y;
                imageView.setX((float)x_cord);
                imageView.setY((float)y_cord);
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        InitData();
    }

    void InitData(){
        onPositionDataPass.handlePositionChange(this.CurrentX, this.CurrentY);
    }

    void ConvertToFragmentCoordinates(int _x, int _y)
    {
        local_x = HALF_MAX_WIDTH + _x;
        local_y = HALF_MAX_HEIGHT - _y;
    }

    void ConvertToDeltaCoordinates(int _x, int _y){
        CurrentX = _x - HALF_MAX_WIDTH;
        CurrentY = HALF_MAX_HEIGHT - _y;
    }

}
