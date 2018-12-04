package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.ArrowheadClientMain;
import eu.arrowhead.client.common.Utility;
import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.ExceptionType;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.model.ArrowheadService;
import eu.arrowhead.client.common.model.ArrowheadSystem;
import eu.arrowhead.client.common.model.ServiceRegistryEntry;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.UriBuilder;

/*
  BasicProviderMain class has the following mandatory functionalities:
    1) Read in command line, and config file properties
    2) Start a normal HTTP server with REST resource classes
    3) Register its service into the Service Registry
 */
public class BasicProviderMain extends ArrowheadClientMain {

  private static String SR_BASE_URI;

  public static void main(String[] args) {
    //Remove the command line argument for secure mode, if present, since the Basic Provider only operates in insecure mode.
    List<String> list = new ArrayList<>(Arrays.asList(args));
    list.remove("-tls");
    args = list.toArray(new String[0]);
    new BasicProviderMain(args);
  }

  private BasicProviderMain(String[] args) {
    //Register the application components the REST library need to know about
    //Set<Class<?>> classes = new HashSet<>(Arrays.asList(TemperatureResource.class, RestResource.class));
    Set<Class<?>> classes = new HashSet<>(Arrays.asList(RPMResource.class, RestResource.class));
    String[] packages = {"eu.arrowhead.client.common"};
    //This (inherited) method reads in the configuration properties, and starts the web server
    init(ClientType.PROVIDER, args, classes, packages);

    //Compile the base of the Service Registry URL
    getServiceRegistryUrl();
    //Compile the request payload
    ServiceRegistryEntry entry = compileRegistrationPayload();
    //Send the registration to the Service Registry
    registerToServiceRegistry(entry);

    //Listen for a stop command
    listenForInput();
  }

  private void getServiceRegistryUrl() {
    String srAddress = props.getProperty("sr_address", "0.0.0.0");
    int srPort = props.getIntProperty("sr_insecure_port", 8442);
    SR_BASE_URI = Utility.getUri(srAddress, srPort, "serviceregistry", false, false);
  }

  private ServiceRegistryEntry compileRegistrationPayload() {
    //Compile the ArrowheadService (providedService)
    String serviceDef = props.getProperty("service_name");
    String serviceUri = props.getProperty("service_uri");
    String interfaceList = props.getProperty("interfaces");
    Set<String> interfaces = new HashSet<>();
    if (interfaceList != null && !interfaceList.isEmpty()) {
      //Interfaces are read from a comma separated list
      interfaces.addAll(Arrays.asList(interfaceList.replaceAll("\\s+", "").split(",")));
    }
    Map<String, String> metadata = new HashMap<>();
    String metadataString = props.getProperty("metadata");
    if (metadataString != null && !metadataString.isEmpty()) {
      //Metadata in the properties file: key1-value1, key2-value2, ...
      String[] parts = metadataString.split(",");
      for (String part : parts) {
        String[] pair = part.split("-");
        metadata.put(pair[0], pair[1]);
      }
    }
    ArrowheadService service = new ArrowheadService(serviceDef, interfaces, metadata);

    //Compile the ArrowheadSystem (provider)
    URI uri;
    try {
      uri = new URI(baseUri);
    } catch (URISyntaxException e) {
      throw new AssertionError("Parsing the BASE_URI resulted in an error.", e);
    }
    String insecProviderName = props.getProperty("insecure_system_name");
    ArrowheadSystem provider = new ArrowheadSystem(insecProviderName, uri.getHost(), uri.getPort(), null);

    //Return the complete request payload
    return new ServiceRegistryEntry(service, provider, serviceUri);
  }

  private void registerToServiceRegistry(ServiceRegistryEntry entry) {
    //Create the full URL (appending "register" to the base URL)
    String registerUri = UriBuilder.fromPath(SR_BASE_URI).path("register").toString();

    //Send the registration request
    try {
      Utility.sendRequest(registerUri, "POST", entry);
    } catch (ArrowheadException e) {
      /*
        Service Registry might return duplicate entry exception, if a previous instance of the web server already registered this service,
        and the deregistration did not happen. It's better to unregister the old entry, in case the request payload changed.
       */
      if (e.getExceptionType() == ExceptionType.DUPLICATE_ENTRY) {
        System.out.println("Received DuplicateEntryException from SR, sending delete request and then registering again.");
        unregisterFromServiceRegistry(entry);
        Utility.sendRequest(registerUri, "POST", entry);
      } else {
        throw e;
      }
    }
    System.out.println("Registering service is successful!");
  }

  private void unregisterFromServiceRegistry(ServiceRegistryEntry entry) {
    //Create the full URL (appending "remove" to the base URL)
    String removeUri = UriBuilder.fromPath(SR_BASE_URI).path("remove").toString();
    Utility.sendRequest(removeUri, "PUT", entry);
    System.out.println("Removing service is successful!");
  }

}