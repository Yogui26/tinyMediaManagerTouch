package org.tinymediamanager.core.entities;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class HdrInfo {

  // https://github.com/xbmc/xbmc/blob/8aaab7f03065971ba46cc31adac110181f98d0dc/xbmc/utils/StreamDetails.cpp#L850
  public enum HdrType {
    HDR_TYPE_DOLBYVISION("dolbyvision"),
    HDR_TYPE_DOLBYVISION2("dolbyvision2"), // TODO: as of 2026 - name=assumption
    HDR_TYPE_HLG("hlg"),
    HDR_TYPE_HLGPLUS("hlgplus"), // TODO: as of 2026 - name=assumption
    HDR_TYPE_HDR10("hdr10"),
    HDR_TYPE_HDR10PLUS("hdr10plus"),
    HDR_TYPE_HDR10PLUSADV("hdr10plusadv"); // TODO: as of 2026 - name=assumption

    public String nfoName = "";

    private HdrType(String nfoName) {
      this.nfoName = nfoName;
    }

    public String getNfoName() {
      return nfoName;
    }
  }

  /**
   * primary/best HDR type
   */
  private HdrType hdrType;
  // Although there could be multiple HDR layers, we forcefully do not keep a list
  // 99% of all medias have a single one, and for the rest, the 2nd type is enough.
  // Compatibility could be a List; filled, when we have DoVi+HLG fallback layer, AND a second HDR10+ layer...
  private HdrType secondHdrType;
  private String  hdrVersion;
  private HdrType compatibilityType;
  // DolbyVision only entries
  private Integer dvProfile;
  private Integer dvLevel;
  private Integer dvCompatibilityId;
  private boolean dvBaseLayer;        // BL
  private boolean dvEnhancementLayer; // FEL/MEL https://github.com/MediaArea/MediaInfoLib/issues/2447
  private boolean dvRpu;              // Reference Picture Unit (DV metadata)

  public HdrType getHdrType() {
    return hdrType;
  }

  public void setHdrType(HdrType hdrType) {
    this.hdrType = hdrType;
  }

  public HdrType getSecondHdrType() {
    return secondHdrType;
  }

  public void setSecondHdrType(HdrType hdrType) {
    this.secondHdrType = hdrType;
  }

  public String getHdrVersion() {
    return hdrVersion;
  }

  public void setHdrVersion(String hdrVersion) {
    this.hdrVersion = hdrVersion;
  }

  public HdrType getCompatibilityType() {
    return compatibilityType;
  }

  public void setCompatibilityType(HdrType compatibilityType) {
    this.compatibilityType = compatibilityType;
  }

  public Integer getDvProfile() {
    return dvProfile;
  }

  public void setDvProfile(Integer hdrProfile) {
    this.dvProfile = hdrProfile;
  }

  public Integer getDvLevel() {
    return dvLevel;
  }

  public void setDvLevel(Integer hdrLevel) {
    this.dvLevel = hdrLevel;
  }

  public Integer getDvCompatibilityId() {
    return dvCompatibilityId;
  }

  public void setDvCompatibilityId(Integer compatibilityId) {
    this.dvCompatibilityId = compatibilityId;
  }

  public boolean hasDvBaseLayer() {
    return dvBaseLayer;
  }

  public void setDvBaseLayer(boolean baseLayer) {
    this.dvBaseLayer = baseLayer;
  }

  public boolean hasDvEnhancementLayer() {
    return dvEnhancementLayer;
  }

  public void setDvEnhancementLayer(boolean enhancementLayer) {
    this.dvEnhancementLayer = enhancementLayer;
  }

  public boolean hasDvRpu() {
    return dvRpu;
  }

  public void setDvRpu(boolean rpu) {
    this.dvRpu = rpu;
  }

  public String getDisplayName() {
    StringBuilder sb = new StringBuilder();
    sb.append(hdrType.getNfoName());
    if (dvProfile != null) {
      sb.append(" ").append(dvProfile);
    }
    if (dvLevel != null) {
      sb.append(", Level ").append(dvLevel);
    }
    if (compatibilityType != null) {
      sb.append(" - ").append(compatibilityType.getNfoName()).append(" compatible");
    }
    return sb.toString();
  }

  /**
   * We save only the first layers version (since version is not used widely)
   * 
   * @param hdrVersion
   */
  public void parseHdrVersion(String hdrVersion) {
    if (StringUtils.isBlank(hdrVersion)) {
      return;
    }
    String[] splitted = hdrVersion.split("/");
    setHdrVersion(splitted[0]);
  }

  /**
   * Parses MediaInfo HDR string, possibly delimited with slash
   * 
   * @param hdrFormat
   */
  public void parseHdrTypes(String hdrFormat) {
    if (StringUtils.isBlank(hdrFormat)) {
      return;
    }
    String[] splitted = hdrFormat.split("/");
    setHdrType(parseHdrType(splitted[0]));
    if (splitted.length > 1) {
      setSecondHdrType(parseHdrType(splitted[1]));
    }
  }

  private HdrType parseHdrType(String hdrFormat) {
    if (StringUtils.isBlank(hdrFormat)) {
      return null;
    }
    String normalized = hdrFormat.trim().toLowerCase(Locale.ROOT);
    if (normalized.contains("dolby vision")) {
      if (normalized.contains("dolby vision 2") || normalized.contains("version 2")) { // TODO: assumed
        return HdrType.HDR_TYPE_DOLBYVISION2;
      }
      return HdrType.HDR_TYPE_DOLBYVISION;
    }
    else if (normalized.contains("2094") || normalized.contains("hdr10+")) {
      if (normalized.contains("adv")) { // TODO: assumed
        return HdrType.HDR_TYPE_HDR10PLUSADV;
      }
      return HdrType.HDR_TYPE_HDR10PLUS;
    }
    else if (normalized.contains("hlg") || normalized.contains("hybrid log-gamma")) {
      if (normalized.contains("hlg+")) {
        return HdrType.HDR_TYPE_HLGPLUS;
      }
      return HdrType.HDR_TYPE_HLG;
    }
    if (normalized.contains("2086") || normalized.contains("hdr10")) {
      return HdrType.HDR_TYPE_HDR10;
    }
    return null;
  }

  public void parseCompatibility(String compatibility) {
    HdrType hdrType = getHdrType();

    // Inherent/direct compatibility
    if (hdrType == HdrType.HDR_TYPE_HDR10PLUSADV) {
      setCompatibilityType(HdrType.HDR_TYPE_HDR10PLUS);
      return;
    }
    if (hdrType == HdrType.HDR_TYPE_HDR10PLUS) {
      setCompatibilityType(HdrType.HDR_TYPE_HDR10);
      return;
    }
    if (hdrType == HdrType.HDR_TYPE_HDR10) {
      // no compatibility here, as this is the base
      return;
    }
    if (StringUtils.isBlank(compatibility)) {
      return;
    }

    String normalized = compatibility.trim().toLowerCase(Locale.ROOT);
    // TODO: HLG+
    if (normalized.contains("hlg")) {
      setCompatibilityType(HdrType.HDR_TYPE_HLG);
      if (isDolbyVisionProfile8()) {
        setDvCompatibilityId(4);
      }
      return;
    }

    if (normalized.contains("hdr10")) {
      setCompatibilityType(HdrType.HDR_TYPE_HDR10);
      if (isDolbyVisionProfile8()) {
        setDvCompatibilityId(1);
      }
    }
  }

  public boolean isDolbyVisionProfile8() {
    return getHdrType() != null && getHdrType() == HdrType.HDR_TYPE_DOLBYVISION && Integer.valueOf(8).equals(getDvProfile());
  }

  public void parseDvProfile(String profile) {
    if (StringUtils.isBlank(profile)) {
      return;
    }
    // Dolby Vision: dvhe.08, hev1.08, avc3.05
    // Extract the numeric profile after the final dot.
    String[] splitted = profile.split("/");
    Matcher matcher = Pattern.compile("\\.(\\d+)$").matcher(splitted[0].trim());
    try {
      if (matcher.find()) {
        setDvProfile(Integer.valueOf(matcher.group(1)));
      }
      // Other formats may provide a plain numeric profile.
      if (splitted[0].trim().matches("\\d+")) {
        setDvProfile(Integer.valueOf(splitted[0].trim()));
      }
    }
    catch (NumberFormatException e) {
      // ignore
    }
  }

  public void parseDvLevel(String level) {
    if (StringUtils.isBlank(level)) {
      return;
    }
    try {
      String[] splitted = level.split("/");
      setDvLevel(Integer.valueOf(splitted[0].trim()));
    }
    catch (NumberFormatException e) {
      // ignore
    }
  }

  public void parseDvSettings(String settings) {
    if (StringUtils.isBlank(settings)) {
      return;
    }
    String value = settings.trim().toUpperCase(Locale.ROOT);
    setDvBaseLayer(value.contains("BL"));
    setDvEnhancementLayer(value.contains("EL")); // TODO: FEL/MEL
    setDvRpu(value.contains("RPU"));
  }
}
