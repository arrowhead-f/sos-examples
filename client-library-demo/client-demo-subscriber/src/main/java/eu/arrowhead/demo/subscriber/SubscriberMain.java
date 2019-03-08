package eu.arrowhead.demo.subscriber;

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.api.ArrowheadSecurityContext;
import eu.arrowhead.common.api.clients.core.EventHandlerClient;
import eu.arrowhead.common.api.server.ArrowheadGrizzlyHttpServer;
import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadSecurityFilter;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.ArrowheadSystem;

class SubscriberMain extends ArrowheadApplication {

  private SubscriberMain(String[] args) throws ArrowheadException {
    super(args);
  }

  @Override
  protected void onStart() throws ArrowheadException {
    final ArrowheadSecurityContext securityContext = ArrowheadSecurityContext
        .createFromProperties(true);
    final ArrowheadHttpServer server = ArrowheadGrizzlyHttpServer
        .createFromProperties(securityContext)
        .addResources(SubscriberResource.class)
        .setSecurityFilter(new ArrowheadSecurityFilter())
        .start();

    final EventHandlerClient eventHandler = EventHandlerClient
        .createFromProperties(securityContext);
    final ArrowheadSystem me = ArrowheadSystem.createFromProperties(server);
    eventHandler.subscribe(getProps().getEventType(), me);
  }

  @Override
  protected void onStop() {

  }

  public static void main(String[] args) throws ArrowheadException {
    new SubscriberMain(args).start();
  }
}
