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

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 9, 2014  
 */
public class EvictionTimerTask implements EvictionAlgorithm, Runnable {
  
  private static final Log LOG = ExoLogger.getLogger(EvictionTimerTask.class);
  /** */
  private long wakeupInterval;
  /** */
  private String name;
  /** */
  final TimeUnit timeUnit;
  /** */
  private Thread thread;
  /** */
  private ExoCache<Serializable, ?> cache;
  
  private final EvictionCacheSelector<Serializable, Object> selector;

  /** */
  ScheduledExecutorService scheduledExecutor;
  
  public static Builder init() {
    return new Builder();
  }
  public EvictionTimerTask(Builder builder) {
    this.wakeupInterval = builder.wakeupInterval;
    this.timeUnit = builder.timeUnit == null ? TimeUnit.MILLISECONDS : builder.timeUnit;
    this.name = builder.name;
    thread = new Thread(this);
    thread.setPriority(Thread.MIN_PRIORITY);
    this.selector = new EvictionCacheSelector<Serializable, Object>();
  }

  public void start(CacheService cacheService) {
    cache = cacheService.getCacheInstance(this.name);
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutor.scheduleWithFixedDelay(thread, wakeupInterval / 2, wakeupInterval, this.timeUnit);
  }
  
  public void stop() {
    if (scheduledExecutor != null) {
      scheduledExecutor.shutdown(); // Disable new tasks from being submitted
      try {
        // Wait a while for existing tasks to terminate
        if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          scheduledExecutor.shutdownNow(); // Cancel currently executing tasks
          // Wait a while for tasks to respond to being cancelled
          if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS))
            LOG.warn("scheduledExecutor did not terminate");
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
        scheduledExecutor.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt();
      }
    }
  }
  
  @Override
  public void process() {
    if (cache != null) {
      try {
        cache.select(this.selector);
        long count = this.selector.getEvictionSize();
        if (count > 0) {
          LOG.info("Evicted items size : " + count);
        }
      } catch (Exception e) {
        LOG.warn("Eviction the cache: " + name + " unsuccessfully. " + e.getMessage());
        LOG.debug(e);
      } finally {
        this.selector.reset();
      }
    }
  }
  
  public void run() {
    try {
      process();
    } catch (Throwable t) {
      LOG.warn("EvictionTimerTask encountered an unexpected error", t);
    }
  }
  
  public static class Builder {
    public long wakeupInterval;
    public TimeUnit timeUnit;
    public String name;

    public Builder() {}
    
    public Builder wakeup(long interval) {
      this.wakeupInterval = interval;
      return this;
    }
    
    public Builder cacheName(String name) {
      this.name = name;
      return this;
    }
    
    public Builder timeUnit(TimeUnit timeUnit) {
      this.timeUnit = timeUnit;
      return this;
    }
    
    public EvictionTimerTask build() {
      return new EvictionTimerTask(this);
    }
  }

}
