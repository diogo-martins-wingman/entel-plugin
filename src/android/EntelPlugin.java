package com.stanleyidesis.cordova.plugin;
// The native Toast API
import android.widget.Toast;
// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
public class EntelPlugin extends CordovaPlugin {
  private static final String DURATION_LONG = "long";
  @Override
  public boolean execute(String action, JSONArray args,
    final CallbackContext callbackContext) {
      // Verify that the user sent a 'show' action
      Toast.makeText(cordova.getActivity(), "dsa", Toast.LENGTH_LONG).show();
      if (!action.equals("show")) {
          System.out.println("teste");
        callbackContext.error("\"" + action + "\" is not a recognized action.");
        return false;
      }
      String message;
      String duration;
      try {
        JSONObject options = args.getJSONObject(0);
        message = options.getString("message");
        duration = options.getString("duration");
      } catch (JSONException e) {
        callbackContext.error("Error encountered: " + e.getMessage());
        return false;
      }
      // Create the toast
      Toast toast = Toast.makeText(cordova.getActivity(), message,
        DURATION_LONG.equals(duration) ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
      // Display toast
      toast.show();
      // Send a positive result to the callbackContext
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
      callbackContext.sendPluginResult(pluginResult);
      return true;
  }
}