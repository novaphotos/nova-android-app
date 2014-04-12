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
    protected int layoutRotation;

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
        int newLayoutRotation = 0;

        if (orientation == 0)
        {
            newLayoutRotation = 0;
        }
        else if (orientation == 90)
        {
            newLayoutRotation = -90;
        }
        else if (orientation == 180)
        {
            newLayoutRotation = 180;
        }
        else if (orientation == 270)
        {
            newLayoutRotation = 90;
        }

        if (newLayoutRotation != layoutRotation)
        {
            Rotate(newLayoutRotation);
        }
    }

    private void Rotate(int newLayoutRotation)
    {
        if (layoutRotation == 180 && newLayoutRotation == -90)
        {
            newLayoutRotation = 270;
        }

        if (layoutRotation == 270 && newLayoutRotation == 0)
        {
            newLayoutRotation = 360;
        }

        if (layoutRotation == -90 && newLayoutRotation == 180)
        {
            newLayoutRotation = -180;
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "rotation", (float) layoutRotation, (float) newLayoutRotation);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        if (newLayoutRotation == 360)
        {
            newLayoutRotation = 0;
        }

        layoutRotation = newLayoutRotation;
    }
}
