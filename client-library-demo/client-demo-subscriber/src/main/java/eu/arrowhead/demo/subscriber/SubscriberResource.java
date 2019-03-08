package eu.arrowhead.demo.subscriber;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.Event;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("subscriber")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubscriberResource extends ArrowheadResource {

  public SubscriberResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @POST
  @Path("notify")
  public Response receiveEvent(Event event) {
    log.info("Received new event: " + event.toString());
    return Response.ok().build();
  }
}
