package com.sneakysquid.nova.app.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.sneakysquid.nova.app.R;
import com.sneakysquid.nova.app.ui.TouchImageView;
import com.sneakysquid.nova.app.util.BitmapWorkerTask;
import com.sneakysquid.nova.app.util.FileUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ViewPhoto extends Activity {

    private static final String TAG = "ViewPhoto";
    public static final String PREFS_NAME = "ViewPhotoPrefs";
    public static final String DISPLAYED_FILE_PREF = "displayedFile";
    private static final String AUX_DIR_NAME = ".aux";
    private static final String NOVA_PICTURE_FOLDER = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/NovaCamera";

    private ViewPager viewPager;
    private String currentFolder;
    private List<File> imageFiles;
    private SharedPreferences preferences;
    private TextView statusTextview;

    // File to display when onCreate or onResume is called
    private File displayedFile;

    // Index of the displayedFile in the array used in the ViewPager
    private int displayedFileIndex;

    // Used when selecting an image using the gallery button
    private static final int SELECT_PICTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_view_photo);

        currentFolder = NOVA_PICTURE_FOLDER;

//        goFullscreen();
        loadCachedData();
        setupUi();

        refreshViewPager();
    }

    @Override
    protected void onResume()
    {
        Log.d(TAG, "onResume()");
        super.onResume();

        refreshViewPager();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause()");
        super.onPause();

        if (displayedFile == null || preferences == null)
        {
            return;
        }

        String filename = displayedFile.exists() ? displayedFile.getAbsolutePath() : "";

        // Save the currently displayed file
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(DISPLAYED_FILE_PREF, filename);
        editor.commit();
    }

    private void loadCachedData()
    {
        preferences = getSharedPreferences(PREFS_NAME, 0);
        displayedFileIndex = 0; // Will be updated in reloadImages() based on displayedFile
        String displayedFileStr = "";
        displayedFileStr = preferences.getString(DISPLAYED_FILE_PREF, displayedFileStr);

        displayedFile = new File(displayedFileStr);

        if (!displayedFile.exists())
        {
            displayedFile = null;
        }
    }

    private void setupUi()
    {
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        statusTextview = (TextView) findViewById(R.id.status_textview);

        initControls();

        ImageButton shareButton = (ImageButton) findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onShareClick();
            }
        });

        ImageButton cameraButton = (ImageButton) findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onCameraClick();
            }
        });

        ImageButton deleteButton = (ImageButton) findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onDeleteClick();
            }
        });

        ImageButton editButton = (ImageButton) findViewById(R.id.edit_button);
        editButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onEditClick();
            }
        });

        ImageButton galleryButton = (ImageButton) findViewById(R.id.gallery_button);
        galleryButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onGalleryClick();
            }
        });
    }

    private void onGalleryClick()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
            "Select Picture"), SELECT_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if(requestCode == SELECT_PICTURE && data != null && data.getData() != null) {
                Uri uri = data.getData();

                final String imageFilePath = FileUtil.getPath(this, uri);

                displayedFile = new File(imageFilePath);
                currentFolder = displayedFile.getParentFile().getAbsolutePath();
            }
        }
    }

    private void initControls()
    {
        if (viewPager == null || imageFiles == null)
        {
            Log.w(TAG, "Unable to initialize viewPager");
            return;
        }

        ImagePagerAdapter adapter = new ImagePagerAdapter(this, imageFiles.toArray(new File[imageFiles.size()]));
        ImagePageChangeListener pageChangeListener = new ImagePageChangeListener();

        viewPager.setAdapter(adapter);
        viewPager.setOnPageChangeListener(pageChangeListener);

        // Ensure displayed file index is still valid
        if (displayedFileIndex < 0)
        {
            displayedFileIndex = 0;
        }

        if (imageFiles.size() > 0 && displayedFileIndex >= imageFiles.size())
        {
            displayedFileIndex = imageFiles.size() - 1;
        }

        if (imageFiles.size() > 0)
        {
            viewPager.setCurrentItem(displayedFileIndex);
            displayedFile = imageFiles.get(displayedFileIndex);
        }

        if (imageFiles.size() > 0)
        {
            // Text will show when the view has scrolled but the image is still being loaded
            statusTextview.setText(getString(R.string.loading));
        }
        else
        {
            statusTextview.setText(getString(R.string.no_images_found));
        }
    }

    private void onDeleteClick()
    {
        if (displayedFile == null)
        {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setMessage("Do you want to delete " + displayedFile.getName() + "?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {
                    deleteDisplayedFile();
                }})
            .setNegativeButton(android.R.string.no, null);

        AlertDialog dialog = builder.create();

        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        dialog.show();
    }

    private void deleteDisplayedFile()
    {
        boolean deleted = false;

        if (displayedFile == null)
        {
            return;
        }

        // Attempt to delete any files created by the Android photo editor
        deleteAuxFiles(this.getContentResolver(), getImageContentUri(this, displayedFile));

        deleted = displayedFile.delete();

        if (!deleted)
        {
            Log.w(TAG, "Failed to delete: " + displayedFile.getAbsolutePath());
            return;
        }

        Log.d(TAG, "Deleted: " + displayedFile.getAbsolutePath());

        displayedFile = null;

        refreshViewPager();
    }

    /**
     * From https://android.googlesource.com/platform/packages/apps/Gallery2/+/master/src/com/android/gallery3d/filtershow/tools/SaveImage.java
     *
     * Remove the files in the auxiliary directory whose names are the same as
     * the source image.
     * @param contentResolver The application's contentResolver
     * @param srcContentUri The content Uri for the source image.
     */
    public static void deleteAuxFiles(ContentResolver contentResolver,
                                      Uri srcContentUri) {
        final String[] fullPath = new String[1];
        String[] queryProjection = new String[] { MediaStore.Images.ImageColumns.DATA };
        querySourceFromContentResolver(contentResolver,
            srcContentUri, queryProjection,
            new ContentResolverQueryCallback() {
                @Override
                public void onCursorResult(Cursor cursor) {
                    fullPath[0] = cursor.getString(0);
                }
            }
        );
        if (fullPath[0] != null) {
            // Construct the auxiliary directory given the source file&#39;s path.
            // Then select and delete all the files starting with the same name
            // under the auxiliary directory.
            File currentFile = new File(fullPath[0]);

            String filename = currentFile.getName();
            int firstDotPos = filename.indexOf(".");
            final String filenameNoExt = (firstDotPos == -1) ? filename :
                filename.substring(0, firstDotPos);
            File auxDir = getLocalAuxDirectory(currentFile);
            if (auxDir.exists()) {
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(filenameNoExt + ".");
                    }
                };

                // Delete all auxiliary files whose name is matching the
                // current local image.
                File[] auxFiles = auxDir.listFiles(filter);
                for (File file : auxFiles) {
                    file.delete();
                }
            }
        }
    }

    // From https://android.googlesource.com/platform/packages/apps/Gallery2/+/master/src/com/android/gallery3d/filtershow/tools/SaveImage.java
    private static File getLocalAuxDirectory(File dstFile) {
        File dstDirectory = dstFile.getParentFile();
        return new File(dstDirectory + "/" + AUX_DIR_NAME);
    }

    // From https://android.googlesource.com/platform/packages/apps/Gallery2/+/master/src/com/android/gallery3d/filtershow/tools/SaveImage.java
    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }

    private static void querySourceFromContentResolver(
        ContentResolver contentResolver, Uri sourceUri, String[] projection,
        ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null,
                null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            new String[] { MediaStore.Images.Media._ID },
            MediaStore.Images.Media.DATA + "=? ",
            new String[] { filePath }, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    private void refreshViewPager()
    {
        reloadImages();
        initControls();
    }

    private void onCameraClick()
    {
        Intent i = new Intent(ViewPhoto.this, NovaCamera.class);
        startActivity(i);
    }

    private void onShareClick()
    {
        if (displayedFile == null || !displayedFile.exists())
        {
            return;
        }

        Uri imageUri = Uri.fromFile(displayedFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        startActivity(Intent.createChooser(intent, "Share"));
    }

    private void onEditClick()
    {
        if (displayedFile == null)
        {
            return;
        }

        // A note about the Android photo editor from the Android source code:
        // ---------------------------
        //      In order to support the new edit-save behavior such that user won't see
        //      the edited image together with the original image, we are adding a new
        //      auxiliary directory for the edited image. Basically, the original image
        //      will be hidden in that directory after edit and user will see the edited
        //      image only.
        //      https://android.googlesource.com/platform/packages/apps/Gallery2/+/master/src/com/android/gallery3d/filtershow/tools/SaveImage.java
        // ---------------------------
        // Currently, the old files are not deleted when a deletion is performed by Nova Camera.

        Intent editIntent = new Intent(Intent.ACTION_EDIT);
        editIntent.setDataAndType(Uri.fromFile(displayedFile), "image/jpeg");
        editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(editIntent, null));
    }

    private void reloadImages()
    {
        final String state = Environment.getExternalStorageState();

        if (!Environment.MEDIA_MOUNTED.equals(state))
        {
            Log.e(TAG, "External storage directory not mounted");
            return;
        }

        imageFiles = new ArrayList<File>();

        File picsFolder = new File(currentFolder);

        if (!picsFolder.exists())
        {
            return;
        }

        File[] files = picsFolder.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith(".jpg");
            }
        });

        if (files == null || files.length == 0)
        {
            return;
        }

        // Sort by last modified date
        Arrays.sort(files, new Comparator<File>()
        {
            public int compare(File f1, File f2)
            {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });

        // Reverse array
        for (int i = 0; i < files.length / 2; i++)
        {
            File temp = files[i];
            files[i] = files[files.length - i - 1];
            files[files.length - i - 1] = temp;
        }

        int j = 0;
        for (File file : files)
        {
            imageFiles.add(file);

            if (displayedFile != null && file != null && displayedFile.getAbsolutePath().equals(file.getAbsolutePath()))
            {
                displayedFileIndex = j;
            }

            j++;
        }
    }

    public class ImagePageChangeListener extends ViewPager.SimpleOnPageChangeListener
    {
        @Override
        public void onPageSelected(int position)
        {
            super.onPageSelected(position);

            if (imageFiles.size() > position)
            {
                displayedFile = imageFiles.get(position);
                displayedFileIndex = position;
            }
        }
    }

    public class ImagePagerAdapter extends PagerAdapter
    {
        Activity activity;
        File[] imageFiles;

        public ImagePagerAdapter(Activity activity, File[] imageFiles)
        {
            this.activity = activity;
            this.imageFiles = imageFiles;
        }

        public int getCount()
        {
            return imageFiles.length;
        }

        public Object instantiateItem(View collection, int position) {
            if (collection == null)
            {
                return new TouchImageView(activity);
            }

            Context context = collection.getContext();

            LayoutInflater inflater = null;
            if (context != null)
            {
                inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }

            View layout = null;
            if (inflater != null)
            {
                layout = inflater.inflate(R.layout.page_image, null);
            }

            TouchImageView imageView= null;

            if (layout == null)
            {
                return new TouchImageView(activity);
            }

            imageView = (TouchImageView) layout.findViewById(R.id.touch_image_view);

            Bitmap image = null;

            if (imageFiles.length > position)
            {
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;

                BitmapWorkerTask task = new BitmapWorkerTask(imageView, imageFiles[position].getAbsolutePath(), width, height);
                task.execute();
            }
            else
            {
                Log.w(TAG, "Attempted to build image page out of range of image filename array");
            }

            ((ViewPager) collection).addView(layout, 0);
            return layout;
        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            ((ViewPager) arg0).removeView((View) arg2);}

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);}

        @Override
        public Parcelable saveState() {
            return null; }}

}
