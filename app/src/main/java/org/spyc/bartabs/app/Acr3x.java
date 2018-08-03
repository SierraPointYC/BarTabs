package org.spyc.bartabs.app;

import com.acs.audiojack.AudioJackReader;

import android.content.Context;
import android.media.AudioManager;

import java.util.Date;

/**
 * This class allows control of the ACR35 reader sleep state and PICC commands
 */
public class Acr3x {

    private Acr3xTransmitter transmitter;
    private Thread mTransmitterThread;
    private AudioManager mAudioManager;
    private AudioJackReader mReader;

    private boolean firstReset = true;  /** Is this the first reset of the reader? */

    /** APDU command for reading a card's UID */
    private final byte[] apdu = { (byte) 0xFF, (byte) 0xCA, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
    /** Timeout for APDU response (in <b>seconds</b>) */
    private final int timeout = 1;
    private final int acr3xCardType = AudioJackReader.PICC_CARD_TYPE_ISO14443_TYPE_A;
//            | AudioJackReader.PICC_CARD_TYPE_ISO14443_TYPE_B
//            | AudioJackReader.PICC_CARD_TYPE_FELICA_212KBPS
//            | AudioJackReader.PICC_CARD_TYPE_FELICA_424KBPS
//            | AudioJackReader.PICC_CARD_TYPE_AUTO_RATS;

    private int acr3xStartAudioLevel = 0;

    private String lastUuid = "";
    private Date lastUuidDate = new Date();

    private static final Acr3x mInstance = new Acr3x();

    public static Acr3x getInstance() {
        return  mInstance;
    }

    private Acr3x() {

    }

    public void setAudioManager(AudioManager audioManager) {
        mAudioManager = audioManager;
        mReader = new AudioJackReader(mAudioManager);
        transmitter = new Acr3xTransmitter(mReader, mAudioManager, timeout,
                apdu, acr3xCardType);
    }

    public void start(final Acr3xNotifListener listener){
        Runnable r = new Runnable(){

            @Override
            public void run() {
                System.out.println("ACR35 reader start");

                acr3xStartAudioLevel = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                System.out.println("acr3x start audio stream level: " + acr3xStartAudioLevel);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                System.out.println("acr3x set audio stream level: " + mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

                mReader.start();
                mReader.setSleepTimeout(30);
                mReader.setOnFirmwareVersionAvailableListener(new AudioJackReader.OnFirmwareVersionAvailableListener() {
                    @Override
                    public void onFirmwareVersionAvailable(AudioJackReader reader,
                                                           String firmwareVersion) {
                        System.out.println("acr3x firmware version: " + firmwareVersion);
                        if(listener != null){
                            listener.onFirmwareVersionAvailable(firmwareVersion);
                        }
                        Acr3x.this.read(listener);

                    }
                });
                mReader.reset(new AudioJackReader.OnResetCompleteListener(){

                    @Override
                    public void onResetComplete(AudioJackReader arg0) {
                        //mReader.getFirmwareVersion();
                        Acr3x.this.read(listener);

                    }

                });
            }
        };

        Thread t = new Thread(r, "Acr3xInitThread");
        t.start();

    }


    /**
     * Sets the ACR35 reader to continuously poll for the presence of a card. If a card is found,
     * the UID will be returned to the Apache Cordova application
     *
     * @param callbackContext: the callback context provided by Cordova
     */
    public void read(final Acr3xNotifListener callbackContext){
        System.out.println("acr3x setting up for reading...");
        firstReset = false;

        /* Set the PICC response APDU callback */
        mReader.setOnPiccResponseApduAvailableListener
                (new AudioJackReader.OnPiccResponseApduAvailableListener() {
                    @Override
                    public void onPiccResponseApduAvailable(AudioJackReader reader,
                                                            byte[] responseApdu) {
                        /* Update the connection status of the transmitter */
                        transmitter.updateStatus(true);

                        /* Print out the UID */
                        String uuid = bytesToHex(responseApdu);

                        if(uuid.equalsIgnoreCase("9000")){
                            return;
                        }

                        if(uuid.equalsIgnoreCase("6300")){
                            return;
                        }

                        if(uuid.endsWith("9000")){
                            uuid = uuid.substring(0, uuid.length() - 4);
                        }

                        if(uuid.equals(lastUuid)){ // na odfiltrovanie opakujucich sa uuid z citacky z predchadzajuceho citania
                            if(new Date().getTime() - lastUuidDate.getTime() < 3000){
                                return;
                            }
                        }

                        lastUuid = uuid;
                        lastUuidDate = new Date();

                        System.out.println("acr3x uuid: " + uuid);

                        if(callbackContext != null){
                            callbackContext.onUUIDAavailable(uuid);
                        }
                        System.out.println("acr3x restarting reader");
                        stopTransmitter();
                        read(callbackContext);
                    }
                });

        /* Set the reset complete callback */
        mReader.setOnResetCompleteListener(new AudioJackReader.OnResetCompleteListener() {
            @Override
            public void onResetComplete(AudioJackReader reader) {
                System.out.println("acr3x reset complete");

                /* If this is the first reset, the ACR35 reader must be turned off and back on again
                   to work reliably... */
                if(firstReset){  //firstReset
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            try{
                                /* Set the reader asleep */
                                mReader.sleep();
                                /* Wait one second */
                                Thread.sleep(500);
                                /* Reset the reader */
                                mReader.reset();

                                firstReset = false;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                // TODO: add exception handling
                            }
                        }
                    });
                    t.start();
                } else {
                    /* Create a new transmitter for the UID read command */
                    startTransmitter();
                }
            }
        });

        mReader.start();
        mReader.reset();
        System.out.println("acr3x setup complete");
    }

    private void startTransmitter() {
        mTransmitterThread = new Thread(transmitter);
        mTransmitterThread.start();
    }

    private void stopTransmitter() {
        if(transmitter != null){
            transmitter.kill();
            try {
                if (mTransmitterThread!= null) {
                    mTransmitterThread.join(10000);
                }
                transmitter = null;
                mTransmitterThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop(){

        stopTransmitter();

        System.out.println("acr3x restoring audio level: " + acr3xStartAudioLevel);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,acr3xStartAudioLevel, 0);
        System.out.println("acr3x set audio stream level: " + mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        if(mReader != null){
            mReader.stop();
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}