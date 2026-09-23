package org.tinymediamanager.core.ai.anthropic;

import java.util.List;

public class AnthropicModelsResponse {
  private List<Model> models;

  public List<Model> getModels() {
    return models;
  }

  public void setModels(List<Model> models) {
    this.models = models;
  }

  public static class Model {
    private String id;
    private String display_name;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getDisplayName() {
      return display_name;
    }

    public void setDisplayName(String display_name) {
      this.display_name = display_name;
    }
  }
}
