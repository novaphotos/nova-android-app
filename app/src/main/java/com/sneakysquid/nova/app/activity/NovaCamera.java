package com.sneakysquid.nova.app.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.camera.PreviewFrameLayout;
import com.sneakysquid.nova.app.R;
import com.sneakysquid.nova.app.camera.PhotoSaver;
import com.sneakysquid.nova.app.camera.TakePhotoCommand;
import com.sneakysquid.nova.app.error.ErrorReporter;
import com.sneakysquid.nova.app.error.ModalErrorReporter;
import com.sneakysquid.nova.app.ui.CameraPreview;
import com.sneakysquid.nova.app.ui.FlashSettingsDialog;
import com.sneakysquid.nova.app.util.CustomOrientationHandler;
import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLink;
import com.sneakysquid.nova.link.NovaLinkStatus;
import com.sneakysquid.nova.link.NovaLinkStatusCallback;
import com.sneakysquid.nova.link.BluetoothLENovaLink;

import java.io.File;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.sneakysquid.nova.link.Debug.assertOnUiThread;

/**
 * The main camera UI for the Nova Camera app.
 */
public class NovaCamera extends Activity implements NovaLinkStatusCallback, Observer, FlashSettingsDialog.FlashSettingsDialogCallback
{
    private static final String TAG = "NovaCamera";

    // workflow related info
    private boolean stillImageIntent;
    private boolean imageCaptureIntent;
    private Uri imageCaptureOutputUri;
    private boolean secureCamera;

    // camera info
    private Camera camera;
    private int cameraId;
    private boolean cameraConfigured;
    private int preferredCamera;
    private int orientation;
    private byte[] lastPhoto;

    // ui controls
    private PreviewFrameLayout previewFrame;
    private CameraPreview preview;
    private FlashSettingsDialog flashSettingsDialog;

    // camera / flash logic
    private NovaLink novaLink;
    private PhotoSaver photoSaver;
    private ErrorReporter errorReporter;
    private CustomOrientationHandler customOrientationHandler;
    private NovaFlashCommand flashCmd = NovaFlashCommand.off();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate(): " + getIntent());
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //set content view AFTER ABOVE sequence (to avoid crash)
        setContentView(R.layout.activity_nova_camera);

        errorReporter = new ModalErrorReporter();

        customOrientationHandler = new CustomOrientationHandler(this);
        customOrientationHandler.addObserver(this);

        novaLink = new BluetoothLENovaLink(this);
        novaLink.registerStatusCallback(this);

        photoSaver = new PhotoSaver(this, errorReporter);

        // todo: cache this
        preferredCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
        
        checkIntent();

        setupUi();
    }

    private void checkIntent()
    {
        Intent intent = getIntent();

        if (intent == null)
        {
            return;
        }

        String action = intent.getAction();

        if (action == null)
        {
            return;
        }

        if (action.equals("android.media.action.STILL_IMAGE_CAMERA_SECURE") ||
            action.equals("android.media.action.STILL_IMAGE_CAMERA"))
        {
            stillImageIntent = true;
        }
        else if (action.equals("android.media.action.IMAGE_CAPTURE_SECURE") ||
                 action.equals("android.media.action.IMAGE_CAPTURE"))
        {
            imageCaptureIntent = true;
            extractCaptureOutputFile(intent);
        }

        if (action.equals("android.media.action.STILL_IMAGE_CAMERA_SECURE") ||
            action.equals("android.media.action.IMAGE_CAPTURE_SECURE"))
        {
            secureCamera = true;
        }
    }

    private void extractCaptureOutputFile(Intent intent)
    {
        Bundle bundle = intent.getExtras();

        if (bundle == null)
        {
            imageCaptureFailed();
            return;
        }

        imageCaptureOutputUri = bundle.getParcelable(MediaStore.EXTRA_OUTPUT);

        if (imageCaptureOutputUri == null)
        {
            imageCaptureFailed();
            return;
        }

        Log.d(TAG, "extra output: " + imageCaptureOutputUri.getPath());
    }

    private void imageCaptureFailed()
    {
        Log.e(TAG, "Image capture intent called, but unable to write to provided output folder.");
        setResult(RESULT_CANCELED);
        finish();
    }

    private void setupUi()
    {
        preview = (CameraPreview) findViewById(R.id.camera_preview);
        previewFrame = (PreviewFrameLayout) findViewById(R.id.preview_frame);
        preview.setPreviewFrameLayout(previewFrame);
        preview.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                hideFlashSettingsDialog();
            }
        });

        flashSettingsDialog = (FlashSettingsDialog) findViewById(R.id.flash_settings_dialog);
        flashSettingsDialog.setCallback(this);
        hideFlashSettingsDialog();

        ImageButton shutterButton = (ImageButton) findViewById(R.id.shutter_button);
        ImageButton galleryButton = (ImageButton) findViewById(R.id.gallery_button);
        // todo: hide switch button on devices with only one camera
        ImageButton switchButton = (ImageButton) findViewById(R.id.switch_button);
        ImageButton modeButton = (ImageButton) findViewById(R.id.mode_button);
        ImageButton prefsButton = (ImageButton) findViewById(R.id.prefs_button);

        shutterButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onShutterClick();
            }
        });
        modeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onModeClick();
            }
        });
        switchButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                onSwitchClick();
            }
        });

        if (stillImageIntent || imageCaptureIntent)
        {
            prefsButton.setVisibility(View.INVISIBLE);
            galleryButton.setImageResource(R.drawable.cancel_icon_temp);
            galleryButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    onCancelClick();
                }
            });
        }
        else
        {
            prefsButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onPrefsClick();
                }
            });
            galleryButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    onGalleryClick();
                }
            });
        }

        if (secureCamera)
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

    private void hideFlashSettingsDialog()
    {
        if (flashSettingsDialog == null)
        {
            return;
        }
        
        flashSettingsDialog.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume(): " + getIntent());
        super.onResume();

        customOrientationHandler.enable();

        goFullscreen();
        initCamera();
        configureCamera();
        startPreview();
        novaLink.enable();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause()");

        customOrientationHandler.disable();

        novaLink.disable();
        stopPreview();
        releaseCamera();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if (secureCamera) {
            unregisterReceiver(screenOffReceiver);
        }

        super.onDestroy();
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

    private void initCamera()
    {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int i = 0; i < Camera.getNumberOfCameras(); i++)
        {
            Camera.getCameraInfo(i, cameraInfo);

            // Attempt to open preferred camera
            if (cameraInfo.facing == preferredCamera)
            {
                Log.d(TAG, "Camera.open(i=" + i + ")");
                try
                {
                    camera = Camera.open(i);
                }
                catch (RuntimeException ex)
                {
                    Log.e(TAG, ex.getStackTrace().toString());
                }
                cameraId = i;
                break;
            }
        }

        // Fall back on whatever is available
        if (camera == null)
        {
            Log.d(TAG, "Camera.open()");
            try
            {
                camera = Camera.open();
            }
            catch (RuntimeException ex)
            {
                Log.e(TAG, ex.getStackTrace().toString());
            }
            cameraId = 0;
        }

        if (camera == null)
        {
            Log.e(TAG, "unable to open camera");
        }
    }

    private void releaseCamera()
    {
        if (camera == null)
        {
            return;
        }

        Log.d(TAG, "camera.release()");
        camera.release();
        camera = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        // no-op
    }

    private Camera.Size getLargestPictureSize(Camera.Parameters parameters)
    {
        Camera.Size result = null;

        List<Camera.Size> dbg = parameters.getSupportedPictureSizes();

        for (Camera.Size size : parameters.getSupportedPictureSizes())
        {
            if (result == null)
            {
                result = size;
            } else
            {
                int resultArea = result.width * result.height;
                int newArea = size.width * size.height;

                if (newArea > resultArea)
                {
                    result = size;
                }
            }
        }

        return (result);
    }

    private void configureCamera()
    {
        if (camera == null)
        {
            Log.e(TAG, "Unable to configure camera: camera is null");
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size pictureSize = getLargestPictureSize(parameters);

        if (pictureSize != null)
        {
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
        }

        parameters.setPictureFormat(ImageFormat.JPEG);
        camera.setParameters(parameters);
        cameraConfigured = true;
    }

    private void startPreview()
    {
        if (!cameraConfigured)
        {
            Log.e(TAG, "Unable to start preview: camera is not configured");
            return;
        }

        preview.beginPreview(camera);
    }

    private void stopPreview()
    {
        preview.endPreview();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode){
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        switch(keyCode){
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                takePicture();
                return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void onShutterClick()
    {
        takePicture();
    }

    private void takePicture()
    {
        if (camera == null) {
            Toast.makeText(this, "Camera not found",
                Toast.LENGTH_SHORT).show();
        } else {
            // Take photo
            Runnable takePhoto = takePhotoCommand(flashCmd, cameraId, orientation, new PhotoHandler());
            takePhoto.run();
        }
    }

    private void onPrefsClick()
    {
        Intent i = new Intent(NovaCamera.this, Settings.class);
        startActivity(i);
    }

    private void onModeClick()
    {
        showFlashSettingsDialog();
    }

    private void showFlashSettingsDialog()
    {
        if (flashSettingsDialog == null)
        {
            return;
        }

        flashSettingsDialog.setVisibility(View.VISIBLE);
    }

    private void onSwitchClick()
    {
        if (preferredCamera == Camera.CameraInfo.CAMERA_FACING_BACK)
        {
            preferredCamera = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else if (preferredCamera == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            preferredCamera = Camera.CameraInfo.CAMERA_FACING_BACK;
        }

        stopPreview();
        restartPreview();
    }

    private void onGalleryClick()
    {
        Intent i = new Intent(NovaCamera.this, ViewPhoto.class);
        startActivity(i);
    }

    private void onCancelClick()
    {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    @Override
    public void onNovaLinkStatusChange(NovaLinkStatus status)
    {
        flashSettingsDialog.setStatus(status);
    }

    private TakePhotoCommand takePhotoCommand(NovaFlashCommand flashCmd, int cameraId, int orientation, TakePhotoCommand.Callback result)
    {
        return new TakePhotoCommand(this, camera, cameraId, orientation, novaLink, flashCmd, result);
    }

    @Override
    public void update(Observable observable, Object o)
    {
        if (observable == customOrientationHandler)
        {
            handleOrientation();
        }
    }

    private void handleOrientation()
    {
        int newOrientation = customOrientationHandler.getOrientation();

        if (newOrientation != orientation)
        {
            // todo: re-orient flash settings dialog
        }

        orientation = newOrientation;
    }

    @Override
    public void onOkClick()
    {
        hideFlashSettingsDialog();
    }

    @Override
    public void onTestClick()
    {
        Log.d(TAG, "test");
        if (novaLink.getStatus() == NovaLinkStatus.Ready)
        {
            novaLink.beginFlash(flashCmd);
        }
    }

    @Override
    public void onFlashSettingsChange(NovaFlashCommand flashCmd)
    {
        Log.d(TAG, "flash settings: " + flashCmd);
        this.flashCmd = flashCmd;
    }

    private class PhotoHandler implements TakePhotoCommand.Callback
    {
        @Override
        public void onPhotoTaken(byte[] jpeg)
        {
            assertOnUiThread();

            lastPhoto = jpeg;

            if (imageCaptureIntent)
            {
                File outputFile = null;

                outputFile = new File(Environment.getExternalStorageDirectory() + "/temp.jpeg");

                photoSaver.save(jpeg, outputFile);

                Intent intent = new Intent(NovaCamera.this, ReviewPhoto.class);
                intent.putExtra("photo-filename", outputFile.getAbsolutePath());
                intent.putExtra("secure-review", secureCamera);
                startActivityForResult(intent, ReviewPhoto.REVIEW_PHOTO_REQUEST);
            }
            else // normal workflow, still image workflow
            {
                // Save
                File saved = photoSaver.save(jpeg);

                if (saved != null)
                {
                    Toast.makeText(NovaCamera.this, "Photo " + saved.getName() + " saved.",
                        Toast.LENGTH_LONG).show();

                    // Set the currently displayed file to the most recently taken photo
                    SharedPreferences preferences = getSharedPreferences(ViewPhoto.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(ViewPhoto.DISPLAYED_FILE_PREF, saved.getAbsolutePath());
                    editor.commit();
                }

                // Trigger the media scanner to scan the newly taken photo
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(saved)));

                // Start preview again (android stops it when a picture is taken)
                restartPreview();
            }
        }
    }

    private void restartPreview()
    {
        // Release and re-initialize camera is needed to avoid the preview
        // becoming sluggish after second call to startPreview.
        releaseCamera();
        initCamera();
        configureCamera();
        startPreview();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_CANCELED)
        {
            Log.d(TAG, "cancelled");
            setResult(resultCode);
            finish();
        }
        else if (resultCode == ReviewPhoto.REVIEW_PHOTO_RESULT_RETAKE)
        {
            Log.d(TAG, "retake photo");
            restartPreview();
        }
        else if (resultCode == ReviewPhoto.REVIEW_PHOTO_RESULT_USE)
        {
            Log.d(TAG, "use photo");
            if (imageCaptureOutputUri != null)
            {
                photoSaver.save(lastPhoto, imageCaptureOutputUri, getContentResolver());
                setResult(RESULT_OK);
            }
            else
            {
                setResult(RESULT_CANCELED);
            }

            finish();
        }
    }

    // close activity when screen turns off. only used in secure workflows.
    private BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
}
