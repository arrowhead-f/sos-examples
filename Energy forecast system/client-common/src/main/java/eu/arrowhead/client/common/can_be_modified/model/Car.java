package eu.arrowhead.client.common.can_be_modified.model;

//Sample model class to demonstrate REST capabilities in the RestResource class
public class Car {

  private String brand;
  private String color;

  //JSON libraries will use the empty constructor to create a new object during deserialization, so it is important to have one
  public Car() {
  }

  public Car(String brand, String color) {
    this.brand = brand;
    this.color = color;
  }

  //Getter methods are used during serialization
  public String getBrand() {
    return brand;
  }

  //After creating the object, JSON libraries use the setter methods during deserialization
  public void setBrand(String brand) {
    this.brand = brand;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

}
