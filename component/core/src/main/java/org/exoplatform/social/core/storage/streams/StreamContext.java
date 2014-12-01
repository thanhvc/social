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
package org.exoplatform.social.core.storage.streams;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.storage.cache.SocialStorageCacheService;
import org.exoplatform.social.core.storage.cache.model.data.IntegerData;
import org.exoplatform.social.core.storage.cache.model.data.ListActivityStreamData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityCountKey;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.IdentityKey;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.cache.selector.MySpacesStreamCountCacheSelector;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.persister.ActivityPersister;
import org.exoplatform.social.core.storage.streams.persister.Persister;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class StreamContext implements Startable {
  /** Logger */
  private static final Log LOG = ExoLogger.getLogger(StreamContext.class);
  /** */
  static final String INTERVAL_ACTIVITY_PERSIST_THRESHOLD = "exo.social.activity.interval.persist.threshold";
  /** */
  static final String ACTIVITY_LIMIT_PERSIST_THRESHOLD = "exo.social.activity.limit.persist.threshold";
  /** */
  static final long DEFAULT_INTERVAL_ACTIVITY_PERSIST_THRESHOLD = 1000; //20s  = 1000 x 20
  /** */
  static final long DEFAULT_ACTIVITY_LIMIT_PERSIST_THRESHOLD = 100; //number per persist storage
  /** */
  private static StreamContext instance;
  /** */
  private long intervalPersistThreshold;
  /** */
  private long limitPersistThreshold;
  /** persister */
  private final Persister activityPersister;
  /** */
  private boolean schedulerOn = true;
  
  /**
   * Constructor by 
   * @param params
   * @param service
   */
  public StreamContext(InitParams params) {
    ValueParam intervalPersistThreshold = params.getValueParam(INTERVAL_ACTIVITY_PERSIST_THRESHOLD);
    ValueParam limitPersistThreshold = params.getValueParam(ACTIVITY_LIMIT_PERSIST_THRESHOLD);
    if (intervalPersistThreshold != null && intervalPersistThreshold.getValue() != null) {
      this.intervalPersistThreshold = Long.valueOf(intervalPersistThreshold.getValue()).longValue();
    } else {
      this.intervalPersistThreshold = DEFAULT_INTERVAL_ACTIVITY_PERSIST_THRESHOLD;
    }
    
    if (limitPersistThreshold != null && limitPersistThreshold.getValue() != null) {
      this.limitPersistThreshold = Long.valueOf(limitPersistThreshold.getValue()).longValue();
    } else {
      this.limitPersistThreshold = DEFAULT_ACTIVITY_LIMIT_PERSIST_THRESHOLD;
    }
    activityPersister = new ActivityPersister(this.intervalPersistThreshold);
  }
  
  /**
   * Simple constructor
   */
  public StreamContext() {
    this.intervalPersistThreshold = DEFAULT_INTERVAL_ACTIVITY_PERSIST_THRESHOLD;
    this.limitPersistThreshold = DEFAULT_ACTIVITY_LIMIT_PERSIST_THRESHOLD;
    activityPersister = new ActivityPersister(this.intervalPersistThreshold);
  }
  
  public void clear() {
    DataChangeMerger.reset();
    getActivityPersister().getScheduler().stop();
  }
  /**
   * Gets interval activity persist threshold time.
   * 
   * @return The period of time to execute a persistence call.
   */
  public long getIntervalPersistThreshold() {
    return intervalPersistThreshold;
  }

  /**
   * Gets number per persist storage.
   * 
   * @return the number per persist storage.
   */
  public long getLimitPersistThreshold() {
    return limitPersistThreshold;
  }

  /**
   * Sets number per persist storage.
   * 
   * @param limitPersistThreshold the number per persist storage.
   */
  public void setLimitPersistThreshold(long limitPersistThreshold) {
    this.limitPersistThreshold = limitPersistThreshold;
  }

  /**
   * Gets the instance from eXo Container
   * @return
   */
  public static StreamContext instanceInContainer() {
    return CommonsUtils.getService(StreamContext.class);
  }
  
  /**
   * Gets the simple instance without configuration on configuration.xml file
   * @return
   */
  public static StreamContext instanceSimple() {
    if (instance == null) {
      instance = new StreamContext();
    }
    
    return instance;
  }
  
  /**
   * 
   * @return
   */
  public static ExoCache<StreamKey, ListActivityStreamData> getStreamCache() {
    SocialStorageCacheService cacheService = CommonsUtils.getService(SocialStorageCacheService.class);
    return cacheService == null ?  null : cacheService.getStreamCache();
  }
  
  /**
   * 
   * @return
   */
  public static ExoCache<ActivityCountKey, IntegerData> getActivitiesCountCache() {
    SocialStorageCacheService cacheService = CommonsUtils.getService(SocialStorageCacheService.class);
    return cacheService == null ?  null : cacheService.getActivitiesCountCache();
  }
  
  /**
   * Clear my spaces stream cache count
   * @param streamOwnerId
   */
  public static void clearMySpacesCountCache(final String streamOwnerId, final ActivityType type) {
    ExoCache<ActivityCountKey, IntegerData> countCache = getActivitiesCountCache();
    if (countCache == null) return;
    try {
      countCache.select(new MySpacesStreamCountCacheSelector(streamOwnerId, type));
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    
  }
  
  /**
   * Puts the activity into the stream
   * 
   * @param posterId
   * @param type
   */
  public static void increaseCount(String posterId, ActivityType type) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(posterId), type);
    
    ExoCache<ActivityCountKey, IntegerData> countCache = StreamContext.getActivitiesCountCache();
    
    if (countCache == null) return;
    
    IntegerData data = countCache.get(key);
    if (data != null) {
      data.increase();
      LOG.debug("postId, " + posterId.toString() + ", " + data.build().intValue());
    } else {
      data = new IntegerData(1); 
      LOG.debug("new cache: postId, " + posterId.toString() + ", " + data.build().intValue());
      countCache.put(key, data);
    }
  }
  
  /**
   * Decrease the count size
   * 
   * @param posterId
   * @param type
   */
  public static void decreaseCount(String posterId, ActivityType type) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(posterId), type);
    
    ExoCache<ActivityCountKey, IntegerData> countCache = StreamContext.getActivitiesCountCache();
    
    if (countCache == null) return;
    
    IntegerData data = countCache.get(key);
    if (data != null) {
      data.decrease();
    }
  }
  
  /**
   * Switch ON/OFF the scheduler
   * @param value
   */
  public void switchSchedulerOnOff(boolean value) {
    this.schedulerOn = value; 
  }
  
  /**
   * Returns the disable/enable scheduler
   * @return
   */
  public boolean isOnScheduler() {
    return this.schedulerOn;
  }

  /**
   * Returns the activity persister
   * @return
   */
  public Persister getActivityPersister() {
    return activityPersister;
  }

  @Override
  public void start() {
    this.activityPersister.getScheduler().start();
    
  }

  @Override
  public void stop() {
    this.activityPersister.getScheduler().stop();
  }
  
  
}
