/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

//Based on the SenML standard for sensor readouts - expected response from the Provider client example
//Put your custom model classes in this package
public class MeasurementEntry {

  private String n;
  private double v;
  private double t;

  public MeasurementEntry() {
  }

  public MeasurementEntry(String n, double v, double t) {
    this.n = n;
    this.v = v;
    this.t = t;
  }

  public String getN() {
    return n;
  }

  public void setN(String n) {
    this.n = n;
  }

  public double getV() {
    return v;
  }

  public void setV(double v) {
    this.v = v;
  }

  public double getT() {
    return t;
  }

  public void setT(double t) {
    this.t = t;
  }

}
