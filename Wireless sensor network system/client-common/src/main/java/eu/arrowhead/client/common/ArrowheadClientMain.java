/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common;

import eu.arrowhead.client.common.exception.ArrowheadException;
import eu.arrowhead.client.common.exception.AuthException;
import eu.arrowhead.client.common.misc.ClientType;
import eu.arrowhead.client.common.misc.SecurityUtils;
import eu.arrowhead.client.common.misc.TypeSafeProperties;
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
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLContextConfigurator.GenericStoreException;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/*
  Common base class for Client main classes.
  Functionalities:
    1) Read in basic command line arguments
    2) Read in the contents of the config files (default + app), check for missing mandatory values
    3) Start an insecure/secure web server (this might be optional in the future)
    4) Listen for exit command + stop the web server
 */
public abstract class ArrowheadClientMain {

  protected boolean isSecure;
  protected String baseUri;
  protected String base64PublicKey;
  protected HttpServer server;
  protected TypeSafeProperties props = Utility.getProp();

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
    if (daemon) {
      System.out.println("In daemon mode, process will terminate for TERM signal...");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Received TERM signal, shutting down...");
        shutdown();
      }));
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
      throw new ServiceConfigurationError("Make sure you gave a valid address in the config file! (Assignable to this JVM and not in use already)",
                                          e);
    }
  }

  protected void startSecureServer(Set<Class<?>> classes, String[] packages) {
    final ResourceConfig config = new ResourceConfig();
    config.registerClasses(classes);
    config.packages(packages);

    SSLContextConfigurator sslCon = new SSLContextConfigurator();
    sslCon.setKeyStoreFile(props.getProperty("keystore"));
    sslCon.setKeyStorePass(props.getProperty("keystorepass"));
    sslCon.setKeyPass(props.getProperty("keypass"));
    sslCon.setTrustStoreFile(props.getProperty("truststore"));
    sslCon.setTrustStorePass(props.getProperty("truststorepass"));
    SSLContext sslContext;
    try {
      sslContext = sslCon.createSSLContext(true);
    } catch (GenericStoreException e) {
      System.out.println("Provided SSLContext is not valid, moving to certificate bootstrapping.");
      try {
        sslCon = CertificateBootstrapper.bootstrap(clientType, props.getProperty("secure_system_name"));
      } catch (ArrowheadException e1) {
        throw new AuthException("Certificate bootstrapping failed with: " + e.getMessage(), e);
      }
      sslContext = sslCon.createSSLContext(true);
      props = Utility.getProp();
    }
    Utility.setSSLContext(sslContext);

    KeyStore keyStore = SecurityUtils.loadKeyStore(props.getProperty("keystore"), props.getProperty("keystorepass"));
    X509Certificate serverCert = SecurityUtils.getFirstCertFromKeyStore(keyStore);
    base64PublicKey = Base64.getEncoder().encodeToString(serverCert.getPublicKey().getEncoded());
    System.out.println("Server PublicKey Base64: " + base64PublicKey);
    String serverCN = SecurityUtils.getCertCNFromSubject(serverCert.getSubjectDN().getName());
    if (!SecurityUtils.isKeyStoreCNArrowheadValid(serverCN)) {
      throw new AuthException(
          "Server CN ( " + serverCN + ") is not compliant with the Arrowhead cert structure, since it does not have 5 parts, or does not end with"
              + " \"arrowhead.eu\".");
    }
    config.property("server_common_name", serverCN);

    URI uri = UriBuilder.fromUri(baseUri).build();
    try {
      server = GrizzlyHttpServerFactory
          .createHttpServer(uri, config, true, new SSLEngineConfigurator(sslCon).setClientMode(false).setNeedClientAuth(true), false);
      server.getServerConfiguration().setAllowPayloadForUndefinedHttpMethods(true);
      server.start();
      System.out.println("Started secure server at: " + baseUri);
    } catch (IOException | ProcessingException e) {
      throw new ServiceConfigurationError("Make sure you gave a valid address in the config file! (Assignable to this JVM and not in use already)",
                                          e);
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
