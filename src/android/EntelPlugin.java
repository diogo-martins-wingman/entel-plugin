package pt.wingman.entel.plugin;

import android.content.Context;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pt.wingman.entel.plugin.FingerprintManager;
import pt.wingman.entel.plugin.FingerprintManagerCallback;

public class EntelPlugin extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        if (action.equals("start")) {
            JSONObject jsonObject = args.getJSONObject(0);
            int compressionAlgorithm = jsonObject.getInt("compressionAlgorithm");
            int compressionRate = jsonObject.getInt("compressionRate");
            boolean latentDetection = jsonObject.getBoolean("latentDetection");
            Context context = cordova.getActivity().getApplicationContext();
            FingerprintManager.getInstance().initialize(context, getFingerprintManagerCallback(context), compressionAlgorithm, compressionRate, latentDetection);
        }

        if ((!action.equals("stop"))) {
            FingerprintManager.getInstance().stop();
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private FingerprintManagerCallback getFingerprintManagerCallback(Context context) {
        return new FingerprintManagerCallback() {
            public void onFingerStatusUpdate(int fingerStatus) {

            }

            public void onBitmapUpdate(int width, int height, byte[] bytes) {

            }

            public void onPercentageUpdate(int percentage) {

            }

            public void onFingerprintStatusUpdate(int fingerprintStatus) {

            }

            public void onError(int errorCode) {
                Toast.makeText(context, "FingerPrintManagerError: " + errorCode, Toast.LENGTH_LONG).show();
            }

            public void onSDKError(String errorMessage) {
                Toast.makeText(context, "SDKError: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }
}
