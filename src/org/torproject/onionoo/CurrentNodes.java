/* Copyright 2011, 2012 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.onionoo;

import java.io.*;
import java.util.*;
import org.torproject.descriptor.*;

/* Store relays and bridges that have been running in the past seven
 * days. */
public class CurrentNodes {
  public SortedSet<Long> getAllValidAfterMillis() {
    return new TreeSet<Long>(this.containedValidAfterMillis);
  }
  public long getLastValidAfterMillis() {
    return this.containedValidAfterMillis.isEmpty() ? -1L :
        this.containedValidAfterMillis.last();
  }
  public long getLastFreshUntilMillis() {
    return this.containedValidAfterMillis.isEmpty() ? -1L :
        this.containedValidAfterMillis.last() + 60L * 60L * 1000L;
  }
  private SortedSet<Long> containedValidAfterMillis = new TreeSet<Long>();
  private SortedMap<String, SearchEntryData> containedRelays =
      new TreeMap<String, SearchEntryData>();
  /* Add a search entry for a relay if this relay wasn't seen before or if
   * its current valid-after time is newer than the currently known
   * valid-after time. */
  private long now = System.currentTimeMillis();
  public void addRelay(String nickname, String fingerprint,
      String address, long validAfterMillis, int orPort, int dirPort,
      SortedSet<String> relayFlags) {
    if (validAfterMillis >= now - 7L * 24L * 60L * 60L * 1000L &&
        (!this.containedRelays.containsKey(fingerprint) ||
        this.containedRelays.get(fingerprint).getValidAfterMillis() <
        validAfterMillis)) {
      SearchEntryData entry = new SearchEntryData(nickname, fingerprint,
          address, validAfterMillis, orPort, dirPort, relayFlags);
      this.containedRelays.put(fingerprint, entry);
      this.containedValidAfterMillis.add(validAfterMillis);
    }
  }
  public SortedMap<String, SearchEntryData> getRelays() {
    return new TreeMap<String, SearchEntryData>(this.containedRelays);
  }
  public void updateRelayNetworkConsensuses() {
    RelayDescriptorReader reader =
        DescriptorSourceFactory.createRelayDescriptorReader();
    reader.addDirectory(new File("in/relay-descriptors/consensuses"));
    reader.setExcludeFiles(new File("status/relay-consensus-history"));
    Iterator<DescriptorFile> descriptorFiles = reader.readDescriptors();
    while (descriptorFiles.hasNext()) {
      DescriptorFile descriptorFile = descriptorFiles.next();
      if (descriptorFile.getDescriptors() != null) {
        for (Descriptor descriptor : descriptorFile.getDescriptors()) {
          updateRelayNetworkStatusConsensus((RelayNetworkStatusConsensus)
              descriptor);
        }
      }
    }
  }
  private void updateRelayNetworkStatusConsensus(
      RelayNetworkStatusConsensus consensus) {
    long validAfterMillis = consensus.getValidAfterMillis();
    for (NetworkStatusEntry entry :
        consensus.getStatusEntries().values()) {
      String nickname = entry.getNickname();
      String fingerprint = entry.getFingerprint();
      String address = entry.getAddress();
      int orPort = entry.getOrPort();
      int dirPort = entry.getDirPort();
      SortedSet<String> relayFlags = entry.getFlags();
      this.addRelay(nickname, fingerprint, address, validAfterMillis,
          orPort, dirPort, relayFlags);
    }
  }
  private SortedSet<Long> containedPublishedMillis = new TreeSet<Long>();
  public void updateBridgeNetworkStatuses() {
    BridgeDescriptorReader reader =
        DescriptorSourceFactory.createBridgeDescriptorReader();
    reader.addDirectory(new File("in/bridge-descriptors/statuses"));
    reader.setExcludeFiles(new File("status/bridge-status-history"));
    Iterator<DescriptorFile> descriptorFiles = reader.readDescriptors();
    while (descriptorFiles.hasNext()) {
      DescriptorFile descriptorFile = descriptorFiles.next();
      if (descriptorFile.getDescriptors() != null) {
        for (Descriptor descriptor : descriptorFile.getDescriptors()) {
          updateBridgeNetworkStatus((BridgeNetworkStatus) descriptor);
        }
      }
    }
  }
  private void updateBridgeNetworkStatus(BridgeNetworkStatus status) {
    long publishedMillis = status.getPublishedMillis();
    for (NetworkStatusEntry entry : status.getStatusEntries().values()) {
      String fingerprint = entry.getFingerprint();
      int orPort = entry.getOrPort();
      int dirPort = entry.getDirPort();
      SortedSet<String> relayFlags = entry.getFlags();
      this.addBridge(fingerprint, publishedMillis, orPort, dirPort,
         relayFlags);
    }
    this.containedPublishedMillis.add(publishedMillis);
  }
  private SortedMap<String, SearchEntryData> containedBridges =
      new TreeMap<String, SearchEntryData>();
  public void addBridge(String fingerprint, long publishedMillis,
      int orPort, int dirPort, SortedSet<String> relayFlags) {
    if (publishedMillis >= now - 7L * 24L * 60L * 60L * 1000L &&
        (!this.containedBridges.containsKey(fingerprint) ||
        this.containedBridges.get(fingerprint).
        getValidAfterMillis() < publishedMillis)) {
      SearchEntryData entry = new SearchEntryData(fingerprint,
          publishedMillis, orPort, dirPort, relayFlags);
      this.containedBridges.put(fingerprint, entry);
    }
  }
  public SortedMap<String, SearchEntryData> getBridges() {
    return new TreeMap<String, SearchEntryData>(this.containedBridges);
  }
  public long getLastPublishedMillis() {
    return this.containedPublishedMillis.isEmpty() ? -1L :
        this.containedPublishedMillis.last();
  }
  public SortedSet<String> getCurrentFingerprints() {
    SortedSet<String> result = new TreeSet<String>();
    result.addAll(this.containedBridges.keySet());
    result.addAll(this.containedRelays.keySet());
    return result;
  }
}
