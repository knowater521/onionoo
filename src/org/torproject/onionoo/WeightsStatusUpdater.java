/* Copyright 2012--2014 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.onionoo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.NetworkStatusEntry;
import org.torproject.descriptor.RelayNetworkStatusConsensus;
import org.torproject.descriptor.ServerDescriptor;

public class WeightsStatusUpdater implements DescriptorListener,
    StatusUpdater {

  private DescriptorSource descriptorSource;

  private DocumentStore documentStore;

  private long now;

  public WeightsStatusUpdater(DescriptorSource descriptorSource,
      DocumentStore documentStore, Time time) {
    this.descriptorSource = descriptorSource;
    this.documentStore = documentStore;
    this.now = time.currentTimeMillis();
    this.registerDescriptorListeners();
  }

  private void registerDescriptorListeners() {
    this.descriptorSource.registerDescriptorListener(this,
        DescriptorType.RELAY_CONSENSUSES);
    this.descriptorSource.registerDescriptorListener(this,
        DescriptorType.RELAY_SERVER_DESCRIPTORS);
  }

  public void processDescriptor(Descriptor descriptor, boolean relay) {
    if (descriptor instanceof ServerDescriptor) {
      this.processRelayServerDescriptor((ServerDescriptor) descriptor);
    } else if (descriptor instanceof RelayNetworkStatusConsensus) {
      this.processRelayNetworkConsensus(
          (RelayNetworkStatusConsensus) descriptor);
    }
  }

  public void updateStatuses() {
    this.updateWeightsHistories();
    Logger.printStatusTime("Updated weights histories");
    this.updateWeightsStatuses();
    Logger.printStatusTime("Updated weights status files");
  }

  private Set<RelayNetworkStatusConsensus> consensuses =
      new HashSet<RelayNetworkStatusConsensus>();

  private void processRelayNetworkConsensus(
      RelayNetworkStatusConsensus consensus) {
    // TODO This does not scale for bulk imports.
    this.consensuses.add(consensus);
  }

  private Set<String> updateWeightsStatuses = new HashSet<String>();

  private Map<String, Set<String>> descriptorDigestsByFingerprint =
      new HashMap<String, Set<String>>();

  private Map<String, Integer> advertisedBandwidths =
      new HashMap<String, Integer>();

  private void processRelayServerDescriptor(
      ServerDescriptor serverDescriptor) {
    String digest = serverDescriptor.getServerDescriptorDigest().
        toUpperCase();
    int advertisedBandwidth = Math.min(Math.min(
        serverDescriptor.getBandwidthBurst(),
        serverDescriptor.getBandwidthObserved()),
        serverDescriptor.getBandwidthRate());
    this.advertisedBandwidths.put(digest, advertisedBandwidth);
    String fingerprint = serverDescriptor.getFingerprint();
    this.updateWeightsStatuses.add(fingerprint);
    if (!this.descriptorDigestsByFingerprint.containsKey(
        fingerprint)) {
      this.descriptorDigestsByFingerprint.put(fingerprint,
          new HashSet<String>());
    }
    this.descriptorDigestsByFingerprint.get(fingerprint).add(digest);
  }

  private void updateWeightsHistories() {
    for (RelayNetworkStatusConsensus consensus : this.consensuses) {
      long validAfterMillis = consensus.getValidAfterMillis(),
          freshUntilMillis = consensus.getFreshUntilMillis();
      SortedMap<String, double[]> pathSelectionWeights =
          this.calculatePathSelectionProbabilities(consensus);
      this.updateWeightsHistory(validAfterMillis, freshUntilMillis,
          pathSelectionWeights);
    }
  }

  private void updateWeightsHistory(long validAfterMillis,
      long freshUntilMillis,
      SortedMap<String, double[]> pathSelectionWeights) {
    String fingerprint = null;
    double[] weights = null;
    do {
      fingerprint = null;
      synchronized (pathSelectionWeights) {
        if (!pathSelectionWeights.isEmpty()) {
          fingerprint = pathSelectionWeights.firstKey();
          weights = pathSelectionWeights.remove(fingerprint);
        }
      }
      if (fingerprint != null) {
        this.addToHistory(fingerprint, validAfterMillis,
            freshUntilMillis, weights);
      }
    } while (fingerprint != null);
  }

  private SortedMap<String, double[]> calculatePathSelectionProbabilities(
      RelayNetworkStatusConsensus consensus) {
    double wgg = 1.0, wgd = 1.0, wmg = 1.0, wmm = 1.0, wme = 1.0,
        wmd = 1.0, wee = 1.0, wed = 1.0;
    SortedMap<String, Integer> bandwidthWeights =
        consensus.getBandwidthWeights();
    if (bandwidthWeights != null) {
      SortedSet<String> missingWeightKeys = new TreeSet<String>(
          Arrays.asList("Wgg,Wgd,Wmg,Wmm,Wme,Wmd,Wee,Wed".split(",")));
      missingWeightKeys.removeAll(bandwidthWeights.keySet());
      if (missingWeightKeys.isEmpty()) {
        wgg = ((double) bandwidthWeights.get("Wgg")) / 10000.0;
        wgd = ((double) bandwidthWeights.get("Wgd")) / 10000.0;
        wmg = ((double) bandwidthWeights.get("Wmg")) / 10000.0;
        wmm = ((double) bandwidthWeights.get("Wmm")) / 10000.0;
        wme = ((double) bandwidthWeights.get("Wme")) / 10000.0;
        wmd = ((double) bandwidthWeights.get("Wmd")) / 10000.0;
        wee = ((double) bandwidthWeights.get("Wee")) / 10000.0;
        wed = ((double) bandwidthWeights.get("Wed")) / 10000.0;
      }
    }
    SortedMap<String, Double>
        advertisedBandwidths = new TreeMap<String, Double>(),
        consensusWeights = new TreeMap<String, Double>(),
        guardWeights = new TreeMap<String, Double>(),
        middleWeights = new TreeMap<String, Double>(),
        exitWeights = new TreeMap<String, Double>();
    double totalAdvertisedBandwidth = 0.0;
    double totalConsensusWeight = 0.0;
    double totalGuardWeight = 0.0;
    double totalMiddleWeight = 0.0;
    double totalExitWeight = 0.0;
    for (NetworkStatusEntry relay :
        consensus.getStatusEntries().values()) {
      String fingerprint = relay.getFingerprint();
      if (!relay.getFlags().contains("Running")) {
        continue;
      }
      boolean isExit = relay.getFlags().contains("Exit") &&
          !relay.getFlags().contains("BadExit");
      boolean isGuard = relay.getFlags().contains("Guard");
      String serverDescriptorDigest = relay.getDescriptor().
          toUpperCase();
      double advertisedBandwidth = 0.0;
      if (!this.advertisedBandwidths.containsKey(
          serverDescriptorDigest)) {
        WeightsStatus weightsStatus = this.documentStore.retrieve(
            WeightsStatus.class, true, fingerprint);
        if (weightsStatus != null) {
          if (!this.descriptorDigestsByFingerprint.containsKey(
              fingerprint)) {
            this.descriptorDigestsByFingerprint.put(fingerprint,
                new HashSet<String>());
          }
          this.descriptorDigestsByFingerprint.get(fingerprint).addAll(
              weightsStatus.getAdvertisedBandwidths().keySet());
          this.advertisedBandwidths.putAll(
              weightsStatus.getAdvertisedBandwidths());
        }
      }
      if (this.advertisedBandwidths.containsKey(
          serverDescriptorDigest)) {
        advertisedBandwidth = (double) this.advertisedBandwidths.get(
            serverDescriptorDigest);
      }
      double consensusWeight = (double) relay.getBandwidth();
      double guardWeight = (double) relay.getBandwidth();
      double middleWeight = (double) relay.getBandwidth();
      double exitWeight = (double) relay.getBandwidth();
      if (isGuard && isExit) {
        guardWeight *= wgd;
        middleWeight *= wmd;
        exitWeight *= wed;
      } else if (isGuard) {
        guardWeight *= wgg;
        middleWeight *= wmg;
        exitWeight = 0.0;
      } else if (isExit) {
        guardWeight = 0.0;
        middleWeight *= wme;
        exitWeight *= wee;
      } else {
        guardWeight = 0.0;
        middleWeight *= wmm;
        exitWeight = 0.0;
      }
      advertisedBandwidths.put(fingerprint, advertisedBandwidth);
      consensusWeights.put(fingerprint, consensusWeight);
      guardWeights.put(fingerprint, guardWeight);
      middleWeights.put(fingerprint, middleWeight);
      exitWeights.put(fingerprint, exitWeight);
      totalAdvertisedBandwidth += advertisedBandwidth;
      totalConsensusWeight += consensusWeight;
      totalGuardWeight += guardWeight;
      totalMiddleWeight += middleWeight;
      totalExitWeight += exitWeight;
    }
    SortedMap<String, double[]> pathSelectionProbabilities =
        new TreeMap<String, double[]>();
    for (NetworkStatusEntry relay :
        consensus.getStatusEntries().values()) {
      String fingerprint = relay.getFingerprint();
      double[] probabilities = new double[] {
          advertisedBandwidths.get(fingerprint)
            / totalAdvertisedBandwidth,
          consensusWeights.get(fingerprint) / totalConsensusWeight,
          guardWeights.get(fingerprint) / totalGuardWeight,
          middleWeights.get(fingerprint) / totalMiddleWeight,
          exitWeights.get(fingerprint) / totalExitWeight };
      pathSelectionProbabilities.put(fingerprint, probabilities);
    }
    return pathSelectionProbabilities;
  }

  private void addToHistory(String fingerprint, long validAfterMillis,
      long freshUntilMillis, double[] weights) {
    WeightsStatus weightsStatus = this.documentStore.retrieve(
        WeightsStatus.class, true, fingerprint);
    if (weightsStatus == null) {
      weightsStatus = new WeightsStatus();
    }
    SortedMap<long[], double[]> history = weightsStatus.getHistory();
    long[] interval = new long[] { validAfterMillis, freshUntilMillis };
    if ((history.headMap(interval).isEmpty() ||
        history.headMap(interval).lastKey()[1] <= validAfterMillis) &&
        (history.tailMap(interval).isEmpty() ||
        history.tailMap(interval).firstKey()[0] >= freshUntilMillis)) {
      history.put(interval, weights);
      this.compressHistory(weightsStatus);
      this.addAdvertisedBandwidths(weightsStatus, fingerprint);
      this.documentStore.store(weightsStatus, fingerprint);
      this.updateWeightsStatuses.remove(fingerprint);
    }
  }

  private void addAdvertisedBandwidths(WeightsStatus weightsStatus,
      String fingerprint) {
    if (this.descriptorDigestsByFingerprint.containsKey(fingerprint)) {
      for (String descriptorDigest :
          this.descriptorDigestsByFingerprint.get(fingerprint)) {
        if (this.advertisedBandwidths.containsKey(descriptorDigest)) {
          int advertisedBandwidth =
              this.advertisedBandwidths.get(descriptorDigest);
          weightsStatus.getAdvertisedBandwidths().put(descriptorDigest,
              advertisedBandwidth);
        }
      }
    }
  }

  private void compressHistory(WeightsStatus weightsStatus) {
    SortedMap<long[], double[]> history = weightsStatus.getHistory();
    SortedMap<long[], double[]> compressedHistory =
        new TreeMap<long[], double[]>(history.comparator());
    long lastStartMillis = 0L, lastEndMillis = 0L;
    double[] lastWeights = null;
    String lastMonthString = "1970-01";
    for (Map.Entry<long[], double[]> e : history.entrySet()) {
      long startMillis = e.getKey()[0], endMillis = e.getKey()[1];
      double[] weights = e.getValue();
      long intervalLengthMillis;
      if (this.now - endMillis <= DateTimeHelper.ONE_WEEK) {
        intervalLengthMillis = DateTimeHelper.ONE_HOUR;
      } else if (this.now - endMillis <=
          DateTimeHelper.ROUGHLY_ONE_MONTH) {
        intervalLengthMillis = DateTimeHelper.FOUR_HOURS;
      } else if (this.now - endMillis <=
          DateTimeHelper.ROUGHLY_THREE_MONTHS) {
        intervalLengthMillis = DateTimeHelper.TWELVE_HOURS;
      } else if (this.now - endMillis <=
          DateTimeHelper.ROUGHLY_ONE_YEAR) {
        intervalLengthMillis = DateTimeHelper.TWO_DAYS;
      } else {
        intervalLengthMillis = DateTimeHelper.TEN_DAYS;
      }
      String monthString = DateTimeHelper.format(startMillis,
          DateTimeHelper.ISO_YEARMONTH_FORMAT);
      if (lastEndMillis == startMillis &&
          ((lastEndMillis - 1L) / intervalLengthMillis) ==
          ((endMillis - 1L) / intervalLengthMillis) &&
          lastMonthString.equals(monthString)) {
        double lastIntervalInHours = (double) ((lastEndMillis
            - lastStartMillis) / DateTimeHelper.ONE_HOUR);
        double currentIntervalInHours = (double) ((endMillis
            - startMillis) / DateTimeHelper.ONE_HOUR);
        double newIntervalInHours = (double) ((endMillis
            - lastStartMillis) / DateTimeHelper.ONE_HOUR);
        for (int i = 0; i < lastWeights.length; i++) {
          lastWeights[i] *= lastIntervalInHours;
          lastWeights[i] += weights[i] * currentIntervalInHours;
          lastWeights[i] /= newIntervalInHours;
        }
        lastEndMillis = endMillis;
      } else {
        if (lastStartMillis > 0L) {
          compressedHistory.put(new long[] { lastStartMillis,
              lastEndMillis }, lastWeights);
        }
        lastStartMillis = startMillis;
        lastEndMillis = endMillis;
        lastWeights = weights;
      }
      lastMonthString = monthString;
    }
    if (lastStartMillis > 0L) {
      compressedHistory.put(new long[] { lastStartMillis, lastEndMillis },
          lastWeights);
    }
    weightsStatus.setHistory(compressedHistory);
  }

  private void updateWeightsStatuses() {
    for (String fingerprint : this.updateWeightsStatuses) {
      WeightsStatus weightsStatus = this.documentStore.retrieve(
          WeightsStatus.class, true, fingerprint);
      if (weightsStatus == null) {
        weightsStatus = new WeightsStatus();
      }
      this.addAdvertisedBandwidths(weightsStatus, fingerprint);
      this.documentStore.store(weightsStatus, fingerprint);
    }
  }

  public String getStatsString() {
    /* TODO Add statistics string. */
    return null;
  }
}
