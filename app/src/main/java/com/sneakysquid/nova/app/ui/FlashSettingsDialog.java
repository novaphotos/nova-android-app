package com.sneakysquid.nova.app.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
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
    public static final String FLASH_CUSTOM_WARM_PREF = "flashCustomWarm";
    public static final String FLASH_CUSTOM_COOL_PREF = "flashCustomCool";
    private String flashMode;
    private int customWarm;
    private int customCool;
    private View customSliders;
    private static final String DEFAULT_FLASH_MODE = "warm";

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
        flashMode = preferences.getString(FLASH_MODE_PREF, DEFAULT_FLASH_MODE);
        customWarm = protectPwmValue(preferences.getInt(FLASH_CUSTOM_WARM_PREF, 255));
        customCool = protectPwmValue(preferences.getInt(FLASH_CUSTOM_COOL_PREF, 255));
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

        RadioButton neutralButton = ((RadioButton) findViewById(R.id.neutral_button));
        neutralButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onNeutralClick();
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

        RadioButton customButton = ((RadioButton)findViewById(R.id.custom_button));
        customButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                onCustomClick();
            }
        });

        customSliders = findViewById(R.id.custom_sliders);

        SeekBar warmSlider = ((SeekBar)findViewById(R.id.warm_slider));
        warmSlider.setProgress(customWarm);
        warmSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                customWarm = protectPwmValue(progress);
                saveFlashSettings();
                if (flashMode.equals("custom")) {
                    onFlashSettingsChange(NovaFlashCommand.custom(customWarm, customCool));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        SeekBar coolSlider = ((SeekBar)findViewById(R.id.cool_slider));
        coolSlider.setProgress(customCool);
        coolSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                customCool = protectPwmValue(progress);
                saveFlashSettings();
                if (flashMode.equals("custom")) {
                    onFlashSettingsChange(NovaFlashCommand.custom(customWarm, customCool));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // no-op
            }
        });

        if (flashMode == null)
        {
            flashMode = DEFAULT_FLASH_MODE;
        }

        if (flashMode.equals("off"))
        {
            offButton.setChecked(true);
            hideCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.off());
        }
        else if (flashMode.equals("warm"))
        {
            warmButton.setChecked(true);
            hideCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.warm());
        }
        else if (flashMode.equals("neutral"))
        {
            neutralButton.setChecked(true);
            hideCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.neutral());
        }
        else if (flashMode.equals("gentle"))
        {
            gentleButton.setChecked(true);
            hideCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.gentle());
        }
        else if (flashMode.equals("bright"))
        {
            brightButton.setChecked(true);
            hideCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.bright());
        }
        else if (flashMode.equals("custom"))
        {
            customButton.setChecked(true);
            showCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.custom(customWarm, customCool));
        }
        else
        {
            flashMode = "off";
            offButton.setChecked(true);
            hideCustomSliders();
            onFlashSettingsChange(NovaFlashCommand.off());
        }
    }

    private int protectPwmValue(int value)
    {
        if (value <= 10) {
            return 0; // Min value
        } else if (value < 63) {
            return 63; // Special case: 0-63 is not ideal for Nova. Snap up to protect the hardware.
        } else if (value < 255) {
            return value; // User input
        } else {
            return 255; // Max value
        }
    }

    private void onBrightClick()
    {
        flashMode = "bright";
        saveFlashSettings();
        hideCustomSliders();
        onFlashSettingsChange(NovaFlashCommand.bright());
    }

    private void onGentleClick()
    {
        flashMode = "gentle";
        saveFlashSettings();
        hideCustomSliders();
        onFlashSettingsChange(NovaFlashCommand.gentle());
    }

    private void onOffClick()
    {
        flashMode = "off";
        saveFlashSettings();
        hideCustomSliders();
        onFlashSettingsChange(NovaFlashCommand.off());
    }

    private void onWarmClick()
    {
        flashMode = "warm";
        saveFlashSettings();
        hideCustomSliders();
        onFlashSettingsChange(NovaFlashCommand.warm());
    }

    private void onNeutralClick()
    {
        flashMode = "neutral";
        saveFlashSettings();
        hideCustomSliders();
        onFlashSettingsChange(NovaFlashCommand.neutral());
    }

    private void onCustomClick()
    {
        flashMode = "custom";
        saveFlashSettings();
        showCustomSliders();
        onFlashSettingsChange(NovaFlashCommand.custom(customWarm, customCool));
    }

    private void showCustomSliders()
    {
        customSliders.setVisibility(VISIBLE);
    }

    private void hideCustomSliders()
    {
        customSliders.setVisibility(GONE);
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

    private void saveFlashSettings()
    {
        if (editor == null)
        {
            return;
        }

        Log.d(TAG, "Save flash settings : " + flashMode + (flashMode == "custom" ? (" (warm=" + customWarm +", cool=" + customCool + ")") : ""));

        editor.putString(FLASH_MODE_PREF, flashMode);
        editor.putInt(FLASH_CUSTOM_WARM_PREF, customWarm);
        editor.putInt(FLASH_CUSTOM_COOL_PREF, customCool);
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
        else if (flashMode.equals("neutral"))
        {
            onFlashSettingsChange(NovaFlashCommand.neutral());
        }
        else if (flashMode.equals("gentle"))
        {
            onFlashSettingsChange(NovaFlashCommand.gentle());
        }
        else if (flashMode.equals("bright"))
        {
            onFlashSettingsChange(NovaFlashCommand.bright());
        }
        else if (flashMode.equals("custom"))
        {
            onFlashSettingsChange(NovaFlashCommand.custom(customWarm, customCool));
        }
        else
        {
            onFlashSettingsChange(NovaFlashCommand.off());
        }
    }

    public void setStatus(NovaLinkStatus status)
    {
        ((TextView)findViewById(R.id.status_text)).setText("Nova: " + status.toString());
    }


}
