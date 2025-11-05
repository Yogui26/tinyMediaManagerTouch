/*
 * Copyright 2012 - 2025 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinymediamanager.scraper.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.CertificationStyle;
import org.tinymediamanager.scraper.entities.MediaCertification2.Certification;

public class MediaCertificationTest extends BasicTest {

  // @Test
  // public void testCertificationNames() {
  // for (Certification cert : Certification.values()) {
  // System.out.println(cert.name() + "\t" + cert.toString());
  // }
  // }

  @Test
  public void testCertificationTemplate() {
    // assertEqual(expected, actual);
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.SHORT)).isEqualTo("FSK 16");
    assertThat(CertificationStyle.formatCertification(MediaCertification.US_PG13, CertificationStyle.MEDIUM)).isEqualTo("US: PG-13");
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.LARGE))
        .isEqualTo("DE:FSK 16 / DE:FSK-16 / DE:FSK16 / DE:16 / DE:ab 16");
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.LARGE_FULL))
        .isEqualTo("Germany:FSK 16 / Germany:FSK-16 / Germany:FSK16 / Germany:16 / Germany:ab 16");
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.TECHNICAL)).isEqualTo("DE_FSK16");
  }

  @Test
  public void testParseCertification() {
    assertThat(MediaCertification.findCertification("FSK12")).isEqualTo(MediaCertification.DE_FSK12);
    assertThat(MediaCertification.findCertification("PG")).isEqualTo(MediaCertification.US_PG);
    assertThat(MediaCertification.findCertification("NR")).isEqualTo(MediaCertification.NOT_RATED);
    assertThat(MediaCertification.findCertification("not rated")).isEqualTo(MediaCertification.NOT_RATED);
    assertThat(MediaCertification.findCertification("V.M.14")).isEqualTo(MediaCertification.IT_VM14);
    assertThat(MediaCertification.findCertification("ab 18")).isEqualTo(MediaCertification.DE_FSK18);

    assertThat(MediaCertification.findCertification("")).isEqualTo(MediaCertification.UNKNOWN);
    assertThat(MediaCertification.findCertification("asdf")).isEqualTo(MediaCertification.UNKNOWN);
    assertThat(MediaCertification.findCertification("MA 15+")).isEqualTo(MediaCertification.AU_MA15);
  }

  @Test
  public void findCertsWhereWeHaveNoMinimumAge() {
    int cnt = 0;
    int unmatched = 0;

    for (Certification cert : MediaCertification2.getInstance().getAllCerts()) {
      cnt++;
      if (cert.minimumAge == -1) {
        unmatched++;
        System.out.println(cert.id + ": " + cert.ratingCodeLabels);
      }
    }

    System.out.println("all: " + cnt);
    System.out.println("matched: " + (cnt - unmatched));
    System.out.println("not matched: " + unmatched);
    // asserts are here, to detect changes
    // if this fails, check downloaded file closely, eg description changes...
    assertEquals(1481, cnt);
    assertEquals(98, unmatched);

    // TODO: what todo with UNRATED ones? minAge 21?

    System.out.println();
    System.out.println("TODO: whats with these?");
    for (Certification cert : MediaCertification2.getInstance().getAllCerts()) {
      if (cert.minimumAge < -1 && cert.ratingValue != 900) {
        System.out.println(cert.id + ": " + cert.ratingCodeLabels);
      }
    }

  }

  @Test
  public void canWeMapAllNewCertToOurExistingOldCerts() {
    MediaCertification2 c = MediaCertification2.getInstance();
    int cnt = 0;
    int unmatched = 0;

    for (MediaCertification old : MediaCertification.values()) {
      cnt++;
      Certification cert = c.getMovieRatingFor(old);
      if (cert == null) {
        cert = c.getTvShowRatingFor(old);
      }
      if (cert == null) {
        unmatched++;
        System.out.println("could not map " + old.name());
      }
    }

    System.out.println("matched: " + (cnt - unmatched));
    System.out.println("not matched: " + unmatched);
    assertEquals(803, cnt);
    assertEquals(287, unmatched);
  }
}
