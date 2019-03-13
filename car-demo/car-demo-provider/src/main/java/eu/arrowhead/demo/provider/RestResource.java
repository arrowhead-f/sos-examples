package eu.arrowhead.demo.provider;

import eu.arrowhead.common.api.server.ArrowheadHttpServer;
import eu.arrowhead.common.api.server.ArrowheadResource;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.demo.model.Car;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * This class demonstrates the most commonly used capabilities of a JAX-RS REST library, specifically the Jersey implementation in this case.
 * You can also implement your clients in different JAX-RS implementations, like Spring for example.
 */
@Path("car")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestResource extends ArrowheadResource {

  private static Integer idCounter = 0;
  private static final ConcurrentHashMap<Integer, Car> cars = new ConcurrentHashMap<>();

  public RestResource(ArrowheadHttpServer server) throws ArrowheadException {
    super(server);
  }

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String simpleGetRequest() {
    return "This is an example REST resource!";
  }

  /**
   * GET requests are usually used to get a resource from the database, often identified by a unique ID.
   * Example URL: http://<server_address>:<server_port>/example/cars/5
   * @param id
   * @return
   */
  @GET
  @Path("cars/{id}")
  public Response getCarById(@PathParam("id") Integer id) {
    Car retrievedCar = cars.get(id);
    if (retrievedCar != null) {
      return Response.status(Status.OK).entity(retrievedCar).build();
    } else {
      return Response.status(Status.OK).build();
    }
  }

  /**
   * Get all cars, optional filter parameters are the brand and color of the cars.
   * QueryParameters are optional in Jersey, for example brand will be null if not specified.
   * Example URL: http://<server_address>:<server_port>/example/cars?brand=Volvo&color=red
   * @param brand
   * @param color
   * @return
   */
  @GET
  @Path("cars")
  public Response getCars(@QueryParam("brand") String brand,
                          @QueryParam("color") String color) {
    List<Car> returnedCars = new ArrayList<>();
    for (Map.Entry<Integer, Car> mapEntry : cars.entrySet()) {
      returnedCars.add(mapEntry.getValue());
    }

    if (brand != null) {
      returnedCars.removeIf(car -> !brand.equals(car.getBrand()));
    }

    if (color != null) {
      returnedCars.removeIf(car -> !color.equals(car.getColor()));
    }

    return Response.status(Status.OK).entity(returnedCars).build();

  }

  /**
   * Return the complete Map with IDs included
   * @return
   */
  @GET
  @Path("raw")
  public Response getAll() {
    return Response.status(Status.OK).entity(cars).build();
  }

  /**
   * POST requests are usually for creating/saving new resources. The resource is in the payload of the request, which will be
   * automatically be deserialized by the JSON library (if the project is configured correctly).
   * @param car
   * @return
   */
  @POST
  @Path("cars")
  public Response createCar(Car car) {
    cars.put(idCounter, car);
    idCounter++;
    return Response.status(Status.CREATED).entity(car).build();
  }

  /**
   * PUT requests are usually for updating existing resources. The ID is from the database, to identify the car instance.
   * Usually PUT requests fully update a resource, meaning fields which are not specified by the client, will also be null
   * in the database (overriding existing data). PATCH requests are used for partial updates.
   * @param id
   * @param updatedCar
   * @return
   */
  @PUT
  @Path("cars/{id}")
  public Response updateCar(@PathParam("id") Integer id,
                            Car updatedCar) {
    Car carFromTheDatabase = cars.get(id);
    if (carFromTheDatabase != null) {
      throw new DataNotFoundException("Car with id " + id + " not found in the database!");
    }
    cars.put(id, updatedCar);
    return Response.status(Status.ACCEPTED).entity(updatedCar).build();
  }

  /**
   * And finally, DELETE requests are usually used to delete a resource from database.
   * @param id
   * @return
   */
  @DELETE
  @Path("cars/{id}")
  public Response deleteCar(@PathParam("id") Integer id) {
    cars.remove(id);
    return Response.ok().build();
  }

}
