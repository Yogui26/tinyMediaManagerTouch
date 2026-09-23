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
package org.tinymediamanager.ui.tvshows.actions;

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
import org.tinymediamanager.core.threading.TmmTask;
import org.tinymediamanager.core.threading.TmmTaskHandle.TaskType;
import org.tinymediamanager.core.threading.TmmTaskManager;
import org.tinymediamanager.core.tvshow.entities.TvShow;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.panels.ModalPopupPanel;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowAiTranslatePlotPanel;

/**
 * The {@link TvShowAiTranslatePlotAction} is used to translate the plot using AI
 *
 * @author Manuel Laggner
 */
public class TvShowAiTranslatePlotAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowAiTranslatePlotAction.class);

  public TvShowAiTranslatePlotAction() {
    putValue(NAME, TmmResourceBundle.getString("tvshow.ai.translateplot"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("tvshow.ai.translateplot.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (StringUtils.isBlank(Settings.getInstance().getAiApiKey())) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("Settings.ai.missingapi"));
      return;
    }

    final List<TvShow> selectedTvShows = TvShowUIModule.getInstance().getSelectionModel().getSelectedTvShows();

    if (selectedTvShows.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    ModalPopupPanel popupPanel = MainWindow.getInstance().createModalPopupPanel();
    popupPanel.setTitle(TmmResourceBundle.getString("tvshow.ai.translateplot"));

    TvShowAiTranslatePlotPanel inputPanel = new TvShowAiTranslatePlotPanel() {
      @Override
      protected void onClose() {
        final MediaLanguages targetLanguage = getSelectedLanguage();
        final String model = getModel();
        final String languageName = targetLanguage.getTitle();

        TmmTaskManager.getInstance()
            .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("tvshow.ai.translateplot"), selectedTvShows.size(), TaskType.BACKGROUND_TASK) {
              @Override
              protected void doInBackground() {
                int i = 0;
                AiService aiService = new AiService();

                for (TvShow tvShow : selectedTvShows) {
                  try {
                    String translatedPlot = aiService.translatePlot(tvShow.getPlot(), languageName, model);
                    if (StringUtils.isNotBlank(translatedPlot)) {
                      tvShow.setPlot(translatedPlot);
                      tvShow.saveToDb();
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
