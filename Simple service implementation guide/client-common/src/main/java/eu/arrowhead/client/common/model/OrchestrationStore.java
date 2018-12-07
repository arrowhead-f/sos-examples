/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

import eu.arrowhead.client.common.exception.BadPayloadException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class OrchestrationStore implements Comparable<OrchestrationStore> {

  private Long id;
  private ArrowheadService service;
  private ArrowheadSystem consumer;
  private ArrowheadSystem providerSystem;
  private ArrowheadCloud providerCloud;
  private Integer priority;
  private Boolean defaultEntry;
  private String name;
  private LocalDateTime lastUpdated;
  private String instruction;
  private Map<String, String> attributes = new HashMap<>();

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

  public OrchestrationStore(ArrowheadService service, ArrowheadSystem consumer, ArrowheadSystem providerSystem, ArrowheadCloud providerCloud,
                            int priority, boolean defaultEntry, String name, LocalDateTime lastUpdated, String instruction,
                            Map<String, String> attributes) {
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
  }

  public OrchestrationStore(ArrowheadService service, ArrowheadSystem consumer, ArrowheadSystem providerSystem, Integer priority,
                            boolean defaultEntry) {
    this.service = service;
    this.consumer = consumer;
    this.providerSystem = providerSystem;
    this.priority = priority;
    this.defaultEntry = defaultEntry;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Boolean isDefaultEntry() {
    return defaultEntry;
  }

  public void setDefaultEntry(Boolean defaultEntry) {
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

  /**
   * Note: This class has a natural ordering that is inconsistent with equals(). <p> The field <i>priority</i> is used to sort instances of this class
   * in a collection. Priority is non-negative. If this.priority < other.priority that means <i>this</i> is more ahead in a collection than
   * <i>other</i> and therefore has a higher priority. This means priority = 0 is the highest priority for a Store entry.
   */
  @Override
  public int compareTo(OrchestrationStore other) {
    return this.priority - other.priority;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OrchestrationStore)) {
      return false;
    }

    OrchestrationStore that = (OrchestrationStore) o;

    if (service != null ? !service.equals(that.service) : that.service != null) {
      return false;
    }
    if (consumer != null ? !consumer.equals(that.consumer) : that.consumer != null) {
      return false;
    }
    if (providerSystem != null ? !providerSystem.equals(that.providerSystem) : that.providerSystem != null) {
      return false;
    }
    if (providerCloud != null ? !providerCloud.equals(that.providerCloud) : that.providerCloud != null) {
      return false;
    }
    if (priority != null ? !priority.equals(that.priority) : that.priority != null) {
      return false;
    }
    return defaultEntry != null ? defaultEntry.equals(that.defaultEntry) : that.defaultEntry == null;
  }

  @Override
  public int hashCode() {
    int result = service != null ? service.hashCode() : 0;
    result = 31 * result + (consumer != null ? consumer.hashCode() : 0);
    result = 31 * result + (providerSystem != null ? providerSystem.hashCode() : 0);
    result = 31 * result + (providerCloud != null ? providerCloud.hashCode() : 0);
    result = 31 * result + (priority != null ? priority.hashCode() : 0);
    result = 31 * result + (defaultEntry != null ? defaultEntry.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("OrchestrationStore{");
    sb.append("service=").append(service);
    sb.append(", consumer=").append(consumer);
    sb.append(", priority=").append(priority);
    sb.append(", defaultEntry=").append(defaultEntry);
    sb.append('}');
    return sb.toString();
  }

  public void validateCrossParameterConstraints() {
    if (defaultEntry && providerCloud != null) {
      throw new BadPayloadException("Default store entries can only have intra-cloud providers!");
    }
  }
}
