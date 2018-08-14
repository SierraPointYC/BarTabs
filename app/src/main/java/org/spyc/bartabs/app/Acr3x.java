package org.spyc.bartabs.app;

import com.acs.audiojack.AudioJackReader;
import com.acs.audiojack.Result;
import com.acs.audiojack.Status;

import android.media.AudioManager;
import android.util.Log;

import java.util.Date;

/**
 * This class allows control of the ACR35 reader sleep state and PICC commands
 */
public class Acr3x {


    private static final String TAG = Acr3x.class.getSimpleName();

    private Thread mThread;
    private Acr3xServiceThread mService;
    private AudioManager mAudioManager;
    private volatile AudioJackReader mReader;
    private Acr3xNotifListener mListener;


    /** APDU command for reading a card's UID */
    private final byte[] apdu = { (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    /** Timeout for APDU response (in <b>seconds</b>) */
    private final int timeout = 3;

    @SuppressWarnings("FieldCanBeLocal")
    private final int acr3xCardType = AudioJackReader.PICC_CARD_TYPE_ISO14443_TYPE_A;
//            | AudioJackReader.PICC_CARD_TYPE_ISO14443_TYPE_B
//            | AudioJackReader.PICC_CARD_TYPE_FELICA_212KBPS
//            | AudioJackReader.PICC_CARD_TYPE_FELICA_424KBPS
//            | AudioJackReader.PICC_CARD_TYPE_AUTO_RATS;


    private String lastUuid = "";
    private Date lastUuidDate = new Date();

    private static final Acr3x mInstance = new Acr3x();

    public static Acr3x getInstance() {
        return  mInstance;
    }

    private Acr3x() {

    }

    public void start(AudioManager audioManager, Acr3xNotifListener listener) {
        Log.i(TAG, "NFC Reader start.");
        if (mReader == null) {
            Log.i(TAG, "Creating new NFC reader.");
            mAudioManager = audioManager;
            mReader = new AudioJackReader(mAudioManager);
            mListener = listener;
            mService = new Acr3xServiceThread();
        }

        mThread = new Thread(mService, "Acr3xServiceThread");
        mThread.start();
    }

    public void stop(){
        mService.stop();
        try {
            if (mThread != null) {
                mThread.join();
                mThread = null;
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Thread join interrupted.");
        }
        Log.i(TAG, "NFC Reader stopped.");
    }

    public void pause() {
        mService.pause();
    }

    public void resume() {
        mService.resume();
    }

    public class Acr3xServiceThread implements Runnable,
            AudioJackReader.OnResultAvailableListener,
            AudioJackReader.OnResetCompleteListener,
            AudioJackReader.OnStatusAvailableListener,
            AudioJackReader.OnFirmwareVersionAvailableListener,
            AudioJackReader.OnPiccAtrAvailableListener,
            AudioJackReader.OnPiccResponseApduAvailableListener {

        private int acr3xStartAudioLevel;
        private volatile boolean isStopped = false;
        private volatile boolean isPaused = false;

        private final Object mResponseEvent = new Object();
        private boolean mResetComplete = false;
        private boolean mStatusReady = false;
        private boolean mResultReady = false;
        private boolean mPiccAtrReady = false;
        private boolean mPiccResponseReady = false;
        private boolean mFirmwareResponseReady = false;
        private String mOpInProgress = null;

        private void resetReader() {
            synchronized (mResponseEvent) {
                mResetComplete = false;
                mResultReady = false;
                mOpInProgress = "reset()";
                mReader.reset();
                if (!mResetComplete && !mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }
                }
                if (!mResetComplete) {
                    Log.e(TAG, "ACR35 reset() timed out.");
                }
            }
        }

        private void putToSleep() {
            synchronized (mResponseEvent) {
                mResultReady = false;
                mOpInProgress = "sleep()";
                mReader.sleep();
                if (!mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }

                }
                if (!mResultReady) {
                    Log.e(TAG, "ACR35 sleep() timed out.");
                }
            }
        }

        private boolean getStatus() {
            synchronized (mResponseEvent) {
                mStatusReady = false;
                mResultReady = false;
                mOpInProgress = "getStatus()";
                boolean res = mReader.getStatus();
                if (!res) {
                    mListener.operationFailure("Failed to queue getStatus().");
                    return false;
                }
                if (!mStatusReady && !mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }

                }
                if (!mStatusReady) {
                    mListener.operationFailure("getStatus() timed out.");
                    return false;
                }
                return true;
            }
        }

        private boolean getFirmwareVersion() {
            synchronized (mResponseEvent) {
                mFirmwareResponseReady = false;
                mResultReady = false;
                mOpInProgress = "getFirmwareVersion()";
                boolean res = mReader.getFirmwareVersion();
                if (!res) {
                    mListener.operationFailure("Failed to queue getFirmwareVersion().");
                    return false;
                }
                if (!mFirmwareResponseReady && !mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }

                }
                if (!mFirmwareResponseReady) {
                    mListener.operationFailure("getFirmwareVersion() timed out.");
                    return false;
                }
            }
            return true;
        }

        private boolean piccPowerOn() {
            synchronized (mResponseEvent) {
                mPiccAtrReady = false;
                mResultReady = false;
                mOpInProgress = "piccPowerOn()";
                boolean res = mReader.piccPowerOn(timeout, acr3xCardType);
                if (!res) {
                    mListener.operationFailure("Failed to queue piccPowerOn().");
                    return false;
                }
                if (!mPiccAtrReady && !mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }

                }
                if (!mPiccAtrReady) {
                    if (!mResultReady) {
                        mListener.operationFailure("piccPowerOn() timed out.");
                    }
                    return false;
                }
                return true;
            }
        }

        private void piccTransmitGetUID() {
            synchronized (mResponseEvent) {
                mPiccResponseReady = false;
                mResultReady = false;
                mOpInProgress = "piccTransmit()";
                boolean res = mReader.piccTransmit(timeout, apdu);
                if (!res) {
                    mListener.operationFailure("Failed to queue piccTransmit().");
                    return;
                }
                if (!mPiccResponseReady && !mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }

                }
                if (!mPiccResponseReady) {
                    if (!mResultReady) {
                        Log.d(TAG,"piccTransmit() timed out.");
                    }
                    mListener.operationFailure("piccTransmit() timed out.");
                }
            }
        }

        private void piccPowerOff() {
            synchronized (mResponseEvent) {
                mResultReady = false;
                mOpInProgress = "piccPowerOff()";
                boolean res = mReader.piccPowerOff();
                if (!res) {
                    System.out.println("ACR35 failed to queue piccPowerOff()");
                    mListener.operationFailure("Failed to queue piccPowerOff().");
                    return;
                }
                if (!mResultReady) {

                    try {
                        mResponseEvent.wait(10000);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Thread interrupted.");
                    }

                }
                if (!mResultReady) {
                    System.out.println("ACR35 piccPowerOff() timed out.");
                    mListener.operationFailure("piccPowerOff() timed out.");
                }
            }
        }

        @Override
        public void run() {
            Log.i(TAG, "NFC Reader thread started.");
            isStopped = false;
            isPaused = false;
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
            acr3xStartAudioLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            mReader.start();
            //mReader.setSleepTimeout(30);
            /* Set the reset complete callback */
            mReader.setOnResetCompleteListener(this);
            mReader.setOnStatusAvailableListener(this);
            mReader.setOnFirmwareVersionAvailableListener(this);
            mReader.setOnPiccAtrAvailableListener(this);
            mReader.setOnPiccResponseApduAvailableListener(this);
            mReader.setOnResultAvailableListener(this);
            boolean responding = false;
            int count = 3;
            resetReader();
            while (!responding && count > 0) {
                responding = getStatus();
                responding &= getFirmwareVersion();
                if (responding) {
                    break;
                }
                try {
                    count--;
                    putToSleep();
                    Thread.sleep(500);
                    mReader.stop();
                    Thread.sleep(500);
                    mReader.start();
                    resetReader();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread interrupted.");
                }

            }
            while (!isStopped) {
                if (!isPaused) {
                    if (piccPowerOn()) {
                        piccTransmitGetUID();
                        piccPowerOff();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Thread interrupted.");
                }
            }
            putToSleep();
            mReader.stop();
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    acr3xStartAudioLevel, 0);
            Log.i(TAG, "NFC Reader thread done.");

        }

        private void stop() {
            Log.i(TAG, "NFC Reader thread received stop request.");
            isStopped = true;
        }

        private void pause() {
            Log.i(TAG, "NFC Reader thread received pause request.");
            isPaused = true;
        }

        private void resume() {
            Log.i(TAG, "NFC Reader thread received resume request.");
            isPaused = false;
        }

        @Override
        public void onResetComplete(AudioJackReader reader) {
            Log.i(TAG, "acr3x reset complete");
            synchronized (mResponseEvent) {
                /* Store the status. */
                //mStatus = status;
                mResetComplete = true;
                /* Trigger the response event. */
                mResponseEvent.notifyAll();
            }
        }

        @Override
        public void onFirmwareVersionAvailable(AudioJackReader reader, String firmwareVersion) {
            mListener.onFirmwareVersionAvailable(firmwareVersion);
            synchronized (mResponseEvent) {
                /* Store the status. */
                //mStatus = status;
                /* Trigger the response event. */
                mFirmwareResponseReady = true;
                mResponseEvent.notifyAll();
            }
        }

        @Override
        public void onPiccResponseApduAvailable(AudioJackReader reader, byte[] bytes) {
            /* Print out the UID */
            String uuid = bytesToHex(bytes);

            if(uuid.equalsIgnoreCase("9000")){
                mListener.operationFailure("Empty UUID.");
                return;
            }

            if(uuid.equalsIgnoreCase("6300")){
                mListener.operationFailure("Failed to get UUID.");
                return;
            }

            if(uuid.endsWith("9000")){
                uuid = uuid.substring(0, uuid.length() - 4);
            }

            if(uuid.equals(lastUuid)){
                if(new Date().getTime() - lastUuidDate.getTime() < 3000){
                    return;
                }
            }

            lastUuid = uuid;
            lastUuidDate = new Date();

            mListener.onUUIDAavailable(uuid);
            synchronized (mResponseEvent) {
                /* Store the status. */
                //mStatus = status;
                /* Trigger the response event. */
                mPiccResponseReady = true;
                mResponseEvent.notifyAll();
            }
        }

        @Override
        public void onResultAvailable(AudioJackReader audioJackReader, Result result) {
            int error_code = result.getErrorCode();
            synchronized (mResponseEvent) {
                /* Trigger the response event. */
                Log.i(TAG, mOpInProgress + " result:" + error_code);
                if (error_code != 0) {
                    if (!(mOpInProgress.equals("piccPowerOn()") && error_code == 246 )) {
                        mListener.operationFailure(mOpInProgress + " result: " + error_code);
                    }
                }

                mResultReady = true;
                mResponseEvent.notifyAll();
            }
        }

        @Override
        public void onStatusAvailable(AudioJackReader audioJackReader, Status status) {
            mListener.onStatusAvailable(status.getBatteryLevel(), status.getSleepTimeout());
            synchronized (mResponseEvent) {
                /* Store the status. */
                //mStatus = status;
                /* Trigger the response event. */
                mStatusReady = true;
                mResponseEvent.notifyAll();
            }
        }

        @Override
        public void onPiccAtrAvailable(AudioJackReader audioJackReader, byte[] bytes) {
            //System.out.println("acr3x picc ATR: " + bytesToHex(bytes));
            mListener.onPiccPowerOn(bytesToHex(bytes));
            synchronized (mResponseEvent) {
                /* Store the status. */
                //mStatus = status;
                /* Trigger the response event. */
                mPiccAtrReady = true;
                mResponseEvent.notifyAll();
            }
        }

    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}