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
package org.exoplatform.social.core.storage.cache.selector;

import org.exoplatform.services.cache.ObjectCacheInfo;
import org.exoplatform.social.core.storage.cache.model.data.ListActivitiesData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.ListActivitiesKey;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 26, 2014  
 */
public class CommentsCacheSelector extends ScopeCacheSelector<ListActivitiesKey, ListActivitiesData> {
  
  private String activityId;

  public CommentsCacheSelector(String activityId) {
    this.activityId = activityId;
  }

  @Override
  public boolean select(final ListActivitiesKey key, final ObjectCacheInfo<? extends ListActivitiesData> ocinfo) {
    if (!super.select(key, ocinfo)) {
      return false;
    }

    return ActivityType.COMMENTS.equals(key.getKey().getActivityType())
        && key.getKey().getActivityKey().getId().equals(this.activityId);
  }

}