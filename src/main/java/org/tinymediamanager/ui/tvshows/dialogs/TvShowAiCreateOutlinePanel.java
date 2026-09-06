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
package org.tinymediamanager.ui.tvshows.dialogs;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TmmStore;
import org.tinymediamanager.core.tvshow.TvShowModuleManager;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.components.label.TmmLabel;
import org.tinymediamanager.ui.panels.AbstractModalInputPanel;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link TvShowAiCreateOutlinePanel} is used to configure parameters for AI outline generation.
 *
 * @author Manuel Laggner
 */
public class TvShowAiCreateOutlinePanel extends AbstractModalInputPanel {
  private static final String             LANGUAGE_KEY  = "tvshow.ai.outline.language";
  private static final String             MAX_WORDS_KEY = "tvshow.ai.outline.maxwords";
  private final JComboBox<MediaLanguages> cbLanguage;
  private final JSpinner                  spMaxWords;

  public TvShowAiCreateOutlinePanel() {
    setLayout(new MigLayout("", "[][200lp, grow]", "[][]"));

    JLabel lblLanguage = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
    add(lblLanguage, "cell 0 0,alignx right");

    cbLanguage = new JComboBox<>(MediaLanguages.valuesSorted());
    add(cbLanguage, "cell 1 0,growx, wmin 0");

    JLabel lblMaxWords = new TmmLabel(TmmResourceBundle.getString("movie.ai.outline.maxwords"));
    add(lblMaxWords, "cell 0 1,alignx right");

    spMaxWords = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
    add(spMaxWords, "cell 1 1,growx, wmin 0");

    loadDefaults();
  }

  public MediaLanguages getSelectedLanguage() {
    return (MediaLanguages) cbLanguage.getSelectedItem();
  }

  public int getMaxWords() {
    return (Integer) spMaxWords.getValue();
  }

  private void loadDefaults() {
    String savedLanguage = TmmStore.getInstance().get(LANGUAGE_KEY);
    if (StringUtils.isNotBlank(savedLanguage)) {
      MediaLanguages lang = MediaLanguages.get(savedLanguage);
      cbLanguage.setSelectedItem(lang);
    }
    else {
      cbLanguage.setSelectedItem(TvShowModuleManager.getInstance().getSettings().getScraperLanguage());
    }

    spMaxWords.setValue(TmmStore.getInstance().getAsInt(MAX_WORDS_KEY, 50));
  }

  @Override
  protected void onClose() {
    TmmStore.getInstance().put(LANGUAGE_KEY, getSelectedLanguage().name());
    TmmStore.getInstance().put(MAX_WORDS_KEY, String.valueOf(getMaxWords()));
    setVisible(false);
  }
}
