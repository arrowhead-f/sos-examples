package eu.arrowhead.client.provider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

@Path("values")
@Produces(MediaType.TEXT_PLAIN)
public class HumidityResource {

  @GET
  @Path("/{nodeID}/humidity")
  public String getIt(@PathParam("nodeID") int nodeID, @Context SecurityContext context, @QueryParam("token") String token,
                      @QueryParam("signature") String signature) {
    if (context.isSecure()) {
      RequestVerification.verifyRequester(context, token, signature);
    }
    System.out.println("Requesting humidity for node " + nodeID);
    return "40%";
  }
}
