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
package org.tinymediamanager.ui.components.label;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;
import org.tinymediamanager.core.ImageCache;
import org.tinymediamanager.core.Settings;
import org.tinymediamanager.core.entities.MediaEntity;
import org.tinymediamanager.core.entities.Person;

/**
 * A label component for displaying a person's image.
 * <p>
 * This class extends {@link ImageLabel} and provides functionality to load and display images for a given {@link Person} entity.
 * </p>
 *
 * @author Manuel Laggner
 */
public class PersonImageLabel extends ImageLabel implements HierarchyListener {
  private SwingWorker<Void, Void> personWorker = null;
  private Person                  person       = null;
  private MediaEntity             mediaEntity  = null;

  public PersonImageLabel() {
    super();
    drawNoImage = false;
    cacheUrl = true;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addHierarchyListener(this);
  }

  @Override
  public void removeNotify() {
    removeHierarchyListener(this);
    super.removeNotify();
  }

  /**
   * Sets the person and media entity for which the image should be displayed.<br>
   * The image is only fetched if this label is currently visible; otherwise the fetch is deferred until this label gets shown (lazy loading).
   *
   * @param mediaEntity
   *          the media entity associated with the person
   * @param person
   *          the person whose image should be shown
   */
  public void setPerson(MediaEntity mediaEntity, Person person) {
    clearImage();

    if (mediaEntity != null && person != null) {
      this.person = person;
      this.mediaEntity = mediaEntity;

      // only fetch the image if this label is visible; otherwise it will be fetched once it gets shown
      if (isShowing()) {
        startLoading();
      }
    }
  }

  /**
   * Starts loading the image for the current person (at most once per person)
   */
  private void startLoading() {
    if (person == null || mediaEntity == null || personWorker != null) {
      return;
    }

    personWorker = new ActorImageLoader(person, mediaEntity);
    personWorker.execute();
  }

  @Override
  public void clearImage() {
    if (personWorker != null && !personWorker.isDone()) {
      personWorker.cancel(true);
    }
    personWorker = null;
    this.person = null;
    this.mediaEntity = null;

    super.clearImage();
  }

  @Override
  public void hierarchyChanged(HierarchyEvent e) {
    // load a deferred image as soon as this label becomes visible
    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
      startLoading();
    }
  }

  /**
   * Inner class for loading actor images asynchronously.
   */
  protected class ActorImageLoader extends SwingWorker<Void, Void> {
    private final Person      actor;
    private final MediaEntity mediaEntity;
    private Path              imagePath = null;
    private String            imageUrl  = null;

    private ActorImageLoader(Person actor, MediaEntity mediaEntity) {
      this.actor = actor;
      this.mediaEntity = mediaEntity;
    }

    @Override
    protected Void doInBackground() {
      // set file (or cached one) if existent
      String actorImageFilename = actor.getNameForStorage();
      if (StringUtils.isNotBlank(actorImageFilename)) {
        Path file = null;

        // we prefer reading it from the cache
        if (Settings.getInstance().isImageCache()) {
          file = ImageCache.getCachedFile(Paths.get(mediaEntity.getPath(), Person.ACTOR_DIR, actorImageFilename));
        }

        // not in the cache - read it from the path
        if (file == null) {
          file = Paths.get(mediaEntity.getPath(), Person.ACTOR_DIR, actorImageFilename);
        }

        if (Files.exists(file)) {
          imagePath = file;
          return null;
        }
      }

      // no file found, try to resolve/cache the url
      if (StringUtils.isNotBlank(actor.getThumbUrl())) {
        if (Settings.getInstance().isImageCache()) {
          Path p = ImageCache.getCachedFile(actor.getThumbUrl());
          if (p != null) {
            imagePath = p;
            return null;
          }
        }

        // last but not least load from url
        imageUrl = actor.getThumbUrl();
      }

      return null;
    }

    @Override
    protected void done() {
      if (isCancelled()) {
        return;
      }

      if (imagePath != null) {
        setImagePath(imagePath.toString());
      }
      else if (StringUtils.isNotBlank(imageUrl)) {
        // we have a url, but no imagePath
        setImageUrl(imageUrl);
      }
      else {
        clearImage();
      }
    }
  }
}
