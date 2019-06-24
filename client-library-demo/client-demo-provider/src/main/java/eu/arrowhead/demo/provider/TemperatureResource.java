package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.demo.model.Temperature;
import java.time.Instant;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("temperature")
@Produces(MediaType.APPLICATION_JSON)
public class TemperatureResource extends ArrowheadResource {

  public TemperatureResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @GET
  public Response get() {
    double temperature = 21.0;
    Temperature readout = new Temperature(Instant.now().getEpochSecond(), temperature);
    return Response.status(200).entity(readout).build();
  }
}
