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
import org.tinymediamanager.core.tvshow.entities.TvShowEpisode;
import org.tinymediamanager.scraper.entities.MediaLanguages;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.tvshows.TvShowUIModule;
import org.tinymediamanager.ui.tvshows.dialogs.TvShowAiCreateOutlineDialog;

/**
 * The {@link TvShowEpisodeAiCreateOutlineAction} is used to create an outline for episodes using AI
 *
 * @author Manuel Laggner
 */
public class TvShowEpisodeAiCreateOutlineAction extends TmmAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(TvShowEpisodeAiCreateOutlineAction.class);

  public TvShowEpisodeAiCreateOutlineAction() {
    putValue(NAME, TmmResourceBundle.getString("episode.ai.createoutline"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("episode.ai.createoutline.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    if (StringUtils.isBlank(Settings.getInstance().getAiApiKey())) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("Settings.ai.missingapi"));
      return;
    }

    final List<TvShowEpisode> selectedEpisodes = TvShowUIModule.getInstance().getSelectionModel().getSelectedEpisodes();

    if (selectedEpisodes.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    TvShowAiCreateOutlineDialog dialog = new TvShowAiCreateOutlineDialog();
    dialog.setVisible(true);

    if (!dialog.shouldProceed()) {
      return;
    }

    final MediaLanguages targetLanguage = dialog.getSelectedLanguage();
    final int maxWords = dialog.getMaxWords();
    final String languageName = targetLanguage.getTitle();

    TmmTaskManager.getInstance()
        .addUnnamedTask(new TmmTask(TmmResourceBundle.getString("episode.ai.createoutline"), selectedEpisodes.size(), TaskType.BACKGROUND_TASK) {
          @Override
          protected void doInBackground() {
            int i = 0;
            AiService aiService = new AiService();

            for (TvShowEpisode episode : selectedEpisodes) {
              try {
                String outline = aiService.generateOutline(episode.getTitle(), episode.getPlot(), maxWords, languageName, 0);
                if (StringUtils.isNotBlank(outline)) {
                  episode.setOutline(outline);
                  episode.saveToDb();
                }
              }
              catch (AiException e) {
                LOGGER.error("AI error, aborting outline generation: {}", e.getMessage());
                break;
              }
              publishState(++i);
              if (cancel) {
                break;
              }
            }
          }
        });
  }
}
