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
package org.tinymediamanager.ui.movies.dialogs;

import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TmmStore;
import org.tinymediamanager.core.ai.AiManager;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.components.label.TmmLabel;
import org.tinymediamanager.ui.panels.AbstractModalInputPanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieAiTranslatePlotPanel} is used to configure the target language for AI plot translation.
 *
 * @author Manuel Laggner
 */
public class MovieAiTranslatePlotPanel extends AbstractModalInputPanel {
  private static final String             LANGUAGE_KEY = "movie.ai.translateplot.language";
  private final JComboBox<MediaLanguages> cbLanguage;
  private final JComboBox<String>         cbModel;

   public MovieAiTranslatePlotPanel() {
     setLayout(new MigLayout("", "[][][grow]", "[][]"));

     JLabel lblLanguage = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
     add(lblLanguage, "cell 0 0,alignx right");

     cbLanguage = new JComboBox<>(MediaLanguages.valuesSorted());
     add(cbLanguage, "cell 1 0,growx, wmin 0");

     JLabel lblModel = new TmmLabel(TmmResourceBundle.getString("Settings.ai.model"));
     add(lblModel, "cell 0 1,alignx right");

     cbModel = new JComboBox<>();
     cbModel.addItem("");
     List<String> cachedModels = AiManager.getAvailableModels();
     if (cachedModels != null && !cachedModels.isEmpty()) {
       cachedModels.forEach(cbModel::addItem);
     }
     String currentModel = Settings.getInstance().getAiModel();
     if (StringUtils.isNotBlank(currentModel)) {
       boolean found = false;
       for (int i = 0; i < cbModel.getItemCount(); i++) {
         if (currentModel.equals(cbModel.getItemAt(i))) {
           found = true;
           break;
         }
       }
       if (!found) {
         cbModel.addItem(currentModel);
       }
     }
     if (StringUtils.isNotBlank(currentModel)) {
       cbModel.setSelectedItem(currentModel);
     }
     else {
       cbModel.setSelectedItem("");
     }
     add(cbModel, "cell 1 1,growx, wmin 0");

     loadDefaults();
   }

   public MediaLanguages getSelectedLanguage() {
     return (MediaLanguages) cbLanguage.getSelectedItem();
   }

   public String getModel() {
     return (String) cbModel.getSelectedItem();
   }

  private void loadDefaults() {
    String savedLanguage = TmmStore.getInstance().get(LANGUAGE_KEY);
    if (StringUtils.isNotBlank(savedLanguage)) {
      MediaLanguages lang = MediaLanguages.get(savedLanguage);
      cbLanguage.setSelectedItem(lang);
    }
    else {
      cbLanguage.setSelectedItem(MovieModuleManager.getInstance().getSettings().getScraperLanguage());
    }
  }

  @Override
  protected void onClose() {
    TmmStore.getInstance().put(LANGUAGE_KEY, getSelectedLanguage().name());
    setVisible(false);
  }
}
