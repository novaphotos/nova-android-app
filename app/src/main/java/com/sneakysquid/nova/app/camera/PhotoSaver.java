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

package com.sneakysquid.nova.app.camera;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.sneakysquid.nova.app.error.ErrorReporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.sneakysquid.nova.link.Debug.debug;

/**
 * @author Joe Walnes
 */
public class PhotoSaver {

    private static final String TAG = "PhotoSaver";
    protected final File dir;
    protected final String suffix;
    protected final String prefix;
    protected final ErrorReporter errorReporter;
    protected final Activity activity;

    public PhotoSaver(Activity activity, ErrorReporter errorReporter) {
        this(activity,
            errorReporter,
            new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "NovaCamera"),
            "IMG_",
            ".jpg");
    }

    public PhotoSaver(Activity activity, ErrorReporter errorReporter, File dir, String prefix, String suffix) {
        this.activity = activity;
        this.errorReporter = errorReporter;
        this.dir = dir;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public File save(byte[] jpeg)
    {
        File result = uniqueFile();

        if (!save(jpeg, result))
        {
            result = null;
        }

        return result;
    }

    public boolean save(byte[] jpeg, File file) {
        debug("onPictureTaken() saving to %s", file);

        boolean result = false;

        // Create the storage directory if it does not exist
        if (!dir.exists())
        {
            if (!dir.mkdirs())
            {
                errorReporter.reportError("Could not create photo directory");
                return false;
            }
        }

        if (!writeToFile(file, jpeg)) {
            errorReporter.reportError("Could not save photo");
        }
        else
        {
            result = true;
        }

        return result;
    }

    public boolean save(byte[] jpeg, Uri uri, ContentResolver contentResolver)
    {
        if (jpeg == null || jpeg.length == 0 || uri == null)
        {
            return false;
        }

        OutputStream stream = null;
        try
        {
            stream = contentResolver.openOutputStream(uri);

            if (stream == null)
            {
                return false;
            }
            stream.write(jpeg);
            stream.close();

            return true;
        }
        catch (IOException ex)
        {
            Log.e(TAG, ex.getMessage());
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException ex)
                {
                    Log.e(TAG, ex.getMessage());
                }
            }
        }

        return false;
    }

    protected File uniqueFile() {
        return new File(dir, prefix + generateId() + suffix).getAbsoluteFile();
    }

    protected String generateId() {
        Date now = new Date();
        long millis = now.getTime() - now.getTime() % 1000;
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(now) + "_" + millis;
    }

    protected boolean writeToFile(File file, byte[] data) {
        try {
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return true;
        } catch (IOException e) {
            debug("failed to write to file " + file.getAbsolutePath() + ": " + e);
            return false;
        }
    }
}
