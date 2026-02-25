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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * The enum Certification. This enum holds all (to tinyMediaManager) known certifications including some parsing information. You can parse a string
 * with {@link #findCertification(String) Certification.findCertification} or {@link #getCertification(String, String) Certification.getCertification}
 * to the corresponding enum.
 * 
 * @author Manuel Laggner / Myron Boyle
 * @since 1.0
 */
public enum MediaCertification {
  // https://en.wikipedia.org/wiki/Television_content_rating_system
  // https://en.wikipedia.org/wiki/Motion_picture_content_rating_system
  // https://help.imdb.com/article/contribution/titles/certificates/GU757M8ZJ9ZPXB39#
  //
  // list generated with org.tinymediamanager.scraper.util.ITCertifications

  // Global NR/UR catch-all - ignore them, as they do not have a proper meaning...
  NOT_RATED(null, "Not rated", new String[] { "Not Rated", "NR" }),
  UNRATED(null, "Unrated", new String[] { "Unrated", "UR" }),

  // well known / most used certifications will be on top
  US_APPROVED(CountryCode.US, "Approved", new String[] { "Approved" }),
  US_G(CountryCode.US, "G", new String[] { "G", "Rated G" }),
  US_NC17(CountryCode.US, "NC-17", new String[] { "NC-17", "Rated NC-17" }),
  US_PASSED(CountryCode.US, "Passed", new String[] { "Passed" }),
  US_PG(CountryCode.US, "PG", new String[] { "PG", "Rated PG" }),
  US_PG13(CountryCode.US, "PG-13", new String[] { "PG-13", "Rated PG-13" }),
  US_R(CountryCode.US, "R", new String[] { "R", "Rated R" }),

  US_TV14(CountryCode.US, "TV-14", new String[] { "TV-14" }),
  US_TVG(CountryCode.US, "TV-G", new String[] { "TV-G" }),
  US_TVMA(CountryCode.US, "TV-MA", new String[] { "TV-MA" }),
  US_TVPG(CountryCode.US, "TV-PG", new String[] { "TV-PG" }),
  US_TVY(CountryCode.US, "TV-Y", new String[] { "TV-Y" }),
  US_TVY7(CountryCode.US, "TV-Y7", new String[] { "TV-Y7" }),

  GB_0(CountryCode.GB, "0+", new String[] { "0+" }),
  GB_12(CountryCode.GB, "12", new String[] { "12", "12+" }),
  GB_12A(CountryCode.GB, "12A", new String[] { "12A" }),
  GB_13(CountryCode.GB, "13+", new String[] { "13+" }),
  GB_14(CountryCode.GB, "14+", new String[] { "14+" }),
  GB_15(CountryCode.GB, "15", new String[] { "15" }),
  GB_16(CountryCode.GB, "16", new String[] { "16" }),
  GB_18(CountryCode.GB, "18", new String[] { "18" }),
  GB_6(CountryCode.GB, "6+", new String[] { "6+" }),
  GB_7(CountryCode.GB, "7+", new String[] { "7+" }),
  GB_9(CountryCode.GB, "9+", new String[] { "9+" }),
  GB_Adult(CountryCode.GB, "Adult", new String[] { "Adult" }),
  GB_All(CountryCode.GB, "All", new String[] { "All" }),
  GB_Caution(CountryCode.GB, "Caution", new String[] { "Caution" }),
  GB_E(CountryCode.GB, "E", new String[] { "E" }),
  GB_Exempt(CountryCode.GB, "Exempt", new String[] { "Exempt" }),
  GB_G(CountryCode.GB, "G", new String[] { "G" }),
  GB_Mature(CountryCode.GB, "Mature", new String[] { "Mature" }),
  GB_PG(CountryCode.GB, "PG", new String[] { "PG" }),
  GB_R18(CountryCode.GB, "R18", new String[] { "R18" }),
  GB_Teen(CountryCode.GB, "Teen", new String[] { "Teen" }),
  GB_U(CountryCode.GB, "U", new String[] { "U" }),
  GB_UC(CountryCode.GB, "UC", new String[] { "UC", "Uc" }),
  GB_Unsuitableforclassification(CountryCode.GB, "Unsuitable for classification", new String[] { "Unsuitable for classification" }),

  DE_Educational(CountryCode.DE, "Educational", new String[] { "Educational" }),
  DE_FSK0(CountryCode.DE, "FSK 0", new String[] { "FSK 0", "FSK-0", "FSK0", "0", "0+" }),
  DE_FSK12(CountryCode.DE, "FSK 12", new String[] { "FSK 12", "FSK-12", "FSK12", "12", "12+", "ab 12" }),
  DE_FSK16(CountryCode.DE, "FSK 16", new String[] { "FSK 16", "FSK-16", "FSK16", "16", "16+", "ab 16" }),
  DE_FSK18(CountryCode.DE, "FSK 18", new String[] { "FSK 18", "FSK-18", "FSK18", "18", "18+", "ab 18" }),
  DE_FSK6(CountryCode.DE, "FSK 6", new String[] { "FSK 6", "FSK-6", "FSK6", "6", "6+", "ab 6" }),
  DE_Infoprogrammgem14JuSchG(CountryCode.DE, "Infoprogramm gemäß § 14 JuSchG", new String[] { "Infoprogramm gemäß § 14 JuSchG" }),
  // DE_Notrated(CountryCode.DE, "Not rated", new String[] { "Not rated" }),
  // DE_Unrated(CountryCode.DE, "Unrated", new String[] { "Unrated" }),
  DE_X(CountryCode.DE, "X", new String[] { "X" }),

  // others down here
  AE_0(CountryCode.AE, "0+", new String[] { "0", "0+" }),
  AE_12(CountryCode.AE, "12+", new String[] { "12", "12+" }),
  AE_13(CountryCode.AE, "13+", new String[] { "13", "13+" }),
  AE_15(CountryCode.AE, "15+", new String[] { "15", "15+" }),
  AE_18(CountryCode.AE, "18+", new String[] { "18", "18+" }),
  AE_18TC(CountryCode.AE, "18TC", new String[] { "18TC" }),
  AE_21(CountryCode.AE, "21+", new String[] { "21", "21+" }),
  AE_FAM(CountryCode.AE, "FAM", new String[] { "FAM" }),
  AE_G(CountryCode.AE, "G", new String[] { "G" }),
  // AE_NotRated(CountryCode.AE, "Not Rated", new String[] { "Not Rated" }),
  AE_PG(CountryCode.AE, "PG", new String[] { "PG" }),
  AE_PG12(CountryCode.AE, "PG-12", new String[] { "PG-12", "PG12" }),
  AE_PG13(CountryCode.AE, "PG-13", new String[] { "PG-13", "PG13" }),
  AE_PG15(CountryCode.AE, "PG-15", new String[] { "PG-15", "PG15" }),
  AE_R(CountryCode.AE, "R", new String[] { "R" }),
  AE_TBC(CountryCode.AE, "TBC", new String[] { "TBC" }),
  AE_U(CountryCode.AE, "U", new String[] { "U" }),

  AM_A(CountryCode.AM, "A", new String[] { "A", "18", "18+" }),
  AM_AO(CountryCode.AM, "AO", new String[] { "AO" }),
  AM_E(CountryCode.AM, "E", new String[] { "E", "5", "5+" }),
  AM_E9(CountryCode.AM, "E9", new String[] { "E9", "9", "9+" }),
  AM_EC(CountryCode.AM, "EC", new String[] { "EC" }),
  AM_GA(CountryCode.AM, "GA", new String[] { "GA", "0", "0+" }),
  AM_M(CountryCode.AM, "M", new String[] { "M", "15", "15+" }), // M=16+
  AM_T(CountryCode.AM, "T", new String[] { "T", "12", "12+" }),
  AM_Y(CountryCode.AM, "Y", new String[] { "Y" }),
  AM_Y7(CountryCode.AM, "Y7", new String[] { "Y7", "7", "7+" }),

  AR_0(CountryCode.AR, "0", new String[] { "0" }),
  AR_10(CountryCode.AR, "10", new String[] { "10", "+10" }),
  AR_12(CountryCode.AR, "12", new String[] { "12", "+12" }),
  AR_13(CountryCode.AR, "13", new String[] { "13", "+13" }),
  AR_14(CountryCode.AR, "14", new String[] { "14", "+14" }),
  AR_16(CountryCode.AR, "16", new String[] { "16", "+16" }),
  AR_18(CountryCode.AR, "18", new String[] { "18", "+18" }),
  AR_7(CountryCode.AR, "7", new String[] { "7" }),
  AR_ATP(CountryCode.AR, "ATP", new String[] { "ATP" }),
  AR_C(CountryCode.AR, "C", new String[] { "C" }),
  AR_SAM13(CountryCode.AR, "SAM 13", new String[] { "SAM 13", "SAM13" }),
  AR_SAM16(CountryCode.AR, "SAM 16", new String[] { "SAM 16", "SAM16" }),
  AR_SAM18(CountryCode.AR, "SAM 18", new String[] { "SAM 18", "SAM18" }),

  AT_10(CountryCode.AT, "10", new String[] { "10", "10+" }),
  AT_12(CountryCode.AT, "12", new String[] { "12", "12+" }),
  AT_14(CountryCode.AT, "14", new String[] { "14", "14+" }),
  AT_16(CountryCode.AT, "16", new String[] { "16", "16+" }),
  AT_18(CountryCode.AT, "18", new String[] { "18", "18+" }),
  AT_6(CountryCode.AT, "6", new String[] { "6", "6+" }),
  AT_8(CountryCode.AT, "8", new String[] { "8", "8+" }),
  AT_AA(CountryCode.AT, "AA", new String[] { "AA", "0", "0+" }),
  AT_Unrestricted(CountryCode.AT, "Unrestricted", new String[] { "Unrestricted" }),

  AU_C(CountryCode.AU, "C", new String[] { "C" }),
  AU_CTC(CountryCode.AU, "CTC", new String[] { "CTC" }),
  AU_E(CountryCode.AU, "E", new String[] { "E", "Exempt" }),
  AU_G(CountryCode.AU, "G", new String[] { "G" }),
  AU_M(CountryCode.AU, "M", new String[] { "M" }),
  AU_MA15(CountryCode.AU, "MA15+", new String[] { "MA15+", "MA 15+" }),
  AU_P(CountryCode.AU, "P", new String[] { "P" }),
  AU_PG(CountryCode.AU, "PG", new String[] { "PG" }),
  AU_R18(CountryCode.AU, "R18+", new String[] { "R18+", "R 18+" }),
  AU_RC(CountryCode.AU, "RC", new String[] { "RC", "Refused Classification", "Banned" }),
  AU_X18(CountryCode.AU, "X18+", new String[] { "X18+", "X 18+" }),

  BB_A(CountryCode.BB, "A", new String[] { "A" }),
  BB_GA(CountryCode.BB, "GA", new String[] { "GA" }),
  BB_PG(CountryCode.BB, "PG", new String[] { "PG" }),
  BB_PG13(CountryCode.BB, "PG-13", new String[] { "PG-13" }),
  BB_R(CountryCode.BB, "R", new String[] { "R" }),

  BD_A(CountryCode.BD, "A", new String[] { "A" }),
  BD_Banned(CountryCode.BD, "Banned", new String[] { "Banned" }),
  BD_NonCommercial(CountryCode.BD, "Non-Commercial", new String[] { "Non-Commercial" }),
  // BD_NotRated(CountryCode.BD, "Not Rated", new String[] { "Not Rated" }),
  BD_U(CountryCode.BD, "U", new String[] { "U" }),

  BE_10(CountryCode.BE, "10", new String[] { "10" }),
  BE_12(CountryCode.BE, "12", new String[] { "12" }),
  BE_14(CountryCode.BE, "14", new String[] { "14" }),
  BE_16(CountryCode.BE, "16", new String[] { "16" }),
  BE_18(CountryCode.BE, "18", new String[] { "18" }),
  BE_6(CountryCode.BE, "6", new String[] { "6" }),
  BE_9(CountryCode.BE, "9", new String[] { "9" }),
  BE_ALTOUS(CountryCode.BE, "AL/TOUS", new String[] { "AL/TOUS", "AL", "AL-G", "AL/G" }),
  BE_KNT(CountryCode.BE, "KNT", new String[] { "KNT" }),
  BE_KT(CountryCode.BE, "KT", new String[] { "KT" }),

  BG_(CountryCode.BG, "?", new String[] { "?" }),
  BG_12(CountryCode.BG, "12", new String[] { "12" }),
  BG_14(CountryCode.BG, "14", new String[] { "14" }),
  BG_16(CountryCode.BG, "16", new String[] { "16" }),
  BG_18(CountryCode.BG, "18", new String[] { "18" }),
  BG_A(CountryCode.BG, "A", new String[] { "A" }), // children
  BG_B(CountryCode.BG, "B", new String[] { "B" }), // for all / no restriction
  BG_C(CountryCode.BG, "C", new String[] { "C", "C+" }), // 12+
  BG_D(CountryCode.BG, "D", new String[] { "D", "D+" }), // 16+
  // BG_Unrated(CountryCode.BG, "Unrated", new String[] { "Unrated" }), // all ages according TMDB
  BG_X(CountryCode.BG, "X", new String[] { "X" }), // 18+

  BR_10(CountryCode.BR, "10", new String[] { "10", "BR-10" }),
  BR_12(CountryCode.BR, "12", new String[] { "12", "BR-12" }),
  BR_14(CountryCode.BR, "14", new String[] { "14", "BR-14" }),
  BR_16(CountryCode.BR, "16", new String[] { "16", "BR-16" }),
  BR_18(CountryCode.BR, "18", new String[] { "18", "BR-18" }),
  BR_A10(CountryCode.BR, "A10", new String[] { "A10" }),
  BR_A12(CountryCode.BR, "A12", new String[] { "A12" }),
  BR_A14(CountryCode.BR, "A14", new String[] { "A14" }),
  BR_A16(CountryCode.BR, "A16", new String[] { "A16" }),
  BR_AL(CountryCode.BR, "AL", new String[] { "AL" }),
  BR_L(CountryCode.BR, "L", new String[] { "L", "BR-L" }),

  BS_A(CountryCode.BS, "A", new String[] { "A" }),
  BS_B(CountryCode.BS, "B", new String[] { "B" }),
  BS_C(CountryCode.BS, "C", new String[] { "C" }),
  BS_D(CountryCode.BS, "D", new String[] { "D" }),
  BS_T(CountryCode.BS, "T", new String[] { "T" }),

  CAQC_13(CountryCode.CA, "13+", new String[] { "13", "13+" }),
  CAQC_16(CountryCode.CA, "16+", new String[] { "16", "16+" }),
  CAQC_18(CountryCode.CA, "18+", new String[] { "18", "18+" }),
  CAQC_8(CountryCode.CA, "8+", new String[] { "8", "8+" }),
  CAQC_G(CountryCode.CA, "G", new String[] { "G" }),

  CA_13(CountryCode.CA, "13", new String[] { "13", "13+" }),
  CA_14(CountryCode.CA, "14+", new String[] { "14", "14+" }),
  CA_14A(CountryCode.CA, "14A", new String[] { "14A" }),
  CA_16(CountryCode.CA, "16", new String[] { "16", "16+" }),
  CA_18(CountryCode.CA, "18", new String[] { "18", "18+" }),
  CA_18A(CountryCode.CA, "18A", new String[] { "18A" }),
  CA_7(CountryCode.CA, "7+", new String[] { "7+" }),
  CA_8(CountryCode.CA, "8", new String[] { "8" }),
  CA_A(CountryCode.CA, "A", new String[] { "A" }), // sex
  CA_C(CountryCode.CA, "C", new String[] { "C" }), // 2-7
  CA_C8(CountryCode.CA, "C8", new String[] { "C8" }), // 8+
  CA_E(CountryCode.CA, "E", new String[] { "E", "Exempt" }),
  CA_G(CountryCode.CA, "G", new String[] { "G" }),
  CA_NC17(CountryCode.CA, "NC-17", new String[] { "NC-17" }),
  CA_PG(CountryCode.CA, "PG", new String[] { "PG" }),
  CA_PG13(CountryCode.CA, "PG-13", new String[] { "PG-13" }),
  CA_Prohibited(CountryCode.CA, "Prohibited", new String[] { "Prohibited" }),
  CA_R(CountryCode.CA, "R", new String[] { "R" }), // 18+
  CA_RC(CountryCode.CA, "RC", new String[] { "RC", "Refused classification" }),
  CA_TV14(CountryCode.CA, "TV-14", new String[] { "TV-14" }),
  CA_TVG(CountryCode.CA, "TV-G", new String[] { "TV-G" }),
  CA_TVMA(CountryCode.CA, "TV-MA", new String[] { "TV-MA" }),
  CA_TVPG(CountryCode.CA, "TV-PG", new String[] { "TV-PG" }),
  CA_TVY(CountryCode.CA, "TV-Y", new String[] { "TV-Y" }),
  CA_TVY7(CountryCode.CA, "TV-Y7", new String[] { "TV-Y7" }),
  CA_TVY7FV(CountryCode.CA, "TV-Y7-FV", new String[] { "TV-Y7-FV" }),

  CH_0(CountryCode.CH, "0", new String[] { "0" }),
  CH_10(CountryCode.CH, "10", new String[] { "10" }),
  CH_12(CountryCode.CH, "12", new String[] { "12" }),
  CH_14(CountryCode.CH, "14", new String[] { "14" }),
  CH_16(CountryCode.CH, "16", new String[] { "16" }),
  CH_18(CountryCode.CH, "18", new String[] { "18" }),
  CH_6(CountryCode.CH, "6", new String[] { "6" }),
  CH_7(CountryCode.CH, "7", new String[] { "7" }),
  CH_8(CountryCode.CH, "8", new String[] { "8" }),
  CH_Btl(CountryCode.CH, "Btl", new String[] { "Btl" }),
  // CH_Unrated(CountryCode.CH, "Unrated", new String[] { "Unrated" }),

  CK_G(CountryCode.CK, "G", new String[] { "G" }),
  CK_MA(CountryCode.CK, "MA", new String[] { "MA" }),
  CK_PG(CountryCode.CK, "PG", new String[] { "PG" }),
  CK_R18(CountryCode.CK, "R18", new String[] { "R18" }),

  CL_14(CountryCode.CL, "14", new String[] { "14" }),
  CL_18(CountryCode.CL, "18", new String[] { "18" }),
  CL_7(CountryCode.CL, "7", new String[] { "7" }),
  CL_A(CountryCode.CL, "A", new String[] { "A" }), // 18+ Adult
  CL_Educational(CountryCode.CL, "Educational", new String[] { "Educational" }),
  CL_Excessiveviolence(CountryCode.CL, "Excessive violence", new String[] { "Excessive violence" }),
  CL_F(CountryCode.CL, "F", new String[] { "F" }), // all
  CL_I(CountryCode.CL, "I", new String[] { "I" }), // all
  CL_I10(CountryCode.CL, "I10", new String[] { "I10" }),
  CL_I12(CountryCode.CL, "I12", new String[] { "I12" }),
  CL_I7(CountryCode.CL, "I7", new String[] { "I7" }),
  CL_Pornography(CountryCode.CL, "Pornography", new String[] { "Pornography" }),
  CL_R(CountryCode.CL, "R", new String[] { "R" }), // 12+ but accompanied
  CL_TE(CountryCode.CL, "TE", new String[] { "TE" }), // GA
  CL_TE7(CountryCode.CL, "TE+7", new String[] { "TE" }), // 7+

  CN_Banned(CountryCode.CN, "Banned", new String[] { "Banned" }),
  CN_Suitableforallages(CountryCode.CN, "Suitable for all ages", new String[] { "Suitable for all ages" }),

  CO_12(CountryCode.CO, "12", new String[] { "12" }),
  CO_15(CountryCode.CO, "15", new String[] { "15" }),
  CO_18(CountryCode.CO, "18", new String[] { "18" }),
  CO_7(CountryCode.CO, "7", new String[] { "7" }),
  CO_Adolescentes(CountryCode.CO, "Adolescentes", new String[] { "Adolescentes", "2" }), // Teens 12-18
  CO_Adultos(CountryCode.CO, "Adultos", new String[] { "Adultos", "4" }),
  CO_Children(CountryCode.CO, "Infantil", new String[] { "Infantil", "1", "TODOS" }), // all
  CO_Familiar(CountryCode.CO, "Familiar", new String[] { "Familiar", "3" }),
  CO_PTA(CountryCode.CO, "PTA", new String[] { "PTA" }),
  CO_Prohibited(CountryCode.CO, "Prohibited", new String[] { "Prohibited" }),
  CO_T(CountryCode.CO, "T", new String[] { "T" }),
  CO_X(CountryCode.CO, "X", new String[] { "X", "18X" }),

  CR_M12(CountryCode.CR, "M12", new String[] { "M12" }),
  CR_M15(CountryCode.CR, "M15", new String[] { "M15" }),
  CR_M18(CountryCode.CR, "M18", new String[] { "M18" }),
  CR_Recommended(CountryCode.CR, "Recommended", new String[] { "Recommended" }),
  CR_TP(CountryCode.CR, "TP", new String[] { "TP" }),
  CR_TP12(CountryCode.CR, "TP12", new String[] { "TP12" }),
  CR_TP7(CountryCode.CR, "TP7", new String[] { "TP7" }),

  CY_12(CountryCode.CY, "12", new String[] { "12" }),
  CY_15(CountryCode.CY, "15", new String[] { "15" }),
  CY_18(CountryCode.CY, "18", new String[] { "18" }),
  CY_K(CountryCode.CY, "K", new String[] { "K" }),

  CZ_(CountryCode.CZ, "*", new String[] { "*" }),
  CZ_0(CountryCode.CZ, "0+", new String[] { "0", "0+" }),
  CZ_12(CountryCode.CZ, "12+", new String[] { "12", "12+" }),
  CZ_15(CountryCode.CZ, "15+", new String[] { "15", "15+" }),
  CZ_18(CountryCode.CZ, "18+", new String[] { "18", "18+" }),
  CZ_5(CountryCode.CZ, "5+", new String[] { "5", "5+" }),
  CZ_7(CountryCode.CZ, "7+", new String[] { "7", "7+" }),
  CZ_9(CountryCode.CZ, "9+", new String[] { "9", "9+" }),
  CZ_U(CountryCode.CZ, "U", new String[] { "U" }), // all
  CZ_E(CountryCode.CZ, "E", new String[] { "E" }),
  CZ_PG(CountryCode.CZ, "PG", new String[] { "PG" }),
  // CZ_Unrated(CountryCode.CZ, "Unrated", new String[] { "Unrated" }),

  DK_11(CountryCode.DK, "11", new String[] { "11" }),
  DK_1115(CountryCode.DK, "11 / 15", new String[] { "11 / 15" }),
  DK_12(CountryCode.DK, "12", new String[] { "12" }),
  DK_15(CountryCode.DK, "15", new String[] { "15" }),
  DK_16(CountryCode.DK, "16", new String[] { "16" }),
  DK_7(CountryCode.DK, "7", new String[] { "7" }),
  DK_A(CountryCode.DK, "A", new String[] { "A" }), // all
  DK_F(CountryCode.DK, "F", new String[] { "F" }), // Exempt from classification

  EC_13(CountryCode.EC, "13+", new String[] { "13+" }),
  EC_7(CountryCode.EC, "7+", new String[] { "7+" }),
  EC_A(CountryCode.EC, "A", new String[] { "A" }),
  EC_All(CountryCode.EC, "All", new String[] { "All" }),
  EC_B(CountryCode.EC, "B", new String[] { "B" }),
  EC_C(CountryCode.EC, "C", new String[] { "C" }),
  // EC_NotRated(CountryCode.EC, "Not Rated", new String[] { "Not Rated" }),
  EC_PG(CountryCode.EC, "PG", new String[] { "PG" }),
  EC_R(CountryCode.EC, "R", new String[] { "R" }),
  EC_TV14(CountryCode.EC, "TV-14", new String[] { "TV-14" }),

  EE_K12(CountryCode.EE, "K-12", new String[] { "K-12", "K12" }),
  EE_K14(CountryCode.EE, "K-14", new String[] { "K-14", "K14" }),
  EE_K16(CountryCode.EE, "K-16", new String[] { "K-16", "K16" }),
  EE_L(CountryCode.EE, "L", new String[] { "L", "ALL" }),
  EE_MS12(CountryCode.EE, "MS-12", new String[] { "MS-12", "MS12" }),
  EE_MS6(CountryCode.EE, "MS-6", new String[] { "MS-6", "MS6" }),
  EE_PERE(CountryCode.EE, "PERE", new String[] { "PERE" }), // family

  EG_0(CountryCode.EG, "0+", new String[] { "0", "0+" }),
  EG_12(CountryCode.EG, "12+", new String[] { "12", "12+", "+12" }),
  EG_13(CountryCode.EG, "13+", new String[] { "13", "13+", "+13" }),
  EG_15(CountryCode.EG, "15+", new String[] { "15", "15+", "+15" }),
  EG_16(CountryCode.EG, "16+", new String[] { "16", "16+", "+16" }),
  EG_18(CountryCode.EG, "18+", new String[] { "18", "18+", "+18" }),
  EG_7(CountryCode.EG, "7+", new String[] { "7", "7+", "+7" }),
  EG_8(CountryCode.EG, "8+", new String[] { "8", "8+", "+8" }),
  EG_Allages(CountryCode.EG, "All ages", new String[] { "All ages" }),
  EG_G(CountryCode.EG, "G", new String[] { "G" }),
  // EG_NotRated(CountryCode.EG, "Not Rated", new String[] { "Not Rated" }),
  EG_PG(CountryCode.EG, "PG", new String[] { "PG" }),
  EG_PG12(CountryCode.EG, "PG-12", new String[] { "PG-12" }),
  EG_PG13(CountryCode.EG, "PG-13", new String[] { "PG-13" }),
  EG_PG15(CountryCode.EG, "PG-15", new String[] { "PG-15" }),
  EG_PG8(CountryCode.EG, "PG8", new String[] { "PG8" }),
  EG_R(CountryCode.EG, "R", new String[] { "R" }),
  EG_R12(CountryCode.EG, "R-12", new String[] { "R-12" }),

  ES_0(CountryCode.ES, "0+", new String[] { "0+" }),
  ES_10(CountryCode.ES, "10", new String[] { "10" }),
  ES_12(CountryCode.ES, "12", new String[] { "12", "12/fig" }),
  ES_13(CountryCode.ES, "13", new String[] { "13" }),
  ES_14(CountryCode.ES, "14", new String[] { "14" }),
  ES_15(CountryCode.ES, "15", new String[] { "15" }),
  ES_16(CountryCode.ES, "16", new String[] { "16", "16/fig" }),
  ES_18(CountryCode.ES, "18", new String[] { "18", "18/fig" }),
  ES_3(CountryCode.ES, "3", new String[] { "3" }),
  ES_4(CountryCode.ES, "4+", new String[] { "4+" }),
  ES_6(CountryCode.ES, "6+", new String[] { "6+" }),
  ES_7(CountryCode.ES, "7", new String[] { "7", "7/fig", "7i", "7/i", "7/i/fig", "I7" }),
  ES_9(CountryCode.ES, "9+", new String[] { "9+" }),
  ES_A(CountryCode.ES, "A", new String[] { "A", "A/fig", "Ai", "A/i", "A/i/fig" }),
  ES_AL(CountryCode.ES, "AL", new String[] { "AL" }),
  ES_APTA(CountryCode.ES, "APTA", new String[] { "APTA" }),
  ES_Banned(CountryCode.ES, "Banned", new String[] { "Banned" }),
  ES_ER(CountryCode.ES, "ER", new String[] { "ER" }),
  ES_ERI(CountryCode.ES, "ERI", new String[] { "ERI" }), // Specially recommended for younger children
  ES_IA(CountryCode.ES, "IA", new String[] { "IA" }), // ??? TVDB has it
  ES_PX(CountryCode.ES, "PX", new String[] { "PX" }),
  ES_T(CountryCode.ES, "T", new String[] { "T" }), // no ideas was T is: https://www.imdb.com/search/title/?certificates=ES:T
  ES_TP(CountryCode.ES, "TP", new String[] { "TP", "A-TP" }), // general/all (tous publics?)
  ES_X(CountryCode.ES, "X", new String[] { "X" }),

  FI_12(CountryCode.FI, "12", new String[] { "12" }),
  FI_16(CountryCode.FI, "16", new String[] { "16" }),
  FI_18(CountryCode.FI, "18", new String[] { "18" }),
  FI_7(CountryCode.FI, "7", new String[] { "7" }),
  FI_K11(CountryCode.FI, "K-11", new String[] { "K-11", "K11" }),
  FI_K12(CountryCode.FI, "K-12", new String[] { "K-12", "K12" }),
  FI_K13(CountryCode.FI, "K-13", new String[] { "K-13", "K13" }),
  FI_K15(CountryCode.FI, "K-15", new String[] { "K-15", "K15" }),
  FI_K16(CountryCode.FI, "K-16", new String[] { "K-16", "K16" }),
  FI_K18(CountryCode.FI, "K-18", new String[] { "K-18", "K18" }),
  FI_K3(CountryCode.FI, "K-3", new String[] { "K-3", "K3" }),
  FI_K7(CountryCode.FI, "K-7", new String[] { "K-7", "K7" }),
  FI_KE(CountryCode.FI, "K-E", new String[] { "K-E" }),
  FI_KK(CountryCode.FI, "KK", new String[] { "KK" }), // banned
  FI_S(CountryCode.FI, "S", new String[] { "S" }), // all
  FI_ST(CountryCode.FI, "S/T", new String[] { "S/T", "ST" }), // all times

  FJ_A(CountryCode.FJ, "A", new String[] { "A" }),
  FJ_G(CountryCode.FJ, "G", new String[] { "G" }),
  FJ_R(CountryCode.FJ, "R", new String[] { "R" }),
  FJ_Y(CountryCode.FJ, "Y", new String[] { "Y" }),

  FR_0(CountryCode.FR, "0+", new String[] { "0+" }),
  FR_10U(CountryCode.FR, "–10", new String[] { "10", "-10" }),
  FR_12U(CountryCode.FR, "–12", new String[] { "12", "-12" }),
  FR_14(CountryCode.FR, "14+", new String[] { "14+" }),
  FR_16U(CountryCode.FR, "–16", new String[] { "16", "-16" }),
  FR_18U(CountryCode.FR, "–18", new String[] { "18", "-18" }),
  FR_6(CountryCode.FR, "6+", new String[] { "6+" }),
  FR_9(CountryCode.FR, "9+", new String[] { "9+" }),
  FR_Prohibited(CountryCode.FR, "Prohibited", new String[] { "Prohibited" }),
  FR_PublicAverti(CountryCode.FR, "Public Averti", new String[] { "Public Averti" }),
  FR_TP(CountryCode.FR, "TP", new String[] { "TP", "Tous Publics", "Tous Publics avec avertissement" }), // all
  FR_U(CountryCode.FR, "U", new String[] { "U" }),
  // FR_Unrated(CountryCode.FR, "Unrated", new String[] { "Unrated" }),
  FR_X(CountryCode.FR, "X", new String[] { "X" }),

  GH_12(CountryCode.GH, "12", new String[] { "12", "12+" }),
  GH_15(CountryCode.GH, "15", new String[] { "15", "15+" }),
  GH_18(CountryCode.GH, "18", new String[] { "18", "18+" }),
  GH_NS(CountryCode.GH, "NS", new String[] { "NS" }),
  GH_PG(CountryCode.GH, "PG", new String[] { "PG" }),
  GH_U(CountryCode.GH, "U", new String[] { "U" }),

  GR_12(CountryCode.GR, "12", new String[] { "12" }),
  GR_13(CountryCode.GR, "13", new String[] { "13" }),
  GR_16(CountryCode.GR, "16", new String[] { "16" }),
  GR_17(CountryCode.GR, "17", new String[] { "17" }),
  GR_18(CountryCode.GR, "18", new String[] { "18" }),
  GR_8(CountryCode.GR, "8", new String[] { "8" }),
  GR_E(CountryCode.GR, "E", new String[] { "E" }),
  GR_K(CountryCode.GR, "K", new String[] { "K" }), // all
  GR_K12(CountryCode.GR, "K-12", new String[] { "K-12", "K12" }),
  GR_K13(CountryCode.GR, "K-13", new String[] { "K-13", "K13" }),
  GR_K15(CountryCode.GR, "K-15", new String[] { "K-15", "K15" }),
  GR_K17(CountryCode.GR, "K-17", new String[] { "K-17", "K17" }),
  GR_K18(CountryCode.GR, "K-18", new String[] { "K-18", "K18" }),
  GR_Unrestricted(CountryCode.GR, "Unrestricted", new String[] { "Unrestricted" }),

  HK_13(CountryCode.HK, "13+", new String[] { "13+" }),
  HK_16(CountryCode.HK, "16+", new String[] { "16+" }),
  HK_18(CountryCode.HK, "18+", new String[] { "18+" }),
  HK_7(CountryCode.HK, "7+", new String[] { "7+" }),
  HK_All(CountryCode.HK, "All", new String[] { "All" }),
  HK_Exempt(CountryCode.HK, "Exempt", new String[] { "Exempt" }),
  HK_I(CountryCode.HK, "I", new String[] { "I" }),
  HK_II(CountryCode.HK, "II", new String[] { "II" }),
  HK_IIA(CountryCode.HK, "IIA", new String[] { "IIA" }),
  HK_IIAIIB(CountryCode.HK, "IIA/IIB", new String[] { "IIA/IIB" }),
  HK_IIB(CountryCode.HK, "IIB", new String[] { "IIB" }),
  HK_III(CountryCode.HK, "III", new String[] { "III" }),
  HK_M(CountryCode.HK, "M", new String[] { "M" }),
  // HK_NotRated(CountryCode.HK, "Not Rated", new String[] { "Not Rated" }),
  HK_PG(CountryCode.HK, "PG", new String[] { "PG" }),
  // HK_Unrated(CountryCode.HK, "Unrated", new String[] { "Unrated" }),

  HR_12(CountryCode.HR, "12", new String[] { "12" }),
  HR_15(CountryCode.HR, "15", new String[] { "15" }),
  HR_18(CountryCode.HR, "18", new String[] { "18" }),
  // HR_Unrated(CountryCode.HR, "Unrated", new String[] { "Unrated" }),

  HU_12(CountryCode.HU, "12", new String[] { "12" }),
  HU_16(CountryCode.HU, "16", new String[] { "16" }),
  HU_18(CountryCode.HU, "18", new String[] { "18" }),
  HU_6(CountryCode.HU, "6", new String[] { "6" }),
  HU_GY(CountryCode.HU, "GY", new String[] { "GY" }),
  HU_Children(CountryCode.HU, "Children", new String[] { "Children" }),
  HU_KN(CountryCode.HU, "KN", new String[] { "KN" }), // all
  // HU_Unrated(CountryCode.HU, "Unrated", new String[] { "Unrated" }), // w/o restriction -> rated
  HU_X(CountryCode.HU, "X", new String[] { "X" }), // adults

  ID_13(CountryCode.ID, "13+", new String[] { "13", "13+" }),
  ID_15(CountryCode.ID, "15", new String[] { "15", "15+" }),
  ID_17(CountryCode.ID, "17+", new String[] { "17", "17+" }),
  ID_21(CountryCode.ID, "21+", new String[] { "21", "21+" }),
  ID_7(CountryCode.ID, "7+", new String[] { "7", "7+" }),
  ID_A(CountryCode.ID, "A", new String[] { "A" }), // 7-12
  ID_ABO(CountryCode.ID, "A–BO", new String[] { "A–BO" }),
  ID_D(CountryCode.ID, "D", new String[] { "D" }), // 18+
  ID_G(CountryCode.ID, "G", new String[] { "G" }),
  // ID_NotRated(CountryCode.ID, "Not Rated", new String[] { "Not Rated" }),
  ID_P(CountryCode.ID, "P", new String[] { "P" }), // preschoolers, 2-6
  ID_PBO(CountryCode.ID, "P–BO", new String[] { "P–BO" }),
  ID_RBO(CountryCode.ID, "R–BO", new String[] { "R–BO" }),
  ID_R(CountryCode.ID, "R", new String[] { "R" }), // 13-17
  ID_SU(CountryCode.ID, "SU", new String[] { "SU" }), // all, 2+
  ID_Semua(CountryCode.ID, "Semua", new String[] { "Semua" }),

  IE_12(CountryCode.IE, "12", new String[] { "12" }),
  IE_12A(CountryCode.IE, "12A", new String[] { "12A" }),
  IE_12PG(CountryCode.IE, "12PG", new String[] { "12PG" }),
  IE_15(CountryCode.IE, "15", new String[] { "15" }),
  IE_15A(CountryCode.IE, "15A", new String[] { "15A" }),
  IE_15PG(CountryCode.IE, "15PG", new String[] { "15PG" }),
  IE_16(CountryCode.IE, "16", new String[] { "16" }),
  IE_18(CountryCode.IE, "18", new String[] { "18" }),
  IE_G(CountryCode.IE, "G", new String[] { "G" }),
  IE_PG(CountryCode.IE, "PG", new String[] { "PG" }),

  IL_12(CountryCode.IL, "12", new String[] { "12", "12+" }),
  IL_13(CountryCode.IL, "13", new String[] { "13", "13+" }),
  IL_14(CountryCode.IL, "14", new String[] { "14", "14+" }),
  IL_15(CountryCode.IL, "15", new String[] { "15", "15+" }),
  IL_16(CountryCode.IL, "16", new String[] { "16", "16+" }),
  IL_18(CountryCode.IL, "18+", new String[] { "18", "18+" }),
  IL_5(CountryCode.IL, "5+", new String[] { "5+" }),
  IL_7(CountryCode.IL, "7+", new String[] { "7+" }),
  IL_8(CountryCode.IL, "8+", new String[] { "8+" }),
  IL_9(CountryCode.IL, "9+", new String[] { "9+" }),
  IL_AP(CountryCode.IL, "AP", new String[] { "AP" }),
  IL_All(CountryCode.IL, "All", new String[] { "All", "0", "0+" }),
  IL_E(CountryCode.IL, "E", new String[] { "E" }), // Exempt from classification
  IL_G(CountryCode.IL, "G", new String[] { "G" }), // GA
  // IL_NotRated(CountryCode.IL, "Not Rated", new String[] { "Not Rated" }),
  // IL_Unrated(CountryCode.IL, "Unrated", new String[] { "Unrated" }),

  IN_12(CountryCode.IN, "12+", new String[] { "12+" }),
  IN_15(CountryCode.IN, "15+", new String[] { "15+" }),
  IN_A(CountryCode.IN, "A", new String[] { "A" }), // 18+
  IN_All(CountryCode.IN, "All", new String[] { "All" }),
  IN_PG(CountryCode.IN, "PG", new String[] { "PG" }),
  IN_S(CountryCode.IN, "S", new String[] { "S" }), // "special class of persons"; whatever that means...?
  IN_U(CountryCode.IN, "U", new String[] { "U" }), // all
  IN_UA(CountryCode.IN, "UA", new String[] { "UA" }), // all, but under 12 accompanied
  IN_UA13(CountryCode.IN, "UA 13+", new String[] { "UA 13+", "U/A 13+", "UA13+" }),
  IN_UA16(CountryCode.IN, "UA 16+", new String[] { "UA 16+", "U/A 16+", "UA16+" }),
  IN_UA7(CountryCode.IN, "UA 7+", new String[] { "UA 7+", "U/A 7+", "UA7+" }),

  IQ_15(CountryCode.IQ, "15+", new String[] { "15+" }),
  IQ_18(CountryCode.IQ, "18+", new String[] { "18+" }),
  IQ_18TC(CountryCode.IQ, "18TC", new String[] { "18TC" }),
  IQ_G(CountryCode.IQ, "G", new String[] { "G" }),
  IQ_PG13(CountryCode.IQ, "PG 13", new String[] { "PG 13" }),
  IQ_PG15(CountryCode.IQ, "PG 15", new String[] { "PG 15" }),

  IS_10(CountryCode.IS, "10", new String[] { "10", "PG" }),
  IS_12(CountryCode.IS, "12", new String[] { "12" }),
  IS_14(CountryCode.IS, "14", new String[] { "14", "PG-13" }),
  IS_16(CountryCode.IS, "16", new String[] { "16" }),
  IS_1618(CountryCode.IS, "16/18", new String[] { "16/18" }),
  IS_18(CountryCode.IS, "18", new String[] { "18", "R" }),
  IS_6(CountryCode.IS, "6", new String[] { "6" }),
  IS_7(CountryCode.IS, "7", new String[] { "7" }),
  IS_9(CountryCode.IS, "9", new String[] { "9" }),
  IS_L(CountryCode.IS, "L", new String[] { "L", "G" }),
  IS_LH(CountryCode.IS, "LH", new String[] { "LH" }),
  IS_NC17(CountryCode.IS, "NC-17", new String[] { "NC-17", "NC17" }),

  IT_10(CountryCode.IT, "10+", new String[] { "10", "10+" }),
  IT_12(CountryCode.IT, "12+", new String[] { "12", "12+" }),
  IT_14(CountryCode.IT, "14+", new String[] { "14", "14+" }),
  IT_18(CountryCode.IT, "18+", new String[] { "18", "18+" }),
  IT_6(CountryCode.IT, "6+", new String[] { "6", "6+" }),
  IT_BA(CountryCode.IT, "BA", new String[] { "BA" }), // Parental guidance suggested
  IT_T(CountryCode.IT, "T", new String[] { "T" }), // all
  IT_VM12(CountryCode.IT, "V.M.12", new String[] { "V.M.12", "VM12" }),
  IT_VM14(CountryCode.IT, "V.M.14", new String[] { "V.M.14", "VM14" }),
  IT_VM18(CountryCode.IT, "V.M.18", new String[] { "V.M.18", "VM18" }),

  JM_A18(CountryCode.JM, "A-18", new String[] { "A-18" }),
  JM_G(CountryCode.JM, "G", new String[] { "G", "U" }),
  JM_PG(CountryCode.JM, "PG", new String[] { "PG" }),
  JM_PG13(CountryCode.JM, "PG-13", new String[] { "PG-13" }),
  JM_T16(CountryCode.JM, "T-16", new String[] { "T-16" }),

  JP_13(CountryCode.JP, "13+", new String[] { "13", "13+" }),
  JP_16(CountryCode.JP, "16+", new String[] { "16", "16+" }),
  JP_5(CountryCode.JP, "5+", new String[] { "5", "5+" }),
  JP_7(CountryCode.JP, "7+", new String[] { "7", "7+" }),
  JP_9(CountryCode.JP, "9+", new String[] { "9", "9+" }),
  JP_A(CountryCode.JP, "A", new String[] { "A" }),
  JP_B(CountryCode.JP, "B", new String[] { "B" }),
  JP_C(CountryCode.JP, "C", new String[] { "C" }),
  JP_D(CountryCode.JP, "D", new String[] { "D" }),
  JP_G(CountryCode.JP, "G", new String[] { "G", "0", "0+" }), // all
  JP_PG12(CountryCode.JP, "PG-12", new String[] { "PG-12", "PG12", "12", "12+" }),
  JP_R15(CountryCode.JP, "R15+", new String[] { "R15+", "15", "15+" }),
  JP_R18(CountryCode.JP, "R18+", new String[] { "R18+", "18", "18+" }),
  // JP_Unrated(CountryCode.JP, "Unrated", new String[] { "Unrated" }),
  JP_Z(CountryCode.JP, "Z", new String[] { "Z" }),

  KE_16(CountryCode.KE, "16", new String[] { "16" }),
  KE_18(CountryCode.KE, "18", new String[] { "18" }),
  KE_GE(CountryCode.KE, "GE", new String[] { "GE" }),
  KE_PG(CountryCode.KE, "PG", new String[] { "PG" }),
  KE_RestrictedBanned(CountryCode.KE, "Restricted/Banned", new String[] { "Restricted/Banned" }),

  KH_G(CountryCode.KH, "G", new String[] { "G" }),
  KH_NC15(CountryCode.KH, "NC15", new String[] { "NC15" }),
  KH_R18(CountryCode.KH, "R18", new String[] { "R18" }),

  KR_12(CountryCode.KR, "12", new String[] { "12" }),
  KR_13(CountryCode.KR, "13+", new String[] { "13+" }),
  KR_15(CountryCode.KR, "15", new String[] { "15" }),
  KR_16(CountryCode.KR, "16+", new String[] { "16+" }),
  KR_18(CountryCode.KR, "18", new String[] { "18", "청불" }),
  KR_19(CountryCode.KR, "19", new String[] { "19" }),
  KR_7(CountryCode.KR, "7", new String[] { "7" }),
  KR_ALL(CountryCode.KR, "ALL", new String[] { "ALL", "All", "전체" }),
  KR_Exempt(CountryCode.KR, "Exempt", new String[] { "Exempt" }),
  // KR_NotRated(CountryCode.KR, "Not Rated", new String[] { "Not Rated" }),
  KR_Restricted(CountryCode.KR, "Restricted", new String[] { "Restricted", "제한", "Restricted Screening" }),

  KW_18(CountryCode.KW, "18+", new String[] { "18+" }),
  KW_E(CountryCode.KW, "E", new String[] { "E" }),
  KW_PG(CountryCode.KW, "PG", new String[] { "PG" }),
  KW_T(CountryCode.KW, "T", new String[] { "T" }),

  // KZ_121416(CountryCode.KZ, "12+/14+/16+", new String[] { "12+/14+/16+" }),
  KZ_12(CountryCode.KZ, "12+", new String[] { "12", "12+" }),
  KZ_14(CountryCode.KZ, "14+", new String[] { "14", "14+" }),
  KZ_16(CountryCode.KZ, "16+", new String[] { "16", "16+" }),
  KZ_18(CountryCode.KZ, "18+", new String[] { "18", "18+" }),
  KZ_21(CountryCode.KZ, "21+", new String[] { "21", "21+" }),
  KZ_6(CountryCode.KZ, "6+", new String[] { "6", "6+" }),
  KZ_6U(CountryCode.KZ, "6-", new String[] { "6-" }),

  LB_18(CountryCode.LB, "18+", new String[] { "18+" }),
  LB_G(CountryCode.LB, "G", new String[] { "G" }),
  LB_PG(CountryCode.LB, "PG", new String[] { "PG" }),
  LB_PG13(CountryCode.LB, "PG13", new String[] { "PG13" }),
  LB_PG16(CountryCode.LB, "PG16", new String[] { "PG16" }),

  LT_N13(CountryCode.LT, "N-13", new String[] { "N-13" }),
  LT_N14(CountryCode.LT, "N-14", new String[] { "N-14" }),
  LT_N16(CountryCode.LT, "N-16", new String[] { "N-16" }),
  LT_N18(CountryCode.LT, "N-18", new String[] { "N-18" }),
  LT_N7(CountryCode.LT, "N-7", new String[] { "N-7" }),
  LT_S(CountryCode.LT, "S", new String[] { "S" }), // 18+ tv like N-18 movies
  LT_V(CountryCode.LT, "V", new String[] { "V" }), // all

  LU_10(CountryCode.LU, "10", new String[] { "10" }),
  LU_12(CountryCode.LU, "12", new String[] { "12" }),
  LU_16(CountryCode.LU, "16", new String[] { "16" }),
  LU_18(CountryCode.LU, "18", new String[] { "18" }),
  LU_6(CountryCode.LU, "6", new String[] { "6" }),
  LU_A(CountryCode.LU, "A", new String[] { "A", "Tous", "TP" }), // all

  LV_12(CountryCode.LV, "12+", new String[] { "12+" }),
  LV_16(CountryCode.LV, "16+", new String[] { "16+" }),
  LV_18(CountryCode.LV, "18+", new String[] { "18+" }),
  LV_7(CountryCode.LV, "7+", new String[] { "7+" }),
  LV_U(CountryCode.LV, "U", new String[] { "U" }),

  MA_10(CountryCode.MA, "-10", new String[] { "-10", "10" }),
  MA_12(CountryCode.MA, "-12", new String[] { "-12", "12" }),
  MA_16(CountryCode.MA, "-16", new String[] { "-16", "16" }),
  MA_Allaudiences(CountryCode.MA, "All audiences", new String[] { "All audiences", "TP" }),

  MK_Bluetriangle(CountryCode.MK, "Blue triangle", new String[] { "Blue triangle" }),
  MK_Greencircle(CountryCode.MK, "Green circle", new String[] { "Green circle" }),
  MK_Orangesquare(CountryCode.MK, "Orange square", new String[] { "Orange square" }),
  MK_Redcross(CountryCode.MK, "Red cross", new String[] { "Red cross" }),
  MK_Yellowcircle(CountryCode.MK, "Yellow circle", new String[] { "Yellow circle" }),

  MO_A(CountryCode.MO, "A", new String[] { "A" }), // ??
  MO_B(CountryCode.MO, "B", new String[] { "B" }), // ??
  MO_C(CountryCode.MO, "C", new String[] { "C" }), // ??
  MO_D(CountryCode.MO, "D", new String[] { "D" }), // ??

  MT_12(CountryCode.MT, "12", new String[] { "12" }),
  MT_12A(CountryCode.MT, "12A", new String[] { "12A" }),
  MT_14(CountryCode.MT, "14", new String[] { "14" }),
  MT_15(CountryCode.MT, "15", new String[] { "15" }),
  MT_16(CountryCode.MT, "16", new String[] { "16" }),
  MT_18(CountryCode.MT, "18", new String[] { "18" }),
  MT_Notfitforexhibition(CountryCode.MT, "Not fit for exhibition", new String[] { "Not fit for exhibition" }),
  MT_PG(CountryCode.MT, "PG", new String[] { "PG" }),
  MT_U(CountryCode.MT, "U", new String[] { "U" }),

  MU_15(CountryCode.MU, "15", new String[] { "15" }),
  MU_18(CountryCode.MU, "18", new String[] { "18" }),
  MU_18R(CountryCode.MU, "18R", new String[] { "18R" }),
  MU_PG(CountryCode.MU, "PG", new String[] { "PG" }),
  MU_Rejected(CountryCode.MU, "Rejected", new String[] { "Rejected" }),
  MU_U(CountryCode.MU, "U", new String[] { "U" }),

  MV_12(CountryCode.MV, "12+", new String[] { "12+" }),
  MV_15(CountryCode.MV, "15+", new String[] { "15+" }),
  MV_18(CountryCode.MV, "18+", new String[] { "18+" }),
  MV_18R(CountryCode.MV, "18+R", new String[] { "18+R" }),
  MV_G(CountryCode.MV, "G", new String[] { "G" }),
  MV_PG(CountryCode.MV, "PG", new String[] { "PG" }),
  MV_PU(CountryCode.MV, "PU", new String[] { "PU" }),

  MX_0(CountryCode.MX, "0+", new String[] { "0+" }),
  MX_12(CountryCode.MX, "12+", new String[] { "12+" }),
  MX_13(CountryCode.MX, "13", new String[] { "13" }),
  MX_16(CountryCode.MX, "16", new String[] { "16" }),
  MX_18(CountryCode.MX, "18", new String[] { "18", "18+" }),
  MX_7(CountryCode.MX, "7", new String[] { "7" }),
  MX_9(CountryCode.MX, "9+", new String[] { "9+" }),
  MX_A(CountryCode.MX, "A", new String[] { "A", "MX:A" }),
  MX_AA(CountryCode.MX, "AA", new String[] { "AA", "MX:AA" }),
  MX_B(CountryCode.MX, "B", new String[] { "B", "MX:B" }),
  MX_B_15(CountryCode.MX, "B-15", new String[] { "B-15", "B15", "MX:B-15" }),
  MX_C(CountryCode.MX, "C", new String[] { "C", "MX:C" }),
  MX_D(CountryCode.MX, "D", new String[] { "D", "MX:D" }),
  MX_SC(CountryCode.MX, "S/C", new String[] { "S/C" }),
  MX_TODO(CountryCode.MX, "TODO", new String[] { "TODO" }),

  MY_13(CountryCode.MY, "13", new String[] { "13" }),
  MY_16(CountryCode.MY, "16", new String[] { "16" }),
  MY_18(CountryCode.MY, "18", new String[] { "18", "18PA", "18PL", "18SG", "18SX" }), // all in one on our side
  MY_Banned(CountryCode.MY, "Banned", new String[] { "Banned" }),
  MY_P12(CountryCode.MY, "P12", new String[] { "P12" }),
  MY_P13(CountryCode.MY, "P13", new String[] { "P13", "PG-13" }),
  MY_U(CountryCode.MY, "U", new String[] { "U" }),

  NG_12(CountryCode.NG, "12", new String[] { "12" }),
  NG_12A(CountryCode.NG, "12A", new String[] { "12A" }),
  NG_15(CountryCode.NG, "15", new String[] { "15" }),
  NG_18(CountryCode.NG, "18", new String[] { "18" }),
  NG_G(CountryCode.NG, "G", new String[] { "G" }),
  NG_PG(CountryCode.NG, "PG", new String[] { "PG" }),
  NG_RE(CountryCode.NG, "RE", new String[] { "RE" }),

  NL_12(CountryCode.NL, "12", new String[] { "12" }),
  NL_13(CountryCode.NL, "13+", new String[] { "13+" }),
  NL_14(CountryCode.NL, "14", new String[] { "14" }),
  NL_15(CountryCode.NL, "15", new String[] { "15", "15+" }),
  NL_16(CountryCode.NL, "16", new String[] { "16" }),
  NL_18(CountryCode.NL, "18", new String[] { "18" }),
  NL_6(CountryCode.NL, "6", new String[] { "6" }),
  NL_7(CountryCode.NL, "7+", new String[] { "7+" }),
  NL_9(CountryCode.NL, "9", new String[] { "9" }),
  NL_AL(CountryCode.NL, "AL", new String[] { "AL" }),

  NO_11(CountryCode.NO, "11", new String[] { "11", "11+" }),
  NO_12(CountryCode.NO, "12", new String[] { "12", "12+" }),
  NO_15(CountryCode.NO, "15", new String[] { "15", "15+" }),
  NO_18(CountryCode.NO, "18", new String[] { "18", "18+" }),
  NO_5(CountryCode.NO, "5", new String[] { "5", "5+" }),
  NO_6(CountryCode.NO, "6", new String[] { "6", "6+" }),
  NO_7(CountryCode.NO, "7", new String[] { "7", "7+" }),
  NO_9(CountryCode.NO, "9", new String[] { "9", "9+" }),
  NO_A(CountryCode.NO, "A", new String[] { "A", "0", "0+" }), // all
  NO_Notapproved(CountryCode.NO, "Not approved", new String[] { "Not approved" }),

  NZ_13(CountryCode.NZ, "13", new String[] { "13" }),
  NZ_16(CountryCode.NZ, "16", new String[] { "16" }),
  NZ_18(CountryCode.NZ, "18", new String[] { "18" }),
  NZ_Exempt(CountryCode.NZ, "Exempt", new String[] { "Exempt" }),
  NZ_G(CountryCode.NZ, "G", new String[] { "G" }),
  NZ_GA(CountryCode.NZ, "GA", new String[] { "GA" }),
  NZ_GY(CountryCode.NZ, "GY", new String[] { "GY" }),
  NZ_M(CountryCode.NZ, "M", new String[] { "M" }),
  NZ_Objectionable(CountryCode.NZ, "Objectionable", new String[] { "Objectionable" }),
  NZ_PG(CountryCode.NZ, "PG", new String[] { "PG" }),
  NZ_R(CountryCode.NZ, "R", new String[] { "R" }), // particular class of persons, or for particular purposes, or both
  NZ_R13(CountryCode.NZ, "R13", new String[] { "R13" }),
  NZ_R15(CountryCode.NZ, "R15", new String[] { "R15" }),
  NZ_R16(CountryCode.NZ, "R16", new String[] { "R16" }),
  NZ_R18(CountryCode.NZ, "R18", new String[] { "R18" }),
  NZ_RP13(CountryCode.NZ, "RP13", new String[] { "RP13" }),
  NZ_RP16(CountryCode.NZ, "RP16", new String[] { "RP16" }),
  NZ_RP18(CountryCode.NZ, "RP18", new String[] { "RP18" }),

  PE_0(CountryCode.PE, "0", new String[] { "0", "0+" }),
  PE_5(CountryCode.PE, "5", new String[] { "5", "5+" }),
  PE_7(CountryCode.PE, "7", new String[] { "7", "7+" }),
  PE_9(CountryCode.PE, "9", new String[] { "9", "9+" }),
  PE_12(CountryCode.PE, "12", new String[] { "12", "12+" }),
  PE_14(CountryCode.PE, "14", new String[] { "14", "14+" }),
  PE_15(CountryCode.PE, "15", new String[] { "15", "15+" }),
  PE_18(CountryCode.PE, "18", new String[] { "18", "18+" }),
  PE_APT(CountryCode.PE, "APT", new String[] { "APT" }),

  PH_14(CountryCode.PH, "14", new String[] { "14" }),
  PH_7(CountryCode.PH, "7+ ", new String[] { "7+ " }),
  PH_All(CountryCode.PH, "All", new String[] { "All" }),
  PH_G(CountryCode.PH, "G", new String[] { "G" }),
  // PH_NotRated(CountryCode.PH, "Not Rated", new String[] { "Not Rated" }),
  PH_PG(CountryCode.PH, "PG", new String[] { "PG" }),
  PH_PG13(CountryCode.PH, "PG-13", new String[] { "PG-13" }),
  PH_R13(CountryCode.PH, "R-13", new String[] { "R-13" }),
  PH_R16(CountryCode.PH, "R-16", new String[] { "R-16" }),
  PH_R18(CountryCode.PH, "R-18", new String[] { "R-18" }),
  PH_SPG(CountryCode.PH, "SPG", new String[] { "SPG" }),
  PH_X(CountryCode.PH, "X", new String[] { "X" }),

  PL_0(CountryCode.PL, "0", new String[] { "0" }),
  PL_10(CountryCode.PL, "10+", new String[] { "10+" }),
  PL_12(CountryCode.PL, "12", new String[] { "12" }),
  PL_15(CountryCode.PL, "15", new String[] { "15" }),
  PL_16(CountryCode.PL, "16", new String[] { "16" }),
  PL_18(CountryCode.PL, "18", new String[] { "18" }),
  PL_21(CountryCode.PL, "21", new String[] { "21" }),
  PL_7(CountryCode.PL, "7", new String[] { "7" }),
  PL_AL(CountryCode.PL, "AL", new String[] { "AL" }),
  PL_AP(CountryCode.PL, "AP", new String[] { "AP" }),
  PL_BOW(CountryCode.PL, "BOW", new String[] { "BOW" }),
  PL_G(CountryCode.PL, "G", new String[] { "G" }),
  PL_Keysymbol(CountryCode.PL, "Key symbol", new String[] { "Key symbol" }),
  // PL_NotRated(CountryCode.PL, "Not Rated", new String[] { "Not Rated" }),

  PR_G(CountryCode.PR, "G", new String[] { "G", "Rated G" }),
  PR_NC17(CountryCode.PR, "NC-17", new String[] { "NC-17", "Rated NC-17" }),
  PR_PG(CountryCode.PR, "PG", new String[] { "PG", "Rated PG" }),
  PR_PG13(CountryCode.PR, "PG-13", new String[] { "PG-13", "Rated PG-13" }),
  PR_R(CountryCode.PR, "R", new String[] { "R", "Rated R" }),
  PR_TV14(CountryCode.PR, "TV-14", new String[] { "TV-14" }),
  PR_TVG(CountryCode.PR, "TV-G", new String[] { "TV-G" }),
  PR_TVMA(CountryCode.PR, "TV-MA", new String[] { "TV-MA" }),
  PR_TVPG(CountryCode.PR, "TV-PG", new String[] { "TV-PG" }),
  PR_TVY(CountryCode.PR, "TV-Y", new String[] { "TV-Y" }),
  PR_TVY7(CountryCode.PR, "TV-Y7", new String[] { "TV-Y7" }),

  PT_0(CountryCode.PT, "Públicos", new String[] { "Para todos os públicos", "Públicos" }), // all Movies
  PT_10AP(CountryCode.PT, "10AP", new String[] { "10AP", "10" }),
  PT_12AP(CountryCode.PT, "12AP", new String[] { "12AP", "12" }),
  PT_16(CountryCode.PT, "16", new String[] { "16" }),
  PT_A(CountryCode.PT, "A", new String[] { "A" }),
  PT_M12(CountryCode.PT, "M/12", new String[] { "M/12", "M_12", "M12" }),
  PT_M14(CountryCode.PT, "M/14", new String[] { "M/14", "M_14", "M14" }),
  PT_M16(CountryCode.PT, "M/16", new String[] { "M/16", "M_16", "M16" }),
  PT_M18(CountryCode.PT, "M/18", new String[] { "M/18", "M_18", "M18" }),
  PT_M3(CountryCode.PT, "M/3", new String[] { "M/3", "M_3", "M3" }),
  // PT_M3612141618(CountryCode.PT, "M/ 3/6/12/14/16/18", new String[] { "M/ 3/6/12/14/16/18" }), // dafuq?
  PT_M4(CountryCode.PT, "M/4", new String[] { "M/4", "M_4", "M4" }),
  PT_M6(CountryCode.PT, "M/6", new String[] { "M/6", "M_6", "M6" }),
  PT_P(CountryCode.PT, "P", new String[] { "P", "M/18- P" }), // pr0n
  PT_T(CountryCode.PT, "T", new String[] { "T" }), // all TV

  RO_12(CountryCode.RO, "12", new String[] { "12" }),
  RO_15(CountryCode.RO, "15", new String[] { "15" }),
  RO_18(CountryCode.RO, "18", new String[] { "18", "18*" }),
  RO_18P(CountryCode.RO, "18+", new String[] { "18+" }),
  RO_AG(CountryCode.RO, "A.G.", new String[] { "A.G.", "AG", "GA" }), // all
  RO_AP(CountryCode.RO, "A.P.", new String[] { "A.P.", "AP" }),
  RO_AP12(CountryCode.RO, "AP-12", new String[] { "AP-12", "PA" }), // parental advisory
  RO_IC(CountryCode.RO, "IC", new String[] { "IC" }), // Prohibition of communication
  RO_IC14(CountryCode.RO, "IC-14", new String[] { "IC-14", "I.C.-14" }),
  RO_IM18(CountryCode.RO, "IM-18", new String[] { "IM-18", "I.M.-18" }),
  RO_IM18XXX(CountryCode.RO, "IM-18-XXX", new String[] { "IM-18-XXX" }),
  RO_N15(CountryCode.RO, "N-15", new String[] { "N-15" }),
  // RO_Unrated(CountryCode.RO, "Unrated", new String[] { "Unrated" }), // all (yes, according to TMDB?)
  RO_XXX(CountryCode.RO, "XXX", new String[] { "XXX" }),

  RU_0(CountryCode.RU, "0+", new String[] { "0+" }),
  RU_12(CountryCode.RU, "12+", new String[] { "12+" }),
  RU_14(CountryCode.RU, "14+", new String[] { "14+" }),
  RU_16(CountryCode.RU, "16+", new String[] { "16+" }),
  RU_18(CountryCode.RU, "18+", new String[] { "18+" }),
  RU_6(CountryCode.RU, "6+", new String[] { "6+" }),
  RU_Refusedclassification(
      CountryCode.RU,
      "Refused classification",
      new String[] { "Banned", "Refused classification", "Фильмы, которым отказано в классификации" }),
  RU_Y(CountryCode.RU, "Y", new String[] { "Y" }),

  SA_G(CountryCode.SA, "G", new String[] { "G" }),
  // SA_NotRated(CountryCode.SA, "Not Rated", new String[] { "Not Rated" }),
  SA_PG(CountryCode.SA, "PG", new String[] { "PG" }),
  SA_PG12(CountryCode.SA, "PG12", new String[] { "PG12" }),
  SA_PG15(CountryCode.SA, "PG15", new String[] { "PG15" }),
  SA_R12(CountryCode.SA, "R12", new String[] { "R12" }),
  SA_R15(CountryCode.SA, "R15", new String[] { "R15" }),
  SA_R18(CountryCode.SA, "R18", new String[] { "R18" }),

  SE_0(CountryCode.SE, "0+", new String[] { "0", "0+" }),
  SE_10(CountryCode.SE, "10+", new String[] { "10", "10+" }),
  SE_11(CountryCode.SE, "11", new String[] { "11", "11+", "Från 11 år" }),
  SE_12(CountryCode.SE, "12", new String[] { "12", "12+" }),
  SE_14(CountryCode.SE, "14", new String[] { "14", "14+" }),
  SE_15(CountryCode.SE, "15", new String[] { "15", "15+", "Från 15 år" }),
  SE_18(CountryCode.SE, "18", new String[] { "18", "18+" }),
  SE_5(CountryCode.SE, "5", new String[] { "5", "5+" }),
  SE_6(CountryCode.SE, "6", new String[] { "6", "6+" }),
  SE_7(CountryCode.SE, "7", new String[] { "7", "7+", "Från 7 år" }),
  SE_9(CountryCode.SE, "9+", new String[] { "9", "9+" }),
  SE_Alla(CountryCode.SE, "Alla", new String[] { "Alla", "ALL" }),
  SE_BTL(CountryCode.SE, "BTL", new String[] { "BTL" }),

  SG_Exempt(CountryCode.SG, "Exempt", new String[] { "Exempt" }),
  SG_G(CountryCode.SG, "G", new String[] { "G" }),
  SG_M18(CountryCode.SG, "M18", new String[] { "M18", "M-18" }),
  SG_NC16(CountryCode.SG, "NC16", new String[] { "NC16", "NC-16" }),
  // SG_NotRated(CountryCode.SG, "Not Rated", new String[] { "Not Rated" }),
  SG_PG(CountryCode.SG, "PG", new String[] { "PG" }),
  SG_PG13(CountryCode.SG, "PG13", new String[] { "PG13", "PG-13" }),
  SG_R21(CountryCode.SG, "R21", new String[] { "R21" }),
  SG_RA(CountryCode.SG, "RA", new String[] { "RA" }),
  SG_Refusedclassification(CountryCode.SG, "Refused classification", new String[] { "Refused classification" }),

  SI_12(CountryCode.SI, "12", new String[] { "12" }),
  SI_15(CountryCode.SI, "15", new String[] { "15" }),
  SI_18(CountryCode.SI, "18", new String[] { "18", "18+", "18++" }),
  SI_VS(CountryCode.SI, "VS", new String[] { "VS" }), // 12+ PG

  SK_12(CountryCode.SK, "12", new String[] { "12", "12+" }),
  SK_15(CountryCode.SK, "15", new String[] { "15", "15+" }),
  SK_18(CountryCode.SK, "18", new String[] { "18" }),
  SK_7(CountryCode.SK, "7", new String[] { "7", "7+" }),
  SK_7U(CountryCode.SK, "-7", new String[] { "-7" }),
  SK_Teddybearshead(CountryCode.SK, "Teddy bear's head", new String[] { "Teddy bear's head" }),
  SK_U(CountryCode.SK, "U", new String[] { "U" }),

  SV_5(CountryCode.SV, "5", new String[] { "5", "5+" }),
  SV_7(CountryCode.SV, "7", new String[] { "7", "7+" }),
  SV_9(CountryCode.SV, "9", new String[] { "9", "9+" }),
  SV_A(CountryCode.SV, "A", new String[] { "A", "0", "0+" }),
  SV_B(CountryCode.SV, "B", new String[] { "B", "12", "12+" }),
  SV_C(CountryCode.SV, "C", new String[] { "C", "15", "15+" }),
  SV_D(CountryCode.SV, "D", new String[] { "D", "18", "18+" }),
  SV_E(CountryCode.SV, "E", new String[] { "E", "21", "21+" }),

  TH_12(CountryCode.TH, "12+", new String[] { "12", "12+", "น 12+" }),
  TH_13(CountryCode.TH, "13+", new String[] { "13", "13+", "น 13+" }),
  TH_15(CountryCode.TH, "15+", new String[] { "15", "15+", "น 15+" }),
  TH_16(CountryCode.TH, "16+", new String[] { "16", "16+", "น 16+" }),
  TH_18(CountryCode.TH, "18+", new String[] { "18", "18+", "น 18+" }),
  TH_20(CountryCode.TH, "20+", new String[] { "20", "20+", "ฉ 20-" }),
  TH_5(CountryCode.TH, "5", new String[] { "5", "5+" }),
  TH_7(CountryCode.TH, "7+", new String[] { "7", "7+" }),
  TH_Adults(CountryCode.TH, "Adults", new String[] { "Adults" }),
  TH_All(CountryCode.TH, "All", new String[] { "All" }),
  TH_Banned(CountryCode.TH, "Banned", new String[] { "Banned", "-" }),
  TH_C(CountryCode.TH, "C", new String[] { "C" }),
  TH_Children(CountryCode.TH, "Children", new String[] { "Children" }),
  TH_G(CountryCode.TH, "G", new String[] { "G", "ท" }),
  TH_General(CountryCode.TH, "General", new String[] { "General" }),
  // TH_NotRated(CountryCode.TH, "Not Rated", new String[] { "Not Rated" }),
  TH_P(CountryCode.TH, "P", new String[] { "P", "ส" }), // educational
  TH_PG(CountryCode.TH, "PG", new String[] { "PG" }),
  TH_PG13(CountryCode.TH, "PG-13", new String[] { "PG-13", "PG 13" }),
  TH_PG18(CountryCode.TH, "PG 18", new String[] { "PG 18" }),
  TH_Preschool(CountryCode.TH, "Preschool", new String[] { "Preschool" }),
  TH_U(CountryCode.TH, "U", new String[] { "U" }),
  TH_u13(CountryCode.TH, "u 13+", new String[] { "u 13+" }),
  TH_u15(CountryCode.TH, "u 15+", new String[] { "u 15+" }),
  TH_u18(CountryCode.TH, "u 18+", new String[] { "u 18+" }),

  TR_10(CountryCode.TR, "10+", new String[] { "10+" }),
  TR_10A(CountryCode.TR, "10A", new String[] { "10A" }),
  TR_13(CountryCode.TR, "13+", new String[] { "13+" }),
  TR_13A(CountryCode.TR, "13A", new String[] { "13A" }),
  TR_16(CountryCode.TR, "16+", new String[] { "16+" }),
  TR_18(CountryCode.TR, "18+", new String[] { "18+" }),
  TR_6(CountryCode.TR, "6+", new String[] { "6+" }),
  TR_6A(CountryCode.TR, "6A", new String[] { "6A" }),
  TR_7(CountryCode.TR, "7+", new String[] { "7+" }),
  TR_Educationalpurposes(CountryCode.TR, "Educational purposes", new String[] { "Educational purposes" }),
  TR_Exempt(CountryCode.TR, "Exempt", new String[] { "Exempt" }),
  TR_General(CountryCode.TR, "General", new String[] { "General", "General Audience", "G", "ALL" }),
  // TR_NotRated(CountryCode.TR, "Not Rated", new String[] { "Not Rated" }),
  TR_Refusedclassification(CountryCode.TR, "Refused classification", new String[] { "Refused classification" }),
  TR_genelzleyicikitlesi(
      CountryCode.TR,
      "genel Ä°zleyici kitlesi",
      new String[] { "genel Ä°zleyici kitlesi", "Genel İzleyici", "Genel İzleyici Kitlesi" }),

  TW_0(CountryCode.TW, "0+", new String[] { "0", "0+" }),
  TW_12(CountryCode.TW, "12+", new String[] { "12", "12+", "PG-12" }),
  TW_13(CountryCode.TW, "13+", new String[] { "13", "13+" }),
  TW_15(CountryCode.TW, "15+", new String[] { "15", "15+" }),
  TW_16(CountryCode.TW, "16+", new String[] { "16", "16+" }),
  TW_18(CountryCode.TW, "18+", new String[] { "18", "18+" }),
  TW_6(CountryCode.TW, "6+", new String[] { "6", "6+", "P" }),
  TW_7(CountryCode.TW, "7+", new String[] { "7+" }),
  TW_All(CountryCode.TW, "All", new String[] { "All", "G" }),
  TW_PG(CountryCode.TW, "PG", new String[] { "PG" }),
  TW_PG15(CountryCode.TW, "PG-15", new String[] { "PG-15" }),
  TW_R(CountryCode.TW, "R", new String[] { "R" }),

  UA_0(CountryCode.UA, "0", new String[] { "0", "0+" }),
  UA_12(CountryCode.UA, "12", new String[] { "12", "12+" }),
  UA_16(CountryCode.UA, "16", new String[] { "16", "16+" }),
  UA_18(CountryCode.UA, "18", new String[] { "18", "18+" }),
  UA_9(CountryCode.UA, "9", new String[] { "9", "9+" }),
  UA_DA(CountryCode.UA, "DA", new String[] { "DA", "ДА" }),
  UA_Denied(CountryCode.UA, "Denied", new String[] { "Denied" }),
  // UA_Unrated(CountryCode.UA, "Unrated", new String[] { "Unrated" }),
  UA_ZA(CountryCode.UA, "ZA", new String[] { "ZA", "ЗА" }),

  VI_G(CountryCode.VI, "G", new String[] { "G", "Rated G" }),
  VI_NC17(CountryCode.VI, "NC-17", new String[] { "NC-17", "Rated NC-17" }),
  VI_PG(CountryCode.VI, "PG", new String[] { "PG", "Rated PG" }),
  VI_PG13(CountryCode.VI, "PG-13", new String[] { "PG-13", "Rated PG-13" }),
  VI_R(CountryCode.VI, "R", new String[] { "R", "Rated R" }),
  VI_TV14(CountryCode.VI, "TV-14", new String[] { "TV-14" }),
  VI_TVG(CountryCode.VI, "TV-G", new String[] { "TV-G" }),
  VI_TVMA(CountryCode.VI, "TV-MA", new String[] { "TV-MA" }),
  VI_TVPG(CountryCode.VI, "TV-PG", new String[] { "TV-PG" }),
  VI_TVY(CountryCode.VI, "TV-Y", new String[] { "TV-Y" }),
  VI_TVY7(CountryCode.VI, "TV-Y7", new String[] { "TV-Y7" }),

  VE_12(CountryCode.VE, "12", new String[] { "12", "12+" }),
  VE_15(CountryCode.VE, "15", new String[] { "15", "15+" }),
  VE_5(CountryCode.VE, "5", new String[] { "5", "5+" }),
  VE_7(CountryCode.VE, "7", new String[] { "7", "7+" }),
  VE_9(CountryCode.VE, "9", new String[] { "9", "9+" }),
  VE_A(CountryCode.VE, "A", new String[] { "A" }),
  VE_AA(CountryCode.VE, "AA", new String[] { "AA" }),
  VE_Adulto(CountryCode.VE, "Adulto", new String[] { "Adulto", "18", "18+" }),
  VE_B(CountryCode.VE, "B", new String[] { "B" }),
  VE_C(CountryCode.VE, "C", new String[] { "C" }),
  VE_D(CountryCode.VE, "D", new String[] { "D" }),
  VE_Supervisado(CountryCode.VE, "Supervisado", new String[] { "Supervisado" }),
  VE_Todousuario(CountryCode.VE, "Todo usuario", new String[] { "Todo usuario", "0", "0+" }),

  VN_C(CountryCode.VN, "C", new String[] { "C" }),
  VN_K(CountryCode.VN, "K", new String[] { "K" }),
  VN_P(CountryCode.VN, "P", new String[] { "P" }), // all
  VN_T13(CountryCode.VN, "T13", new String[] { "T13", "C13" }),
  VN_T16(CountryCode.VN, "T16", new String[] { "T16", "C16" }),
  VN_T18(CountryCode.VN, "T18", new String[] { "T18", "C18" }),

  ZA_1012PG(CountryCode.ZA, "10-12 PG", new String[] { "10-12 PG", "10–12PG", "10-12PG" }),
  ZA_12(CountryCode.ZA, "12+", new String[] { "12+" }),
  ZA_13(CountryCode.ZA, "13", new String[] { "13" }),
  ZA_14(CountryCode.ZA, "14+", new String[] { "14+" }),
  ZA_16(CountryCode.ZA, "16", new String[] { "16", "16+" }),
  ZA_18(CountryCode.ZA, "18", new String[] { "18", "18+" }),
  ZA_6(CountryCode.ZA, "6+", new String[] { "6+" }),
  ZA_79PG(CountryCode.ZA, "7–9PG", new String[] { "7–9PG", "7-9 PG", "7-9PG" }),
  ZA_9(CountryCode.ZA, "9+", new String[] { "9+" }),
  ZA_A(CountryCode.ZA, "A", new String[] { "A" }),
  ZA_All(CountryCode.ZA, "All", new String[] { "All" }),
  // ZA_NotRated(CountryCode.ZA, "Not Rated", new String[] { "Not Rated" }),
  ZA_PG(CountryCode.ZA, "PG", new String[] { "PG" }),
  ZA_PG13(CountryCode.ZA, "PG13", new String[] { "PG13" }),
  ZA_R18(CountryCode.ZA, "R18", new String[] { "R18" }),
  ZA_X18(CountryCode.ZA, "X18", new String[] { "X18" }),
  ZA_XX(CountryCode.ZA, "XX", new String[] { "XX" }),

  // last but not least our representation of a "null" value
  @JsonEnumDefaultValue
  UNKNOWN(null, "Unknown", new String[] { "Unknown" });

  private final CountryCode country;
  private final String      name;
  private final String[]    possibleNotations;

  /**
   * Instantiates a new certification.
   * 
   * @param country
   *          the country
   * @param name
   *          the name
   * @param possibleNotations
   *          the possible notations
   */
  MediaCertification(CountryCode country, String name, String[] possibleNotations) {
    this.country = country;
    this.name = name;
    this.possibleNotations = possibleNotations;
  }

  /**
   * Gets the country.
   * 
   * @return the country
   */
  public CountryCode getCountry() {
    return country;
  }

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * gets the localized name
   * 
   * @return the localized name
   */
  public String getLocalizedName() {
    return toString();
  }

  /**
   * Gets the possible notations.
   * 
   * @return the possible notations
   */
  public String[] getPossibleNotations() {
    return possibleNotations;
  }

  /**
   * Get the certifications for the given country.
   * 
   * @param country
   *          the country
   * @return the certifications for the given country
   */
  public static List<MediaCertification> getCertificationsforCountry(CountryCode country) {
    List<MediaCertification> certifications = new ArrayList<>();

    for (MediaCertification cert : MediaCertification.values()) {
      if (cert.getCountry() == country) {
        certifications.add(cert);
      }
    }

    // at last - add global unknown/not_rate/unrated
    for (MediaCertification cert : MediaCertification.values()) {
      if (cert.getCountry() == null) {
        certifications.add(cert);
      }
    }

    return certifications;
  }

  @Override
  public String toString() {
    if (this == UNKNOWN) {
      try {
        return TmmResourceBundle.getString("MediaCertification." + name());
      }
      catch (Exception ignored) {
        // fallback
        return this.name;
      }
    }
    else {
      return name;
    }
  }

  /**
   * Gets the certification.
   * 
   * @param country
   *          the country
   * @param name
   *          the name
   * @return the certification
   */
  public static MediaCertification getCertification(String country, String name) {
    if (country != null && country.equals("CA-QC")) {
      country = "CA"; // downgrade Quebec; we only have countries in here (TMDB)
    }
    CountryCode countryCode = CountryCode.getByCode(country);
    return getCertification(countryCode, name);
  }

  /**
   * generates a certification string from certs list, country alpha2.
   * 
   * @param certs
   *          list of certifications
   * @return certification string like "US:R / UK:15 / SW:15"
   */
  public static String generateCertificationStringFromList(List<MediaCertification> certs) {
    if (certs == null || certs.isEmpty()) {
      return "";
    }
    String certstring = "";
    for (MediaCertification c : certs) {
      if (c.getCountry() == CountryCode.GB) {
        certstring += " / UK:" + c.getName();
      }
      else {
        certstring += " / " + c.getCountry().getAlpha2() + ":" + c.getName();
        certstring += " / " + c.getCountry().getName() + ":" + c.getName();
      }
    }
    return certstring.substring(3).strip(); // strip off first slash
  }

  /**
   * generates a certification string for country alpha2 (including all different variants); so skins parsing with substr will find them :)<br>
   * eg: "DE:FSK 16 / DE:FSK16 / DE:16 / DE:ab 16".
   * 
   * @param cert
   *          the cert
   * @return certification string like "US:R / UK:15 / SW:15"
   */
  public static String generateCertificationStringWithAlternateNames(MediaCertification cert) {
    return generateCertificationStringWithAlternateNames(cert, false);
  }

  /**
   * generates a certification string for country alpha2 or country name (including all different variants); so skins parsing with substr will find
   * them :)<br>
   * eg: "DE:FSK 16 / DE:FSK16 / DE:16 / DE:ab 16". eg: "Germany:FSK 16 / Germany:FSK16 / Germany:16 / Germany:ab 16".
   *
   * @param cert
   *          the cert
   * @param withCountryName
   *          true/false
   * @return certification string like "US:R / UK:15 / SW:15"
   */
  public static String generateCertificationStringWithAlternateNames(MediaCertification cert, boolean withCountryName) {
    if (cert == null) {
      return "";
    }
    if (cert == UNKNOWN) {
      return cert.name;
    }
    if (cert == NOT_RATED) {
      return "NR";
    }
    if (cert.getCountry() == null) {
      withCountryName = false;
    }
    String certstring = "";
    for (String notation : cert.getPossibleNotations()) {
      if (withCountryName) {
        certstring += " / " + cert.getCountry().getName() + ":" + notation;
      }
      else {
        if (cert.getCountry() == CountryCode.GB) {
          certstring += " / UK:" + notation;
        }
        else {
          if (cert.getCountry() != null) {
            certstring += " / " + cert.getCountry().getAlpha2() + ":" + notation;
          }
          else {
            certstring += " / " + notation;
          }
        }
      }
    }
    return certstring.substring(3).strip(); // strip off first slash
  }

  /**
   * Find certification.
   * 
   * @param name
   *          the name
   * @return the certification
   */
  public static MediaCertification findCertification(String name) {
    if (StringUtils.isBlank(name)) {
      return UNKNOWN;
    }

    // for GB-15 instead of GB_15
    String alternateName = name.replace("-", "_");

    for (MediaCertification cert : MediaCertification.values()) {
      // check if the ENUM name matches
      if (cert.name().equalsIgnoreCase(name)) {
        return cert;
      }
      if (cert.name().equalsIgnoreCase(alternateName)) {
        return cert;
      }
      // check if the name matches
      if (cert.getName().equalsIgnoreCase(name)) {
        return cert;
      }
      // check if one of the possible notations matches
      for (String notation : cert.possibleNotations) {
        if (notation.equalsIgnoreCase(name)) {
          return cert;
        }
      }
    }

    return UNKNOWN;
  }

  /**
   * Gets the certification.
   * 
   * @param country
   *          the country
   * @param name
   *          the name
   * @return the certification
   */
  public static MediaCertification getCertification(CountryCode country, String name) {
    if (StringUtils.isBlank(name)) {
      return UNKNOWN;
    }

    // for GB-15 instead of GB_15
    String alternateName = name.replace("-", "_");

    // try to find the certification
    for (MediaCertification cert : MediaCertification.getCertificationsforCountry(country)) {
      // check if the ENUM name matches
      if (cert.name().equalsIgnoreCase(name)) {
        return cert;
      }
      if (cert.name().equalsIgnoreCase(alternateName)) {
        return cert;
      }
      // check if the name matches
      if (cert.getName().equalsIgnoreCase(name)) {
        return cert;
      }
      // check if one of the possible notations matches
      for (String notation : cert.possibleNotations) {
        if (notation.equalsIgnoreCase(name)) {
          return cert;
        }
      }
    }

    // not found? does it match our global NR/UR?!
    for (MediaCertification cert : MediaCertification.getCertificationsforCountry(null)) {
      if (cert.name().equalsIgnoreCase(name)) {
        return cert;
      }
      if (cert.name().equalsIgnoreCase(alternateName)) {
        return cert;
      }
      if (cert.getName().equalsIgnoreCase(name)) {
        return cert;
      }
      for (String notation : cert.possibleNotations) {
        if (notation.equalsIgnoreCase(name)) {
          return cert;
        }
      }
    }
    return UNKNOWN;
  }

  /**
   * gets the MPAA String from any US (!) movie/TV show certification<br>
   */
  public static String getMPAAString(MediaCertification cert) {
    // http://en.wikipedia.org/wiki/Motion_picture_rating_system#Comparison
    switch (cert) {
      // movies
      case US_G:
        return "Rated G";

      case US_PG:
        return "Rated PG";

      case US_PG13:
        return "Rated PG-13";

      case US_R:
        return "Rated R";

      case US_NC17:
        return "Rated NC-17";

      case NOT_RATED:
        return "NR";

      // TV shows
      case US_TVY7:
      case US_TV14:
      case US_TVPG:
      case US_TVMA:
      case US_TVG:
      case US_TVY:
        return cert.getName();

      default:
        return "";
    }
  }

  /**
   * Called on de-serialization. Nice to migrate old ENUMs ;)
   * 
   * @param enumName
   *          called with the ENUM name()
   * @return
   */
  @JsonCreator
  static MediaCertification mapDeprecated(String enumName) {
    try {
      // if it works, we had a valid ENUM
      return MediaCertification.valueOf(enumName);
    }
    catch (Exception e) {
      // else we migrate our LanguageGlobals to Globals ;)
      if (enumName.contains("_NotRated") || enumName.contains("_Notrated")) {
        return NOT_RATED;
      }
      if (enumName.contains("_Unrated")) {
        return UNRATED;
      }
      // did we forgot something - return default
      return UNKNOWN;
    }
  }
}
