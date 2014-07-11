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
package org.exoplatform.social.core.storage.cache.eviction;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jul
 * 9, 2014
 */
public class CacheEvictionConfig extends BaseComponentPlugin {

  /** */
  static final String CACHE_NAME = "cache.name";
  /** */
  static final String WAKEUP_INTERVAL_SECONDS = "wakeup.interval.seconds";
  /** */
  public static final int WAKEUP_DEFAULT = 5000;
  /** */
  private final String cacheName;
  /** */
  private final long wakeupInterval;

  public CacheEvictionConfig(InitParams params) {
    ValueParam cacheNameParam = params.getValueParam(CACHE_NAME);
    ValueParam wakeupIntervalParam = params.getValueParam(WAKEUP_INTERVAL_SECONDS);
    //
    if (cacheNameParam != null && cacheNameParam.getValue() != null) {
      this.cacheName = cacheNameParam.getValue();
    } else {
      this.cacheName = "";
    }
    //
    if (wakeupIntervalParam != null && wakeupIntervalParam.getValue() != null) {
      this.wakeupInterval = Long.parseLong(wakeupIntervalParam.getValue());
    } else {
      this.wakeupInterval = -1L;
    }
  }

  public String getCacheName() {
    return cacheName;
  }

  /**
   * Returns the wakeup interval to evict the cache item
   * if return -1, don't need to evict
   * @return
   */
  public long getWakeupInterval() {
    return wakeupInterval;
  }

  
}
