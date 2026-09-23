package org.tinymediamanager.core.ai.openai;

import java.util.List;

public class OpenAiModelsResponse {
  private boolean     success;
  private List<Model> data;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public List<Model> getData() {
    return data;
  }

  public void setData(List<Model> data) {
    this.data = data;
  }

  public static class Model {
    private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }
}
