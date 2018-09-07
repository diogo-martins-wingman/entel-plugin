package pt.wingman.entel.plugin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Base64;

import com.morpho.android.usb.USBManager;
import com.morpho.morphosmart.sdk.CallbackMask;
import com.morpho.morphosmart.sdk.CallbackMessage;
import com.morpho.morphosmart.sdk.CompressionAlgorithm;
import com.morpho.morphosmart.sdk.DetectionMode;
import com.morpho.morphosmart.sdk.ErrorCodes;
import com.morpho.morphosmart.sdk.IMsoSecu;
import com.morpho.morphosmart.sdk.LatentDetection;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.morpho.morphosmart.sdk.MorphoImage;
import com.morpho.morphosmart.sdk.SecuConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import morpho.msosecu.sdk.api.MsoSecu;
import pt.wingman.entel.plugin.definitions.FingerprintError;
import pt.wingman.entel.plugin.definitions.FingerprintMessageType;
import pt.wingman.entel.plugin.definitions.FingerprintStatus;

public class FingerprintManager {
    //region static
    private static final String USB_PERMISSION = "com.morpho.android.usb.USB_PERMISSION";
    private static FingerprintManager fingerprintManager;
    private static final int callbackCmd = ((((CallbackMask.MORPHO_CALLBACK_IMAGE_CMD.getValue() | CallbackMask.MORPHO_CALLBACK_ENROLLMENT_CMD.getValue()) | CallbackMask.MORPHO_CALLBACK_COMMAND_CMD.getValue()) | CallbackMask.MORPHO_CALLBACK_CODEQUALITY.getValue()) | CallbackMask.MORPHO_CALLBACK_DETECTQUALITY.getValue()) & (~CallbackMask.MORPHO_CALLBACK_ENROLLMENT_CMD.getValue());

    public static synchronized FingerprintManager getInstance() {
        if (fingerprintManager == null)
            fingerprintManager = new FingerprintManager();
        return fingerprintManager;
    }

    static {
        try {
            System.loadLibrary("MSO_Secu");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
    //endregion

    private Context context;
    private FingerprintManagerCallback fingerprintManagerCallback;
    private MorphoDevice morphoDevice;
    private FingerPrintUsbDeviceConnection fingerPrintUsbDeviceConnection;
    private SecuConfig secuConfig;
    private int compressionAlgorithmValue;
    private int compressionRate;
    private boolean isLatentDetection;

    public void initialize(Context newContext, FingerprintManagerCallback newFingerPrintManagerCallback, int newCompressionAlgorithmValue, int newCompressionRate, boolean newLatentDetection) {
        context = newContext;
        fingerprintManagerCallback = newFingerPrintManagerCallback;
        compressionAlgorithmValue = newCompressionAlgorithmValue;
        compressionRate = newCompressionRate;
        isLatentDetection = newLatentDetection;
        morphoDevice = new MorphoDevice();
        fingerPrintUsbDeviceConnection = new FingerPrintUsbDeviceConnection();
        secuConfig = new SecuConfig();
        USBManager.getInstance().initialize(context, context.getPackageName() + ".USB_ACTION", true);
        if (USBManager.getInstance().isDevicesHasPermission()) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STARTED);
            connectToMorphoDevice();
        } else {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onError(FingerprintError.PERMISSION_DENIED);
        }
    }

    private void connectToMorphoDevice() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDeviceHashMap = usbManager.getDeviceList();
        if (usbDeviceHashMap.isEmpty()) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onError(FingerprintError.NO_DEVICE_FOUND);
        } else {
            UsbDevice usbDevice = usbDeviceHashMap.values().iterator().next();
            if (MorphoUtils.isSupported(usbDevice.getVendorId(), usbDevice.getProductId())) {
                if (usbManager.hasPermission(usbDevice)) {
                    updateUsbDeviceConnection(usbManager, usbDevice);
                } else {
                    usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(context, 0, new Intent(USB_PERMISSION), 0));
                }
            }
        }
    }

    private void updateUsbDeviceConnection(UsbManager usbManager, UsbDevice usbDevice) {
        fingerPrintUsbDeviceConnection.updateData(usbDevice, usbManager.openDevice(usbDevice));
        if (fingerPrintUsbDeviceConnection.isValidConnection()) {
            readMorphoDevice(fingerPrintUsbDeviceConnection);
        } else {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onError(FingerprintError.INVALID_CONNECTION);
        }
    }

    private void readMorphoDevice(FingerPrintUsbDeviceConnection fingerPrintUsbDeviceConnection) {
        int morphoErrorCode;
        morphoErrorCode = this.morphoDevice.openUsbDeviceFD(fingerPrintUsbDeviceConnection.sensorBus, fingerPrintUsbDeviceConnection.sensorAddress, fingerPrintUsbDeviceConnection.sensorFileDescriptor, 0);
        if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onSDKError(getSDKErrorMessage(morphoErrorCode));
            return;
        }

        morphoErrorCode = this.morphoDevice.getSecuConfig(secuConfig);
        if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onSDKError(getSDKErrorMessage(morphoErrorCode));
            return;
        }

        IMsoSecu iMsoSecu = new MsoSecu();
        iMsoSecu.setOpenSSLPath("sdcard/Keys/");
        if (secuConfig.isModeOfferedSecurity()) {
            morphoErrorCode = this.morphoDevice.offeredSecuOpen();
            if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                fingerprintManagerCallback.onSDKError(getSDKErrorMessage(morphoErrorCode));
                return;
            }
        }

        if (secuConfig.isModeTunneling()) {
            ArrayList<Byte> hostCertificate = new ArrayList<>();
            iMsoSecu.getHostCertif(hostCertificate);
            morphoErrorCode = this.morphoDevice.tunnelingOpen(MorphoUtils.toByteArray(hostCertificate));
            if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                fingerprintManagerCallback.onSDKError(getSDKErrorMessage(morphoErrorCode));
                return;
            }
        }

        getFingerPrint(observer);
    }

    public void getFingerPrint(final Observer observer) {
        new Thread(new Runnable() {
            public void run() {
                int timeOut = 0;
                int acquisitionThreshold = 0;
                MorphoImage morphoImage = new MorphoImage();
                LatentDetection latentDetection = isLatentDetection ? LatentDetection.LATENT_DETECT_ENABLE : LatentDetection.LATENT_DETECT_DISABLE;
                CompressionAlgorithm compressionAlgorithm = MorphoUtils.getCompressionAlgorithm(compressionAlgorithmValue);
                int detectModeChoice = DetectionMode.MORPHO_ENROLL_DETECT_MODE.getValue();
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.SCANNING);
                int morphoErrorCode = morphoDevice.getImage(timeOut, acquisitionThreshold, compressionAlgorithm, compressionRate, detectModeChoice, latentDetection, morphoImage, callbackCmd, observer);
                if (morphoErrorCode == ErrorCodes.MORPHO_OK) {
                    //fingerprintManagerCallback.onPercentageUpdate(100);
                    stop();
                } else {
                    fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                    fingerprintManagerCallback.onSDKError(getSDKErrorMessage(morphoErrorCode));
                }
            }
        }).start();
    }

    private Observer observer = new Observer() {
        public synchronized void update(Observable observable, Object object) {
            try {
                CallbackMessage message = (CallbackMessage) object;
                switch (message.getMessageType()) {
                    case FingerprintMessageType.FINGER_POSITION:
                        fingerprintManagerCallback.onFingerStatusUpdate((Integer) message.getMessage());
                        break;
                    case FingerprintMessageType.BITMAP_UPDATE:
                        byte[] bytes = (byte[]) message.getMessage();
                        MorphoImage morphoImage = MorphoImage.getMorphoImageFromLive(bytes);
                        int width = morphoImage.getMorphoImageHeader().getNbColumn();
                        int height = morphoImage.getMorphoImageHeader().getNbRow();
                        fingerprintManagerCallback.onBitmapUpdate(width, height, Base64.encodeToString(bytes, Base64.DEFAULT));
                        break;
                    case FingerprintMessageType.PERCENTAGE_UPDATE:
                        fingerprintManagerCallback.onPercentageUpdate((int) message.getMessage());
                        break;
                }
            } catch (Exception e) {
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                fingerprintManagerCallback.onError(FingerprintError.UNEXPECTED);
            }
        }
    };

    public void stop() {
        if (morphoDevice != null) {
            morphoDevice.cancelLiveAcquisition();
            closeDevice();
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
        }
    }

    private void closeDevice() {
        if (secuConfig.isModeOfferedSecurity()) {
            this.morphoDevice.offeredSecuClose();
        }
        if (secuConfig.isModeTunneling()) {
            this.morphoDevice.tunnelingClose();
        }
        this.morphoDevice.closeDevice();
    }

    //region Error
    private String getSDKErrorMessage(int morphoErrorCode) {
        return ErrorCodes.getError(morphoErrorCode, morphoDevice.getInternalError());
    }
    //endregion

}
