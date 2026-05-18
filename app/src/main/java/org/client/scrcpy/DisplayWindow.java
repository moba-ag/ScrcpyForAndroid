package org.client.scrcpy;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class DisplayWindow extends FrameLayout {
    private static final String TAG = "DisplayWindow";
    OnClickListener closeListener;
    OnMoveCallback moveCallback;
    OnActionCallback actionCallback;
    OnTouchListener onDisplayTouchListener;

    private float oldX;
    private float oldY;

    ViewGroup header;
    ViewGroup container;
    SurfaceView surfaceView;
    ViewGroup actionbar;

    public DisplayWindow(Context context) {
        super(context);
        init();
    }

    public DisplayWindow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DisplayWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        setClipChildren(false);
        setClipToPadding(false);
        LayoutInflater.from(getContext()).inflate(R.layout.window_display,this,true);

        container = findViewById(R.id.container);
        surfaceView = findViewById(R.id.surface);
        actionbar = findViewById(R.id.actionbar);

        findViewById(R.id.iv_close).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
                    if (closeListener != null) {
                        closeListener.onClick(view);
                    }
                }
                return false;
            }
        });

        header = findViewById(R.id.header);
        header.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final int action = motionEvent.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        oldX = motionEvent.getRawX();
                        oldY = motionEvent.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float disX = motionEvent.getRawX()-oldX;
                        float disY = motionEvent.getRawY()-oldY;
                        if (moveCallback != null) {
                            moveCallback.onMove(disX, disY);
                        }
                        oldX = motionEvent.getRawX();
                        oldY = motionEvent.getRawY();
                        break;
                }
                return true;
            }
        });

        findViewById(R.id.iv_resize).setOnTouchListener(new OnTouchListener() {
            private float startX, startY;
            private int startWidth, startHeight;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        startWidth = container.getWidth();
                        startHeight = container.getHeight();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - startX;
                        int newWidth = (int) (startWidth + deltaX);
                        if (newWidth < 200) newWidth = 200;

                        // Maintain aspect ratio based on width
                        float ratio = (float) startWidth / startHeight;
                        int newHeight = (int) (newWidth / ratio);

                        ViewGroup.LayoutParams lp = container.getLayoutParams();
                        lp.width = newWidth;
                        lp.height = newHeight;
                        container.setLayoutParams(lp);

                        ViewGroup.LayoutParams lp2 = header.getLayoutParams();
                        lp2.width = newWidth;
                        header.setLayoutParams(lp2);

                        requestLayout();
                        return true;
                }
                return false;
            }
        });

        findViewById(R.id.iv_mini).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
                    if (container.isShown()){
                        container.setVisibility(View.GONE);
                        actionbar.setVisibility(View.GONE);
                    }else{
                        container.setVisibility(View.VISIBLE);
                        actionbar.setVisibility(View.VISIBLE);
                    }
                }
                return false;
            }
        });
        findViewById(R.id.action_back).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
                    actionCallback.onAction(0);
                }
                return false;
            }
        });
        findViewById(R.id.action_home).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
                    actionCallback.onAction(1);
                }
                return false;
            }
        });
        findViewById(R.id.action_menu).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN){
                    actionCallback.onAction(2);
                }
                return false;
            }
        });
        container.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return onDisplayTouchListener.onTouch(view,motionEvent);
            }
        });
    }

    public void setCloseListener(OnClickListener closeListener) {
        this.closeListener = closeListener;
    }

    public void setMoveCallback(OnMoveCallback moveCallback) {
        this.moveCallback = moveCallback;
    }

    public void setActionCallback(OnActionCallback actionCallback) {
        this.actionCallback = actionCallback;
    }

    public void setOnDisplayTouchListener(OnTouchListener onDisplayTouchListener) {
        this.onDisplayTouchListener = onDisplayTouchListener;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setRemote(int w, int h) {
        if (w <= 0 || h <= 0) return;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        display.getRealMetrics(metrics);

        final float maxAllowedHeight = metrics.heightPixels * 0.8f;
        final float maxAllowedWidth = metrics.widthPixels * 0.8f;

        post(new Runnable() {
            @Override
            public void run() {
                float remote_aspect_ratio = (float) w / h;
                int targetWidth, targetHeight;

                // Try to scale by height first
                targetHeight = (int) (maxAllowedHeight - actionbar.getMeasuredHeight() - header.getMeasuredHeight());
                targetWidth = (int) (targetHeight * remote_aspect_ratio);

                // If it's too wide, scale by width instead
                if (targetWidth > maxAllowedWidth) {
                    targetWidth = (int) maxAllowedWidth;
                    targetHeight = (int) (targetWidth / remote_aspect_ratio);
                }

                ViewGroup.LayoutParams lp = container.getLayoutParams();
                lp.width = targetWidth;
                lp.height = targetHeight;
                container.setLayoutParams(lp);

                ViewGroup.LayoutParams lp2 = header.getLayoutParams();
                lp2.width = targetWidth;
                header.setLayoutParams(lp2);

                requestLayout();
            }
        });
    }

    public void hideHintTip(){
        findViewById(R.id.hint).setVisibility(GONE);
    }

    public Surface getDisplaySurface(){
        return surfaceView.getHolder().getSurface();
    }

    public int getSurfaceWidth(){
        return container.getMeasuredWidth();
    }

    public int getSurfaceHeight(){
        return container.getMeasuredHeight();
    }

    public interface OnMoveCallback {
        void onMove(float x,float y);
    }
    public interface OnActionCallback{
        void onAction(int actionType);
    }
}
