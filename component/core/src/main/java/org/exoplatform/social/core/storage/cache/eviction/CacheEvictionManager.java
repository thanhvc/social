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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.exoplatform.services.cache.CacheService;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 9, 2014  
 */
public class CacheEvictionManager implements Startable {
  
  /** */
  private final List<CacheEvictionConfig> evictionConfigs;
  
  /** */
  private List<EvictionTimerTask> tasks;
  
  /** */
  private CacheService cacheService;
  
  public CacheEvictionManager(CacheService cacheService) {
    this.evictionConfigs = new ArrayList<CacheEvictionConfig>();
    this.cacheService = cacheService;
  }
  
  public void addEvictionConfig(CacheEvictionConfig evictionConfig) {
    evictionConfigs.add(evictionConfig);
  }

  @Override
  public void start() {
    tasks = new ArrayList<EvictionTimerTask>(this.evictionConfigs.size());
    
    for (CacheEvictionConfig config : evictionConfigs) {
      EvictionTimerTask t = EvictionTimerTask.init()
                                                .cacheName(config.getCacheName())
                                                .wakeup(config.getWakeupInterval())
                                                .timeUnit(TimeUnit.SECONDS)
                                                .build();
      t.start(this.cacheService);
      tasks.add(t);
    }
  }

  @Override
  public void stop() {
    for(EvictionTimerTask task : tasks) {
      task.stop();
    }
  }
  /**
   * Gets the number of eviction tasks
   * @return
   */
  public int getNumberOfTasks() {
    return tasks.size();
  }
  
  /**
   * Gets the eviction configuration by name
   * @return NULL if not found
   */
  public CacheEvictionConfig getEvictionConfig(String name) {

    for (CacheEvictionConfig config : evictionConfigs) {
      if (config.getCacheName().equalsIgnoreCase(name)) {
        return config;
      }
    }

    return null;

  }
}
