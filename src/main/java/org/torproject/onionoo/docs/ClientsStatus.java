/* Copyright 2014--2016 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.onionoo.docs;

import org.torproject.onionoo.util.TimeFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

public class ClientsStatus extends Document {

  private static Logger log = LoggerFactory.getLogger(
      ClientsStatus.class);

  private transient boolean isDirty = false;

  public boolean isDirty() {
    return this.isDirty;
  }

  public void clearDirty() {
    this.isDirty = false;
  }

  private SortedSet<ClientsHistory> history =
      new TreeSet<ClientsHistory>();

  public void setHistory(SortedSet<ClientsHistory> history) {
    this.history = history;
  }

  public SortedSet<ClientsHistory> getHistory() {
    return this.history;
  }

  @Override
  public void setFromDocumentString(String documentString) {
    try (Scanner s = new Scanner(documentString)) {
      while (s.hasNextLine()) {
        String line = s.nextLine();
        ClientsHistory parsedLine = ClientsHistory.fromString(line);
        if (parsedLine != null) {
          this.history.add(parsedLine);
        } else {
          log.error("Could not parse clients history line '"
              + line + "'.  Skipping.");
        }
      }
    }
  }

  /** Adds all given clients history objects that don't overlap with
   * existing clients history objects. */
  public void addToHistory(SortedSet<ClientsHistory> newIntervals) {
    for (ClientsHistory interval : newIntervals) {
      if ((this.history.headSet(interval).isEmpty()
          || this.history.headSet(interval).last().getEndMillis()
          <= interval.getStartMillis())
          && (this.history.tailSet(interval).isEmpty()
          || this.history.tailSet(interval).first().getStartMillis()
          >= interval.getEndMillis())) {
        this.history.add(interval);
        this.isDirty = true;
      }
    }
  }

  /** Compresses the history of clients objects by merging adjacent
   * intervals, depending on how far back in the past they lie. */
  public void compressHistory() {
    SortedSet<ClientsHistory> uncompressedHistory =
        new TreeSet<ClientsHistory>(this.history);
    history.clear();
    ClientsHistory lastResponses = null;
    String lastMonthString = "1970-01";
    long now = TimeFactory.getTime().currentTimeMillis();
    for (ClientsHistory responses : uncompressedHistory) {
      long intervalLengthMillis;
      if (now - responses.getEndMillis()
          <= DateTimeHelper.ROUGHLY_THREE_MONTHS) {
        intervalLengthMillis = DateTimeHelper.ONE_DAY;
      } else if (now - responses.getEndMillis()
          <= DateTimeHelper.ROUGHLY_ONE_YEAR) {
        intervalLengthMillis = DateTimeHelper.TWO_DAYS;
      } else {
        intervalLengthMillis = DateTimeHelper.TEN_DAYS;
      }
      String monthString = DateTimeHelper.format(
          responses.getStartMillis(),
          DateTimeHelper.ISO_YEARMONTH_FORMAT);
      if (lastResponses != null
          && lastResponses.getEndMillis() == responses.getStartMillis()
          && ((lastResponses.getEndMillis() - 1L) / intervalLengthMillis)
          == ((responses.getEndMillis() - 1L) / intervalLengthMillis)
          && lastMonthString.equals(monthString)) {
        lastResponses.addResponses(responses);
      } else {
        if (lastResponses != null) {
          this.history.add(lastResponses);
        }
        lastResponses = responses;
      }
      lastMonthString = monthString;
    }
    if (lastResponses != null) {
      this.history.add(lastResponses);
    }
  }

  @Override
  public String toDocumentString() {
    StringBuilder sb = new StringBuilder();
    for (ClientsHistory interval : this.history) {
      sb.append(interval.toString() + "\n");
    }
    return sb.toString();
  }
}

