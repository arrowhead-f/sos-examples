package eu.arrowhead.client.common.model;

import org.glassfish.jersey.internal.guava.MoreObjects;

public class RPMInput {

  private Integer RPM;

  public RPMInput() {
  }

  public RPMInput(Integer RPM) {
    this.RPM = RPM;
  }

  public Integer getRPM() {
    return RPM;
  }

  public void setRPM(Integer RPM) {
    this.RPM = RPM;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("RPM", RPM).toString();
  }
}
