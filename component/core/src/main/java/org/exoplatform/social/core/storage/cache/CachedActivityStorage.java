/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.social.core.storage.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.Validate;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.service.utils.LogWatch;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.activity.filter.ActivityFilter;
import org.exoplatform.social.core.activity.filter.ActivityUpdateFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.ActivityStorageException;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.cache.loader.ServiceContext;
import org.exoplatform.social.core.storage.cache.model.data.ActivityData;
import org.exoplatform.social.core.storage.cache.model.data.IntegerData;
import org.exoplatform.social.core.storage.cache.model.data.ListActivitiesData;
import org.exoplatform.social.core.storage.cache.model.data.ListActivityStreamData;
import org.exoplatform.social.core.storage.cache.model.data.ListIdentitiesData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityCountKey;
import org.exoplatform.social.core.storage.cache.model.key.ActivityKey;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.IdentityKey;
import org.exoplatform.social.core.storage.cache.model.key.ListActivitiesKey;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.cache.selector.ActivityOwnerCacheSelector;
import org.exoplatform.social.core.storage.cache.selector.ActivityStreamOwnerCacheSelector;
import org.exoplatform.social.core.storage.cache.selector.NewerOlderStreamCountCacheSelector;
import org.exoplatform.social.core.storage.cache.selector.ScopeCacheSelector;
import org.exoplatform.social.core.storage.impl.ActivityBuilderWhere;
import org.exoplatform.social.core.storage.impl.ActivityStorageImpl;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.StreamContext;
import org.exoplatform.social.core.storage.streams.StreamHelper;
import org.exoplatform.social.core.storage.streams.event.DataChange;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.DataChangeQueue;
import org.exoplatform.social.core.storage.streams.event.DataContext;
import org.exoplatform.social.core.storage.streams.event.StreamChange;
import org.exoplatform.social.core.storage.streams.persister.Persister;
import org.exoplatform.social.core.storage.streams.persister.PersisterInvoker;
import org.exoplatform.social.core.storage.streams.persister.PersisterScheduler;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class CachedActivityStorage implements ActivityStorage, Persister {

  /** Logger */
  private static final Log LOG = ExoLogger.getLogger(CachedActivityStorage.class);

  private final ExoCache<ActivityKey, ActivityData> exoActivityCache;
  private final ExoCache<ActivityCountKey, IntegerData> exoActivitiesCountCache;
  private final ExoCache<ListActivitiesKey, ListActivitiesData> exoActivitiesCache;
  private final ExoCache<StreamKey, ListActivityStreamData> exoStreamCache;

  private final FutureExoCache<ActivityKey, ActivityData, ServiceContext<ActivityData>> activityCache;
  private final FutureExoCache<ActivityCountKey, IntegerData, ServiceContext<IntegerData>> activitiesCountCache;
  private final FutureExoCache<ListActivitiesKey, ListActivitiesData, ServiceContext<ListActivitiesData>> activitiesCache;
  private final FutureStreamExoCache<String, StreamKey, ListActivityStreamData, ServiceContext<ListActivityStreamData>> streamCache;

  private final ActivityStorageImpl storage;
  
  /** */
  private final PersisterScheduler persisterScheduler;
  /** */
  final LogWatch logWatch;

  public void clearCache() {

    try {
      exoActivitiesCache.select(new ScopeCacheSelector<ListActivitiesKey, ListActivitiesData>());
      exoActivitiesCountCache.select(new NewerOlderStreamCountCacheSelector());
    }
    catch (Exception e) {
      LOG.error(e);
    }

  }
  
  void clearOwnerCache(String ownerId) {

    try {
      exoActivityCache.select(new ActivityOwnerCacheSelector(ownerId));
    }
    catch (Exception e) {
      LOG.error(e);
    }

    clearCache();

  }

  /**
   * Clears activities of input owner from cache.
   * 
   * @param streamOwner owner of stream to be cleared.
   * 
   */
  void clearOwnerStreamCache(String streamOwner) {
    try {
      exoActivityCache.select(new ActivityStreamOwnerCacheSelector(streamOwner));
    }
    catch (Exception e) {
      LOG.error(e);
    }
    
    clearCache();
  }
  
  /**
   * Clear activity cached.
   * 
   * @param activityId
   * @since 1.2.8
   */
  public void clearActivityCached(String activityId) {
    ActivityKey key = new ActivityKey(activityId);
    exoActivityCache.remove(key);
    clearCache();
  }
  
  private void updateCommentCountCaching(String activityId, boolean isIncrease) {
    ActivityCountKey key = new ActivityCountKey(activityId, ActivityType.COMMENTS);
    IntegerData data = exoActivitiesCountCache.get(key);
    if (data != null) {
      if (isIncrease) {
        data.increase();
      } else {
        data.decrease();
      }
    }
    
    
  }
  
  /**
   * Build the activity list from the caches Ids.
   *
   * @param data ids
   * @return activities
   */
  private List<ExoSocialActivity> buildActivities(ListActivitiesData data) {

    List<ExoSocialActivity> activities = new ArrayList<ExoSocialActivity>();
    for (ActivityKey k : data.getIds()) {
      ExoSocialActivity a = getActivity(k.getId());
      activities.add(a);
    }
    return activities;

  }

  /**
   * Build the ids from the activity list.
   *
   * @param activities activities
   * @return ids
   */
  private ListActivitiesData buildIds(List<ExoSocialActivity> activities) {

    List<ActivityKey> data = new ArrayList<ActivityKey>();
    for (ExoSocialActivity a : activities) {
      if (a == null) {
        continue;
      }
      ActivityKey k = new ActivityKey(a.getId());
      exoActivityCache.put(k, new ActivityData(a));
      data.add(k);
    }
    return new ListActivitiesData(data);

  }

  public CachedActivityStorage(final ActivityStorageImpl storage, final SocialStorageCacheService cacheService) {

    //
    this.storage = storage;
    this.storage.setStorage(this);

    //
    this.exoActivityCache = cacheService.getActivityCache();
    this.exoActivitiesCountCache = cacheService.getActivitiesCountCache();
    this.exoActivitiesCache = cacheService.getActivitiesCache();
    this.exoStreamCache = cacheService.getStreamCache();

    //
    this.activityCache = CacheType.ACTIVITY.createFutureCache(exoActivityCache);
    this.activitiesCountCache = CacheType.ACTIVITIES_COUNT.createFutureCache(exoActivitiesCountCache);
    this.activitiesCache = CacheType.ACTIVITIES.createFutureCache(exoActivitiesCache);
    this.streamCache = CacheType.STREAM.createFutureStreamCache(exoStreamCache);
    
    this.persisterScheduler = PersisterScheduler.init()
                                                .persister(this)
                                                .wakeup(StreamContext.instanceInContainer().getIntervalPersistThreshold())
                                                .timeUnit(TimeUnit.MILLISECONDS)
                                                .build();
    this.persisterScheduler.start();
    logWatch = new LogWatch("Persister action ");

  }
  
  /**
   * {@inheritDoc}
   */
  public ExoSocialActivity getActivity(final String activityId) throws ActivityStorageException {

    if (activityId == null || activityId.length() == 0) {
      return ActivityData.NULL.build();
    }
    //
    ActivityKey key = new ActivityKey(activityId);

    //
    ActivityData activity = activityCache.get(
        new ServiceContext<ActivityData>() {
          public ActivityData execute() {
            try {
              ExoSocialActivity got = storage.getActivity(activityId);
              if (got != null) {
                return new ActivityData(got);
              }
              else {
                return ActivityData.NULL;
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key);

    //
    return activity.build();

  }
  
  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getUserActivities(final Identity owner) throws ActivityStorageException {
    return storage.getUserActivities(owner);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getUserActivities(final Identity owner, final long offset, final long limit)
                                                                                            throws ActivityStorageException {
    final StreamKey key = StreamKey.init(owner.getId()).key(ActivityType.USER);

    //
    ListActivityStreamData keys = streamCache.get(
        new ServiceContext<ListActivityStreamData>() {
          public ListActivityStreamData execute() {
            List<ExoSocialActivity> got = storage.getUserActivities(owner, offset, limit);
            return buildStreamDataIds(key, got);
          }
        },
        key, offset, limit);

    //
    return buildActivitiesFromStream(keys, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public void saveComment(final ExoSocialActivity activity, final ExoSocialActivity comment) throws ActivityStorageException {
    List<String> oldMentioners = Collections.emptyList();
    if (activity.getId() != null) {
      ExoSocialActivity old = getActivity(activity.getId());
      oldMentioners = StorageUtils.getIdentityIds(old.getMentionedIds());
    }
    
    storage.saveComment(activity, comment);
    
    //get new mentioners list
    List<String> newMentioners = StorageUtils.getIdentityIds(activity.getMentionedIds());
    //
    //compares and get new mentioners of new comment
    List<String> addedMentioners = StorageUtils.sub(newMentioners, oldMentioners);
    StreamHelper.MOVE.addComment(comment.getUserId(), activity);
    StreamHelper.MOVE.addOrMoveMentioners(addedMentioners, activity);
    this.commit(false);
    //
    exoActivityCache.put(new ActivityKey(comment.getId()), new ActivityData(getActivity(comment.getId())));
    ActivityKey activityKey = new ActivityKey(activity.getId());
    exoActivityCache.put(activityKey, new ActivityData(activity));
    clearCache();
    updateCommentCountCaching(activity.getId(), true);
  }

  /**
   * {@inheritDoc}
   */
  public ExoSocialActivity saveActivity(final Identity owner, final ExoSocialActivity activity) throws ActivityStorageException {
    try {
      Validate.notNull(owner, "owner must not be null.");
      Validate.notNull(activity, "activity must not be null.");
    } catch (IllegalArgumentException e) {
      throw new ActivityStorageException(ActivityStorageException.Type.ILLEGAL_ARGUMENTS, e.getMessage(), e);
    }
    
    boolean isNew = activity.getId() == null;
    //
    ExoSocialActivity a = storage.saveActivity(owner, activity);
    
    if (isNew && !activity.isHidden()) {
      StreamHelper.ADD.addActivity(activity);
      StreamHelper.ADD.addMentioners(activity);
      this.commit(false);
    }
    //
    ActivityKey key = new ActivityKey(a.getId());
    exoActivityCache.put(key, new ActivityData(a));
    clearCache();
    //
    return a;
  }

  /**
   * {@inheritDoc}
   */
  public ExoSocialActivity getParentActivity(final ExoSocialActivity comment) throws ActivityStorageException {
    return getActivity(getActivity(comment.getId()).getParentId());
  }

  /**
   * {@inheritDoc}
   */
  public void deleteActivity(final String activityId) throws ActivityStorageException {
    ActivityKey key = new ActivityKey(activityId);
    ActivityData data = exoActivityCache.get(key);
    if (data == null) {
      return;
    } else if (data.isDeleted()) {
      storage.deleteActivity(activityId);
      exoActivityCache.remove(key);
    } else {
      ExoSocialActivity activity = storage.getActivity(activityId);
      StreamHelper.REMOVE.removeActivity(activity);
      this.commit(false);
      exoActivityCache.remove(key);
      exoActivityCache.put(key, ActivityData.REMOVED);
      clearCache();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void deleteComment(final String activityId, final String commentId) throws ActivityStorageException {
    //get old mentioners list
    ExoSocialActivity old = getActivity(activityId);
    List<String> oldMentioners = StorageUtils.getIdentityIds(old.getMentionedIds());
    //
    storage.deleteComment(activityId, commentId);
    ActivityKey activityKey = new ActivityKey(activityId);
    exoActivityCache.remove(activityKey);
    ExoSocialActivity comment = getActivity(commentId);
    exoActivityCache.remove(new ActivityKey(commentId));
    //
    //re-loading the activity with refresh mentioners list
    ExoSocialActivity newActivity = getActivity(activityId);
    //get new mentioners list
    List<String> newMentioners = StorageUtils.getIdentityIds(newActivity.getMentionedIds());
    //
    //compares between old and new mentioners, to get removed mentioners list.
    List<String> removedMentioners = StorageUtils.sub(oldMentioners, newMentioners);
    //excluded the commenter list
    List<String> commenters = StorageUtils.getIdentityIds(newActivity.getCommentedIds());
    List<String> removedList = StorageUtils.sub(removedMentioners, commenters);
    //case when remove the comment, the activity removes out commenter's stream
    String removedCommenterId = comment.getUserId();
    if (!newMentioners.contains(removedCommenterId) && !commenters.contains(removedCommenterId)) {
      StreamHelper.REMOVE.removeComment(removedCommenterId, newActivity);
    }
    
    StreamHelper.REMOVE.removeMentioners(removedList, newActivity);
    this.commit(false);
    clearActivityCached(activityId);
    updateCommentCountCaching(activityId, false);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfIdentities(final List<Identity> connectionList, final long offset,
                                                           final long limit) throws ActivityStorageException {

    //
    List<IdentityKey> keyskeys = new ArrayList<IdentityKey>();
    for (Identity i : connectionList) {
      keyskeys.add(new IdentityKey(i));
    }
    ListActivitiesKey listKey = new ListActivitiesKey(new ListIdentitiesData(keyskeys), 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getActivitiesOfIdentities(connectionList, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfIdentities(final List<Identity> connectionList, final TimestampType type,
                                                           final long offset, final long limit) throws ActivityStorageException {
    return storage.getActivitiesOfIdentities(connectionList, type, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfUserActivities(final Identity owner) throws ActivityStorageException {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfUserActivities(owner));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_USER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfNewerOnUserActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                          final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.NEWER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getNewerOnUserActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_USER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfOlderOnUserActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnUserActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                          final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getOlderOnUserActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivityFeed(final Identity ownerIdentity, final int offset, final int limit) {
    
    final StreamKey key = StreamKey.init(ownerIdentity.getId()).key(ActivityType.FEED);

    //
    ListActivityStreamData keys = streamCache.get(
        new ServiceContext<ListActivityStreamData>() {
          public ListActivityStreamData execute() {
            List<ExoSocialActivity> got = storage.getActivityFeed(ownerIdentity, offset, limit);
            return buildStreamDataIds(key, got);
          }
        },
        key, offset, limit);

    //
    return buildActivitiesFromStream(keys, offset, limit);
  }
  
  /**
   * Build the activity list from the caches Ids.
   *
   * @param data ids
   * @return activities
   */
  private List<ExoSocialActivity> buildActivitiesFromStream(ListActivityStreamData data, final long offset, final long limit) {
    List<ExoSocialActivity> activities = new ArrayList<ExoSocialActivity>();
    long to = Math.min(data.size(), offset + limit);
    List<String> ids = Collections.synchronizedList(data.subList((int)offset, (int)to));
    for (String id : ids) {
      ExoSocialActivity a = getActivity(id);
      if (a != null && !a.isHidden()) {
          activities.add(a);
      }
    }
    return activities;

  }
  
   /**
   * Build the ids from the activity list.
   *
   * @return remoteId
   * @param activities activities
   * 
   */
  private ListActivityStreamData buildStreamDataIds(StreamKey key, List<ExoSocialActivity> activities) {

    ListActivityStreamData data = this.exoStreamCache.get(key);
    if (data == null) {
      data = new ListActivityStreamData(key);
      this.exoStreamCache.put(key, data);
    }
    
    //
    for(int i = 0, len = activities.size(); i < len; i++) {
      ExoSocialActivity a = activities.get(i);
      //handle the activity is NULL
      if (a == null || a.isHidden()) {
        continue;
      }
      //
      if (!data.contains(a.getId())) {
        data.insertLast(a.getId());
      }
    }
    return data;

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfActivitesOnActivityFeed(final Identity ownerIdentity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfActivitesOnActivityFeed(ownerIdentity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_FEED);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfNewerOnActivityFeed(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                        final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getNewerOnActivityFeed(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_FEED);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfOlderOnActivityFeed(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnActivityFeed(final Identity ownerIdentity, final ExoSocialActivity baseActivity,
                                                        final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getOlderOnActivityFeed(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfConnections(final Identity ownerIdentity, final int offset, final int limit) {
    final StreamKey key = StreamKey.init(ownerIdentity.getId()).key(ActivityType.CONNECTION);

    //
    ListActivityStreamData keys = streamCache.get(
        new ServiceContext<ListActivityStreamData>() {
          public ListActivityStreamData execute() {
            List<ExoSocialActivity> got = storage.getActivitiesOfConnections(ownerIdentity, offset, limit);
            return buildStreamDataIds(key, got);
          }
        },
        key, offset, limit);

    //
    return buildActivitiesFromStream(keys, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfActivitiesOfConnections(final Identity ownerIdentity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfActivitiesOfConnections(ownerIdentity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getActivitiesOfIdentity(final Identity ownerIdentity, final long offset, final long limit)
                                                                                       throws ActivityStorageException {
    
    return getUserActivities(ownerIdentity, offset, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivitiesOfConnections(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_CONNECTION);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnActivitiesOfConnections(final Identity ownerIdentity,
                                                                   final ExoSocialActivity baseActivity, final long limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(),
                                                ActivityType.NEWER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getNewerOnActivitiesOfConnections(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnActivitiesOfConnections(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_CONNECTION);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnActivitiesOfConnections(final Identity ownerIdentity,
                                                                   final ExoSocialActivity baseActivity, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(),
                                                ActivityType.OLDER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getOlderOnActivitiesOfConnections(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getUserSpacesActivities(final Identity ownerIdentity, final int offset, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getUserSpacesActivities(ownerIdentity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfUserSpacesActivities(final Identity ownerIdentity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfUserSpacesActivities(ownerIdentity));
          }
        },
        key)
        .build();
    
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserSpacesActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACES);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerOnUserSpacesActivities(final Identity ownerIdentity,
                                                                final ExoSocialActivity baseActivity, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getNewerOnUserSpacesActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderOnUserSpacesActivities(final Identity ownerIdentity, final ExoSocialActivity baseActivity) {

    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACES);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderOnUserSpacesActivities(final Identity ownerIdentity,
                                                                final ExoSocialActivity baseActivity, final int limit) {

    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getOlderOnUserSpacesActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);

  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getComments(final ExoSocialActivity existingActivity, final int offset, final int limit) {
    ActivityCountKey key = new ActivityCountKey(existingActivity.getId(), ActivityType.COMMENTS);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getComments(existingActivity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfComments(final ExoSocialActivity existingActivity) {
    //
    ActivityCountKey key = new ActivityCountKey(existingActivity.getId(), ActivityType.COMMENTS);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfComments(existingActivity));
          }
        },
        key)
        .build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment) {
    return storage.getNumberOfNewerComments(existingActivity, baseComment);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getNewerComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment,
                                                  final int limit) {
    return storage.getNewerComments(existingActivity, baseComment, limit);
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfOlderComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment) {
    return storage.getNumberOfOlderComments(existingActivity, baseComment);
  }

  /**
   * {@inheritDoc}
   */
  public List<ExoSocialActivity> getOlderComments(final ExoSocialActivity existingActivity, final ExoSocialActivity baseComment,
                                                  final int limit) {
    return storage.getOlderComments(existingActivity, baseComment, limit);
  }

  /**
   * {@inheritDoc}
   */
  public SortedSet<ActivityProcessor> getActivityProcessors() {
    return storage.getActivityProcessors();
  }

  /**
   * {@inheritDoc}
   */
  public void updateActivity(final ExoSocialActivity existingActivity) throws ActivityStorageException {
    //
    ActivityKey key = new ActivityKey(existingActivity.getId());
    ActivityData oldData = exoActivityCache.get(key);
    ExoSocialActivity oldA = oldData.build();
    boolean isChangeHiddenToDisplay = oldA.isHidden() != existingActivity.isHidden() && !existingActivity.isHidden();
    if (isChangeHiddenToDisplay) {
      existingActivity.setPostedTime(System.currentTimeMillis());
      existingActivity.setUpdated(System.currentTimeMillis());
    }
    
    boolean isKeepTitle = existingActivity.getTitle() == null;
    boolean isKeepBody = existingActivity.getBody() == null;
    boolean isKeepParams = existingActivity.getTemplateParams() == null;
    //
    storage.updateActivity(existingActivity);
    
    if (isChangeHiddenToDisplay) {
      StreamHelper.ADD.addActivity(existingActivity);
      StreamHelper.ADD.addMentioners(existingActivity);
      this.commit(false);
    }

    //
    if (isKeepTitle) {
      existingActivity.setTitle(oldA.getTitle());
    }
    //
    if (isKeepBody) {
      existingActivity.setBody(oldA.getBody());
    }
    //
    if (isKeepParams) {
      existingActivity.setTemplateParams(oldA.getTemplateParams());
    }
    
    exoActivityCache.put(key, new ActivityData(existingActivity));
    
    //
    clearCache();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivityFeed(final Identity ownerIdentity, final Long sinceTime) {

    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_FEED);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfNewerOnActivityFeed(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserActivities(final Identity ownerIdentity, final Long sinceTime) {

    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_USER);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfNewerOnUserActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnActivitiesOfConnections(final Identity ownerIdentity, final Long sinceTime) {

    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), sinceTime, ActivityType.NEWER_CONNECTION);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfNewerOnActivitiesOfConnections(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  /**
   * {@inheritDoc}
   */
  public int getNumberOfNewerOnUserSpacesActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_SPACE);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity,
                                                                              sinceTime));
      }
    }, key).build();
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfIdentities(ActivityBuilderWhere where,
                                                           ActivityFilter filter,
                                                           final long offset,
                                                           final long limit) throws ActivityStorageException {
    final List<Identity> connectionList = where.getOwners();
    //
    List<IdentityKey> keyskeys = new ArrayList<IdentityKey>();
    for (Identity i : connectionList) {
      keyskeys.add(new IdentityKey(i));
    }
    ListActivitiesKey listKey = new ListActivitiesKey(new ListIdentitiesData(keyskeys), 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getActivitiesOfIdentities(connectionList, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfSpaceActivities(final Identity spaceIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(spaceIdentity), ActivityType.SPACE);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfSpaceActivities(spaceIdentity));
          }
        },
        key)
        .build();
  }
  
  @Override
  public int getNumberOfSpaceActivitiesForUpgrade(final Identity spaceIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(spaceIdentity), ActivityType.SPACE_FOR_UPGRADE);

    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfSpaceActivitiesForUpgrade(spaceIdentity));
          }
        },
        key);
    
    ActivityCountKey keySpace =
        new ActivityCountKey(new IdentityKey(spaceIdentity), ActivityType.SPACE);
    exoActivitiesCountCache.put(keySpace, countData);
    
    return countData.build();
  }

  @Override
  public List<ExoSocialActivity> getSpaceActivities(final Identity ownerIdentity, final int offset, final int limit) {
    final StreamKey key = StreamKey.init(ownerIdentity.getId()).key(ActivityType.SPACE);

    //
    ListActivityStreamData keys = streamCache.get(
        new ServiceContext<ListActivityStreamData>() {
          public ListActivityStreamData execute() {
            List<ExoSocialActivity> got = storage.getSpaceActivities(ownerIdentity, offset, limit);
            return buildStreamDataIds(key, got);
          }
        },
        key, offset, limit);

    //
    return buildActivitiesFromStream(keys, offset, limit);
  }
  
  @Override
  public List<ExoSocialActivity> getSpaceActivitiesForUpgrade(final Identity ownerIdentity, final int offset, final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getSpaceActivitiesForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(final Identity posterIdentity, 
                                                       final int offset, 
                                                       final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(posterIdentity), ActivityType.POSTER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getActivitiesByPoster(posterIdentity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }
  
  @Override
  public List<ExoSocialActivity> getActivitiesByPoster(final Identity posterIdentity, 
                                                       final int offset, 
                                                       final int limit,
                                                       final String...activityTypes) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(posterIdentity), ActivityType.POSTER, activityTypes);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getActivitiesByPoster(posterIdentity, offset, limit, activityTypes);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }
  
  @Override
  public int getNumberOfActivitiesByPoster(final Identity posterIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(posterIdentity), ActivityType.POSTER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfActivitiesByPoster(posterIdentity));
          }
        },
        key)
        .build();
  }
  
  @Override
  public int getNumberOfActivitiesByPoster(final Identity ownerIdentity, final Identity viewerIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), new IdentityKey(viewerIdentity), ActivityType.POSTER);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfActivitiesByPoster(ownerIdentity, viewerIdentity));
          }
        },
        key)
        .build();
  }
  
  @Override
  public List<ExoSocialActivity> getNewerOnSpaceActivities(final Identity ownerIdentity,
                                                           final ExoSocialActivity baseActivity,
                                                           final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getNewerOnSpaceActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(final Identity ownerIdentity,
                                               final ExoSocialActivity baseActivity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.NEWER_SPACE);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfNewerOnSpaceActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
  }

  @Override
  public List<ExoSocialActivity> getOlderOnSpaceActivities(final Identity ownerIdentity,
                                                            final ExoSocialActivity baseActivity,
                                                            final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getOlderOnSpaceActivities(ownerIdentity, baseActivity, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(final Identity ownerIdentity,
                                               final ExoSocialActivity baseActivity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), baseActivity.getId(), ActivityType.OLDER_SPACE);

    //
    return activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, baseActivity));
          }
        },
        key)
        .build();
  }

  @Override
  public int getNumberOfNewerOnSpaceActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.NEWER_SPACE);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfNewerOnUserSpacesActivities(ownerIdentity,
                                                                              sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfUpdatedOnActivityFeed(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnActivityFeed(owner, filter);
  }

  @Override
  public int getNumberOfMultiUpdated(Identity owner, Map<String, Long> sinceTimes) {
    return storage.getNumberOfMultiUpdated(owner, sinceTimes);
  }
  
  public List<ExoSocialActivity> getNewerFeedActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getNewerFeedActivities(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }
  
  public List<ExoSocialActivity> getNewerSpaceActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getNewerSpaceActivities(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getNewerUserActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getNewerUserActivities(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getNewerUserSpacesActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getNewerUserSpacesActivities(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }
  
  @Override
  public List<ExoSocialActivity> getNewerActivitiesOfConnections(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.NEWER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getNewerActivitiesOfConnections(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }
  
  @Override
  public int getNumberOfUpdatedOnUserActivities(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnUserActivities(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnActivitiesOfConnections(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnActivitiesOfConnections(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnUserSpacesActivities(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnUserSpacesActivities(owner, filter);
  }

  @Override
  public int getNumberOfUpdatedOnSpaceActivities(Identity owner, ActivityUpdateFilter filter) {
    return storage.getNumberOfUpdatedOnSpaceActivities(owner, filter);
  }

  @Override
  public List<ExoSocialActivity> getActivities(final Identity owner,
                                               final Identity viewer,
                                               final long offset,
                                               final long limit) throws ActivityStorageException {
    
    final StreamKey key = StreamKey.init(owner.getId()).key(ActivityType.VIEWER);

    //
    ListActivityStreamData keys = streamCache.get(
        new ServiceContext<ListActivityStreamData>() {
          public ListActivityStreamData execute() {
            List<ExoSocialActivity> got = storage.getActivities(owner, viewer, offset, limit);
            return buildStreamDataIds(key, got);
          }
        },
        key, offset, limit);

    //
    return buildActivitiesFromStream(keys, offset, limit);
  }

  @Override
  public List<ExoSocialActivity> getOlderFeedActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.OLDER_FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getOlderFeedActivities(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderUserActivities(final Identity ownerIdentity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), sinceTime, ActivityType.OLDER_USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getOlderUserActivities(ownerIdentity, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);

    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderUserSpacesActivities(final Identity ownerIdentity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), sinceTime, ActivityType.OLDER_SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getOlderUserSpacesActivities(ownerIdentity, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderActivitiesOfConnections(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.OLDER_CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getOlderActivitiesOfConnections(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderSpaceActivities(final Identity owner, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), sinceTime, ActivityType.OLDER_SPACE);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getOlderSpaceActivities(owner, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfOlderOnActivityFeed(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_FEED);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfOlderOnActivityFeed(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnUserActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_USER);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfOlderOnUserActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnActivitiesOfConnections(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_CONNECTION);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfOlderOnActivitiesOfConnections(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnUserSpacesActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_SPACES);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfOlderOnUserSpacesActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderOnSpaceActivities(final Identity ownerIdentity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity),
                                                sinceTime,
                                                ActivityType.OLDER_SPACE);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfOlderOnSpaceActivities(ownerIdentity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public List<ExoSocialActivity> getNewerComments(final ExoSocialActivity existingActivity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.NEWER_COMMENTS);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getNewerComments(existingActivity, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public List<ExoSocialActivity> getOlderComments(final ExoSocialActivity existingActivity, final Long sinceTime, final int limit) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.OLDER_COMMENTS);
    ListActivitiesKey listKey = new ListActivitiesKey(key, 0, limit);

    ListActivitiesData keys = activitiesCache.get(new ServiceContext<ListActivitiesData>() {
      public ListActivitiesData execute() {
        List<ExoSocialActivity> got = storage.getOlderComments(existingActivity, sinceTime, limit);
        return buildIds(got);
      }
    }, listKey);
    
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfNewerComments(final ExoSocialActivity existingActivity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.NEWER_COMMENTS);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfNewerComments(existingActivity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public int getNumberOfOlderComments(final ExoSocialActivity existingActivity, final Long sinceTime) {
    ActivityCountKey key = new ActivityCountKey(new ActivityKey(existingActivity.getId()), sinceTime, ActivityType.OLDER_COMMENTS);

    return activitiesCountCache.get(new ServiceContext<IntegerData>() {
      public IntegerData execute() {
        return new IntegerData(storage.getNumberOfOlderComments(existingActivity, sinceTime));
      }
    }, key).build();
  }

  @Override
  public List<ExoSocialActivity> getUserActivitiesForUpgrade(final Identity owner, final long offset, final long limit) throws ActivityStorageException {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getUserActivitiesForUpgrade(owner, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfUserActivitiesForUpgrade(final Identity owner) throws ActivityStorageException {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(owner), ActivityType.USER_FOR_UPGRADE);

    //
    IntegerData countData =  activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfUserActivitiesForUpgrade(owner));
          }
        },
        key);
    
    //
    ActivityCountKey keyUser =
        new ActivityCountKey(new IdentityKey(owner), ActivityType.USER);
    exoActivitiesCountCache.put(keyUser, countData);
    
    //
    return countData.build();
    
  }

  @Override
  public List<ExoSocialActivity> getActivityFeedForUpgrade(final Identity ownerIdentity,
                                                           final int offset,
                                                           final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getActivityFeedForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfActivitesOnActivityFeedForUpgrade(final Identity ownerIdentity) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED_FOR_UPGRADE);
    
    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfActivitesOnActivityFeedForUpgrade(ownerIdentity));
          }
        },
        key);
        
    //
    ActivityCountKey keyFeed =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.FEED);
    exoActivitiesCountCache.put(keyFeed, countData);
    
    //
    return countData.build();
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnectionsForUpgrade(final Identity ownerIdentity,
                                                                      final int offset,
                                                                      final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getActivitiesOfConnectionsForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfActivitiesOfConnectionsForUpgrade(final Identity ownerIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION_FOR_UPGRADE);

    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfActivitiesOfConnectionsForUpgrade(ownerIdentity));
          }
        },
        key);
    
    //
    ActivityCountKey keyConnection =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.CONNECTION);
    exoActivitiesCountCache.put(keyConnection, countData);
    
    //
    return countData.build();
  }

  @Override
  public List<ExoSocialActivity> getUserSpacesActivitiesForUpgrade(final Identity ownerIdentity,
                                                                   final int offset,
                                                                   final int limit) {
    //
    ActivityCountKey key = new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);
    ListActivitiesKey listKey = new ListActivitiesKey(key, offset, limit);

    //
    ListActivitiesData keys = activitiesCache.get(
        new ServiceContext<ListActivitiesData>() {
          public ListActivitiesData execute() {
            List<ExoSocialActivity> got = storage.getUserSpacesActivitiesForUpgrade(ownerIdentity, offset, limit);
            return buildIds(got);
          }
        },
        listKey);

    //
    return buildActivities(keys);
  }

  @Override
  public int getNumberOfUserSpacesActivitiesForUpgrade(final Identity ownerIdentity) {
    //
    ActivityCountKey key =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES_FOR_UPGRADE);

    //
    IntegerData countData = activitiesCountCache.get(
        new ServiceContext<IntegerData>() {
          public IntegerData execute() {
            return new IntegerData(storage.getNumberOfUserSpacesActivitiesForUpgrade(ownerIdentity));
          }
        },
        key);
    
    ActivityCountKey keySpaces =
        new ActivityCountKey(new IdentityKey(ownerIdentity), ActivityType.SPACES);
    exoActivitiesCountCache.put(keySpaces, countData);
    
    return countData.build();
  }

  @Override
  public void setInjectStreams(boolean mustInject) {
    storage.setInjectStreams(mustInject);
    
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
          //
          for (Map.Entry<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>> e : map.entrySet()) {
            PersisterInvoker.persist(e.getKey(), e.getValue());
          }
        } finally {
          logWatch.stop();
          LOG.info(changes.size() + " streams affected, consumed time: " + logWatch.getElapsedTime() + "ms");
        }
                
      }
    }
  }
  
  /**
   * Gets the scheduler
   * @return
   */
  public PersisterScheduler getScheduler() {
    return this.persisterScheduler;
  }
  
}
