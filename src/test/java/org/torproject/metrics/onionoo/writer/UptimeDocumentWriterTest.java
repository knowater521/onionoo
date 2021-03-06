/* Copyright 2014--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.metrics.onionoo.writer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.torproject.metrics.onionoo.docs.DateTimeHelper;
import org.torproject.metrics.onionoo.docs.DocumentStoreFactory;
import org.torproject.metrics.onionoo.docs.DummyDocumentStore;
import org.torproject.metrics.onionoo.docs.GraphHistory;
import org.torproject.metrics.onionoo.docs.UptimeDocument;
import org.torproject.metrics.onionoo.docs.UptimeStatus;
import org.torproject.metrics.onionoo.updater.DescriptorSourceFactory;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class UptimeDocumentWriterTest {

  private static final long TEST_TIME = DateTimeHelper.parse(
      "2014-03-23 12:00:00");

  private DummyDocumentStore documentStore;

  @Before
  public void createDummyDocumentStore() {
    this.documentStore = new DummyDocumentStore();
    DocumentStoreFactory.setDocumentStore(this.documentStore);
  }

  @Test
  public void testNoStatuses() {
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Without providing any data, nothing should be written "
        + "to disk.", 0,
        this.documentStore.getPerformedStoreOperations());
  }

  private static final String ALL_RELAYS_FINGERPRINT = null;

  private static final String GABELMOO_FINGERPRINT =
      "F2044413DAC2E02E3D6BCF4735A19BCA1DE97281";

  private void addStatusOneWeekSample(String allRelaysUptime,
      String gabelmooUptime) {
    UptimeStatus status = new UptimeStatus();
    status.setFromDocumentString(allRelaysUptime);
    this.documentStore.addDocument(status, ALL_RELAYS_FINGERPRINT);
    status = new UptimeStatus();
    status.setFromDocumentString(gabelmooUptime);
    this.documentStore.addDocument(status, GABELMOO_FINGERPRINT);
  }

  private static final long ONE_SECOND = 1000L;
  private static final long ONE_HOUR = 60L * 60L * ONE_SECOND;
  private static final long FOUR_HOURS = 4L * ONE_HOUR;

  private void assertOneMonthGraph(UptimeDocument document, int graphs,
      String first, String last, int count, List<Integer> values) {
    this.assertGraph(document, graphs, "1_month", first, last,
        (int) (FOUR_HOURS / ONE_SECOND), count, values);
  }

  private void assertFiveYearGraph(UptimeDocument document, int graphs,
      String first, String last, int count, List<Integer> values) {
    this.assertGraph(document, graphs, "5_years", first, last,
        (int) (DateTimeHelper.TEN_DAYS / ONE_SECOND), count, values);
  }

  private void assertGraph(UptimeDocument document, int graphs,
      String graphName, String first, String last, int interval,
      int count, List<Integer> values) {
    assertEquals("Should contain exactly " + graphs + " graphs.", graphs,
        document.getUptime().size());
    assertTrue("Should contain a graph for " + graphName + ".",
        document.getUptime().containsKey(graphName));
    GraphHistory history = document.getUptime().get(graphName);
    assertEquals("First data point should be " + first + ".",
        DateTimeHelper.parse(first), history.getFirst());
    assertEquals("Last data point should be " + last + ".",
        DateTimeHelper.parse(last), history.getLast());
    assertEquals("Interval should be " + interval + " seconds.", interval,
        (int) history.getInterval());
    assertEquals("Factor should be 1.0 / 999.0.", 1.0 / 999.0,
        history.getFactor(), 0.01);
    assertEquals("There should be " + count + " data points.", count,
        (int) history.getCount());
    assertEquals("Count should be the same as the number of values.",
        count, history.getValues().size());
    if (values == null) {
      for (int value : history.getValues()) {
        assertEquals("All values should be 999.", 999, value);
      }
    } else {
      assertEquals("Values are not as expected.", values,
          history.getValues());
    }
  }

  @Test
  public void testOneHourUptime() {
    this.addStatusOneWeekSample("r 2014-03-23-11 1\n",
        "r 2014-03-23-11 1\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    assertEquals("Should not contain any graph.", 0,
        document.getUptime().size());
  }

  @Test
  public void testEightHoursUptime() {
    this.addStatusOneWeekSample("r 2014-03-23-04 8\n",
        "r 2014-03-23-04 8\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-23 06:00:00",
        "2014-03-23 10:00:00", 2, null);
  }

  @Test
  public void testTwoHoursUptimeSeparatedByNull() {
    this.addStatusOneWeekSample("r 2014-03-23-09 1\nr 2014-03-23-11 1\n",
        "r 2014-03-23-09 1\nr 2014-03-23-11 1\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    assertEquals("Should not contain any graph.", 0,
        document.getUptime().size());
  }

  @Test
  public void testEightHoursUptimeSeparatedByFourHoursDowntime() {
    this.addStatusOneWeekSample("r 2014-03-23-00 12\n",
        "r 2014-03-23-00 4\nr 2014-03-23-08 4\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-23 02:00:00",
        "2014-03-23 10:00:00", 3,
        Arrays.asList(999, 0, 999));
  }

  @Test
  public void testEightHoursUptimeThenDowntime() {
    this.addStatusOneWeekSample("r 2014-03-23-00 12\n",
        "r 2014-03-23-00 8\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-23 02:00:00",
        "2014-03-23 10:00:00", 3,
        Arrays.asList(999, 999, 0));
  }

  @Test
  public void testOneWeekUptime() {
    this.addStatusOneWeekSample("r 2014-03-16-13 168\n",
        "r 2014-03-16-13 168\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-16 14:00:00",
        "2014-03-23 10:00:00", 42, null);
  }

  @Test
  public void testOneWeekOneHourUptime() {
    this.addStatusOneWeekSample("r 2014-03-16-12 169\n",
        "r 2014-03-16-12 169\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-16 14:00:00",
        "2014-03-23 10:00:00", 42, null);
  }

  @Test
  public void testOneMonthPartialIntervalOnline() {
    this.addStatusOneWeekSample("r 2014-03-16-08 8\n",
        "r 2014-03-16-11 5\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-16 10:00:00",
        "2014-03-16 14:00:00", 2, null);
  }

  @Test
  public void testOneMonthPartialIntervalOnOff() {
    this.addStatusOneWeekSample("r 2014-03-16-08 8\n",
        "r 2014-03-16-10 1\nr 2014-03-16-12 1\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertOneMonthGraph(document, 1, "2014-03-16 10:00:00",
        "2014-03-16 14:00:00", 2,
        Arrays.asList(999, 499));
  }

  @Test
  public void testFiveYearsLessThan20Percent() {
    /* This relay was running for exactly 11 days and 23 hours over 2 years ago.
     * This time period exactly matches 100% of a data point interval of 10 days
     * plus a tiny bit less than 20% of the next data point interval. */
    this.addStatusOneWeekSample("r 2012-03-01-00 287\n",
        "r 2012-03-01-00 287\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    assertEquals("Should not contain any graph.", 0,
        document.getUptime().size());
  }

  @Test
  public void testFiveYearsAtLeast20Percent() {
    /* This relay was running for exactly 12 days over 2 years ago. This time
     * period exactly matches 100% of a data point interval of 10 days plus 20%
     * of the next data point interval. */
    this.addStatusOneWeekSample("r 2012-03-01-00 288\n",
        "r 2012-03-01-00 288\n");
    UptimeDocumentWriter writer = new UptimeDocumentWriter();
    DescriptorSourceFactory.getDescriptorSource().readDescriptors();
    writer.writeDocuments(TEST_TIME);
    assertEquals("Should write exactly one document.", 1,
        this.documentStore.getPerformedStoreOperations());
    UptimeDocument document = this.documentStore.getDocument(
        UptimeDocument.class, GABELMOO_FINGERPRINT);
    this.assertFiveYearGraph(document, 1, "2012-03-06 00:00:00",
        "2012-03-16 00:00:00", 2, Arrays.asList(999, 999));
  }
}

