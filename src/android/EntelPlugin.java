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
            int compressionAlgorithm;
            int compressionRate;
            boolean latentDetection;
            
            try {
                JSONObject jsonObject = args.getJSONObject(0);
                compressionAlgorithm = jsonObject.getInt("compressionAlgorithm");
                compressionRate = jsonObject.getInt("compressionRate");
                latentDetection = jsonObject.getBoolean("latentDetection");
            } catch (JSONException e) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.JSON_EXCEPTION);
                callbackContext.error(e.getMessage());
                return false;
            }
         
            Context context = cordova.getActivity().getApplicationContext();
            FingerprintManager.getInstance().initialize(context, getFingerprintManagerCallback(context, callbackContext), compressionAlgorithm, compressionRate, latentDetection);

            PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        if (action.equals("stop")) {
            FingerprintManager.getInstance().stop();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        callbackContext.sendPluginResult(pluginResult);
        return true;
    }

    private FingerprintManagerCallback getFingerprintManagerCallback(Context context, CallbackContext callbackContext) {
        return new FingerprintManagerCallback() {
            public void onFingerStatusUpdate(int fingerStatus) {
                PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT, fingerStatus);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }

            public void onBitmapUpdate(int width, int height, String base64String) {

            }

            public void onPercentageUpdate(int percentage) {

            }

            public void onFingerprintStatusUpdate(int fingerprintStatus) {

            }

            public void onError(int errorCode) {
                //Toast.makeText(context, "FingerPrintManagerError: " + errorCode, Toast.LENGTH_LONG).show();
            }

            public void onSDKError(String errorMessage) {
                //Toast.makeText(context, "SDKError: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }
}
