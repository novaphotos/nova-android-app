package com.sneakysquid.nova.app.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * From http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
 */
public class BitmapUtil
{
    public static int calculateInSampleSize(
        BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            try
            {
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((height / inSampleSize) > reqHeight
                    || (width / inSampleSize) > reqWidth)
                {
                    inSampleSize *= 2;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromFile(String pathName, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }
}
