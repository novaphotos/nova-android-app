package com.sneakysquid.nova.app.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.view.OrientationEventListener;

import com.sneakysquid.nova.link.NovaFlashCallback;
import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLink;
import com.sneakysquid.nova.link.NovaLinkStatus;

import static com.sneakysquid.nova.util.Debug.assertOnUiThread;
import static com.sneakysquid.nova.util.Debug.debug;

/**
 * Runnable task that implements the asynchronous flow of steps required to take a photo.
 * <p/>
 * These are (each step can take some time):
 * <ul>
 * <li>Begin auto-focusing camera</li>
 * <li>Wait for focus, then trigger flash</li>
 * <li>Wait for flash ack, then take picture</li>
 * <li>Wait for JPEG, then trigger external PictureCallback</li>
 * </ul>
 *
 * @author Joe Walnes
 */
public class TakePhotoCommand implements Runnable, Camera.AutoFocusCallback, NovaFlashCallback, Camera.ShutterCallback, Camera.PictureCallback {
    private static final String TAG = "TakePhotoCommand";
    private final Activity activity;
    private final Camera camera;
    private int cameraId;
    private int orientation;
    private final NovaLink novaLink;
    private final NovaFlashCommand flashCmd;
    private final Callback result;

    public interface Callback {
        void onPhotoTaken(byte[] jpeg);
    }

    public TakePhotoCommand(Activity activity,
                            Camera camera,
                            int cameraId,
                            int orientation,
                            NovaLink novaLink,
                            NovaFlashCommand flashCmd,
                            Callback result) {
        this.activity = activity;
        this.camera = camera;
        this.cameraId = cameraId;
        this.orientation = orientation;
        this.novaLink = novaLink;
        this.flashCmd = flashCmd;
        this.result = result;
    }

    @Override
    public void run() {
        takePhoto();
    }

    private void takePhoto() {
        debug("takePhoto()");
        assertOnUiThread();

        // Step 1: Auto-focus
        camera.autoFocus(this); // -> callback: onAutoFocus()
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        debug("onAutoFocus(%s)", success);
        assertOnUiThread();

        // TODO: Handle success==false

        // Play auto-focus complete sound. Preference for this?
        (new MediaActionSound()).play(MediaActionSound.FOCUS_COMPLETE);

        // Step 1: Auto-focus DONE.
        triggerFlash();
    }

    @SuppressWarnings("ConstantConditions")
    private void triggerFlash() {
        debug("triggerFlash(%s)", flashCmd);
        assertOnUiThread();

        if (flashCmd == null || flashCmd.isPointless() || novaLink == null
            || novaLink.getStatus() != NovaLinkStatus.Ready) {
            // Flash not needed, or not possible. Skip to step 3 and just take the photo.
            takePicture();
        } else {
            // Step 2: Trigger flash
            novaLink.flash(flashCmd, this); // -> callback: onNovaFlashAcknowledged
        }
    }

    @Override
    public void onNovaFlashAcknowledged(boolean success) {
        debug("onNovaFlashAcknowledged(%s)", success);
        assertOnUiThread();

        // Step 2: Trigger flash DONE
        // TODO: Handle success==false

        takePicture();
    }

    private void takePicture() {
        debug("takePicture()");
        assertOnUiThread();

        // Step 3: Take picture

        // Set rotation value for picture about to be taken
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(translateOrientation(orientation));
        camera.setParameters(parameters);

        camera.takePicture(this, null, this); // -> callback: onShutter(), and onPictureTaken()
    }

    public int translateOrientation(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0;

        android.hardware.Camera.CameraInfo info =
            new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        orientation = (orientation + 45) / 90 * 90;

        int rotation = 0;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (info.orientation - orientation + 360) % 360;
        } else {  // back-facing camera
            rotation = (info.orientation + orientation) % 360;
        }

        return rotation;
    }

    @Override
    public void onShutter() {
        debug("onShutter()");
        assertOnUiThread();

        // TODO: Shutter finished. Deactivate flash ASAP
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        debug("onPictureTaken()");
        assertOnUiThread();

        // Step 3: Take picture DONE

        // Step 4: Call result
        result.onPhotoTaken(data);
    }

    private void delay(final int millis, final Runnable task) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                }
                activity.runOnUiThread(task);
            }
        }).start();
    }

}
