/*
 *  Copyright (c) 2018 AITIA International Inc.
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.client.common.misc;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public class TypeSafeProperties extends Properties {

  public int getIntProperty(String key, int defaultValue) {
    String val = getProperty(key);
    try {
      return (val == null) ? defaultValue : Integer.valueOf(val);
    } catch (NumberFormatException e) {
      System.out
          .println(val + " is not a valid number! Please fix the \"" + key + "\" property! Using default value (" + defaultValue + ") instead!");
      return defaultValue;
    }
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    String val = getProperty(key);
    return (val == null) ? defaultValue : Boolean.valueOf(val);
  }

  //NOTE add more data types later if needed


  //These methods are here to make sure TypeSafeProperties are saved to file in alphabetical order (sorted by key value)
  @Override
  public Set<Object> keySet() {
    return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet()));
  }

  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {

    Set<Map.Entry<Object, Object>> set1 = super.entrySet();
    Set<Map.Entry<Object, Object>> set2 = new LinkedHashSet<Entry<Object, Object>>(set1.size());

    Iterator<Entry<Object, Object>> iterator = set1.stream().sorted(new Comparator<Entry<Object, Object>>() {

      @Override
      public int compare(java.util.Map.Entry<Object, Object> o1, java.util.Map.Entry<Object, Object> o2) {
        return o1.getKey().toString().compareTo(o2.getKey().toString());
      }
    }).iterator();

    while (iterator.hasNext()) {
      set2.add(iterator.next());
    }

    return set2;
  }

  @Override
  public synchronized Enumeration<Object> keys() {
    return Collections.enumeration(new TreeSet<Object>(super.keySet()));
  }
}
