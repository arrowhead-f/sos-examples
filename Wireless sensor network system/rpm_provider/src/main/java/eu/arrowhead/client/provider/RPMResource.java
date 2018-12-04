package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.model.MeasurementEntry;
import eu.arrowhead.client.common.model.TemperatureReadout;
import eu.arrowhead.client.provider.model.RPMInput;
import eu.arrowhead.client.provider.model.RPMOutput;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("controller")
public class RPMResource {

  @POST
  @Path("rpm")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRPM(@Valid RPMInput input, @Context SecurityContext context, @QueryParam("token") String token,
                         @QueryParam("signature") String signature) {
    if (context.isSecure()) {
      RequestVerification.verifyRequester(context, token, signature);
    }

    System.out.println(input.toString());

    return Response.status(200).entity(new RPMOutput(10000)).build();
  }
}
