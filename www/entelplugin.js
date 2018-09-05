// Empty constructor
function EntelPlugin() {}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'
ToastyPlugin.prototype.show = function(message, duration, successCallback, errorCallback) {
  var options = {};
  options.message = message;
  options.duration = duration;
  cordova.exec(successCallback, errorCallback, 'EntelPlugin', 'show', [options]);
}

// Installation constructor that binds ToastyPlugin to window
EntelPlugin.install = function() {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.entelPlugin = new EntelPlugin();
  return window.plugins.entelPlugin;
};
cordova.addConstructor(EntelPlugin.install);