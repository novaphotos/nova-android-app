package com.sneakysquid.nova.app.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.sneakysquid.nova.app.R;
import com.sneakysquid.nova.app.util.BitmapWorkerTask;

public class ReviewPhoto extends Activity {
    public static final int REVIEW_PHOTO_REQUEST = 1;
    public static final int REVIEW_PHOTO_RESULT_USE = 1001;
    public static final int REVIEW_PHOTO_RESULT_RETAKE = 1002;

    private String photoFilename;
    private boolean secureReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_review_photo);

        setupUi();
        goFullscreen();
    }

    @Override
    protected void onDestroy()
    {
        if (secureReview)
        {
            unregisterReceiver(screenOffReceiver);
        }

        super.onDestroy();
    }

    private void setupUi()
    {
        ImageView imageView = (ImageView)findViewById(R.id.review_image_view);

        if (imageView == null)
        {
            return;
        }

        extractIntentData();

        if (photoFilename == null || photoFilename.equals(""))
        {
            return;
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        new BitmapWorkerTask(imageView, photoFilename, width, height).execute();

        findViewById(R.id.use_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onUse();
            }
        });

        findViewById(R.id.retake_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onRetake();
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onCancel();
            }
        });

        if (secureReview)
        {
            // Change the window flags so that secure camera can show when locked
            Window win = getWindow();
            WindowManager.LayoutParams params = win.getAttributes();
            params.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
            win.setAttributes(params);

            // Filter for screen off so that we can finish secure camera activity
            // when screen is off.
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            registerReceiver(screenOffReceiver, filter);
        }
    }

    private void goFullscreen()
    {
        Window window = getWindow();
        View decorView = window.getDecorView();

        // hide navigation bar
        int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void onCancel()
    {
        setResultAndFinish(RESULT_CANCELED);
    }

    private void onRetake()
    {
        setResultAndFinish(ReviewPhoto.REVIEW_PHOTO_RESULT_RETAKE);
    }

    private void onUse()
    {
        setResultAndFinish(ReviewPhoto.REVIEW_PHOTO_RESULT_USE);
    }

    private void setResultAndFinish(int resultCode)
    {
        setResult(resultCode);
        finish();
    }

    private void extractIntentData()
    {
        Intent intent = getIntent();

        if (intent == null)
        {
            return;
        }

        photoFilename = intent.getStringExtra("photo-filename");
        secureReview = intent.getBooleanExtra("secure-review", true);
    }

    // close activity when screen turns off. only used in secure workflows.
    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ReviewPhoto.this.setResultAndFinish(RESULT_CANCELED);
        }
    };
}
