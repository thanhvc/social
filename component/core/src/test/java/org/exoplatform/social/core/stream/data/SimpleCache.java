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
package org.exoplatform.social.core.stream.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class SimpleCache<K, V> {
  
  private final Map<K, V> caching;
  
  public SimpleCache() {
   caching = new HashMap<K, V>(); 
  }
  
  public void put(K key, V value) {
    caching.put(key, value);
  }
  
  public boolean remove(K key) {
    return caching.remove(key) != null;
  }
  
  
  public boolean containsKey(K key) {
    return caching.containsKey(key);
  }
  
  public V get(K key) {
    return caching.get(key);
  }
  
  public int size() {
    return caching.size();
  }
  
  public Set<K> keys() {
    return caching.keySet();
  }
  
  public void clear() {
    this.caching.clear();
  }

}
