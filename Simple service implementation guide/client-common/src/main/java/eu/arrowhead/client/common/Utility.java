/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.exception.BadPayloadException;
import eu.arrowhead.client.common.exception.DataNotFoundException;
import eu.arrowhead.client.common.exception.DnsException;
import eu.arrowhead.client.common.exception.DuplicateEntryException;
import eu.arrowhead.client.common.exception.ErrorMessage;
import eu.arrowhead.client.common.exception.UnavailableServerException;
import eu.arrowhead.client.common.misc.JacksonJsonProviderAtRest;
import eu.arrowhead.client.common.misc.PasswordGenerator;
import eu.arrowhead.client.common.misc.TypeSafeProperties;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

//Contains static utility methods for the project, most important one is the sendRequest method!
public final class Utility {

  private static Client client = createClient(null);
  private static Client sslClient;

  private static final ObjectMapper mapper = JacksonJsonProviderAtRest.getMapper();
  private static final HostnameVerifier allHostsValid = (hostname, session) -> {
    // Decide whether to allow the connection...
    return true;
  };

  private static final String DEFAULT_CONF = "default.conf";
  private static final String DEFAULT_CONF_DIR = "config" + File.separator + "default.conf";
  private static final String APP_CONF = "app.conf";
  private static final String APP_CONF_DIR = "config" + File.separator + "app.conf";


  private Utility() throws AssertionError {
    throw new AssertionError("Arrowhead Common:Utility is a non-instantiable class");
  }

  private static Client createClient(SSLContext context) {
    ClientConfig configuration = new ClientConfig();
    configuration.property(ClientProperties.CONNECT_TIMEOUT, 30000);
    configuration.property(ClientProperties.READ_TIMEOUT, 30000);

    Client client;
    if (context != null) {
      client = ClientBuilder.newBuilder().sslContext(context).withConfig(configuration).hostnameVerifier(allHostsValid).build();
    } else {
      client = ClientBuilder.newClient(configuration);
    }
    client.register(JacksonJsonProviderAtRest.class);
    return client;
  }

  public static void setSSLContext(SSLContext context) {
    sslClient = createClient(context);
  }

  //Sends a HTTP request to the given url, with the given HTTP method type and given payload
  public static <T> Response sendRequest(String uri, String method, T payload, SSLContext givenContext) {
    boolean isSecure = false;
    if (uri == null) {
      throw new NullPointerException("send (HTTP) request method received null URL");
    }
    if (uri.startsWith("https")) {
      isSecure = true;
    }

    if (isSecure && sslClient == null) {
      throw new AuthException(
          "SSL Context is not set, but secure request sending was invoked. An insecure module can not send requests to secure modules.",
          Status.UNAUTHORIZED.getStatusCode());
    }
    Client usedClient = isSecure ? givenContext != null ? createClient(givenContext) : sslClient : client;

    Builder request = usedClient.target(UriBuilder.fromUri(uri).build()).request().header("Content-type", "application/json");
    Response response; // will not be null after the switch-case
    try {
      switch (method) {
        case "GET":
          response = request.get();
          break;
        case "POST":
          response = request.post(Entity.json(payload));
          break;
        case "PUT":
          response = request.put(Entity.json(payload));
          break;
        case "DELETE":
          response = request.delete();
          break;
        default:
          throw new NotAllowedException("Invalid method type was given to the Utility.sendRequest() method");
      }
    } catch (ProcessingException e) {
      if (e.getCause().getMessage().contains("PKIX path")) {
        throw new AuthException("The system at " + uri + " is not part of the same certificate chain of trust!", Status.UNAUTHORIZED.getStatusCode(),
                                e);
      } else {
        throw new UnavailableServerException("Could not get any response from: " + uri, Status.SERVICE_UNAVAILABLE.getStatusCode(), e);
      }
    }

    // If the response status code does not start with 2 the request was not successful
    if (!(response.getStatusInfo().getFamily() == Family.SUCCESSFUL)) {
      handleException(response, uri);
    }

    return response;
  }

  public static <T> Response sendRequest(String uri, String method, T payload) {
    return sendRequest(uri, method, payload, null);
  }

  private static void handleException(Response response, String uri) {
    //The response body has to be extracted before the stream closes
    String errorMessageBody = toPrettyJson(null, response.getEntity());
    if (errorMessageBody == null || errorMessageBody.equals("null")) {
      response.bufferEntity();
      errorMessageBody = response.readEntity(String.class);
    }

    ErrorMessage errorMessage;
    try {
      errorMessage = response.readEntity(ErrorMessage.class);
    } catch (RuntimeException e) {
      throw new ArrowheadException("Unknown error occurred at " + uri, e);
    }
    if (errorMessage == null || errorMessage.getExceptionType() == null) {
      System.out.println("Request failed, response status code: " + response.getStatus());
      System.out.println("Request failed, response body: " + errorMessageBody);
      throw new ArrowheadException("Unknown error occurred at " + uri);
    } else {
      System.out.println(Utility.toPrettyJson(null, errorMessage));
      switch (errorMessage.getExceptionType()) {
        case ARROWHEAD:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case AUTH:
          throw new AuthException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_METHOD:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_PAYLOAD:
          throw new BadPayloadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case BAD_URI:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case DATA_NOT_FOUND:
          throw new DataNotFoundException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case DNSSD:
          throw new DnsException(errorMessage.getErrorMessage(), errorMessage.getErrorCode(), errorMessage.getOrigin());
        case DUPLICATE_ENTRY:
          throw new DuplicateEntryException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case GENERIC:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case JSON_PROCESSING:
          throw new ArrowheadException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
        case UNAVAILABLE:
          throw new UnavailableServerException(errorMessage.getErrorMessage(), errorMessage.getErrorCode());
      }
    }
  }

  public static String getUri(String address, int port, String serviceUri, boolean isSecure, boolean serverStart) {
    if (address == null) {
      throw new NullPointerException("Address can not be null (Utility:getUri throws NPE)");
    }

    UriBuilder ub = UriBuilder.fromPath("").host(address);
    if (isSecure) {
      ub.scheme("https");
    } else {
      ub.scheme("http");
    }
    if (port > 0) {
      ub.port(port);
    }
    if (serviceUri != null) {
      ub.path(serviceUri);
    }

    String url = ub.toString();
    try {
      new URI(url);
    } catch (URISyntaxException e) {
      if (serverStart) {
        throw new ServiceConfigurationError(url + " is not a valid URL to start a HTTP server! Please fix the address field in the properties file.");
      } else {
        throw new ArrowheadException(url + " is not a valid URL!");
      }
    }

    return url;
  }

  public static String stripEndSlash(String uri) {
    if (uri != null && uri.endsWith("/")) {
      return uri.substring(0, uri.length() - 1);
    }
    return uri;
  }

  //Fetch the request payload directly from the InputStream without a JSON serializer
  public static String getRequestPayload(InputStream is) {
    StringBuilder sb = new StringBuilder();
    String line;
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("getRequestPayload InputStreamReader has unsupported character set! Code needs to be changed!", e);
    } catch (IOException e) {
      throw new RuntimeException("IOException occured while reading an incoming request payload", e);
    }

    if (!sb.toString().isEmpty()) {
      String payload = toPrettyJson(sb.toString(), null);
      return payload != null ? payload : "";
    } else {
      return "";
    }
  }

  public static String toPrettyJson(String jsonString, Object obj) {
    try {
      if (jsonString != null) {
        jsonString = jsonString.trim();
        if (jsonString.startsWith("{")) {
          Object tempObj = mapper.readValue(jsonString, Object.class);
          return mapper.writeValueAsString(tempObj);
        } else {
          Object[] tempObj = mapper.readValue(jsonString, Object[].class);
          return mapper.writeValueAsString(tempObj);
        }
      }
      if (obj != null) {
        return mapper.writeValueAsString(obj);
      }
    } catch (IOException e) {
      throw new ArrowheadException(
          "Jackson library threw IOException during JSON serialization! Wrapping it in RuntimeException. Exception message: " + e.getMessage(), e);
    }
    return null;
  }

  public static <T> T fromJson(String json, Class<T> parsedClass) {
    try {
      return mapper.readValue(json, parsedClass);
    } catch (IOException e) {
      throw new ArrowheadException("Jackson library threw exception during JSON parsing!", e);
    }
  }

  public static String loadJsonFromFile(String pathName) {
    StringBuilder sb;
    try {
      File file = new File(pathName);
      FileInputStream is = new FileInputStream(file);

      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line).append("\n");
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException(e.getClass().toString() + ": " + e.getMessage(), e);
    }

    if (!sb.toString().isEmpty()) {
      return sb.toString();
    } else {
      return null;
    }
  }

  public static TypeSafeProperties getProp(String fileName) {
    TypeSafeProperties prop = new TypeSafeProperties();
    try {
      File file = new File("config" + File.separator + fileName);
      FileInputStream inputStream = new FileInputStream(file);
      prop.load(inputStream);
    } catch (FileNotFoundException ex) {
      throw new ServiceConfigurationError(
          fileName + " file not found, make sure you have the correct working directory set! (directory where the config folder can be found)", ex);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return prop;
  }

  public static TypeSafeProperties getProp() {
    TypeSafeProperties prop = new TypeSafeProperties();

    try {
      if (Files.isReadable(Paths.get(DEFAULT_CONF))) {
        prop.load(new FileInputStream(new File(DEFAULT_CONF)));
      } else if (Files.isReadable(Paths.get(DEFAULT_CONF_DIR))) {
        prop.load(new FileInputStream(new File(DEFAULT_CONF_DIR)));
      } else {
        throw new ServiceConfigurationError("default.conf file not found in the working directory! (" + System.getProperty("user.dir") + ")");
      }

      if (Files.isReadable(Paths.get(APP_CONF))) {
        prop.load(new FileInputStream(new File(APP_CONF)));
      } else if (Files.isReadable(Paths.get(APP_CONF_DIR))) {
        prop.load(new FileInputStream(new File(APP_CONF_DIR)));
      }
    } catch (IOException e) {
      throw new AssertionError("File loading failed...", e);
    }

    return prop;
  }

  public static void checkProperties(Set<String> propertyNames, List<String> mandatoryProperties) {
    if (mandatoryProperties == null || mandatoryProperties.isEmpty()) {
      return;
    }
    //Arrays.asList() returns immutable lists, so we have to copy it first
    List<String> properties = new ArrayList<>(mandatoryProperties);
    if (!propertyNames.containsAll(mandatoryProperties)) {
      properties.removeIf(propertyNames::contains);
      throw new ServiceConfigurationError("Missing field(s) from config file: " + properties.toString());
    }
  }

  public static boolean isHostAvailable(String host, int port, int timeout) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeout);
      return true;
    } catch (IOException e) {
      return false; // Either timeout or unreachable or failed DNS lookup.
    }
  }

  public static String getRandomPassword() {
    PasswordGenerator generator = new PasswordGenerator.Builder().useDigits(true).useLower(true).useUpper(true).usePunctuation(false).build();
    return generator.generate(12);
  }

  public static boolean isBlank(final String str) {
    return (str == null || "".equals(str.trim()));
  }
}
