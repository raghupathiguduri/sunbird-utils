package org.sunbird.telemetry.util;

import com.google.gson.Gson;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpHeaders;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.models.util.PropertiesCache;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.TelemetryV3Request;

/**
 * Dispatcher for telemetry data to Sunbird telemetry service. Sunbird telemetry service is
 * responsible for storing telemetry data in Sunbird and/or Ekstep platform based on configuration.
 *
 * @author Manzarul
 */
public class SunbirdTelemetryEventConsumer {

  private static SunbirdTelemetryEventConsumer consumer = new SunbirdTelemetryEventConsumer();

  private SunbirdTelemetryEventConsumer() {}

  public static SunbirdTelemetryEventConsumer getInstance() {
    if (null == consumer) {
      consumer = new SunbirdTelemetryEventConsumer();
    }
    return consumer;
  }

  public void consume(Request request) {
    ProjectLogger.log("SunbirdTelemetryEventConsumer:consume called.", LoggerEnum.INFO.name());
    if (request != null) {
      try {
    	String telemetryReq =  new Gson().toJson(getTelemetryRequest(request));
    	ProjectLogger.log("SunbirdTelemetryEventConsumer:consume telemetry request:" + telemetryReq, LoggerEnum.INFO.name());
        String response =
            HttpUtil.sendPostRequest(
                getTelemetryUrl(),telemetryReq , getHeaders());
        ProjectLogger.log(
            "SunbirdTelemetryEventConsumer:consume: Request process status = " + response,
            LoggerEnum.INFO.name());
      } catch (Exception e) {
        ProjectLogger.log(
            "SunbirdTelemetryEventConsumer:consume: Generic exception occurred in sending telemetry request = "
                + e.getMessage(),
            e);
        ProjectLogger.log(
            "SunbirdTelemetryEventConsumer:consume: Failure request = "
                + new Gson().toJson(getTelemetryRequest(request)),
            LoggerEnum.INFO.name());
      }
    }
  }

  public Map<String, String> getHeaders() {
    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    return headers;
  }

  /**
   * This method will return telemetry url. it will read sunbird_lms_base_url key for base url .
   * First it will try to read value from environment in case of absence it will read value from
   * property cache.
   *
   * @return Complete url for telemetry service.
   */
  public String getTelemetryUrl() {
    String telemetryBaseUrl =
        ProjectUtil.getConfigValue(JsonKey.SUNBIRD_TELEMETRY_BASE_URL)
            + PropertiesCache.getInstance().getProperty(JsonKey.SUNBIRD_TELEMETRY_API_PATH);
    ProjectLogger.log(
        "SunbirdTelemetryEventConsumer:getTelemetryUrl: url = " + telemetryBaseUrl,
        LoggerEnum.INFO.name());
    return telemetryBaseUrl;
  }

  /**
   * This method will transform incoming requested data to Telemetry request structure.
   *
   * @param request Request that contains telemetry data generated by Sunbird.
   * @return Telemetry request structure.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Map<String, Object> getTelemetryRequest(Request request) {
    TelemetryV3Request telemetryV3Request = new TelemetryV3Request();
    if (request.getRequest().get(JsonKey.ETS) != null
        && request.getRequest().get(JsonKey.ETS) instanceof BigInteger) {
      telemetryV3Request.setEts(((BigInteger) request.getRequest().get(JsonKey.ETS)).longValue());
    }
    if (request.getRequest().get(JsonKey.EVENTS) != null
        && request.getRequest().get(JsonKey.EVENTS) instanceof List
        && !(((List) request.getRequest().get(JsonKey.EVENTS)).isEmpty())) {
      List<Map<String, Object>> events =
          (List<Map<String, Object>>) request.getRequest().get(JsonKey.EVENTS);
      telemetryV3Request.setEvents(events);
      ProjectLogger.log(
          "SunbirdTelemetryEventConsumer:getTelemetryRequest: Events count = " + events.size(),
          LoggerEnum.INFO.name());
    }
    Map<String, Object> map = new HashMap<>();
    map.put(JsonKey.REQUEST, telemetryV3Request);
    return map;
  }
}
