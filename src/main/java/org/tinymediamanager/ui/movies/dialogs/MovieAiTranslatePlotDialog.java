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

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.TmmStore;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.components.label.TmmLabel;
import org.tinymediamanager.ui.dialogs.TmmDialog;

import net.miginfocom.swing.MigLayout;

/**
 * The class {@link MovieAiTranslatePlotDialog} is used to configure the target language for AI plot translation.
 *
 * @author Manuel Laggner
 */
public class MovieAiTranslatePlotDialog extends TmmDialog {
  private static final String             LANGUAGE_KEY = "movie.ai.translateplot.language";
  private final JComboBox<MediaLanguages> cbLanguage;

  private boolean                         proceed      = false;

  public MovieAiTranslatePlotDialog() {
    super(TmmResourceBundle.getString("movie.ai.translateplot"), "movieAiTranslatePlot");
    setMinimumSize(new Dimension(400, 150));

    JPanel panelContent = new JPanel();
    getContentPane().add(panelContent, BorderLayout.CENTER);
    panelContent.setLayout(new MigLayout("", "[][200lp, grow]", "[]"));

    JLabel lblLanguage = new TmmLabel(TmmResourceBundle.getString("metatag.language"));
    panelContent.add(lblLanguage, "cell 0 0,alignx right");

    cbLanguage = new JComboBox<>(MediaLanguages.valuesSorted());
    panelContent.add(cbLanguage, "cell 1 0,growx, wmin 0");

    {
      JButton cancelButton = new JButton(TmmResourceBundle.getString("Button.cancel"));
      cancelButton.setIcon(IconManager.CANCEL_INV);
      cancelButton.addActionListener(e -> {
        proceed = false;
        setVisible(false);
      });
      addButton(cancelButton);

      JButton btnProceed = new JButton(TmmResourceBundle.getString("Button.proceed"));
      btnProceed.setIcon(IconManager.APPLY_INV);
      btnProceed.addActionListener(e -> {
        proceed = true;
        TmmStore.getInstance().put(LANGUAGE_KEY, getSelectedLanguage().name());
        setVisible(false);
      });
      addDefaultButton(btnProceed);
    }
  }

  public MediaLanguages getSelectedLanguage() {
    return (MediaLanguages) cbLanguage.getSelectedItem();
  }

  public boolean shouldProceed() {
    return proceed;
  }

  @Override
  public void setVisible(boolean visible) {
    if (visible) {
      loadDefaults();
    }
    super.setVisible(visible);
  }

  private void loadDefaults() {
    String savedLanguage = TmmStore.getInstance().get(LANGUAGE_KEY);
    if (StringUtils.isNotBlank(savedLanguage)) {
      cbLanguage.setSelectedItem(MediaLanguages.get(savedLanguage));
    }
    else {
      cbLanguage.setSelectedItem(MovieModuleManager.getInstance().getSettings().getScraperLanguage());
    }
  }
}
