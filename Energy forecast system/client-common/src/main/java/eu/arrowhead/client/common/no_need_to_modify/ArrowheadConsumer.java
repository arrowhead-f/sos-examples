package eu.arrowhead.client.common.no_need_to_modify;

import eu.arrowhead.client.common.no_need_to_modify.misc.TypeSafeProperties;
import eu.arrowhead.client.common.no_need_to_modify.model.ArrowheadService;
import eu.arrowhead.client.common.no_need_to_modify.model.ArrowheadSystem;
import eu.arrowhead.client.common.no_need_to_modify.model.ServiceRequestForm;
import org.glassfish.jersey.SslConfigurator;

import javax.net.ssl.SSLContext;
import java.util.Map;

public abstract class ArrowheadConsumer {
    protected final TypeSafeProperties props = Utility.getProp("app.properties");
    private final ArrowheadSystem consumer;
    protected boolean isSecure = false;

    public ArrowheadConsumer(String[] args, String systemName) {
        //Prints the working directory for extra information. Working directory should always contain a config folder with the app.properties file!
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        for (String arg : args) {
            if (arg.equals("-tls")) {
                isSecure = true;
            }
        }

        if (isSecure) {
            SslConfigurator sslConfig = SslConfigurator.newInstance().trustStoreFile(props.getProperty("truststore"))
                    .trustStorePassword(props.getProperty("truststorepass"))
                    .keyStoreFile(props.getProperty("keystore")).keyStorePassword(props.getProperty("keystorepass"))
                    .keyPassword(props.getProperty("keypass"));
            SSLContext sslContext = sslConfig.createSSLContext();
            Utility.setSSLContext(sslContext);
        }


        /*
            ArrowheadSystem: systemName, (address, port, authenticationInfo)
            Since this Consumer skeleton will not receive HTTP requests (does not povide any services on its own),
            the address, port and authenticationInfo fields can be set to anything.
        */
        consumer = new ArrowheadSystem(systemName, "null", 0, "null");
    }

    protected ServiceRequestForm buildServiceRequestForm(ArrowheadService service, Map<String, Boolean> orchestrationFlags) {
      ServiceRequestForm srf = new ServiceRequestForm.Builder(consumer).requestedService(service).orchestrationFlags(orchestrationFlags).build();
      System.out.println("Service Request payload: " + Utility.toPrettyJson(null, srf));
      return srf;
    }
}
