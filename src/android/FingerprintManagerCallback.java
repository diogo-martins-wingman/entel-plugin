package pt.wingman.entel.plugin;

public interface FingerprintManagerCallback {
    void onFingerStatusUpdate(int fingerStatus);

    void onBitmapUpdate(int width, int height, byte[] bytes);

    void onPercentageUpdate(int percentage);

    void onFingerprintStatusUpdate(int fingerprintStatus);

    void onError(int errorCode);

    void onSDKError(String errorMessage);
}
