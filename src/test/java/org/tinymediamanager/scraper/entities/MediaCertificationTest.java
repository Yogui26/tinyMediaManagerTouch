/*
 * Copyright 2012 - 2026 Manuel Laggner
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

import org.junit.Test;
import org.tinymediamanager.core.BasicTest;
import org.tinymediamanager.core.CertificationStyle;

public class MediaCertificationTest extends BasicTest {

  /**
   * just to verify, if all certs can be rendered in all styles....
   */
  @Test
  public void renderAll() {
    for (CertificationStyle style : CertificationStyle.values()) {
      for (MediaCertification cert : MediaCertification.values()) {
        String s = CertificationStyle.formatCertification(cert, style);
        System.out.print(s);
      }
      System.out.println();
    }
  }

  @Test
  public void testCertificationTemplate() {
    // assertEqual(expected, actual);
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.SHORT)).isEqualTo("FSK 16");
    assertThat(CertificationStyle.formatCertification(MediaCertification.US_PG13, CertificationStyle.MEDIUM)).isEqualTo("US: PG-13");
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.LARGE))
        .isEqualTo("DE:FSK 16 / DE:FSK-16 / DE:FSK16 / DE:16 / DE:16+ / DE:ab 16");
    assertThat(CertificationStyle.formatCertification(MediaCertification.DE_FSK16, CertificationStyle.LARGE_FULL))
        .isEqualTo("Germany:FSK 16 / Germany:FSK-16 / Germany:FSK16 / Germany:16 / Germany:16+ / Germany:ab 16");
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

}
