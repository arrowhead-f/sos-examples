/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

import java.util.ArrayList;
import java.util.List;

public class IntraCloudAuthEntry {

  private ArrowheadSystem consumer;
  private List<ArrowheadSystem> providerList = new ArrayList<>();
  private List<ArrowheadService> serviceList = new ArrayList<>();

  public IntraCloudAuthEntry() {
  }

  public IntraCloudAuthEntry(ArrowheadSystem consumer, List<ArrowheadSystem> providerList, List<ArrowheadService> serviceList) {
    this.consumer = consumer;
    this.providerList = providerList;
    this.serviceList = serviceList;
  }

  public ArrowheadSystem getConsumer() {
    return consumer;
  }

  public void setConsumer(ArrowheadSystem consumer) {
    this.consumer = consumer;
  }

  public List<ArrowheadSystem> getProviderList() {
    return providerList;
  }

  public void setProviderList(List<ArrowheadSystem> providerList) {
    this.providerList = providerList;
  }

  public List<ArrowheadService> getServiceList() {
    return serviceList;
  }

  public void setServiceList(List<ArrowheadService> serviceList) {
    this.serviceList = serviceList;
  }

}
