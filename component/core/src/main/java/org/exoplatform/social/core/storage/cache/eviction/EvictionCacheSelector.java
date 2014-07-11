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

import java.util.concurrent.atomic.AtomicLong;

import org.exoplatform.services.cache.CachedObjectSelector;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.ObjectCacheInfo;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 9, 2014  
 */
public class EvictionCacheSelector<Serializable, V> implements CachedObjectSelector {
  private AtomicLong numberOfEvicted = new AtomicLong(0);
  @Override
  public boolean select(java.io.Serializable key, ObjectCacheInfo ocinfo) {
    return isValid(ocinfo.getExpireTime());
  }

  @Override
  public void onSelect(ExoCache cache, java.io.Serializable key, ObjectCacheInfo ocinfo) throws Exception {
    this.numberOfEvicted.incrementAndGet();
    cache.remove(key);
  }
  
  private boolean isValid(long expirationTime) {
    return System.currentTimeMillis() < expirationTime;
  }
  
  /**
   * Reset eviction cache item counter
   */
  public void reset() {
    numberOfEvicted.set(0);
  }
  
  /**
   * Gets number of eviction items
   * @return
   */
  public long getEvictionSize() {
    return this.numberOfEvicted.get();
  }
  
}
