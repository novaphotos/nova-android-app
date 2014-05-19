package com.sneakysquid.nova.app.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import com.sneakysquid.nova.app.util.CustomOrientationHandler;

import java.util.Observable;
import java.util.Observer;

/**
 * A layout that will always right itself depending on the device orientation, with animation.
 * Intended for use in activities that are fixed to portrait or landscape mode.
 */
public class GravityLayout extends FrameLayout implements Observer
{
    private static final String TAG = "GravityLayout";
    protected CustomOrientationHandler customOrientationHandler;
    protected int currentOrientation;

    public GravityLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        sharedConstructor(context);
    }

    public GravityLayout(Context context)
    {
        super(context);

        sharedConstructor(context);
    }

    private void sharedConstructor(Context context)
    {
        customOrientationHandler = new CustomOrientationHandler(context);
        customOrientationHandler.addObserver(this);
        customOrientationHandler.enable();
    }

    @Override
    public void update(Observable observable, Object o)
    {
        if (observable == customOrientationHandler)
        {
            handleCustomOrientation(customOrientationHandler.getOrientation());
        }
    }

    private void handleCustomOrientation(int orientation)
    {
        int convertedOrientation = 0;

        if (orientation == 0)
        {
            convertedOrientation = 0;
        }
        else if (orientation == 90)
        {
            convertedOrientation = -90;
        }
        else if (orientation == 180)
        {
            convertedOrientation = 180;
        }
        else if (orientation == 270)
        {
            convertedOrientation = 90;
        }

        if (convertedOrientation != currentOrientation)
        {
            Rotate(convertedOrientation);
        }
    }

    private void Rotate(int newOrientation)
    {
        if (currentOrientation == 180 && newOrientation == -90)
        {
            newOrientation = 270;
        }

        if (currentOrientation == 270 && newOrientation == 0)
        {
            newOrientation = 360;
        }

        if (currentOrientation == -90 && newOrientation == 180)
        {
            newOrientation = -180;
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "rotation", (float) currentOrientation, (float) newOrientation);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        if (newOrientation == 360)
        {
            newOrientation = 0;
        }

        currentOrientation = newOrientation;
    }
}
