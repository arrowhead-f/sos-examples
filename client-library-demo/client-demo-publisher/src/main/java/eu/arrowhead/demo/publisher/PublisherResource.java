package eu.arrowhead.demo.publisher;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("publisher")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PublisherResource extends ArrowheadResource {

  public PublisherResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @POST
  @Path("feedback")
  public Response eventFeedback(Map<String, Boolean> results) {
    log.info("Event publishing results: " + results.toString());
    return Response.ok().build();
  }
}