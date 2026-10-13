package org.tinymediamanager.core.entities;

public class HdrInfo {

  // https://github.com/xbmc/xbmc/blob/8aaab7f03065971ba46cc31adac110181f98d0dc/xbmc/utils/StreamDetails.cpp#L850
  public enum HdrType {
    HDR_TYPE_DOLBYVISION("dolbyvision"),
    HDR_TYPE_DOLBYVISION2("dolbyvision2"), // TODO: as of 2026 - name=assumption
    HDR_TYPE_HLG("hlg"),
    HDR_TYPE_HLGPLUS("hlgplus"),
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

  private HdrType hdrType;
  private Integer hdrProfile;
  private Integer hdrLevel;
  private String  hdrVersion;
  // Although there could be multiple HDR layers, we forcefully do not keep a list
  // 95% of all medias have a single one, and for the rest, the "compatibility" type is enough.
  private Integer compatibilityId;
  private HdrType compatibilityType;
  private boolean baseLayer;        // BL
  private boolean enhancementLayer; // FEL/MEL https://github.com/MediaArea/MediaInfoLib/issues/2447
  private boolean rpu;

  public String getDisplayName() {
    StringBuilder sb = new StringBuilder();
    sb.append(hdrType.getNfoName());
    if (hdrProfile != null) {
      sb.append(" ").append(hdrProfile);
    }
    if (hdrLevel != null) {
      sb.append(", Level ").append(hdrLevel);
    }
    if (compatibilityType != null) {
      sb.append(" - ").append(compatibilityType.getNfoName()).append(" compatible");
    }
    return sb.toString();
  }

  public HdrType getHdrType() {
    return hdrType;
  }

  public void setHdrType(HdrType hdrType) {
    this.hdrType = hdrType;
  }

  public Integer getHdrProfile() {
    return hdrProfile;
  }

  public void setHdrProfile(Integer hdrProfile) {
    this.hdrProfile = hdrProfile;
  }

  public Integer getHdrLevel() {
    return hdrLevel;
  }

  public void setHdrLevel(Integer hdrLevel) {
    this.hdrLevel = hdrLevel;
  }

  public String getHdrVersion() {
    return hdrVersion;
  }

  public void setHdrVersion(String hdrVersion) {
    this.hdrVersion = hdrVersion;
  }

  public Integer getCompatibilityId() {
    return compatibilityId;
  }

  public void setCompatibilityId(Integer compatibilityId) {
    this.compatibilityId = compatibilityId;
  }

  public HdrType getCompatibilityType() {
    return compatibilityType;
  }

  public void setCompatibilityType(HdrType compatibilityType) {
    this.compatibilityType = compatibilityType;
  }

  public boolean isBaseLayer() {
    return baseLayer;
  }

  public void setBaseLayer(boolean baseLayer) {
    this.baseLayer = baseLayer;
  }

  public boolean isEnhancementLayer() {
    return enhancementLayer;
  }

  public void setEnhancementLayer(boolean enhancementLayer) {
    this.enhancementLayer = enhancementLayer;
  }

  public boolean isRpu() {
    return rpu;
  }

  public void setRpu(boolean rpu) {
    this.rpu = rpu;
  }
}
