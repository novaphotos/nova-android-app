package com.sneakysquid.nova.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.sneakysquid.nova.app.R;
import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLinkStatus;

/**
 * Created by luke on 3/19/14.
 */
public class FlashSettingsDialog extends LinearLayout
{
    public interface FlashSettingsDialogCallback
    {
        void onOkClick();
        void onTestClick();
        void onFlashSettingsChange(NovaFlashCommand cmd);
    }

    private FlashSettingsDialogCallback callback;
    private SharedPreferences.Editor editor;
    private static final String TAG = "FlashSettingsDialog";
    public static final String PREFS_NAME = "FlashSettingsPrefs";
    public static final String FLASH_MODE_PREF = "flashMode";
    private String flashMode;
    private static final String DEFAULT_FLASH_MODE = "gentle";

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
        flashMode = DEFAULT_FLASH_MODE;
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

        findViewById(R.id.test_button).setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onTestClick();
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

        if (flashMode == null)
        {
            flashMode = DEFAULT_FLASH_MODE;
        }

        if (flashMode.equals("off"))
        {
            offButton.setChecked(true);
            onFlashSettingsChange(NovaFlashCommand.off());
        }
        else if (flashMode.equals("warm"))
        {
            warmButton.setChecked(true);
            onFlashSettingsChange(NovaFlashCommand.warm());
        }
        else if (flashMode.equals("gentle"))
        {
            gentleButton.setChecked(true);
            onFlashSettingsChange(NovaFlashCommand.gentle());
        }
        else if (flashMode.equals("bright"))
        {
            brightButton.setChecked(true);
            onFlashSettingsChange(NovaFlashCommand.bright());
        }
    }

    private void onBrightClick()
    {
        flashMode = "bright";
        saveFlashSettings(flashMode);
        onFlashSettingsChange(NovaFlashCommand.bright());
    }

    private void onGentleClick()
    {
        flashMode = "gentle";
        saveFlashSettings(flashMode);
        onFlashSettingsChange(NovaFlashCommand.gentle());
    }

    private void onOffClick()
    {
        flashMode = "off";
        saveFlashSettings(flashMode);
        onFlashSettingsChange(NovaFlashCommand.off());
    }

    private void onWarmClick()
    {
        flashMode = "warm";
        saveFlashSettings(flashMode);
        onFlashSettingsChange(NovaFlashCommand.warm());
    }

    private void onOkClick()
    {
        if (callback != null)
        {
            callback.onOkClick();
        }
    }

    private void onTestClick()
    {
        if (callback != null)
        {
            callback.onTestClick();
        }
    }

    private void onFlashSettingsChange(NovaFlashCommand cmd)
    {
        if (callback != null)
        {
            callback.onFlashSettingsChange(cmd);
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

        if (flashMode.equals("off"))
        {
            onFlashSettingsChange(NovaFlashCommand.off());
        }
        else if (flashMode.equals("warm"))
        {
            onFlashSettingsChange(NovaFlashCommand.warm());
        }
        else if (flashMode.equals("gentle"))
        {
            onFlashSettingsChange(NovaFlashCommand.gentle());
        }
        else if (flashMode.equals("bright"))
        {
            onFlashSettingsChange(NovaFlashCommand.bright());
        }
    }

    public void setStatus(NovaLinkStatus status)
    {
        ((TextView)findViewById(R.id.status_text)).setText("Nova: " + status.toString());
    }


}
