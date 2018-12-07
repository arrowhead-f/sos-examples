/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.model;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventFilter {

  private long id;
  private String eventType;
  private ArrowheadSystem consumer;
  private Set<ArrowheadSystem> sources = new HashSet<>();
  private ZonedDateTime startDate;
  private ZonedDateTime endDate;
  private Map<String, String> filterMetadata = new HashMap<>();
  private String notifyUri;
  private boolean matchMetadata;

  public EventFilter() {
  }

  public EventFilter(String eventType, ArrowheadSystem consumer, Set<ArrowheadSystem> sources, ZonedDateTime startDate, ZonedDateTime endDate,
                     Map<String, String> filterMetadata, String notifyUri, boolean matchMetadata) {
    this.eventType = eventType;
    this.consumer = consumer;
    this.sources = sources;
    this.startDate = startDate;
    this.endDate = endDate;
    this.filterMetadata = filterMetadata;
    this.notifyUri = notifyUri;
    this.matchMetadata = matchMetadata;
  }

  public EventFilter(String eventType, ArrowheadSystem consumer, String notifyUri) {
    this.eventType = eventType;
    this.consumer = consumer;
    this.notifyUri = notifyUri;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public ArrowheadSystem getConsumer() {
    return consumer;
  }

  public void setConsumer(ArrowheadSystem consumer) {
    this.consumer = consumer;
  }

  public Set<ArrowheadSystem> getSources() {
    return sources;
  }

  public void setSources(Set<ArrowheadSystem> sources) {
    this.sources = sources;
  }

  public ZonedDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(ZonedDateTime startDate) {
    this.startDate = startDate;
  }

  public ZonedDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(ZonedDateTime endDate) {
    this.endDate = endDate;
  }

  public Map<String, String> getFilterMetadata() {
    return filterMetadata;
  }

  public void setFilterMetadata(Map<String, String> filterMetadata) {
    this.filterMetadata = filterMetadata;
  }

  public String getNotifyUri() {
    return notifyUri;
  }

  public void setNotifyUri(String notifyUri) {
    this.notifyUri = notifyUri;
  }

  public boolean isMatchMetadata() {
    return matchMetadata;
  }

  public void setMatchMetadata(boolean matchMetadata) {
    this.matchMetadata = matchMetadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EventFilter)) {
      return false;
    }

    EventFilter that = (EventFilter) o;

    if (eventType != null ? !eventType.equals(that.eventType) : that.eventType != null) {
      return false;
    }
    return consumer.equals(that.consumer);
  }

  @Override
  public int hashCode() {
    int result = eventType != null ? eventType.hashCode() : 0;
    result = 31 * result + consumer.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("EventFilter{");
    sb.append("eventType='").append(eventType).append('\'');
    sb.append(", consumer=").append(consumer);
    sb.append('}');
    return sb.toString();
  }
}
