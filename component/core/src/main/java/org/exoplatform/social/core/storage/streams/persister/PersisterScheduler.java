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
package org.exoplatform.social.core.storage.streams.persister;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.storage.streams.StreamContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class PersisterScheduler implements PersistAlgorithm, Runnable {
  private static final Log LOG = ExoLogger.getLogger(PersisterScheduler.class);
  /** */
  final Persister persister;
  /** */
  private long wakeupInterval;
  /** */
  final TimeUnit timeUnit;
  
  private Thread thread;
  
  private CountDownLatch countDownLatch = new CountDownLatch(0);

  /** */
  ScheduledExecutorService scheduledExecutor;
  
  public static Builder init() {
    return new Builder();
  }
  
  public PersisterScheduler(Builder builder) {
    this.wakeupInterval = builder.wakeupInterval;
    this.persister = builder.persister;
    this.timeUnit = builder.timeUnit == null ? TimeUnit.MILLISECONDS : builder.timeUnit;
    thread = new Thread(this);
    thread.setPriority(Thread.MAX_PRIORITY);
  }

  public void start() {
    scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutor.scheduleWithFixedDelay(thread, wakeupInterval / 2, wakeupInterval, this.timeUnit);
  }
  
  public void stop() {
    if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
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
  
  public void run() {
    try {
      //
      persister.commit(true);
    } catch (Throwable t) {
      LOG.warn("Persist task encountered an unexpected error", t);
    }
  }
  
  public static class Builder {
    public Persister persister;
    public long wakeupInterval;
    public TimeUnit timeUnit;

    public Builder() {}
    
    public Builder persister(Persister persister) {
      this.persister = persister;
      return this;
    }
    
    public Builder wakeup(long interval) {
      this.wakeupInterval = interval;
      return this;
    }
    
    public Builder timeUnit(TimeUnit timeUnit) {
      this.timeUnit = timeUnit;
      return this;
    }
    
    public PersisterScheduler build() {
      return new PersisterScheduler(this);
    }
  }

  public boolean shoudldPersist(int changedSize) {
    return changedSize >= StreamContext.instanceInContainer().getLimitPersistThreshold();
    
  }
  
  public CountDownLatch getSynchronizationLock() {
    return this.countDownLatch;
  }
  
  public CountDownLatch newSynchronizationLock(int number) {
    if (countDownLatch.getCount() == 0) {
      countDownLatch = new CountDownLatch(number);
    }
    
    return this.countDownLatch;
  }
  
  public void countDown() {
    if (countDownLatch.getCount() > 0) {
      countDownLatch.countDown();
    }
  }
  
  public void resetWakeupInterval(long intervalPersistThreshold) {
    this.wakeupInterval = intervalPersistThreshold;
    this.stop();
    this.start();
  }
}