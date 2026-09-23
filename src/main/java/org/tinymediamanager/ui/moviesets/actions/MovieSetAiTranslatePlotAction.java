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
package org.tinymediamanager.ui.moviesets.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.ai.AiException;
import org.tinymediamanager.core.ai.AiService;
import org.tinymediamanager.core.movie.entities.MovieSet;
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle.TaskType;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.moviesets.MovieSetUIModule;
import org.tinymediamanager.ui.moviesets.dialogs.MovieSetAiTranslatePlotPanel;
import org.tinymediamanager.ui.panels.ModalPopupPanel;

/**
 * The {@link MovieSetAiTranslatePlotAction} is used to translate the plot of movie sets using AI
 *
 * @author Manuel Laggner
 */
public class MovieSetAiTranslatePlotAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(MovieSetAiTranslatePlotAction.class);

  public MovieSetAiTranslatePlotAction() {
    putValue(NAME, TmmResourceBundle.getString("movieset.ai.translateplot"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movieset.ai.translateplot.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (StringUtils.isBlank(Settings.getInstance().getAiApiKey())) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("Settings.ai.missingapi"));
      return;
    }

    final List<MovieSet> selectedMovieSets = MovieSetUIModule.getInstance().getSelectionModel().getSelectedMovieSets();

    if (selectedMovieSets.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    ModalPopupPanel popupPanel = MainWindow.getInstance().createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("movieset.ai.translateplot"));

    MovieSetAiTranslatePlotPanel inputPanel = new MovieSetAiTranslatePlotPanel() {
      @Override
      protected void onClose() {
        final MediaLanguages targetLanguage = getSelectedLanguage();
        final String model = getModel();
        final String languageName = targetLanguage.getTitle();

        TmmTaskManager.getInstance()
            .addUnnamedTask(
                new TmmTask(TmmResourceBundle.getString("movieset.ai.translateplot"), selectedMovieSets.size(), TaskType.BACKGROUND_TASK) {
                  @Override
                  protected void doInBackground() {
                    int i = 0;
                    AiService aiService = new AiService();

                    for (MovieSet movieSet : selectedMovieSets) {
                      try {
                        String translatedPlot = aiService.translatePlot(movieSet.getPlot(), languageName, model);
                        if (StringUtils.isNotBlank(translatedPlot)) {
                          movieSet.setPlot(translatedPlot);
                          movieSet.saveToDb();
                        }
                      }
                      catch (AiException ex) {
                        LOGGER.error("AI error, aborting plot translation: {}", ex.getMessage());
                        break;
                      }
                      publishState(++i);
                      if (cancel) {
                        break;
                      }
                    }
                  }
                });
        super.onClose();
      }
    };

    popupPanel.setContent(inputPanel);
    MainWindow.getInstance().showModalPopupPanel(popupPanel);
  }
}
