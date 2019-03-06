package eu.arrowhead.demo.model;

public class Temperature {

  private long timestamp;
  private double temperature;

  public Temperature() {
  }

  public Temperature(long timestamp, double temperature) {
    this.timestamp = timestamp;
    this.temperature = temperature;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }
}
