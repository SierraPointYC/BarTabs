package org.spyc.bartabs.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * An spyc full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class IdleActivity extends AppCompatActivity {

    private static final String TAG = IdleActivity.class.getSimpleName();

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private boolean mUseNfc = false;
    AudioManager mAudioManager;
    private Acr3x nfcReader;

    private Acr3xNotifListener mNfcListener = new Acr3xNotifListener() {
        @Override
        public void onPiccPowerOn(final String piccAtr) {
            Log.i(TAG, "NFC Reader power on ATR: " + piccAtr);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(IdleActivity.this, "NFC Reader power on ATR:" + piccAtr, Toast.LENGTH_SHORT).show();
                }
            });
        }
        @Override
        public void onUUIDAavailable(final String uuid) {
            Log.i(TAG, "NFC Reader received UUID: " + uuid);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(IdleActivity.this, "NFC Reader received UUID:" + uuid, Toast.LENGTH_SHORT).show();
                    nfcReader.pause();
                    startLoginActivity(uuid);
                }
            });
        }

        @Override
        public void onFirmwareVersionAvailable(final String firmwareVersion) {
            Log.i(TAG, "NFC Reader firmaware version: " + firmwareVersion);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(IdleActivity.this, "NFC Reader received firmware version:" + firmwareVersion, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onStatusAvailable(final int battery, final int sleep) {
            Log.i(TAG, "NFC Reader status: battery = " + battery + ", sleep = " + sleep);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(IdleActivity.this, "NFC Reader status: battery = " + battery + ", sleep = " + sleep, Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void operationFailure(final String error) {
            Log.i(TAG, "NFC reader:" + error);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(IdleActivity.this, "NFC reader:" + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private void startLoginActivity(String tag) {
        Intent in=new Intent(IdleActivity.this,LoginActivity.class);
        if (tag != null) {
            in.putExtra(LoginActivity.USER_TAG_EXTRA, tag);
        }
        startActivity(in);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mUseNfc = sharedPref.getBoolean("switch_nfc", false);


        setContentView(R.layout.activity_idle);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //findViewById(R.id.openTabButton).setOnTouchListener(mDelayHideTouchListener);

        findViewById(R.id.openTabButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLoginActivity(null);
            }
        });
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        nfcReader = Acr3x.getInstance();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected void onStart() {
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide();
        if (mUseNfc) {
            nfcReader.start(mAudioManager, mNfcListener);
        }
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (mUseNfc) {
            nfcReader.stop();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (mUseNfc) {
            nfcReader.resume();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mUseNfc) {
            nfcReader.pause();
        }
        super.onPause();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide() {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, 100);
    }

}
