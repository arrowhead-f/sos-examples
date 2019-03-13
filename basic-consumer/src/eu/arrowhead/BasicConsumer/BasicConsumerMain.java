/*
 * Copyright (c) 2018 AITIA International Inc.
 *
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.BasicConsumer;

import com.google.gson.Gson;
import eu.arrowhead.BasicConsumer.model.ArrowheadSystem;
import eu.arrowhead.BasicConsumer.model.OrchestrationResponse;
import eu.arrowhead.BasicConsumer.model.TemperatureReadout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.json.JSONException;
import org.json.JSONObject;

//Simple java project with minimal dependencies
public class BasicConsumerMain {

  private static Properties prop;
  private static final String ORCH_URI = getProp().getProperty("orch_uri", "http://localhost:8440/orchestrator/orchestration");

  public static void main(String[] args) throws Exception {
    String payload = compileSRF();
    System.out.println("Sending this request form for the Orchestrator: " + payload);

    String providerURI = sendServiceRequest(payload);
    System.out.println("Received provider system URL: " + providerURI);

    double temperature = connectToProvider(providerURI);
    System.out.println("The indoor temperature is " + temperature + " degrees celsius.");
  }

  private static String compileSRF() throws JSONException {
    JSONObject requesterSystem = new JSONObject();
    JSONObject requestedService = new JSONObject();
    JSONObject orchestrationFlags = new JSONObject();

    requesterSystem.put("systemName", "client1");
    requesterSystem.put("address", "localhost");

    requestedService.put("serviceDefinition", "IndoorTemperature");
    List<String> interfaces = new ArrayList<>();
    interfaces.add("json");
    requestedService.put("interfaces", interfaces);

    orchestrationFlags.put("overrideStore", true);
    orchestrationFlags.put("matchmaking", true);

    JSONObject payload = new JSONObject();
    payload.put("requesterSystem", requesterSystem);
    payload.put("requestedService", requestedService);
    payload.put("orchestrationFlags", orchestrationFlags);

    return payload.toString(4);
  }

  private static String sendServiceRequest(String payload) throws Exception {
    URL url = new URL(ORCH_URI);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    connection.setRequestProperty("Accept", "application/json");
    connection.setRequestMethod("POST");

    OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
    wr.write(payload);
    wr.close();

    StringBuilder sb = new StringBuilder();
    int HttpResult = connection.getResponseCode();
    if (HttpResult == HttpURLConnection.HTTP_OK) {
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      br.close();
    } else {
      throw new Exception(connection.getResponseMessage());
    }

    System.out.println("Orchestrator response : " + sb.toString());
    OrchestrationResponse response = new Gson().fromJson(sb.toString(), OrchestrationResponse.class);
    ArrowheadSystem provider = response.getResponse().get(0).getProvider();
    String serviceUri = response.getResponse().get(0).getServiceURI();

    if (provider.getPort() > 0) {
      if (serviceUri == null) {
        return "http://" + provider.getAddress() + ":" + provider.getPort();
      } else if (serviceUri.startsWith("/")) {
        return "http://" + provider.getAddress() + ":" + provider.getPort() + serviceUri;
      } else {
        return "http://" + provider.getAddress() + ":" + provider.getPort() + "/" + serviceUri;
      }
    } else {
      if (serviceUri == null) {
        return "http://" + provider.getAddress();
      } else if (serviceUri.startsWith("/")) {
        return "http://" + provider.getAddress() + serviceUri;
      } else {
        return "http://" + provider.getAddress() + "/" + serviceUri;
      }
    }
  }

  private static double connectToProvider(String URL) throws Exception {
    URL url = new URL(URL);

    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
    connection.setRequestProperty("charset", "utf-8");
    connection.setRequestMethod("GET");

    StringBuilder sb = new StringBuilder();
    int HttpResult = connection.getResponseCode();
    if (HttpResult == HttpURLConnection.HTTP_OK) {
      BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      br.close();
    } else {
      throw new Exception(connection.getResponseMessage());
    }

    TemperatureReadout readout = new Gson().fromJson(sb.toString(), TemperatureReadout.class);
    if (readout.getE().get(0) == null) {
      throw new RuntimeException("Provider did not send any MeasurementEntry.");
    } else {
      return readout.getE().get(0).getV();
    }
  }

  private static synchronized Properties getProp() {
    try {
      if (prop == null) {
        prop = new Properties();
        File file = new File("config" + File.separator + "app.properties");
        FileInputStream inputStream = new FileInputStream(file);
        prop.load(inputStream);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return prop;
  }
}
