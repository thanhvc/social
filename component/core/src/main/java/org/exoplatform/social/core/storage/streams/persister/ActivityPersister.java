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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.service.utils.LogWatch;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.event.DataChange;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.DataChangeQueue;
import org.exoplatform.social.core.storage.streams.event.DataContext;
import org.exoplatform.social.core.storage.streams.event.StreamChange;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 25, 2014  
 */
public class ActivityPersister implements Persister {
  /** Logger */
  private static final Log LOG = ExoLogger.getLogger(ActivityPersister.class);
  /** */
  private final PersisterScheduler persisterScheduler;
  /** */
  final LogWatch logWatch;
  
  public ActivityPersister(long intervalPersistThreshold) {
    
    this.persisterScheduler = PersisterScheduler.init()
        .persister(this)
        .wakeup(intervalPersistThreshold)
        .timeUnit(TimeUnit.MILLISECONDS)
        .build();
    logWatch = new LogWatch("Persister action ");
  }

  @Override
  public void commit(boolean forceCommit) {
    persistFixedSize(forceCommit);
  }
  
  private void persistFixedSize(boolean forcePersist) {
    DataContext<StreamChange<StreamKey, String>> context = DataChangeMerger.getDataContext();
    if (persisterScheduler.shoudldPersist(context.getChangesSize()) || forcePersist) {
      DataChangeQueue<StreamChange<StreamKey, String>> changes = context.popChanges();
      if (changes != null && changes.size() > 0) {
        try {
          
          logWatch.start();
          Map<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>> map = DataChangeMerger.transformToMap(changes);
          persisterScheduler.newSynchronizationLock(1);
          //
          for (Map.Entry<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>> e : map.entrySet()) {
            PersisterInvoker.persist(e.getKey(), e.getValue());
            StorageUtils.persist();
          }
        } finally {
          persisterScheduler.countDown();
          logWatch.stop();
          LOG.debug(changes.size() + " streams affected, consumed time: " + logWatch.getElapsedTime() + "ms");
        }
      }
    }
  }
  
  /**
   * Gets the scheduler
   * @return
   */
  @Override
  public PersisterScheduler getScheduler() {
    return this.persisterScheduler;
  }

}
