/**
 * Request code to get a status response from controller.
 * @const
 */
var REQUEST_STATUS = 3004;

/**
 * All following are temparature values, factor 10, Celsius.
 * @const
 * @type {number}
 */
var INDEX_TEMP_OUTGOING = 10;
var INDEX_TEMP_RETURN = 11;
var INDEX_TEMP_RETURN_SHOULD = 12;
var INDEX_TEMP_OUTDOORS = 15;
var INDEX_TEMP_WATER = 17;
var INDEX_TEMP_WATER_SHOULD = 18;
var INDEX_TEMP_SOURCE_IN = 19;
var INDEX_TEMP_SOURCE_OUT = 20;

/** 
 * All following are time values, Seconds.
 * @const
 */
var INDEX_TIME_PUMP_ACTIVE = 67;
var INDEX_TIME_REST = 71;
var INDEX_TIME_COMPRESSOR_NOOP = 73;
var INDEX_TIME_RETURN_LOWER = 74;
var INDEX_TIME_RETURN_HIGHER = 75;

function HeatingDisplayControl($scope) {
  // localized strings
  $scope.strAppDescription = chrome.i18n.getMessage("appDesc");
  $scope.strTitleSettings = chrome.i18n.getMessage("titleSettings");
  $scope.strLabelHost = chrome.i18n.getMessage("hostname");
  $scope.strLabelPort = chrome.i18n.getMessage("port");
  $scope.strTitleAbout = chrome.i18n.getMessage("titleAbout");
  
  $scope.strLabelTempOutgoing = chrome.i18n.getMessage("tempOutgoing");
  $scope.strLabelTempReturn = chrome.i18n.getMessage("tempReturn");
  $scope.strLabelTempReturnShould = chrome.i18n.getMessage("tempReturnShould");
  $scope.strLabelTempOutdoors = chrome.i18n.getMessage("tempOutdoors");
  $scope.strLabelTempWater = chrome.i18n.getMessage("tempWater");
  $scope.strLabelTempWaterShould = chrome.i18n.getMessage("tempWaterShould");
  $scope.strLabelTempSourceIn = chrome.i18n.getMessage("tempSourceIn");
  $scope.strLabelTempSourceOut = chrome.i18n.getMessage("tempSourceOut");

  $scope.strLabelTimeReceived = chrome.i18n.getMessage("timeReceived");
  $scope.strLabelTimeCompressorNoop = chrome.i18n.getMessage("timeCompressorNoop");
  $scope.strLabelTimePumpActive = chrome.i18n.getMessage("timePumpActive");
  $scope.strLabelTimeRest = chrome.i18n.getMessage("timeRest");
  $scope.strLabelTimeReturnLower = chrome.i18n.getMessage("timeReturnLower");
  $scope.strLabelTimeReturnHigher = chrome.i18n.getMessage("timeReturnHigher");

  $scope.host = "waermepumpe";
  $scope.port = 8888;

  $scope.tcpClient;
  $scope.timeoutRunnable;

  $scope.refreshIntervalMs = 2000;

  $scope.timeReceived = "00:00:00";
  $scope.timeCompressorNoop = "0 h 0 min 0 sec";
  $scope.timePumpActive = "0 h 0 min 0 sec";
  $scope.timeRest = "0 h 0 min 0 sec";
  $scope.timeReturnLower = "0 h 0 min 0 sec";
  $scope.timeReturnHigher = "0 h 0 min 0 sec";

  $scope.tempOutdoors = 0.0;
  $scope.tempOutgoing = 0.0;
  $scope.tempReturn = 0.0;
  $scope.tempReturnShould = 0.0;
  $scope.tempWater = 0.0;
  $scope.tempWaterShould = 0.0;
  $scope.tempSourceIn = 0.0;
  $scope.tempSourceOut = 0.0;

  $scope.textStatus = chrome.i18n.getMessage("disconnected");
  $scope.textBtnConnect = chrome.i18n.getMessage("connect");
  $scope.textRequestStatus = "OFF";

  $scope.isRequestStatus = false;
  $scope.isShowingSettings = false;

    // Notice that chrome.storage.sync.get is asynchronous
  chrome.storage.sync.get('connectionSettings', function(value) {
    // The $apply is only necessary to execute the function inside Angular scope
    $scope.$apply(function() {
      $scope.loadSettings(value);
    });
  });

  // If there is saved data in storage, use it.
  $scope.loadSettings = function(value) {
    if (value && value.connectionSettings) {
      if (value.connectionSettings.host) {
        $scope.host = value.connectionSettings.host;
      }
      if (value.connectionSettings.port) {
        $scope.port = value.connectionSettings.port;
      }
    }
  } 

  $scope.saveSettings = function() {
    var settings = { host: $scope.host, port: $scope.port };
    chrome.storage.sync.set({'connectionSettings': settings});
  };

  $scope.toggleConnection = function() {
    if (!$scope.tcpClient || !$scope.tcpClient.isConnected) {
      // TODO get host and port from settings UI
      $scope.disconnect();
      $scope.connect();
      $scope.textBtnConnect = chrome.i18n.getMessage("disconnect");
    } else {
      $scope.disconnect();
      $scope.textBtnConnect = chrome.i18n.getMessage("connect");
    }
  }

  $scope.connect = function() {
    $scope.tcpClient = new TcpClient($scope.host, $scope.port);
    $scope.tcpClient.connect(function () {
      // connected, display status
      $scope.textStatus = chrome.i18n.getMessage("connected") + " " + $scope.host + ":" + $scope.port;

      // add response listener
      $scope.tcpClient.addResponseListener(function (data) {
        if (data[0] != REQUEST_STATUS) {
          $scope.textStatus = "Invalid response: request code does not match";
          return;
        }
        
        // current time
        $scope.timeReceived = moment().lang(chrome.i18n.getMessage("@@ui_locale")).format("L HH:mm:ss");
        
        // time values
        $scope.timeCompressorNoop = $scope.getValue(data, INDEX_TIME_COMPRESSOR_NOOP);
        $scope.timePumpActive = $scope.getValue(data, INDEX_TIME_PUMP_ACTIVE);
        $scope.timeRest = $scope.getValue(data, INDEX_TIME_REST);
        $scope.timeReturnLower = $scope.getValue(data, INDEX_TIME_RETURN_LOWER);
        $scope.timeReturnHigher = $scope.getValue(data, INDEX_TIME_RETURN_HIGHER);

        // temp values
        $scope.tempOutdoors = $scope.getValue(data, INDEX_TEMP_OUTDOORS);
        $scope.tempOutgoing = $scope.getValue(data, INDEX_TEMP_OUTGOING);
        $scope.tempReturn = $scope.getValue(data, INDEX_TEMP_RETURN);
        $scope.tempReturnShould = $scope.getValue(data, INDEX_TEMP_RETURN_SHOULD);
        $scope.tempWater = $scope.getValue(data, INDEX_TEMP_WATER);
        $scope.tempWaterShould = $scope.getValue(data, INDEX_TEMP_WATER_SHOULD);
        $scope.tempSourceIn = $scope.getValue(data, INDEX_TEMP_SOURCE_IN);
        $scope.tempSourceOut = $scope.getValue(data, INDEX_TEMP_SOURCE_OUT);

        // force angular to update watched values
        $scope.$apply();

        // schedule next request?
        if ($scope.isRequestStatus) {
          $scope.timeoutRunnable = window.setTimeout(function () {
            $scope.requestStatus();
          }, $scope.refreshIntervalMs);
        }
      });

      // start requesting status updates
      $scope.setStatusUpdatesState(true);
    });
  }

  $scope.disconnect = function() {
    $scope.setStatusUpdatesState(false);
    if ($scope.tcpClient) {
      $scope.textStatus = chrome.i18n.getMessage("disconnected");
      $scope.tcpClient.disconnect();
    }
  }

  $scope.setStatusUpdatesState = function(isEnabled) {
    $scope.isRequestStatus = isEnabled;
    if (isEnabled) {
      $scope.textRequestStatus = "ON";
      $scope.requestStatus();
    } else {
      window.clearTimeout($scope.timeoutRunnable);
      $scope.textRequestStatus = "OFF";
    }
  }

  $scope.requestStatus = function() {
    if ($scope.tcpClient) {
      $scope.tcpClient.sendInteger(REQUEST_STATUS);
    }
  }

  $scope.getValue = function(array, index) {
    switch (index) {
      case INDEX_TEMP_OUTGOING:
      case INDEX_TEMP_RETURN:
      case INDEX_TEMP_RETURN_SHOULD:
      case INDEX_TEMP_OUTDOORS:
      case INDEX_TEMP_WATER:
      case INDEX_TEMP_WATER_SHOULD:
      case INDEX_TEMP_SOURCE_IN:
      case INDEX_TEMP_SOURCE_OUT:
        return $scope.getTemperatureValue(array, index);
      case INDEX_TIME_RETURN_HIGHER:
      case INDEX_TIME_RETURN_LOWER:
      case INDEX_TIME_COMPRESSOR_NOOP:
      case INDEX_TIME_PUMP_ACTIVE:
      case INDEX_TIME_REST:
        return $scope.getTimeValue(array, index);
    }
    return "n/a";
  }

  $scope.getTemperatureValue = function(array, index) {
    // offset by 3 (exclude request code, status code and length field)
    return (array[index + 3] / 10);
  }

  $scope.getTimeValue = function(array, index) {
    // offset by 3 (exclude request code, status code and length field)
    var seconds = array[index + 3];

    var hours = (seconds - (seconds % 3600)) / 3600;
    seconds = seconds - (hours * 3600);
    var minutes = (seconds - (seconds % 60)) / 60;
    seconds = seconds % 60;
    return hours + " h " + minutes + " min " + seconds + " sec";
  }

  $scope.toggleSettings = function() {
    $scope.isShowingSettings = !$scope.isShowingSettings;
  }

}
