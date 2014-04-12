/*
 * Copyright (C) 2013 Sneaky Squid LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sneakysquid.nova.app.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.android.camera.PreviewFrameLayout;

import java.io.IOException;
import java.util.List;

/**
 * @author Joe Walnes
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = "CameraPreview";

    private SurfaceHolder holder;
    private Camera camera;
    private Activity activity;
    private PreviewFrameLayout previewFrameLayout;

    public CameraPreview(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        if (context instanceof Activity)
        {
            activity = (Activity)context;
        }

        holder = getHolder();

        if (holder == null)
        {
            Log.e(TAG, "Surface holder is null");
            return;
        }

        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setPreviewFrameLayout(PreviewFrameLayout previewFrameLayout)
    {
        this.previewFrameLayout = previewFrameLayout;
    }

    public void beginPreview(Camera camera) {
        this.camera = camera;

        startPreview();
    }

    public void endPreview() {
        stopPreview();
        camera = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // no-op
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // no-op
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Handle resize/rotate.
        if (holder == null || holder.getSurface() == null || camera == null) {
            Log.d(TAG, "unable to handle surfaceChanged");
            return;
        }

        configureCamera(w, h);
        startPreview();
    }

    private void startPreview()
    {
        if (camera == null || holder == null)
        {
            Log.e(TAG, "startPreview(): failed -- camera or holder is null");
            return;
        }

        configureCamera(getWidth(), getHeight());

        Log.d(TAG, "camera.startPreview()");
        camera.startPreview();
    }

    private void stopPreview()
    {
        if (camera == null)
        {
            Log.w(TAG, "stopPreview(): camera is null");
            return;
        }

        Log.d(TAG, "camera.stopPreview()");
        camera.stopPreview();
    }

    private void configureCamera(int previewSurfaceWidth, int previewSurfaceHeight)
    {
        if (camera == null || holder == null)
        {
            Log.e(TAG, "Unable to configure camera: camera or holder is null");
            return;
        }

        if (holder.getSurface() == null)
        {
            Log.e(TAG, "Unable to configure camera: preview holder surface is null");
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size pictureSize = getLargestPictureSize(parameters);

        if (pictureSize != null)
        {
            Log.d(TAG, "parameters.setPictureSize(width=" + pictureSize.width + ", height=" + pictureSize.height + ")");
            parameters.setPictureSize(pictureSize.width, pictureSize.height);
        }

        Log.d(TAG, "parameters.setPictureFormat(ImageFormat.JPEG);");
        parameters.setPictureFormat(ImageFormat.JPEG);

        if (camera == null)
        {
            return;
        }

        Log.d(TAG, "previewSurfaceWidth: " + previewSurfaceWidth + ", previewSurfaceHeight: " + previewSurfaceHeight);
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        // Divide height by width because camera preview is always in portrait orientation
        Camera.Size previewSize = getOptimalPreviewSize(activity, sizes, (double)previewSurfaceHeight / previewSurfaceWidth); //getBestPreviewSize(previewSurfaceWidth, previewSurfaceHeight, parameters);

        Log.d(TAG, "parameters.setPreviewSize(width=" + previewSize.width + ", height=" + previewSize.height + ")");
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        try
        {
            Log.d(TAG, "camera.setParameters()");
            camera.setParameters(parameters);
        }
        // todo: handle more specific exception type(s)
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (previewFrameLayout != null)
        {
            Camera.Parameters appliedParams = camera.getParameters();
            Camera.Size actualPreviewSize = appliedParams.getPreviewSize();

            if (actualPreviewSize != null)
            {
                previewFrameLayout.setAspectRatio((double)actualPreviewSize.width / actualPreviewSize.height);
            }
        }

        // NovaCamera activity always runs in portrait mode, so orientation is always 90
        Log.d(TAG, "camera.setDisplayOrientation(90)");
        camera.setDisplayOrientation(90);

        try
        {
            Log.d(TAG, "camera.setPreviewDisplay");
            camera.setPreviewDisplay(holder);
        }
        catch (IOException e)
        {
            Log.d(TAG, "Could not preview camera");
        }
    }

    private Camera.Size getLargestPictureSize(Camera.Parameters parameters)
    {
        Camera.Size result = null;

        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();

        if (sizes != null)
        {
            for (Camera.Size size : sizes)
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
        }

        return (result);
    }

    private static Point getDefaultDisplaySize(Activity activity, Point size) {
        Display d = activity.getWindowManager().getDefaultDisplay();
        d.getSize(size);
        return size;
    }

    public static Camera.Size getOptimalPreviewSize(Activity currentActivity,
                                                    List<Camera.Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        Point point = getDefaultDisplaySize(currentActivity, new Point());
        int targetHeight = Math.min(point.x, point.y);
        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size found matching the aspect ratio: " + targetRatio);
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}