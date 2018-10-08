/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.no_need_to_modify.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import eu.arrowhead.client.common.no_need_to_modify.exception.BadPayloadException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties({"alwaysMandatoryFields"})
public class OrchestrationStore extends ArrowheadBase implements Comparable<OrchestrationStore> {

  private static final Set<String> alwaysMandatoryFields = new HashSet<>(
      Arrays.asList("service", "consumer", "providerSystem", "priority", "defaultEntry"));

  private ArrowheadService service;
  private ArrowheadSystem consumer;
  private ArrowheadSystem providerSystem;
  private ArrowheadCloud providerCloud;
  private Integer priority;
  private boolean defaultEntry;
  private String name;
  private LocalDateTime lastUpdated;
  private String instruction;
  private Map<String, String> attributes = new HashMap<>();
  private String serviceURI;

  public OrchestrationStore() {
  }

  public OrchestrationStore(ArrowheadService service, ArrowheadSystem consumer, ArrowheadSystem providerSystem, ArrowheadCloud providerCloud,
                            int priority) {
    this.service = service;
    this.consumer = consumer;
    this.providerSystem = providerSystem;
    this.providerCloud = providerCloud;
    this.priority = priority;
  }

  public OrchestrationStore(ArrowheadService service, ArrowheadSystem consumer, ArrowheadSystem providerSystem, Integer priority,
                            boolean defaultEntry) {
    this.service = service;
    this.consumer = consumer;
    this.providerSystem = providerSystem;
    this.priority = priority;
    this.defaultEntry = defaultEntry;
  }

  public OrchestrationStore(ArrowheadService service, ArrowheadSystem consumer, ArrowheadSystem providerSystem, ArrowheadCloud providerCloud,
                            Integer priority, boolean defaultEntry, String name, LocalDateTime lastUpdated, String instruction,
                            Map<String, String> attributes, String serviceURI) {
    this.service = service;
    this.consumer = consumer;
    this.providerSystem = providerSystem;
    this.providerCloud = providerCloud;
    this.priority = priority;
    this.defaultEntry = defaultEntry;
    this.name = name;
    this.lastUpdated = lastUpdated;
    this.instruction = instruction;
    this.attributes = attributes;
    this.serviceURI = serviceURI;
  }

  public ArrowheadService getService() {
    return service;
  }

  public void setService(ArrowheadService service) {
    this.service = service;
  }

  public ArrowheadSystem getConsumer() {
    return consumer;
  }

  public void setConsumer(ArrowheadSystem consumer) {
    this.consumer = consumer;
  }

  public ArrowheadSystem getProviderSystem() {
    return providerSystem;
  }

  public void setProviderSystem(ArrowheadSystem providerSystem) {
    this.providerSystem = providerSystem;
  }

  public ArrowheadCloud getProviderCloud() {
    return providerCloud;
  }

  public void setProviderCloud(ArrowheadCloud providerCloud) {
    this.providerCloud = providerCloud;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public boolean isDefaultEntry() {
    return defaultEntry;
  }

  public void setDefaultEntry(boolean defaultEntry) {
    this.defaultEntry = defaultEntry;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public LocalDateTime getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getInstruction() {
    return instruction;
  }

  public void setInstruction(String instruction) {
    this.instruction = instruction;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public String getServiceURI() {
    return serviceURI;
  }

  public void setServiceURI(String serviceURI) {
    this.serviceURI = serviceURI;
  }

  public Set<String> missingFields(boolean throwException, Set<String> mandatoryFields) {
    Set<String> mf = new HashSet<>(alwaysMandatoryFields);
    if (mandatoryFields != null) {
      mf.addAll(mandatoryFields);
    }
    Set<String> nonNullFields = getFieldNamesWithNonNullValue();
    mf.removeAll(nonNullFields);
    if (service != null) {
      mf = service.missingFields(false, false, mf);
    }

    Set<String> fromConsumer = new HashSet<>();
    Set<String> fromProvider = new HashSet<>();
    if (consumer != null) {
      fromConsumer = consumer.missingFields(false, mf);
    }
    if (providerSystem != null) {
      fromProvider = providerSystem.missingFields(false, mf);
    }
    mf = new HashSet<>(fromConsumer);
    mf.addAll(fromProvider);

    if (priority < 0) {
      mf.add("Priority can not be negative!");
    }

    if (providerCloud != null) {
      if (defaultEntry) {
        mf.add("Default store entries can only have intra-cloud providers!");
      } else {
        Set<String> fromCloud = providerCloud.missingFields(false, new HashSet<>(Arrays.asList("ArrowheadCloud:address", "gatekeeperServiceURI")));
        mf.addAll(fromCloud);
      }
    }

    if (throwException && !mf.isEmpty()) {
      throw new BadPayloadException("Missing mandatory fields for " + getClass().getSimpleName() + ": " + String.join(", ", mf));
    }
    return mf;
  }

  /**
   * Note: This class has a natural ordering that is inconsistent with equals(). <p> The field <i>priority</i> is used to sort instances of this class
   * in a collection. Priority is non-negative. If this.priority < other.priority that means <i>this</i> is more ahead in a collection than
   * <i>other</i> and therefore has a higher priority. This means priority = 0 is the highest priority for a Store entry.
   */
  @Override
  public int compareTo(OrchestrationStore other) {
    return this.priority - other.priority;
  }

}
