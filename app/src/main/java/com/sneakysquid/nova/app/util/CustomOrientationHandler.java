package com.sneakysquid.nova.app.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.OrientationEventListener;

import java.util.Observable;

/**
 * Translates from degrees in OrientationEventListener to screen orientation (0, 90, 180, 270).
 * Useful for situations where the layout is locked to portrait or landscape mode but the view
 * still needs to handle screen orientation changes. Note that this may not match the native
 * Android behavior completely.
 */
public class CustomOrientationHandler extends Observable implements Orientable
{
    private static final String TAG = "CustomOrientationHandler";
    protected OrientationEventListener orientationEventListener;
    protected Context context;

    protected int orientation;

    public CustomOrientationHandler(Context context)
    {
        this.context = context;
        setupListeners(context);
    }

    private void setupListeners(Context context)
    {
        PackageManager manager = context.getPackageManager();
        boolean hasAccelerometer = false;

        if (manager != null)
        {
            hasAccelerometer = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        }

        if (!hasAccelerometer)
        {
            Log.w(TAG, "No accelerometer, orientation events will not be handled");
            return;
        }

        orientationEventListener = new MyOrientationEventListener(context, this);
    }

    public void enable()
    {
        if (orientationEventListener == null)
        {
            return;
        }

        orientationEventListener.enable();
    }

    public void disable()
    {
        if (orientationEventListener == null)
        {
            return;
        }

        orientationEventListener.disable();
    }

    // Always returns one of: 0, 90, 180, 270
    public int getOrientation()
    {
        return orientation;
    }

    // Orientable implementation used by MyOrientationEventListener. Should not be
    // called from classes other than MyOrientationEventListener.
    @Override
    public void setOrientation(int orientation)
    {
        if (this.orientation != orientation)
        {
            this.orientation = orientation;
            setChanged();
            notifyObservers();
        }
    }

    private static class MyOrientationEventListener extends OrientationEventListener
    {
        private Orientable orientable;
        private int previousOrientation;

        // The orientation that the accelerometer is reporting.
        // Ignored until it is the same for ORIENTATION_CHANGE_THRESHOLD consecutive updates.
        protected int newOrientation;
        protected int orientationChangeCount;

        // Number of consecutive consistent orientation measurements that must be received before
        // an orientation change will be triggered.
        protected static final int ORIENTATION_CHANGE_THRESHOLD = 3;

        public MyOrientationEventListener(Context context, Orientable orientable)
        {
            super(context);
            newOrientation = -1;
            this.orientable = orientable;
        }

        @Override
        public void onOrientationChanged(int orientation)
        {
            int convertedOrientation = 0;

            // Note that if newOrientation == ORIENTATION_UNKNOWN the first condition will be true
            if (orientation >= 315 || orientation < 45)
            {
                convertedOrientation = 0;
            } else if (orientation >= 45 && orientation < 135)
            {
                convertedOrientation = 90;
            } else if (orientation >= 135 && orientation < 225)
            {
                convertedOrientation = 180;
            } else if (orientation >= 225 && orientation < 315)
            {
                convertedOrientation = 270;
            }

            // Prevents jittering back and forth when on the borderline
            if (Math.abs(orientation - previousOrientation) < 55)
            {
                return;
            }

            if (convertedOrientation == newOrientation)
            {
                orientationChangeCount++;
            }
            else
            {
                orientationChangeCount = 0;
                newOrientation = convertedOrientation;
            }

            if (orientationChangeCount <= ORIENTATION_CHANGE_THRESHOLD)
            {
                return;
            }

            orientable.setOrientation(convertedOrientation);
            previousOrientation = convertedOrientation;

            this.newOrientation = convertedOrientation;
            orientationChangeCount = 0;
        }
    }
}
