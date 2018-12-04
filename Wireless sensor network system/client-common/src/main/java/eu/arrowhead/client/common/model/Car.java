/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

import java.util.Objects;
import org.glassfish.jersey.internal.guava.MoreObjects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Car)) {
      return false;
    }
    Car car = (Car) o;
    return Objects.equals(brand, car.brand) && Objects.equals(color, car.color);
  }

  @Override
  public int hashCode() {
    return Objects.hash(brand, color);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("brand", brand).add("color", color).toString();
  }
}
