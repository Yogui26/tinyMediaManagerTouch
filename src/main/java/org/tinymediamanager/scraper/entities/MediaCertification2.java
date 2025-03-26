package org.tinymediamanager.scraper.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MediaCertification2 {
  private static final Logger        LOGGER       = LoggerFactory.getLogger(MediaCertification2.class);
  private static MediaCertification2 instance;
  protected Map<String, Country>     countryCerts = new TreeMap<String, MediaCertification2.Country>();
  protected Map<String, Country>     csvCerts     = new TreeMap<String, MediaCertification2.Country>();

  public static synchronized MediaCertification2 getInstance() {
    if (instance == null) {
      instance = new MediaCertification2();
    }
    return instance;
  }

  private MediaCertification2() {
  }

  /**
   * gets an unmodifiable list of all certs (~ 1500)
   * 
   * @return
   */
  public List<Certification> getAllCerts() {
    List<Certification> certs = new ArrayList<>();
    for (Country country : countryCerts.values()) {
      if (country.movie != null) {
        certs.addAll(country.movie);
      }
      if (country.show != null) {
        certs.addAll(country.show);
      }
    }
    return Collections.unmodifiableList(certs);
  }

  public Country getCountryCerts(String country) {
    return countryCerts.get(country);
  }

  public Country getCsvCountryCerts(String country) {
    return csvCerts.get(country);
  }

  public Country getCountryCerts(CountryCode cc) {
    return countryCerts.get(cc.getAlpha2());
  }

  public void load() {
    int certCnt = 0;
    try {
      LOGGER.debug("Reading JSON certifications");
      InputStream json = MediaCertification2.class.getResourceAsStream("/certifications.json");
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(json);
      node = node.get("data");

      // foreach country
      Iterator<Entry<String, JsonNode>> countries = node.fields();
      while (countries.hasNext()) {
        Entry<String, JsonNode> c = countries.next();
        Country country = new Country();
        country.countryCode = c.getKey();

        // foreach movies / tv / other exception names
        Iterator<Entry<String, JsonNode>> system = c.getValue().fields();
        while (system.hasNext()) {
          Entry<String, JsonNode> sys = system.next();
          List<Certification> list = new ArrayList<>();

          // foreach certificate
          Iterator<Entry<String, JsonNode>> certs = sys.getValue().fields();
          while (certs.hasNext()) {
            Entry<String, JsonNode> cert = certs.next();
            Certification crt = mapper.treeToValue(cert.getValue(), Certification.class);
            crt.id = cert.getKey();

            if (crt.minimumAge == -1) {
              crt.calcMinimumAge(); // detect, if not loaded
            }
            // if we couldn't detect any, and we have a low rating, use that (err: MV_TV_P13 has 100, but is for 13yr)
            if (crt.minimumAge == -1 && crt.ratingValue <= 100) {
              crt.minimumAge = 0;
            }

            list.add(crt);
            certCnt++; // easier than counting afterwards
          }

          if (sys.getKey().endsWith("_TV")) {
            country.show = list;
          }
          else {
            if (country.movie == null) {
              country.movie = list;
            }
            else {
              country.movie.addAll(list);// for CA, which have 2 movie rating systems
            }
          }
        }
        countryCerts.put(c.getKey(), country);
      }
    }
    catch (IOException e) {
      LOGGER.error("Error parsing certifications: {}", e.getMessage());
    }
    LOGGER.debug("Loaded {} countries with {} certifications...", countryCerts.size(), certCnt);

    certCnt = 0;
    LOGGER.debug("Reading CSV certifications"); // ~ 245 deprecated. Mark em?
    InputStream is = MediaCertification2.class.getResourceAsStream("/certifications.csv");
    try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader)) {
      String line;
      Country country = new Country();
      String lastCC = "";
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("sep=") || line.contains("COUNTRY_CODE")) {
          continue; // headers
        }
        // do NOT split on comma yet, since it can (and will) be within a text delimiter!!
        String safeLine = line.replace("\",\"", "|"); // ","
        safeLine = safeLine.replaceAll("^\\\"", "");
        safeLine = safeLine.replaceAll("\\\"$", "");
        String[] split = safeLine.split("\\|");
        // split[0] - COUNTRY_CODE
        // split[1] - COUNTRY_NAME
        // split[2] - SYSTEM_CODE (cc-movie|tv)
        // split[3] - RATING_CODE
        // split[4] - DESCRIPTION
        // split[5] - STATE

        if (split[3].equals("-")) {
          continue; // PT, MK
        }

        String cc = split[0];
        if (!lastCC.equals(cc) && !lastCC.isEmpty()) {
          csvCerts.put(lastCC, country);
          country = new Country();
        }
        country.countryCode = cc;

        certCnt++;
        Certification crt = new Certification();
        // ID style quite same like JSON
        crt.id = split[2].toUpperCase(Locale.ROOT).replace("-", "_") + "_" + split[3].replace("+", "Plus").replace("-", "_");
        crt.displayValue = split[3];
        crt.ratingCodeLabels = split[3];
        crt.description = split[4];
        if (crt.minimumAge == -1) {
          crt.calcMinimumAge(); // detect, if not loaded
        }

        if (split[2].endsWith("-tv")) {
          if (country.show == null) {
            country.show = new ArrayList<>();
          }
          country.show.add(crt);
        }
        else {
          if (country.movie == null) {
            country.movie = new ArrayList<>();
          }
          country.movie.add(crt);
        }

        lastCC = cc;
      }
    }
    catch (IOException e) {
      LOGGER.error("Error parsing CSV certifications: {}", e.getMessage());
    }
    LOGGER.debug("Loaded {} countries with {} certifications...", csvCerts.size(), certCnt);
  }

  /**
   * Returns a movie rating
   * 
   * @param country
   *          2 char iso string
   * @param rating
   *          plain text
   * @return the rating or NULL
   */
  public Certification getMovieRatingFor(String country, String rating) {
    return getRatingFor(MediaType.MOVIE, country, rating);
  }

  public Certification getMovieRatingFor(MediaCertification old) {
    if (old.getCountry() == null) {
      return null; // unknown, unrated, notrated...
    }
    Certification cert = getMovieRatingFor(old.getCountry().getAlpha2(), old.getName());
    if (cert == null) {
      for (String alt : old.getPossibleNotations()) {
        cert = getMovieRatingFor(old.getCountry().getAlpha2(), alt);
        if (cert != null) {
          break;
        }
      }
    }
    if (cert != null) {
      cert.oldTmmEnum = old.name();
    }
    return cert;
  }

  /**
   * Returns a show rating
   * 
   * @param country
   *          2 char iso string
   * @param rating
   *          plain text
   * @return the rating or NULL
   */
  public Certification getTvShowRatingFor(String country, String rating) {
    return getRatingFor(MediaType.TV_SHOW, country, rating);
  }

  public Certification getTvShowRatingFor(MediaCertification old) {
    if (old.getCountry() == null) {
      return null; // unknown, unrated, notrated...
    }
    Certification cert = getTvShowRatingFor(old.getCountry().getAlpha2(), old.getName());
    if (cert == null) {
      for (String alt : old.getPossibleNotations()) {
        cert = getTvShowRatingFor(old.getCountry().getAlpha2(), alt);
        if (cert != null) {
          break;
        }
      }
    }
    if (cert != null) {
      cert.oldTmmEnum = old.name();
    }
    return cert;
  }

  /**
   * Returns a movie rating
   * 
   * @param type
   *          MediaType.MOVIE or MediaType.TV_SHOW
   * @param country
   *          2 char iso string
   * @param rating
   *          plain text
   * @return the rating or NULL
   */
  public Certification getRatingFor(MediaType type, String country, String rating) {
    if (StringUtils.isAnyBlank(country, rating)) {
      return null;
    }
    Country c = countryCerts.get(country.toUpperCase(Locale.ROOT));
    if (c != null) {
      List<Certification> certs = type == MediaType.MOVIE ? c.movie : c.show;
      if (certs != null) {
        // iterate over found country certs
        for (Certification cert : certs) {
          String[] splitted = cert.ratingCodeLabels.split("\\|");
          for (String code : splitted) {
            if (rating.equalsIgnoreCase(code)) {
              return cert; // unique code
            }
            if (cert.clean(code).equalsIgnoreCase(cert.clean(rating))) {
              return cert;
            }
            if (code.equals("UR") && rating.equalsIgnoreCase("Unrated")) {
              return cert;
            }
          }
          if (rating.equalsIgnoreCase(cert.displayValue)) {
            return cert; // localized name
          }

          int possibleAge = MetadataUtil.parseInt(cert.clean(rating), -1);
          if (possibleAge == -1) {
            possibleAge = MetadataUtil.parseInt(rating.replaceAll("\\D", " "), -1); // use a space, to not count "1_2" as 12
          }
          if (possibleAge > -1) {
            if (cert.minimumAge == possibleAge) {
              System.out.println(
                  "matched " + type + " by age - check names: " + cert.id + " (" + cert.minimumAge + ") - " + rating + " (" + possibleAge + ")");
              return cert;
            }
            // Fallback: also consider a one year higher difference as match?!
            if (cert.minimumAge - 1 == possibleAge) {
              System.out.println("matched " + type + " by higher age - check names: " + cert.id + " (" + cert.minimumAge + ") - " + rating + " ("
                  + possibleAge + ")");
              return cert;
            }
          }
        } // end foreach country cert
      }
    }
    else {
      LOGGER.debug("Country {} not found in {} certificates", country, type);
    }
    return null;
  }

  public class Country {
    public String              countryCode = "";  // uppercase
    public List<Certification> movie       = null;;
    public List<Certification> show        = null;
    // EXCEPTIONS
    // AT - movies as FSK
    // AU - movies as OFLC
    // CA - movies as CHVRS & RCQ
    // DE - movies as FSK
    // FR - movies as CNC
    // GB - movies as BBFC
    // IE - movies as IFCO
    // JP - movies as EIRIN
    // NZ - movies as OFLC
    // US - movies as MPAA
  }

  // used internally and ALSO used for json parsing, hence the extends/public
  public static class Certification extends BaseJsonEntity {
    public String id               = ""; // country_type_code
    public String description      = "";
    public String displayValue     = ""; // regional display value
    public String ratingCodeLabels = ""; // can also be delimited like "ALL|0+"
    public int    ratingValue      = 0;  // the higher, the more restricted 0-900
    public int    minimumAge       = -1; // our calculation of the minimum age required to view this
    public String oldTmmEnum       = ""; // if we can match, this is our old enum name

    private void calcMinimumAge() {
      // NR not-rated is considered as unrestricted
      // UR un-rated is highly restricted
      if (ratingValue >= 900 || ratingCodeLabels.equals("UR") || displayValue.equals("Unrated")) {
        minimumAge = -2;
        return;
      }
      else if (ratingCodeLabels.equals("NR") || displayValue.equals("Not Rated")) {
        minimumAge = 0;
        return;
      }

      // description often has the correct age in text, whereas the codeLabels usually are numbered from 1-3, mistakenly used as age
      // so priorize description first; see HK, PL_TV
      int ageFromDesc = MetadataUtil.parseInt(description, -1);
      if (ageFromDesc == -1) {
        ageFromDesc = MetadataUtil.parseInt(clean(description), -1);
      }
      if (ageFromDesc == -1) {
        String teen = StrgUtils.substr(description, ".*?([12]\\d).*"); // 10-21
        if (!teen.isEmpty()) {
          ageFromDesc = MetadataUtil.parseInt(teen);
        }
        else {
          String young = StrgUtils.substr(description, ".*(\\d).*"); // 0-9
          if (!young.isEmpty()) {
            ageFromDesc = MetadataUtil.parseInt(young);
          }
        }
      }

      int ageFromLabel = -1;
      String[] codeLabels = ratingCodeLabels.split("\\|");
      for (String code : codeLabels) {
        ageFromLabel = MetadataUtil.parseInt(code, -1);
        if (ageFromLabel < 0) { // -12, is not minus ;)
          ageFromLabel = MetadataUtil.parseInt(clean(code), -1);
        }
        if (ageFromLabel == -1) {
          String teen = StrgUtils.substr(code, ".*?([12]\\d).*");// 10-21
          if (!teen.isEmpty()) {
            ageFromLabel = MetadataUtil.parseInt(teen);
          }
          else {
            String young = StrgUtils.substr(code, ".*?(\\d).*"); // 0-9
            if (!code.isEmpty()) {
              ageFromLabel = MetadataUtil.parseInt(young, -1);
            }
          }
        }
        if (ageFromLabel > -1) {
          break;
        }
      }

      minimumAge = Math.max(ageFromDesc, ageFromLabel); // better be safe

      if (minimumAge > 21) {
        LOGGER.warn("Whoops, that age calculation for {} ({}) certification might be wrong!!!", id, minimumAge);
      }
      if (minimumAge > 0 && minimumAge < 6) {
        LOGGER.info("Found 'young' age of {}; cert: {}:{} - {}", minimumAge, id, ratingCodeLabels, description);
      }
    }

    // clean out only selective strings, not \D regex!!
    private String clean(String name) {
      name = name.replace("K-", "");
      name = name.replace("N-", "");
      name = name.replace("B-", "");
      name = name.replace("TV-", "");
      name = name.replace("PG-", "");
      name = name.replace("PG ", "");
      name = name.replace("PG", "");
      name = name.replace("M", "");
      name = name.replace("A", "");
      name = name.replace("K", "");
      name = name.replace("X", "");
      name = name.replace("R", "");
      name = name.replace("T", "");
      name = name.replace("u", "");
      // only at start/end!
      name = name.replaceFirst("^\\+", "");
      name = name.replaceFirst("^\\-", "");
      name = name.replaceFirst("\\+$", "");
      name = name.replaceFirst("\\-$", "");
      return name.strip();
    }
  }
}
