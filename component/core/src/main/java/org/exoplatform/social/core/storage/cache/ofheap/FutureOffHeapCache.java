/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
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
package org.exoplatform.social.core.storage.cache.ofheap;

import java.io.Serializable;

import org.apache.directmemory.cache.CacheService;
import org.exoplatform.commons.cache.future.Loader;
import org.exoplatform.social.core.storage.cache.FutureCache;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Apr 27, 2015  
 */
public class FutureOffHeapCache<K extends Serializable, V extends Serializable, C> extends FutureCache<K, V, C> {

  /** . */
  private final CacheService<K, V> cache;

  public FutureOffHeapCache(Loader<K, V, C> loader, CacheService<K, V> cache) {
    super(loader);

    //
    this.cache = cache;
  }

  public void clear() {
    cache.clear();
  }

  public void remove(K key) {
    cache.free(key);
  }

  @Override
  protected V get(K key) {
    return cache.retrieve(key);
  }

  @Override
  protected void put(K key, V entry) {
    cache.put(key, entry);
  }
}
