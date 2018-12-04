package eu.arrowhead.client.provider.model;

import org.glassfish.jersey.internal.guava.MoreObjects;

public class RPMOutput {

  private Integer currentRPM;

  public RPMOutput() {
  }

  public RPMOutput(Integer currentRPM) {
    this.currentRPM = currentRPM;
  }

  public Integer getCurrentRPM() {
    return currentRPM;
  }

  public void setCurrentRPM(Integer currentRPM) {
    this.currentRPM = currentRPM;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("currentRPM", currentRPM).toString();
  }
}
