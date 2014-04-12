package com.sneakysquid.nova.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.sneakysquid.nova.app.R;

/**
 * Created by luke on 3/19/14.
 */
public class FlashSettingsDialog extends LinearLayout
{
    public interface FlashSettingsDialogCallback
    {
        void onOkClick();
    }

    private FlashSettingsDialogCallback callback;
    private SharedPreferences.Editor editor;
    private static final String TAG = "FlashSettingsDialog";
    public static final String PREFS_NAME = "FlashSettingsPrefs";
    public static final String FLASH_MODE_PREF = "flashMode";
    private String flashMode;

    public FlashSettingsDialog(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.flash_settings_dialog, this);

        loadPreferences(context);
        setupUi();
    }

    private void loadPreferences(Context context)
    {
        // Create preferences editor, load flash settings from preferences.
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);

        if (preferences == null)
        {
            Log.w(TAG, "Unable to load preferences");
            return;
        }

        this.editor = preferences.edit();
        flashMode = "gentle";
        preferences.getString(FLASH_MODE_PREF, flashMode);
    }

    private void setupUi()
    {
        findViewById(R.id.ok_button).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onOkClick();
            }
        });

        findViewById(R.id.dialog_area).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // do nothing. just need to block clicks from reaching the parent.
            }
        });

        RadioButton offButton = ((RadioButton) findViewById(R.id.off_button));
        offButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onOffClick();
            }
        });

        RadioButton warmButton = ((RadioButton) findViewById(R.id.warm_button));
        warmButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onWarmClick();
            }
        });

        RadioButton gentleButton = ((RadioButton)findViewById(R.id.gentle_button));
        gentleButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onGentleClick();
            }
        });

        RadioButton brightButton = ((RadioButton)findViewById(R.id.bright_button));
        brightButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onBrightClick();
            }
        });

        if (flashMode.equals("off"))
        {
            offButton.setChecked(true);
        }
        else if (flashMode.equals("warm"))
        {
            warmButton.setChecked(true);
        }
        else if (flashMode.equals("gentle"))
        {
            gentleButton.setChecked(true);
        }
        else if (flashMode.equals("bright"))
        {
            brightButton.setChecked(true);
        }
    }

    private void onBrightClick()
    {
        flashMode = "bright";
        saveFlashSettings(flashMode);
    }

    private void onGentleClick()
    {
        flashMode = "gentle";
        saveFlashSettings(flashMode);
    }

    private void onOffClick()
    {
        flashMode = "off";
        saveFlashSettings(flashMode);
    }

    private void onWarmClick()
    {
        flashMode = "warm";
        saveFlashSettings(flashMode);
    }

    private void onOkClick()
    {
        if (callback != null)
        {
            callback.onOkClick();
        }
    }

    private void saveFlashSettings(String mode)
    {
        if (editor == null)
        {
            return;
        }

        editor.putString(FLASH_MODE_PREF, mode);
        editor.commit();
    }

    public void setCallback(FlashSettingsDialogCallback callback)
    {
        this.callback = callback;
    }
}
