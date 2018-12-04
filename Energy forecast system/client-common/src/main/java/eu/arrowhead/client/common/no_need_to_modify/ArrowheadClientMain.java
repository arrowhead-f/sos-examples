/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.no_need_to_modify;

import eu.arrowhead.client.common.can_be_modified.misc.ClientType;
import eu.arrowhead.client.common.no_need_to_modify.exception.AuthException;
import eu.arrowhead.client.common.no_need_to_modify.misc.SecurityUtils;
import eu.arrowhead.client.common.no_need_to_modify.misc.TypeSafeProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/*
  Common base class for Client main classes.
  Functionalities:
    1) Read in basic command line arguments
    2) Read in the contents of app.properties, check for missing mandatory values
    3) Start an insecure/secure web server (this might be optional in the future)
    4) Listen for exit command + stop the web server
 */
public abstract class ArrowheadClientMain {

  protected boolean isSecure;
  protected String baseUri;
  protected String base64PublicKey;
  protected HttpServer server;
  protected final TypeSafeProperties props = Utility.getProp("app.properties");

  private boolean daemon;
  private ClientType clientType;

  protected void init(ClientType client, String[] args, Set<Class<?>> classes, String[] packages) {
    System.out.println("Working directory: " + System.getProperty("user.dir"));
    clientType = client;
    System.setProperty("client_type", clientType.toString());

    for (String arg : args) {
      switch (arg) {
        case "-daemon":
          daemon = true;
          System.out.println("Starting server as daemon!");
          break;
        case "-d":
          System.setProperty("debug_mode", "true");
          System.out.println("Starting server in debug mode!");
          break;
        case "-tls":
          isSecure = true;
          break;
      }
    }

    String address = props.getProperty("address", "0.0.0.0");
    int port = isSecure ? props.getIntProperty("secure_port", clientType.getSecurePort())
                        : props.getIntProperty("insecure_port", clientType.getInsecurePort());
    baseUri = Utility.getUri(address, port, null, isSecure, true);

    if (isSecure) {
      List<String> allMandatoryProperties = new ArrayList<>(clientType.getAlwaysMandatoryFields());
      allMandatoryProperties.addAll(clientType.getSecureMandatoryFields());
      Utility.checkProperties(props.stringPropertyNames(), allMandatoryProperties);
      startSecureServer(classes, packages);
    } else {
      Utility.checkProperties(props.stringPropertyNames(), clientType.getAlwaysMandatoryFields());
      startServer(classes, packages);
    }
  }

  protected void listenForInput() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Received TERM signal, shutting down...");
      shutdown();
    }));
    if (daemon) {
      System.out.println("In daemon mode, process will only terminate for TERM signal...");
    } else {
      System.out.println("Type \"stop\" to shutdown " + clientType + " Server...");
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      try {
        while (!input.equals("stop")) {
          input = br.readLine();
        }
        br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      shutdown();
    }
  }

  private void startServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(classes);
    config.packages(packages);

    URI uri = UriBuilder.fromUri(baseUri).build();
    try {
      server = GrizzlyHttpServerFactory.createHttpServer(uri, config, false);
      server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
      server.start();
      System.out.println("Started insecure server at: " + baseUri);
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError(
          "Make sure you gave a valid address in the app.properties file! (Assignable to this JVM and not in use already)", e);
    }
  }

  protected void startSecureServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(classes);
    config.packages(packages);

    String keystorePath = props.getProperty("keystore");
    String keystorePass = props.getProperty("keystorepass");
    String keyPass = props.getProperty("keypass");
    String truststorePath = props.getProperty("truststore");
    String truststorePass = props.getProperty("truststorepass");

    SSLContextConfigurator sslCon = new SSLContextConfigurator();
    sslCon.setKeyStoreFile(keystorePath);
    sslCon.setKeyStorePass(keystorePass);
    sslCon.setKeyPass(keyPass);
    sslCon.setTrustStoreFile(truststorePath);
    sslCon.setTrustStorePass(truststorePass);
    if (!sslCon.validateConfiguration(true)) {
      throw new AuthException("SSL Context is not valid, check the certificate files or app.properties!", Status.UNAUTHORIZED.getStatusCode());
    }

    SSLContext sslContext = sslCon.createSSLContext();
    Utility.setSSLContext(sslContext);

    KeyStore keyStore = SecurityUtils.loadKeyStore(keystorePath, keystorePass);
    X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
    base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    System.out.println("Server PublicKey Base64: " + base64PublicKey);
    String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
    if (!SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
      throw new AuthException(
          "Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert structure, since it does not have 5 parts, or does not end with" + " \"arrowhead.eu\".", Status.UNAUTHORIZED.getStatusCode());
    }
    config.property("server_common_name", serverCN);

    URI uri = UriBuilder.fromUri(baseUri).build();
    try {
      server = GrizzlyHttpServerFactory.createHttpServer(uri, config, true, new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(true), false);
      server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
      server.start();
      System.out.println("Started secure server at: " + baseUri);
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError(
          "Make sure you gave a valid address in the app.properties file! (Assignable to this JVM and not in use already)", e);
    }
  }

  protected void shutdown() {
    if (server != null) {
      server.shutdownNow();
    }
    System.out.println(clientType + " Server stopped");
    System.exit(0);
  }

}
