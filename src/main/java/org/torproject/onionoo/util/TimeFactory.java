/* Copyright 2014--2016 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.onionoo.util;

public class TimeFactory {

  private static Time timeInstance;

  /** Sets a custom singleton time instance that will be returned by
   * {@link #getTime} rather than creating an instance upon first
   * invocation. */
  public static void setTime(Time time) {
    timeInstance = time;
  }

  /** Returns the singleton node indexer instance that gets created upon
   * first invocation of this method. */
  public static Time getTime() {
    if (timeInstance == null) {
      timeInstance = new Time();
    }
    return timeInstance;
  }
}

