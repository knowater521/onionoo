/* Copyright 2014--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.metrics.onionoo.updater;

public interface StatusUpdater {

  void updateStatuses();

  String getStatsString();
}

