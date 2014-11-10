package com.noelportugal.halloween.nfc;

import com.noelportugal.halloween.hue.HueBridgeLink;
import com.noelportugal.halloween.hue.HueBridgeLinkCallback;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import java.util.List;
import java.util.Random;


public class Main implements HueBridgeLinkCallback {
    static final byte PN532_MIFARE_ISO14443A = 0x00;
    static final String defaultColor = "0 0 0";
    
    private Random rand = new Random();

    private static PHHueSDK phHueSDK;
    private static PHBridge bridge;
    private static PHLightState lightState;
    private static final int maxBrightness = 254;
    private static final int minBrightness = 15;
    
    private static final String[] UIDS = {
        "36076607291754756",
        "36358082404879876",
        "36358082404944388"
    };
    
    private static final String[] GROUPS = {
        "AL", // 1
        "BM", // 2
        "CD", // 3
        "E",   // 4
        "FGHIJK",   // 5
    };  
    private static final int[][] GROUP_COLORS = {
        {0,255,0}, // 1 Green
        {255,0,0}, // 2 Red
        {50,0,255}, // 3 Blue
        {200,255,0}, // 4 Yellow
        {255,0,255}, // 5 Purlple
    };
    private static final int MAX_HUE = 65535;
    private int randomColor;
    
    public Main(){
        // Prepare the Hue Bridge link
        HueBridgeLink bridgeLink = new HueBridgeLink();
        bridgeLink.connect(this);
        phHueSDK = PHHueSDK.getInstance();
    
    }
    
    @Override
    public void onBridgeReady() {
        bridge = phHueSDK.getSelectedBridge();
        allOff();
    }
    
    private static float[] rgbGroupColor (int[] groupColors, PHLight light) {
        return PHUtilities.calculateXYFromRGB(groupColors[0], groupColors[1], groupColors[2], light.getModelNumber());
    }
    
    private static long getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }
    
    private void start() throws InterruptedException{
        IPN532Interface pn532Interface = new PN532Spi();
        PN532 nfc = new PN532(pn532Interface);
        
        System.out.println("Starting up...");
        nfc.begin();
        Thread.sleep(1000);

        long versiondata = nfc.getFirmwareVersion();
        if (versiondata == 0) {
            System.out.println("Didn't find PN53x board");
            return;
        }
        
        System.out.print("Found chip PN5: ");
        System.out.println(Long.toHexString((versiondata >> 24) & 0xFF));
	System.out.print("Firmware versioin: ");
        System.out.print(Long.toHexString((versiondata >> 16) & 0xFF));
        System.out.print('.');
        System.out.println(Long.toHexString((versiondata >> 8) & 0xFF));
        // configure board to read RFID tags
        nfc.SAMConfig();
        System.out.println("Station ready to Scan");
        byte[] buffer = new byte[8];
        
        String winner = UIDS[rand.nextInt(2)];

        
        while (true) {
            int readLength = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, buffer);

            if (readLength > 0) {
                String uid = "";
                System.out.print("    UID: ");
                uid = String.valueOf(getDec(buffer));
                System.out.println(uid);
                
                try {
                                   

                    if (uid.equals(winner)){
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/mpg321", "-q","/home/pi/Sounds/win1.mp3"});
                        win();
                        winner = UIDS[rand.nextInt(2)];
                    }else{
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/mpg321", "-q","/home/pi/Sounds/fail.mp3"});
                        fail();
                    }
                    
                    
                    // wait for 1 seconds before next scan
                    Thread.sleep(1000);
                    System.out.println("Station ready to Scan");
                } 
                catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
            // sleep for 200 ms before going back to scan for NFC tag
            Thread.sleep(200);
        }
        
    }
    
    private void allOff(){
        lightState = new PHLightState();
        lightState.setOn(false);
        bridge.setLightStateForDefaultGroup(lightState);
    }
    
    private void fail() throws InterruptedException{
        lightState = new PHLightState();
        lightState.setOn(true);
        lightState.setHue(65280);
        bridge.setLightStateForDefaultGroup(lightState);
        Thread.sleep(2000);
        allOff();
        
    }
    private void win(){
                    System.out.println("Starting Random Light Thread");
            PHBridge bridge = phHueSDK.getSelectedBridge();
            List<PHLight> allLights = bridge.getResourceCache().getAllLights();
            Random rand = new Random();
            
            int i =0;
            while (i++ < 6) {
                System.out.println("while loop: " + i);
                try {
                    PHLightState lightState = new PHLightState();
                    lightState.setOn(true);
                    randomColor = rand.nextInt(MAX_HUE);
                    lightState.setBrightness(maxBrightness);
                    lightState.setHue(randomColor);
                    bridge.setLightStateForDefaultGroup(lightState);
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    System.err.println("error:" + ex.getMessage());
                }
            }
            
            allOff();

    }
       
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Exchange Hue Lights Server");
        Main halloween = new Main();
        halloween.start();
    }
    

}