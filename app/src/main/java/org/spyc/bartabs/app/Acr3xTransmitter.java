package org.spyc.bartabs.app;

import com.acs.audiojack.AudioJackReader;

import android.media.AudioManager;

    /**
     * This class sets up an independent thread for card polling, and is linked to the
     * <code>setOnPiccResponseApduAvailableListener</code> callback function
     */

    public class Acr3xTransmitter implements Runnable {

        private AudioJackReader mReader;
        private AudioManager mAudioManager;
        //private CallbackContext mContext;

        private boolean killMe = false;          /** Stop the polling thread? */
        private int itersWithoutResponse = 0;    /** The number of iterations that have passed with no
         response from the reader */
        private boolean readerConnected = true;  /** Is the reader currently connected? */

        private int cardType;
        private int timeout;
        private byte[] apdu;

        /**
         * @param mReader: AudioJack reader service
         * @param mAudioManager: system audio service
         * @param timeout: time in <b>seconds</b> to wait for commands to complete
         * @param apdu: byte array containing the command to be sent
         * @param cardType: the integer representing card type
         */
        public Acr3xTransmitter(AudioJackReader mReader, AudioManager mAudioManager,
                                int timeout, byte[] apdu, int cardType){
            this.mReader = mReader;
            this.mAudioManager = mAudioManager;
            this.timeout = timeout;
            this.apdu = apdu;
            this.cardType = cardType;
        }


        /**
         * Stops the polling thread
         */
        public synchronized void kill(){
            killMe = true;
        }


        /**
         * Updates the connection status of the reader (links to APDU response callback)
         */
        public synchronized void updateStatus(boolean status){
            readerConnected = status;
        }

        private synchronized boolean transmit() {
            if(killMe){
                return false;
            }
            /* If the reader is not connected, increment no. of iterations without response */
            if(!readerConnected){
                itersWithoutResponse++;
            }
            /* Else, reset the number of iterations without a response */
            else{
                itersWithoutResponse = 0;
            }
            /* Reset the connection state */
            readerConnected = false;

            if(itersWithoutResponse == 20) {
                System.out.println("acr3x disconnected");
                /* Kill this thread */
                return false;
            } else if(!mAudioManager.isWiredHeadsetOn()) {
                System.out.println("acr3x not connected");
                /* Kill this thread */
                return false;
            } else{
                System.out.println("acr3x reading...");
                /* Power on the PICC */
                mReader.piccPowerOn(timeout, cardType);
                /* Transmit the APDU */
                mReader.piccTransmit(timeout, apdu);
            }
            return true;
        }
        /**
         * Sends the APDU command for reading a card UID every second
         */
        @Override
        public void run() {
            try {
                synchronized (this) {
                    readerConnected = true;
                    killMe = false;
                }
                /* Wait one second for stability */
                Thread.sleep(1000);

                while (transmit()) {
                    Thread.sleep(1000);
                }
                /* Power off the PICC */
                mReader.piccPowerOff();
                /* Set the reader asleep */
                mReader.sleep();
                /* Stop the reader service */
                mReader.stop();

            } catch (InterruptedException e) {
                e.printStackTrace();
                // TODO: add exception handling
            }
        }

    }

