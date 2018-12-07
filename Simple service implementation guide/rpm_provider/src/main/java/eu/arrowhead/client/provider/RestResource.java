package eu.arrowhead.client.provider;

import eu.arrowhead.client.common.exception.DataNotFoundException;
import eu.arrowhead.client.common.model.Car;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
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

/*
   This class demonstrates the most commonly used capabilities of a JAX-RS REST library, specifically the Jersey implementation in this case.
   You can also implement your clients in different JAX-RS implementations, like Spring for example.
 */

@Path("example") //base path after the port
//Every REST method will consume and produce JSON payloads (not plain text, or XML for example)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestResource {

  //In-memory mocked database
  private static Integer idCounter = 0;
  private static final ConcurrentHashMap<Integer, Car> cars = new ConcurrentHashMap<>();


  @GET
  @Produces(MediaType.TEXT_PLAIN) //individual methods can override class level annotations
  public String simpleGetRequest() {
    return "This is an example REST resource!";
  }

  /*
    GET requests are usually used to get a resource from the database, often identified by a unique ID.
    Example URL: http://<server_address>:<server_port>/example/cars/5
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

  /*
    Get all cars, optional filter parameters are the brand and color of the cars.
    QueryParameters are optional in Jersey, for example brand will be null if not specified.
    Example URL: http://<server_address>:<server_port>/example/cars?brand=Volvo&color=red
   */
  @GET
  @Path("cars")
  public Response getCars(@QueryParam("brand") String brand, @QueryParam("color") String color) {
    //Get all the cars in a list
    List<Car> returnedCars = new ArrayList<>();
    for (Entry<Integer, Car> mapEntry : cars.entrySet()) {
      returnedCars.add(mapEntry.getValue());
    }

    //Filter the list based on the specified brand and color
    if (brand != null) {
      returnedCars.removeIf(car -> !brand.equals(car.getBrand()));
    }
    if (color != null) {
      returnedCars.removeIf(car -> !color.equals(car.getColor()));
    }

    //Response contains the status code, and the response entity
    return Response.status(Status.OK).entity(returnedCars).build();
  }

  //Return the complete Map with IDs included
  @GET
  @Path("raw")
  public Response getAll() {
    return Response.status(Status.OK).entity(cars).build();
  }

  /*
     POST requests are usually for creating/saving new resources. The resource is in the payload of the request, which will be
     automatically be deserialized by the JSON library (if the project is configured correctly).
   */
  @POST
  @Path("cars")
  public Response createCar(Car car) {
    //Save the car instance to the database
    cars.put(idCounter, car);
    //Increment the ID for the next call of this method
    idCounter++;
    return Response.status(Status.CREATED).entity(car).build();
  }

  /*
     PUT requests are usually for updating existing resources. The ID is from the database, to identify the car instance.
     Usually PUT requests fully update a resource, meaning fields which are not specified by the client, will also be null
     in the database (overriding existing data). PATCH requests are used for partial updates.
   */
  @PUT
  @Path("cars/{id}")
  public Response updateCar(@PathParam("id") Integer id, Car updatedCar) {
    Car carFromTheDatabase = cars.get(id);
    //Throw an exception if the car with the specified ID does not exist
    if (carFromTheDatabase != null) {
      throw new DataNotFoundException("Car with id " + id + " not found in the database!");
    }
    //Update the car
    cars.put(id, updatedCar);

    //Return a response with Accepted status code
    return Response.status(Status.ACCEPTED).entity(updatedCar).build();
  }

  /*
     And finally, DELETE requests are usually used to delete a resource from database.
   */
  @DELETE
  @Path("cars/{id}")
  public Response deleteCar(@PathParam("id") Integer id) {
    cars.remove(id);
    return Response.ok().build();
  }

}
