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
package org.exoplatform.social.core.storage.streams.listener;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.ActivityStreamStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.ActivityStreamStorageImpl.ActivityRefType;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.event.StreamChange;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class StreamChangeListener implements DataChangeListener<StreamChange<StreamKey, String>> {
  
  private static final Log LOG = ExoLogger.getLogger(StreamChangeListener.class);

  public void onAdd(StreamChange<StreamKey, String> target) {
    String identityId = target.getKey().getStreamOwnerId();
    IdentityStorage identityStorage = getIdentityStorage();
    Identity identity = identityStorage.findIdentityById(identityId);
    if (identity == null) return;
    
    ActivityType type = target.getKey().getType();
    
    ActivityStreamStorage streamStorage = getStreamStorage();
    ActivityStorage activityStorage = getActivityStorage();
    
    String activityId = target.getHandle();
    ExoSocialActivity activity = activityStorage.getActivity(activityId);
    
    ActivityRefType refType = StorageUtils.translateToRefType(type);
    if (refType != null) {
      LOG.debug(Thread.currentThread().getName() + " - ADD - " + target.toString());
      streamStorage.createActivityRef(identity, activity, refType);
      //persist remaining data to JCR
//      if(ActivityRefType.FEED == refType && identityId.equals(activity.getPosterId())) {
//        getActivityStorageImpl().persist(activity);
//      }
    }
  }

  public void onDelete(StreamChange<StreamKey, String> target) {
    String identityId = target.getKey().getStreamOwnerId();
    IdentityStorage identityStorage = getIdentityStorage();
    Identity identity = identityStorage.findIdentityById(identityId);
    if (identity == null) return;
    
    ActivityType type = target.getKey().getType();
    
    ActivityStreamStorage streamStorage = getStreamStorage();
    String activityId = target.getHandle();
    
    ActivityRefType refType = StorageUtils.translateToRefType(type);
    if (refType != null) {
      LOG.debug(Thread.currentThread().getName() + " - REMOVE - " + target.toString());
      streamStorage.removeActivityRef(identity, activityId, refType);
    }
    
  }

  public void onUpdate(StreamChange<StreamKey, String> target) {
    String identityId = target.getKey().getStreamOwnerId();
    IdentityStorage identityStorage = getIdentityStorage();
    Identity identity = identityStorage.findIdentityById(identityId);
    if (identity == null) return;
    
    ActivityType type = target.getKey().getType();
    
    ActivityStreamStorage streamStorage = getStreamStorage();
    String activityId = target.getHandle();
    
    ActivityRefType refType = StorageUtils.translateToRefType(type);
    if (refType != null) {
      LOG.debug(Thread.currentThread().getName() + " - UPDATE - " + target.toString());
      streamStorage.updateActivityRef(identity, activityId, refType);
    }
  }
  
  private ActivityStreamStorage getStreamStorage() {
    return CommonsUtils.getService(ActivityStreamStorage.class);
  }
  
  private ActivityStorage getActivityStorage() {
    return CommonsUtils.getService(ActivityStorage.class);
  }
  
  private ActivityStorageImpl getActivityStorageImpl() {
    return CommonsUtils.getService(ActivityStorageImpl.class);
  }
  
  private IdentityStorage getIdentityStorage() {
    return CommonsUtils.getService(IdentityStorage.class);
  }
  
  

}
