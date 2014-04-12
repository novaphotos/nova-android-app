/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * A layout which handles the preview aspect ratio.
 */
@SuppressLint("WrongCall") // Linter doesn't like explicit calls to .onXXX(), but we really want to do that here.
public class PreviewFrameLayout extends RelativeLayout implements LayoutChangeNotifier {

    private static final String TAG = "PreviewFrameLayout";

    private int horizontalShift = 0;

    /** A callback to be invoked when the preview frame's size changes. */
    public interface OnSizeChangedListener {
        public void onSizeChanged(int width, int height);
    }

    private double mAspectRatio;
    private OnSizeChangedListener mListener;
    private LayoutChangeHelper mLayoutChangeHelper;

    public PreviewFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAspectRatio(4.0 / 3.0);
        mLayoutChangeHelper = new LayoutChangeHelper(this);
    }

    public void setAspectRatio(double ratio) {
        if (ratio <= 0.0) throw new IllegalArgumentException();

        Log.d(TAG, "setAspectRatio(): " + ratio);

        if (mAspectRatio != ratio) {
            mAspectRatio = ratio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec)
    {
        Log.d(TAG, "onMeasure()");
        int parentWidth = MeasureSpec.getSize(widthSpec);
        int parentHeight = MeasureSpec.getSize(heightSpec);
        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        // MODIFIED FROM ORIGINAL VERSION -luke.hunter@gmail.com

        // Get the padding of the border background.
        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();

        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding;
        previewHeight -= vPadding;

        boolean widthLonger = previewWidth > previewHeight;
        int longSide = (widthLonger ? previewWidth : previewHeight);
        int shortSide = (widthLonger ? previewHeight : previewWidth);
        if (longSide > shortSide * mAspectRatio)
        {
            longSide = (int) ((double) shortSide * mAspectRatio);
        } else
        {
            shortSide = (int) ((double) longSide / mAspectRatio);
        }
        if (widthLonger)
        {
            previewWidth = longSide;
            previewHeight = shortSide;
        } else
        {
            previewWidth = shortSide;
            previewHeight = longSide;
        }

        // Add the padding of the border.
        previewWidth += hPadding;
        previewHeight += vPadding;

        int scaledWidth = parentWidth;
        int scaledHeight = parentHeight;
        horizontalShift = 0;

        if (parentHeight > previewHeight)
        {
            // Preview is shorter than parent, zoom in and re-center
            double scaleFactor = (double) parentHeight / previewHeight;
            scaledWidth = (int) (previewWidth * scaleFactor);
            horizontalShift = -((scaledWidth - previewWidth) / 2);
        }
        else if (parentWidth > previewWidth)
        {
            // Preview is skinnier than parent, zoom in
            double scaleFactor = (double) parentWidth / previewWidth;
            scaledWidth = parentWidth;
            scaledHeight = (int) (previewHeight * scaleFactor);
        }

        // Ask children to follow the new preview dimension.
        super.onMeasure(MeasureSpec.makeMeasureSpec(scaledWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(scaledHeight, MeasureSpec.EXACTLY));
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mListener != null) mListener.onSizeChanged(w, h);
    }

    @Override
    public void setOnLayoutChangeListener(
        LayoutChangeNotifier.Listener listener) {
//        mLayoutChangeHelper.setOnLayoutChangeListener(listener);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout()");
        View v = getChildAt(0);
        if (v != null)
        {
            v.layout(l + horizontalShift, t, r + horizontalShift, b);
        }
        mLayoutChangeHelper.onLayout(changed, l, t, r, b);
    }
}
