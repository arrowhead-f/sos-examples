package eu.arrowhead.demo.consumer;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadConverter;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.HttpClient;
import eu.arrowhead.common.api.clients.HttpClient.Method;
import eu.arrowhead.common.api.clients.OrchestrationStrategy;
import eu.arrowhead.common.api.clients.core.OrchestrationClient;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.ArrowheadSystem;
import eu.arrowhead.common.model.OrchestrationFlags;
import eu.arrowhead.common.model.ServiceRequestForm;
import eu.arrowhead.demo.model.Car;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

class ConsumerMain extends ArrowheadApplication {

  public ConsumerMain(String[] args) throws ArrowheadException {
    super(args);
  }

  @Override
  protected void onStart() {
    final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext.createFromProperties(true);

    final ArrowheadSystem me = ArrowheadSystem.createFromProperties();
    final ServiceRequestForm srf = new ServiceRequestForm.Builder(me)
        .requestedService(getProps().getServiceName(), "JSON", getProps().isSecure())
        .flag(OrchestrationFlags.Flags.OVERRIDE_STORE, true)
        .flag(OrchestrationFlags.Flags.PING_PROVIDERS, false)
        .flag(OrchestrationFlags.Flags.METADATA_SEARCH, false)
        .flag(OrchestrationFlags.Flags.ENABLE_INTER_CLOUD, false)
        .build();
    log.info("Service Request payload: " + ArrowheadConverter.json().toString(srf));

    final OrchestrationClient orchestration = OrchestrationClient.createFromProperties(securityContext);
    final OrchestrationStrategy strategy = new OrchestrationStrategy.Always(orchestration, srf);
    final HttpClient client = new HttpClient(strategy, securityContext);

    Response response;

    log.info("Saying Hello:");
    response = client.request(Method.GET);
    log.info(String.format("%d  %s", response.getStatus(), response.readEntity(String.class)));

    log.info("Creating new car:");
    response = client.request(Method.POST, UriBuilder.fromPath("/cars"), new Car("ArrowMobile", "Blue"));
    log.info(String.format("%d  %s", response.getStatus(), response.readEntity(Car.class)));

    log.info("Getting all cars:");
    response = client.request(Method.GET, UriBuilder.fromPath("/cars"));
    log.info(String.format("%d  %s", response.getStatus(), response.readEntity(String.class)));
  }

  @Override
  protected void onStop() {

  }

  public static void main(String[] args) throws ArrowheadException {
    new ConsumerMain(args).start();
  }
}