/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.manager.proxy;

import java.util.List;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.listeners.Callback;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.ActivityManagerImpl;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * May 16, 2014  
 */
public class ActivityProxyManager implements ActivityManager {
  private ActivityManagerImpl activityManagerDefault;
  private ActivityManager activityManagerPlugin;
  
  private ActivityManager getManager() {
    if (activityManagerPlugin == null) {
      if (activityManagerDefault == null) {
        this.activityManagerDefault = CommonsUtils.getService(ActivityManagerImpl.class);
      }
      return this.activityManagerDefault;
    } else {
      return activityManagerPlugin;
    }
  }
  
  /**
   * Register the other Activity Manager 
   * @param activityManager
   */
  public void addPlugin(ActivityManager activityManager) {
    this.activityManagerPlugin = activityManager;
  }

  @Override
  public void saveActivityNoReturn(Identity streamOwner, ExoSocialActivity activity) {
    getManager().saveActivityNoReturn(streamOwner, activity);
    
  }

  public ExoSocialActivity saveActivity(Identity streamOwner,
                                        ExoSocialActivity activity,
                                        Callback callback) {
    return getManager().saveActivity(streamOwner, activity, callback);
  }

  @Override
  public void saveActivityNoReturn(ExoSocialActivity activity) {
    getManager().saveActivityNoReturn(activity);
  }

  @Override
  public void saveActivity(Identity streamOwner, String type, String title) {
    getManager().saveActivity(streamOwner, type, title);
  }

  @Override
  public ExoSocialActivity getActivity(String activityId) {
    return getManager().getActivity(activityId);
  }

  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) {
    return getManager().getParentActivity(comment);
  }

  @Override
  public void updateActivity(ExoSocialActivity activity) {
    getManager().updateActivity(activity);
    
  }

  @Override
  public void deleteActivity(ExoSocialActivity activity) {
    getManager().deleteActivity(activity);
  }

  @Override
  public void deleteActivity(String activityId) {
    getManager().deleteActivity(activityId);
    
  }

  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity newComment) {
    getManager().saveComment(activity, newComment);
  }

  @Override
  public ExoSocialActivity saveComment(ExoSocialActivity activity,
                                       ExoSocialActivity newComment,
                                       Callback callback) {
    return getManager().saveComment(activity, newComment, callback);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getCommentsWithListAccess(ExoSocialActivity activity) {
    return getManager().getCommentsWithListAccess(activity);
  }

  @Override
  public void deleteComment(String activityId, String commentId) {
    getManager().deleteComment(activityId, commentId);
  }

  @Override
  public void deleteComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    getManager().deleteComment(activity, comment);
    
  }

  @Override
  public void saveLike(ExoSocialActivity activity, Identity identity) {
    getManager().saveLike(activity, identity);
    
  }

  @Override
  public void deleteLike(ExoSocialActivity activity, Identity identity) {
    getManager().deleteLike(activity, identity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity identity) {
    return getManager().getActivitiesWithListAccess(identity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity ownerIdentity,
                                                                           Identity viewerIdentity) {
    return getManager().getActivitiesWithListAccess(ownerIdentity, viewerIdentity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfConnectionsWithListAccess(Identity identity) {
    return getManager().getActivitiesOfConnectionsWithListAccess(identity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfSpaceWithListAccess(Identity spaceIdentity) {
    return getManager().getActivitiesOfSpaceWithListAccess(spaceIdentity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfUserSpacesWithListAccess(Identity identity) {
    return getManager().getActivitiesOfUserSpacesWithListAccess(identity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivityFeedWithListAccess(Identity identity) {
    return getManager().getActivityFeedWithListAccess(identity);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity poster) {
    return getManager().getActivitiesByPoster(poster);
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity,
                                                                     String... activityTypes) {
    return getManager().getActivitiesByPoster(posterIdentity, activityTypes);
  }

  @Override
  public void addProcessor(ActivityProcessor activityProcessor) {
    getManager().addProcessor(activityProcessor);
    
  }

  @Override
  public void addProcessorPlugin(BaseActivityProcessorPlugin activityProcessorPlugin) {
    getManager().addProcessorPlugin(activityProcessorPlugin);
  }

  @Override
  public void addActivityEventListener(ActivityListenerPlugin activityListenerPlugin) {
    getManager().addActivityEventListener(activityListenerPlugin);
    
  }

  @Override
  public ExoSocialActivity saveActivity(Identity streamOwner, ExoSocialActivity activity) {
    return getManager().saveActivity(streamOwner, activity);
  }

  @Override
  public ExoSocialActivity saveActivity(ExoSocialActivity activity) {
    return getManager().saveActivity(activity);
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity identity) throws ActivityStorageException {
    return getManager().getActivities(identity);
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity identity, long start, long limit) throws ActivityStorageException {
    return getManager().getActivities(identity, start, limit);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity) throws ActivityStorageException {
    return getManager().getActivitiesOfConnections(ownerIdentity);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity,
                                                            int offset,
                                                            int length) throws ActivityStorageException {
    return getManager().getActivitiesOfConnections(ownerIdentity, offset, length);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfUserSpaces(Identity ownerIdentity) {
    return getManager().getActivitiesOfUserSpaces(ownerIdentity);
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity identity) throws ActivityStorageException {
    return getManager().getActivityFeed(identity);
  }

  @Override
  public void removeLike(ExoSocialActivity activity, Identity identity) throws ActivityStorageException {
    getManager().removeLike(activity, identity);
  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity activity) throws ActivityStorageException {
    return getManager().getComments(activity);
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, String type, String title) throws ActivityStorageException {
    return getManager().recordActivity(owner, type, title);
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, ExoSocialActivity activity) throws Exception {
    return getManager().recordActivity(owner, activity);
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, String type, String title, String body) throws ActivityStorageException {
    return getManager().recordActivity(owner, type, title, body);
  }

  @Override
  public int getActivitiesCount(Identity owner) throws ActivityStorageException {
    return getManager().getActivitiesCount(owner);
  }

  @Override
  public void processActivitiy(ExoSocialActivity activity) {
    getManager().processActivitiy(activity);
  }

}
