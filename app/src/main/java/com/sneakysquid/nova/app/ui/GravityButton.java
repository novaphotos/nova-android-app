package com.sneakysquid.nova.app.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;

import com.sneakysquid.nova.app.util.CustomOrientationHandler;

import java.util.Observable;
import java.util.Observer;

/**
 * A button that will always right itself depending on the device orientation, with animation.
 * Intended for use in layouts that are fixed to portrait or landscape mode.
 */
public class GravityButton extends ImageButton implements Observer
{
    private static final String TAG = "GravityButton";
    protected CustomOrientationHandler customOrientationHandler;
    protected int buttonRotation;

    public GravityButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        sharedConstructor(context);
    }

    public GravityButton(Context context)
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
        int newButtonRotation = 0;

        if (orientation == 0)
        {
            newButtonRotation = 0;
        }
        else if (orientation == 90)
        {
            newButtonRotation = -90;
        }
        else if (orientation == 180)
        {
            newButtonRotation = 180;
        }
        else if (orientation == 270)
        {
            newButtonRotation = 90;
        }

        if (newButtonRotation != buttonRotation)
        {
            Rotate(newButtonRotation);
        }
    }

    private void Rotate(int newButtonRotation)
    {
        if (buttonRotation == 180 && newButtonRotation == -90)
        {
            newButtonRotation = 270;
        }

        if (buttonRotation == 270 && newButtonRotation == 0)
        {
            newButtonRotation = 360;
        }

        if (buttonRotation == -90 && newButtonRotation == 180)
        {
            newButtonRotation = -180;
        }

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "rotation", (float) buttonRotation, (float) newButtonRotation);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        if (newButtonRotation == 360)
        {
            newButtonRotation = 0;
        }

        buttonRotation = newButtonRotation;
    }
}
