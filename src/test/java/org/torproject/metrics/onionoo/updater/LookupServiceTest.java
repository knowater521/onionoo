/* Copyright 2013--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.metrics.onionoo.updater;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class LookupServiceTest {

  private List<String> geoLite2CityBlocksIPv4Lines;
  private List<String> geoLite2CityLocationsEnLines;
  private List<String> geoLite2AsnBlocksIpv4Lines;

  private LookupService lookupService;

  private SortedSet<String> addressStrings = new TreeSet<>();

  private SortedMap<String, LookupResult> lookupResults;

  private void populateLines() {
    this.geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    this.geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    this.geoLite2CityBlocksIPv4Lines.add("8.8.0.0/21,6252001,6252001,,0,"
        + "0,,38.0000,-97.0000");
    this.geoLite2CityBlocksIPv4Lines.add("8.8.8.0/24,5375480,6252001,,0,"
        + "0,94035,37.3860,-122.0838");
    this.geoLite2CityBlocksIPv4Lines.add("8.8.9.0/24,6252001,6252001,,0,"
        + "0,,38.0000,-97.0000");
    this.geoLite2CityLocationsEnLines = new ArrayList<>();
    this.geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    this.geoLite2CityLocationsEnLines.add("6252001,en,NA,"
        + "\"North America\",US,\"United States\",,,,,,,");
    this.geoLite2CityLocationsEnLines.add("5375480,en,NA,"
        + "\"North America\",US,\"United States\",CA,California,,,"
        + "\"Mountain View\",807,America/Los_Angeles");
    this.geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    this.geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    this.geoLite2AsnBlocksIpv4Lines.add(
        "8.8.6.0/23,3356,\"Level 3 Parent, LLC\"");
    this.geoLite2AsnBlocksIpv4Lines.add("8.8.8.0/24,15169,\"Google LLC\"");
    this.geoLite2AsnBlocksIpv4Lines.add(
        "8.8.9.0/24,3356,\"Level 3 Parent, LLC\"");
  }

  private void writeCsvFiles() {
    try {
      this.writeCsvFile(this.geoLite2CityBlocksIPv4Lines,
          "GeoLite2-City-Blocks-IPv4.csv");
      this.writeCsvFile(this.geoLite2CityLocationsEnLines,
          "GeoLite2-City-Locations-en.csv");
      this.writeCsvFile(this.geoLite2AsnBlocksIpv4Lines,
          "GeoLite2-ASN-Blocks-IPv4.csv");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeCsvFile(List<String> lines, String fileName)
      throws IOException {
    if (lines != null && !lines.isEmpty()) {
      try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
          new FileOutputStream(new File(this.tempGeoipDir, fileName)),
          StandardCharsets.UTF_8))) {

        for (String line : lines) {
          bw.write(line);
          bw.newLine();
        }
      }
    }
  }

  private void performLookups() {
    this.lookupService = new LookupService(this.tempGeoipDir);
    this.lookupResults = this.lookupService.lookup(this.addressStrings);
  }

  private void assertLookupResult(List<String> geoLite2CityBlocksLines,
      List<String> geoLite2CityLocationsLines,
      List<String> geoLite2AsnBlocksIpv4Lines, String addressString,
      String countryCode, String countryName, String regionName,
      String cityName, Float latitude, Float longitude, String asNumber,
      String asName) {
    this.addressStrings.add(addressString);
    this.populateLines();
    if (geoLite2CityBlocksLines != null) {
      this.geoLite2CityBlocksIPv4Lines = geoLite2CityBlocksLines;
    }
    if (geoLite2CityLocationsLines != null) {
      this.geoLite2CityLocationsEnLines = geoLite2CityLocationsLines;
    }
    if (geoLite2AsnBlocksIpv4Lines != null) {
      this.geoLite2AsnBlocksIpv4Lines = geoLite2AsnBlocksIpv4Lines;
    }
    this.writeCsvFiles();
    /* Disable log messages printed to System.err. */
    System.setErr(new PrintStream(new OutputStream() {
      public void write(int someInt) {
      }
    }));
    this.performLookups();
    if (countryCode == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getCountryCode() == null);
    } else {
      assertEquals(countryCode,
          this.lookupResults.get(addressString).getCountryCode());
    }
    if (countryName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getCountryName() == null);
    } else {
      assertEquals(countryName,
          this.lookupResults.get(addressString).getCountryName());
    }
    if (regionName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getRegionName() == null);
    } else {
      assertEquals(regionName,
          this.lookupResults.get(addressString).getRegionName());
    }
    if (cityName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getCityName() == null);
    } else {
      assertEquals(cityName,
          this.lookupResults.get(addressString).getCityName());
    }
    if (latitude == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getLatitude() == null);
    } else {
      assertEquals(latitude,
          this.lookupResults.get(addressString).getLatitude(), 0.01);
    }
    if (longitude == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getLongitude() == null);
    } else {
      assertEquals(longitude,
          this.lookupResults.get(addressString).getLongitude(), 0.01);
    }
    if (asNumber == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getAsNumber() == null);
    } else {
      assertEquals(asNumber,
          this.lookupResults.get(addressString).getAsNumber());
    }
    if (asName == null) {
      assertTrue(!this.lookupResults.containsKey(addressString)
          || this.lookupResults.get(addressString).getAsName() == null);
    } else {
      assertEquals(asName,
          this.lookupResults.get(addressString).getAsName());
    }
  }

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private File tempGeoipDir;

  @Before
  public void createTempGeoipDir() throws IOException {
    this.tempGeoipDir = this.tempFolder.newFolder("geoip");
  }

  @Test()
  public void testLookup8888() {
    this.assertLookupResult(null, null, null, "8.8.8.8", "us",
        "United States", "California", "Mountain View", 37.3860f,
        -122.0838f, "AS15169", "Google LLC");
  }

  @Test()
  public void testLookup8880() {
    this.assertLookupResult(null, null, null, "8.8.8.0", "us",
        "United States", "California", "Mountain View", 37.3860f,
        -122.0838f, "AS15169", "Google LLC");
  }

  @Test()
  public void testLookup888255() {
    this.assertLookupResult(null, null, null, "8.8.8.255", "us",
        "United States", "California", "Mountain View", 37.3860f,
        -122.0838f, "AS15169", "Google LLC");
  }

  @Test()
  public void testLookup888256() {
    this.assertLookupResult(null, null, null, "8.8.8.256", null, null,
        null, null, null, null, null, null);
  }

  @Test()
  public void testLookup888Minus1() {
    this.assertLookupResult(null, null, null, "8.8.8.-1", null, null,
        null, null, null, null, null, null);
  }

  @Test()
  public void testLookup000() {
    this.assertLookupResult(null, null, null, "0.0.0.0", null, null, null,
        null, null, null, null, null);
  }

  @Test()
  public void testLookupNoBlocksLines() {
    this.assertLookupResult(new ArrayList<>(), null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoLocationLines() {
    this.assertLookupResult(null, new ArrayList<>(), null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoGeoipAsNum2Lines() {
    this.assertLookupResult(null, null, new ArrayList<>(),
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupNoCorrespondingLocation() {
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("6252001,en,NA,"
        + "\"North America\",US,\"United States\",,,,,,,");
    this.assertLookupResult(null, geoLite2CityLocationsEnLines, null,
        "8.8.8.8", null, null, null, null, 37.3860f, -122.0838f,
        "AS15169", "Google LLC");
  }

  @Test()
  public void testLookupBlocksStartNotANumber() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("one/24,5375480,6252001,,0,"
        + "0,94035,37.3860,-122.0838");
    this.assertLookupResult(
        geoLite2CityBlocksIPv4Lines, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupBlocksLocationX() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("8.8.8.0/24,X,X,,0,0,94035,37.3860,"
        + "-122.0838");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupBlocksLocationEmpty() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("8.8.8.0/24,,,,0,0,,,");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines, null, null,
        "8.8.8.8", null, null, null, null, null, null, "AS15169",
        "Google LLC");
  }

  @Test()
  public void testLookupBlocksTooFewFields() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("8.8.8.0/24,5375480,6252001,,0,"
        + "0,94035,37.3860");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines, null, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupBlocksExtraneousField() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude,accuracy_radius");
    geoLite2CityBlocksIPv4Lines.add("8.8.8.0/24,5375480,6252001,,0,"
        + "0,94035,37.3860,-122.0838,937");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines, null, null,
        "8.8.8.8", "us", "United States", "California", "Mountain View",
        37.3860f, -122.0838f, "AS15169", "Google LLC");
  }

  @Test()
  public void testLookupBlocksThreeExtraneousFields() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude,accuracy_radius,more,even_more,wow_so_many_fields");
    geoLite2CityBlocksIPv4Lines.add("8.8.8.0/24,5375480,6252001,,0,"
        + "0,94035,37.3860,-122.0838,937,1,2,30000000000000");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines, null, null,
        "8.8.8.8", "us", "United States", "California", "Mountain View",
        37.3860f, -122.0838f, "AS15169", "Google LLC");
  }

  @Test()
  public void testLookupLocationLocIdNotANumber() {
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("threetwoonenineone,en,NA,"
        + "\"North America\",US,\"United States\",CA,California,,,"
        + "\"Mountain View\",807,America/Los_Angeles");
    this.assertLookupResult(null, geoLite2CityLocationsEnLines, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupLocationTooFewFields() {
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("threetwoonenineone,en,NA,"
        + "\"North America\",US,\"United States\",CA,California,,,"
        + "\"Mountain View\",807");
    this.assertLookupResult(null, geoLite2CityLocationsEnLines, null,
        "8.8.8.8", null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4IllegalIpAddress() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add(
        "eighteighteightzero/24,15169,\"Google LLC\"");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4MaskTooSmall() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("8.8.8.0/7,15169,\"Google LLC\"");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4MaskTooBig() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("8.8.8.0/33,15169,\"Google LLC\"");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4MaskMissing() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("8.8.8.0,15169,\"Google LLC\"");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4TooFewFields() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("8.8.8.0/24,15169");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4TooFewFieldsNoAsNumber() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("8.8.8.0/24,,\"Google LLC\"");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        null, null, null, null, null, null, null, null);
  }

  @Test()
  public void testLookupGeoLite2AsnBlocksIpv4AsNameEmpty() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("8.8.8.0/24,15169,");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines, "8.8.8.8",
        "us", "United States", "California", "Mountain View", 37.3860f,
        -122.0838f, "AS15169", "");
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationTurkey() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("46.1.133.0/24,307515,298795,,0,0,,"
        + "39.1458,34.1639");
    geoLite2CityBlocksIPv4Lines.add("46.196.12.0/24,738927,298795,,0,0,,"
        + "40.9780,27.5085");
    geoLite2CityBlocksIPv4Lines.add("78.180.14.0/24,745169,298795,,0,0,,"
        + "40.0781,29.5133");
    geoLite2CityBlocksIPv4Lines.add("81.215.1.0/24,749748,298795,,0,0,,"
        + "40.6000,33.6153");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("307515,en,AS,Asia,TR,Turkey,40,"
        + "\"K\u0131r\u015Fehir\",,,\"K\u0131r\u015Fehir\",,"
        + "Europe/Istanbul");
    geoLite2CityLocationsEnLines.add("738927,en,AS,Asia,TR,Turkey,59,"
        + "\"Tekirda\u011F\",,,\"Tekirda\u011F\",,Europe/Istanbul");
    geoLite2CityLocationsEnLines.add("745169,en,AS,Asia,TR,Turkey,16,"
        + "Bursa,,,\u0130neg\u00F6l,,Europe/Istanbul");
    geoLite2CityLocationsEnLines.add("749748,en,AS,Asia,TR,Turkey,18,"
        + "\"\u00C7ank\u0131r\u0131\",,,\"\u00C7ank\u0131r\u0131\",,"
        + "Europe/Istanbul");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "46.1.133.0", "tr", "Turkey",
        "K\u0131r\u015Fehir", "K\u0131r\u015Fehir", 39.1458f, 34.1639f,
        null, null);
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "46.196.12.0", "tr", "Turkey",
        "Tekirda\u011F", "Tekirda\u011F", 40.9780f, 27.5085f, null, null);
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "78.180.14.0", "tr", "Turkey",
        "Bursa", "\u0130neg\u00F6l", 40.0781f, 29.5133f, null, null);
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "81.215.1.0", "tr", "Turkey",
        "\u00C7ank\u0131r\u0131", "\u00C7ank\u0131r\u0131", 40.6000f,
        33.6153f, null, null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationLatvia() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("78.28.192.0/24,456202,458258,,0,0,,"
        + "56.5000,27.3167");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("456202,en,EU,Europe,LV,Latvia,REZ,"
        + "Rezekne,,,\"R\u0113zekne\",,Europe/Riga");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "78.28.192.0", "lv", "Latvia",
        "Rezekne", "R\u0113zekne", 56.5000f, 27.3167f, null, null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationAzerbaijan() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("94.20.148.0/24,585170,587116,,0,0,,"
        + "41.1919,47.1706");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("585170,en,AS,Asia,AZ,Azerbaijan,"
        + "SAK,\"Shaki City\",,,\"\u015E\u01DDki\",,Asia/Baku");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "94.20.148.0", "az",
        "Azerbaijan", "Shaki City", "\u015E\u01DDki", 41.1919f, 47.1706f,
        null, null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationVietnam() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("115.78.92.0/23,1587976,1562822,,0,0,"
        + ",10.2333,106.3833");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("1587976,en,AS,Asia,VN,Vietnam,50,"
        + "\"Tinh Ben Tre\",,,\"B\u1EBFn Tre\",,Asia/Ho_Chi_Minh");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "115.78.92.0", "vn",
        "Vietnam", "Tinh Ben Tre", "B\u1EBFn Tre", 10.2333f, 106.3833f,
        null, null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationJapan() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("113.154.131.0/24,1848333,1861060,,0,"
        + "0,1012236,35.8000,139.1833");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("1848333,en,AS,Asia,JP,Japan,13,"
        + "\"T\u014Dky\u014D\",,,Yokoo,,Asia/Tokyo");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "113.154.131.0", "jp",
        "Japan", "T\u014Dky\u014D", "Yokoo", 35.8000f, 139.1833f, null,
        null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationDenmark() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("2.110.246.0/24,2625001,2623032,,0,0,"
        + "5970,54.8880,10.4112");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("2625001,en,EU,Europe,DK,Denmark,83,"
        + "\"South Denmark\",,,\"\u00C6r\u00F8sk\u00F8bing\",,"
        + "Europe/Copenhagen");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "2.110.246.0", "dk",
        "Denmark", "South Denmark", "\u00C6r\u00F8sk\u00F8bing", 54.8880f,
        10.4112f, null, null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationGermany() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("37.209.30.128/25,2947444,2921044,,0,"
        + "0,,48.6833,9.0167");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("2947444,en,EU,Europe,DE,Germany,BW,"
        + "\"Baden-W\u00FCrttemberg Region\",,,B\u00F6blingen,,"
        + "Europe/Berlin");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "37.209.30.128", "de",
        "Germany", "Baden-W\u00FCrttemberg Region", "B\u00F6blingen",
        48.6833f, 9.0167f, null, null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationPoland() {
    List<String> geoLite2CityBlocksIPv4Lines = new ArrayList<>();
    geoLite2CityBlocksIPv4Lines.add("network,geoname_id,"
        + "registered_country_geoname_id,represented_country_geoname_id,"
        + "is_anonymous_proxy,is_satellite_provider,postal_code,latitude,"
        + "longitude");
    geoLite2CityBlocksIPv4Lines.add("5.185.94.0/24,3099434,798544,,0,0,,"
        + "54.3608,18.6583");
    List<String> geoLite2CityLocationsEnLines = new ArrayList<>();
    geoLite2CityLocationsEnLines.add("geoname_id,locale_code,"
        + "continent_code,continent_name,country_iso_code,country_name,"
        + "subdivision_1_iso_code,subdivision_1_name,"
        + "subdivision_2_iso_code,subdivision_2_name,city_name,"
        + "metro_code,time_zone");
    geoLite2CityLocationsEnLines.add("3099434,en,EU,Europe,PL,Poland,PM,"
        + "\"Pomeranian Voivodeship\",,,\"Gda\u0144sk\",,Europe/Warsaw");
    this.assertLookupResult(geoLite2CityBlocksIPv4Lines,
        geoLite2CityLocationsEnLines, null, "5.185.94.0", "pl", "Poland",
        "Pomeranian Voivodeship", "Gda\u0144sk", 54.3608f, 18.6583f, null,
        null);
  }

  @Test()
  @SuppressWarnings("AvoidEscapedUnicodeCharacters")
  public void testLookupLocationAsNameNonAscii() {
    List<String> geoLite2AsnBlocksIpv4Lines = new ArrayList<>();
    geoLite2AsnBlocksIpv4Lines.add("network,autonomous_system_number,"
        + "autonomous_system_organization");
    geoLite2AsnBlocksIpv4Lines.add("191.52.240.0/21,52693,"
        + "\"Conectel Telecomunica\u00E7\u00F5es e Inform\u00E1tica Ltda ME\"");
    geoLite2AsnBlocksIpv4Lines.add("191.102.248.0/21,262934,\"IP\u00B7RED\"");
    geoLite2AsnBlocksIpv4Lines.add("192.100.157.0/24,263226,"
        + "\"COMPA\u00D1\u00CDA FINANCIERA ARGENTINA S.A.\"");
    //this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines,
    //    "191.52.240.0", null, null, null, null, null, null, "AS52693",
    //    "Conectel Telecomunica\u00E7\u00F5es e Inform\u00E1tica Ltda ME");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines,
        "191.102.248.0", null, null, null, null, null, null, "AS262934",
        "IP\u00B7RED");
    this.assertLookupResult(null, null, geoLite2AsnBlocksIpv4Lines,
        "192.100.157.0", null, null, null, null, null, null, "AS263226",
        "COMPA\u00D1\u00CDA FINANCIERA ARGENTINA S.A.");
  }
}

