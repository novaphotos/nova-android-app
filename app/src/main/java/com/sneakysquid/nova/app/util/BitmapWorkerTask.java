package com.sneakysquid.nova.app.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Loads a bitmap in the background, downsampled based on the provided width/height.
 */
public class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
    private static final String TAG = "BitmapWorkerTask";
    private final WeakReference<ImageView> imageViewReference;
    private int data = 0;
    private String bitmapFilename;
    private int width;
    private int height;

    public BitmapWorkerTask(ImageView imageView, String bitmapFilename, int width, int height) {
        this.bitmapFilename = bitmapFilename;
        this.width = width;
        this.height = height;
        // Use a WeakReference to ensure the ImageView can be garbage collected
        imageViewReference = new WeakReference<ImageView>(imageView);
    }

    // Decode image in background.
    @Override
    protected Bitmap doInBackground(Integer... params) {
        Bitmap result = BitmapUtil.decodeSampledBitmapFromFile(bitmapFilename, width, height);

        ExifInterface exif = null;
        try
        {
            exif = new ExifInterface(bitmapFilename);
        }
        catch (IOException e)
        {
            Log.e(TAG, e.getStackTrace().toString());
        }

        if (exif == null)
        {
            // Unable to read exif data, return image un-rotated.
            return result;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        return rotateBitmap(result, orientation);
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation)
    {
        Matrix matrix = new Matrix();

        switch (orientation)
        {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        
        try
        {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e)
        {
            Log.e(TAG, "Out of memory: " + e.getStackTrace().toString());
            e.printStackTrace();
            return null;
        }
    }

    // Once complete, see if ImageView is still around and set bitmap.
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (imageViewReference != null && bitmap != null) {
            final ImageView imageView = imageViewReference.get();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
