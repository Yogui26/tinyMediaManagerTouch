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
package org.tinymediamanager.ui.movies.actions;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.tinymediamanager.core.TmmResourceBundle;
import org.tinymediamanager.core.movie.MovieModuleManager;
import org.tinymediamanager.core.movie.MovieRenamer;
import org.tinymediamanager.core.movie.entities.Movie;
import org.tinymediamanager.ui.IconManager;
import org.tinymediamanager.ui.MainWindow;
import org.tinymediamanager.ui.actions.TmmAction;
import org.tinymediamanager.ui.movies.MovieUIModule;

/**
 * The class {@link MovieUndoRenameAction} is used to undo renaming of movies
 * 
 * @author Manuel Laggner
 */
public class MovieUndoRenameAction extends TmmAction {
  public MovieUndoRenameAction() {
    putValue(SMALL_ICON, IconManager.UNDO);
    putValue(NAME, TmmResourceBundle.getString("movie.rename.undo"));
    putValue(SHORT_DESCRIPTION, TmmResourceBundle.getString("movie.rename.undo.desc"));
  }

  @Override
  protected void processAction(ActionEvent e) {
    List<Movie> selectedMovies = MovieUIModule.getInstance().getSelectionModel().getSelectedMovies();

    if (selectedMovies.isEmpty()) {
      JOptionPane.showMessageDialog(MainWindow.getInstance(), TmmResourceBundle.getString("tmm.nothingselected"));
      return;
    }

    // take a snapshot of all folders involved, to be able to re-evaluate the MMD flag of affected movies afterwards
    Set<Path> touchedFolders = new LinkedHashSet<>();
    for (Movie movie : selectedMovies) {
      touchedFolders.add(movie.getPathNIO().toAbsolutePath());
      if (movie.getRenameHistory() != null) {
        touchedFolders.add(Paths.get(movie.getRenameHistory().getOldPath()).toAbsolutePath());
        touchedFolders.add(Paths.get(movie.getRenameHistory().getNewPath()).toAbsolutePath());
      }
    }

    // undo rename
    for (Movie movie : selectedMovies) {
      MovieRenamer.undoRename(movie);
    }

    // re-check the MMD flag of all movies in the old AND new folders of the undone movies
    MovieModuleManager.getInstance().getMovieList().reevaluateMMDForFolders(touchedFolders);
  }
}
