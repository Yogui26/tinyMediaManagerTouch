package org.tinymediamanager.scraper.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.util.ListUtils;
import org.tinymediamanager.scraper.util.MetadataUtil;
import org.tinymediamanager.scraper.util.StrgUtils;

public class MediaCertification2 {
  private static final Logger        LOGGER       = LoggerFactory.getLogger(MediaCertification2.class);
  private static MediaCertification2 instance;
  protected Map<String, Country>     countryCerts = new TreeMap<String, MediaCertification2.Country>();

  public static synchronized MediaCertification2 getInstance() {
    if (instance == null) {
      instance = new MediaCertification2();
      instance.loadCsv();
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

  public Map<String, Country> getCertsMap() {
    return countryCerts;
  }

  public void loadCsv() {
    int certCnt = 0;
    LOGGER.debug("Reading CSV certifications"); // ~ 245 deprecated. Old ones which have been officially removed...
    InputStream is = MediaCertification2.class.getResourceAsStream("/umc-content-ratings-addendum.csv");
    try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader)) {
      String line;
      Country country = new Country();
      String lastCC = "";
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("sep=") || line.contains("COUNTRY_CODE")) {
          continue; // headers
        }
        // do NOT split on comma yet, since it can (and will) be within a text delimiter (description)!!
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

        // remove surrounding single quote
        if (split[3].startsWith("'") && split[3].endsWith("'")) {
          split[3] = split[3].substring(1, split[3].length() - 1);
        }

        if (split[3].equals("-")) {
          continue; // PT, MK
        }

        String cc = split[0];
        if (!lastCC.equals(cc) && !lastCC.isEmpty()) {
          countryCerts.put(lastCC, country);
          country = new Country();
        }
        country.countryCode = cc;

        certCnt++;
        Certification crt = new Certification();

        if (split[5].equals("DEPRECATED")) {
          crt.deprecated = true;
          // continue;
        }

        // ID style quite same like JSON
        crt.id = split[2].toUpperCase(Locale.ROOT).replace("-", "_") + "_" + split[3].replace("+", "Plus").replace("-", "_");
        crt.id2 = cc + "_" + split[3].replaceAll("[-+/ ]", ""); // just selected chars, as TH has unicode
        crt.displayValue = split[3];
        crt.ratingCodeLabels = split[3];
        crt.description = split[4];

        crt.alternateNames.add(split[3]); // id
        crt.alternateNames.add(split[3].replaceAll("[^A-Za-z0-9]", "")); // w/o special chars
        Set<String> ex = getMappingNames(cc, split[3]);
        if (!ex.isEmpty()) {
          crt.alternateNames.addAll(ex);
        }

        crt.minimumAge = getAgeExceptions(crt.id);
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
    LOGGER.debug("Loaded {} countries with {} certifications...", countryCerts.size(), certCnt);
  }

  private Set<String> getMappingNames(String cc, String cert) {
    Set<String> ret = new HashSet<>();
    if (cc.equals("BE") && cert.equals("AL-G")) {
      ret.add("AL"); // TMDB
    }
    else if (cc.equals("PT") && cert.equals("T")) {
      ret.add("Públicos"); // TMDB
    }

    return ret;
  }

  private int getAgeExceptions(String id) {
    return switch (id) {
      case "CO_TV_2" -> 12; // Colombia; Teenager
      case "CO_TV_3" -> 12; // Colombia; Family + PG
      case "CO_TV_4" -> 18; // Colombia; Adult
      case "HK_FCO_IIA" -> 13; // Equivalent to PG-13
      case "HK_FCO_IIB" -> 17; // Equivalent to R
      case "HK_TV_IIA" -> 13;// Equivalent to PG-13
      case "HK_TV_IIB" -> 17;// Equivalent to R
      default -> -1;
    };
  }

  /**
   * use that for MANUAL verification of the cert age
   */
  public void printAgeStatisticsForVerification() {
    // https://rating-system.fandom.com
    int cnt = 0;
    LOGGER.info("Certifications for ALL AGES");
    LOGGER.info("===========================");
    for (Certification c : getAllCerts()) {
      if (c.minimumAge == 0) {
        cnt++;
        LOGGER.info("Found 'ALL' age of {}; cert: {}:{} - {}", c.minimumAge, c.id, c.ratingCodeLabels, c.description);
      }
    }
    LOGGER.debug("Sum: {}", cnt);
    cnt = 0;

    LOGGER.info("Certifications for kids 1-5");
    LOGGER.info("===========================");
    for (Certification c : getAllCerts()) {
      if (c.minimumAge > 0 && c.minimumAge <= 5) {
        cnt++;
        LOGGER.info("Found 'young' age of {}; cert: {}:{} - {}", c.minimumAge, c.id, c.ratingCodeLabels, c.description);
      }
    }
    LOGGER.debug("Sum: {}", cnt);
    cnt = 0;

    LOGGER.info("Certifications for kids 6-11");
    LOGGER.info("============================");
    for (Certification c : getAllCerts()) {
      if (c.minimumAge > 6 && c.minimumAge <= 11) {
        cnt++;
        LOGGER.info("Found 'young' age of {}; cert: {}:{} - {}", c.minimumAge, c.id, c.ratingCodeLabels, c.description);
      }
    }
    LOGGER.debug("Sum: {}", cnt);
    cnt = 0;

    LOGGER.info("Certifications for kids 12-21");
    LOGGER.info("=============================");
    for (Certification c : getAllCerts()) {
      if (c.minimumAge > 12 && c.minimumAge <= 21) {
        cnt++;
        LOGGER.info("Found 'teen' age of {}; cert: {}:{} - {}", c.minimumAge, c.id, c.ratingCodeLabels, c.description);
      }
    }
    LOGGER.debug("Sum: {}", cnt);
    cnt = 0;

    LOGGER.info("INVALID Certifications -1 or >21");
    LOGGER.info("=============================");
    for (Certification c : getAllCerts()) {
      if (c.minimumAge == -1 || c.minimumAge > 21) {
        // -2 are unrated ones... (which could be ALL possible formats IF rated!)
        cnt++;
        LOGGER.info("Whoops, that age calculation for {} cert: {}:{} - {}", c.minimumAge, c.id, c.ratingCodeLabels, c.description);
      }
    }
    LOGGER.debug("Sum: {}", cnt);
    cnt = 0;
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
          if (cert.alternateNames.contains(rating)) {
            return cert; // alternate naming
          }
        }
        // second round - check via age
        for (Certification cert : certs) {
          int possibleAge = MetadataUtil.parseInt(cert.clean(rating), -1);
          if (possibleAge == -1) {
            possibleAge = MetadataUtil.parseInt(rating.replaceAll("\\D", " "), -1); // use a space, to not count "1_2" as 12
          }
          if (possibleAge > -1) {
            if (cert.minimumAge == possibleAge) {
              // LOGGER.debug(
              // "matched " + type + " by age - check names: " + cert.id + " (" + cert.minimumAge + ") - " + rating + " (" + possibleAge + ")");
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
    public String                     countryCode = "";   // uppercase
    public Map<String, Certification> merged      = null; // same base ID in TV & movies? add as alternate...
    public List<Certification>        movie       = null;;
    public List<Certification>        show        = null;
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

    /**
     * builds a combined list for movie/show certs, and adding them to alternate names
     */
    public void mergeCerts() {
      merged = new TreeMap<>(); // sorted!
      for (Certification cert : ListUtils.nullSafe(movie)) {
        if (cert.alternateNames.isEmpty()) {
          cert.alternateNames.add(cert.ratingCodeLabels); // add ourself to alt
          cert.alternateNames.add(cert.ratingCodeLabels.replaceAll("[^A-Za-z0-9]", "")); // w/o special chars
          if (cert.ratingCodeLabels.equals("UR")) {
            cert.alternateNames.add("Unrated");
          }
          if (cert.ratingCodeLabels.equals("NR")) {
            cert.alternateNames.add("Not Rated");
          }
        }
        if (!merged.keySet().contains(cert.id2)) {
          merged.put(cert.id2, cert);
        }
        else {
          Certification dupe = merged.get(cert.id2);
          if (!dupe.alternateNames.contains(cert.ratingCodeLabels)) {
            dupe.alternateNames.add(cert.ratingCodeLabels);
            merged.put(cert.id2, dupe);
          }
        }
      }

      for (Certification cert : ListUtils.nullSafe(show)) {
        if (cert.alternateNames.isEmpty()) {
          cert.alternateNames.add(cert.ratingCodeLabels); // add ourself to alt
          cert.alternateNames.add(cert.ratingCodeLabels.replaceAll("[^A-Za-z0-9]", "")); // w/o special chars
          if (cert.ratingCodeLabels.equals("UR")) {
            cert.alternateNames.add("Unrated");
          }
        }
        if (!merged.keySet().contains(cert.id2)) {
          merged.put(cert.id2, cert);
        }
        else {
          Certification dupe = merged.get(cert.id2);
          if (!dupe.alternateNames.contains(cert.ratingCodeLabels)) {
            dupe.alternateNames.add(cert.ratingCodeLabels);
            merged.put(cert.id2, dupe);
          }
        }
      }
    }
  }

  // used internally and ALSO used for json parsing, hence the extends/public
  public static class Certification extends BaseJsonEntity {
    public String      id               = "";             // country_type_value
    public String      id2              = "";             // country_value
    public String      description      = "";
    public String      displayValue     = "";             // regional display value
    public String      ratingCodeLabels = "";             // can also be delimited like "ALL|0+" (in json)
    public Set<String> alternateNames   = new HashSet<>();
    public int         ratingValue      = 0;              // the higher, the more restricted 0-900
    public int         minimumAge       = -1;             // our calculation of the minimum age required to view this
    public int         minimumAgePG     = -1;             // with parents, the minimum age can be lower
    public String      oldTmmEnum       = "";             // if we can match, this is our old enum name
    public boolean     deprecated       = false;
    public boolean     hasImage         = false;

    private void calcMinimumAge() {
      // NR not-rated is considered as unrestricted
      // UR un-rated is highly restricted
      if (ratingValue >= 900 || ratingCodeLabels.equals("UR") || description.equals("Unrated")) {
        minimumAge = -2;
        return;
      }
      else if (ratingCodeLabels.equals("NR") || displayValue.equals("Not Rated")) {
        minimumAge = 0;
        return;
      }

      String LDesc = description.toLowerCase(Locale.ROOT);
      if (ratingCodeLabels.contains("PG") || LDesc.contains("parental guidance")) {
        minimumAgePG = 0; // so not -1
      }
      if (LDesc.contains("without age restriction") || LDesc.contains("no restriction") || LDesc.contains("without restriction")
          || LDesc.contains("for all") || LDesc.contains("general audiences") || LDesc.contains("all audiences") || LDesc.contains("all ages")
          || LDesc.contains("any ages") || LDesc.contains("general viewing")) {
        minimumAge = 0;
        minimumAgePG = 0;
      }
      else {
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
            String young = StrgUtils.substr(description, ".*?(\\d).*"); // 0-9
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
