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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.cache.model.data.ActivityData;
import org.exoplatform.social.core.storage.cache.model.data.ListActivityStreamData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityKey;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.IdentityKey;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.streams.event.DataChange;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.DataChangeQueue;
import org.exoplatform.social.core.storage.streams.event.DataContext;
import org.exoplatform.social.core.storage.streams.event.StreamChange;
import org.exoplatform.social.core.storage.streams.persister.Persister;
import org.exoplatform.social.core.storage.streams.persister.PersisterInvoker;
import org.exoplatform.social.core.storage.streams.persister.PersisterScheduler;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 21, 2014  
 */
public class CachedActivityData implements Persister {
  /** */
  private static SimpleCache<StreamKey, ListActivityStreamData> streamCaching = new SimpleCache<StreamKey, ListActivityStreamData>();
  /** */
  private static SimpleCache<ActivityKey, ActivityData> activityCaching = new SimpleCache<ActivityKey, ActivityData>();
  /** */
  private static SimpleCache<ActivityKey, List<ActivityKey>> commentCaching = new SimpleCache<ActivityKey, List<ActivityKey>>();
  /** */
  private final PersisterScheduler persisterScheduler;
  
  public CachedActivityData() {
    this.persisterScheduler = PersisterScheduler.init()
                                                .persister(this)
                                                .wakeup(1000)
                                                .timeUnit(TimeUnit.MILLISECONDS)
                                                .build();
    this.persisterScheduler.start();
  }
  
  /**
   * 
   * @param posterId
   * @param activity
   * @return
   */
  public static ExoSocialActivity saveActivity(String posterId, ExoSocialActivity activity) {
    
    activity.setId(UUID.randomUUID().toString());
    ActivityKey key = new ActivityKey(activity.getId());
    ActivityData data = new ActivityData(activity);
    activityCaching.put(key, data);
    
    putStream(posterId, activity);
    putMyConnectionStream(posterId, activity);
    
    return activity;
  }
  
  /**
   * Removes the given activityId
   * 
   * @param activityId
   */
  public static void removeActivity(String activityId) {
    ActivityKey key = new ActivityKey(activityId);
    ActivityData data = activityCaching.get(key);
    if (data != null) {
      String posterId = data.build().getUserId();
      removeStream(posterId, activityId);
      removeMyConnectionStream(posterId, activityId);
      
      //
      activityCaching.remove(key);
    }
  }
  
 
  
  /**
   * Puts the activity to poster's stream
   * 
   * @param posterId
   * @param activity
   */
  private static void removeStream(String posterId, String activityId) {
    removeFromStream(posterId, activityId, ActivityType.FEED);
    removeFromStream(posterId, activityId, ActivityType.USER);
  }
  
  /**
   * Puts the activity into the stream
   * 
   * @param posterId
   * @param activity
   * @param type
   */
  private static void removeFromStream(String posterId, String activityId, ActivityType type) {
    StreamKey newKey = StreamKey.init(posterId).key(type);
    
    ListActivityStreamData data = streamCaching.get(newKey);
    if (data == null) return;
    
    data.remove(activityId, posterId);
  }
  
  /**
   * Puts the activity to poster's connections stream
   * 
   * @param posterId
   * @param activity
   */
  private static void removeMyConnectionStream(String posterId, String activityId) {
    List<IdentityKey> connections = CachedRelationshipData.getConnections(posterId);
    
    if (connections == null) return;
    
    for(IdentityKey key : connections) {
      removeFromStream(key.getId(), activityId, ActivityType.FEED);
      removeFromStream(key.getId(), activityId, ActivityType.CONNECTION);
    }
  }
  
  /**
   * Posts the new comment into the existing activity.
   * 
   * @param posterId
   * @param comment
   */
  public static void saveComment(String posterId, ExoSocialActivity comment) {
    comment.setId(UUID.randomUUID().toString());
    ActivityKey commentKey = new ActivityKey(comment.getId());
    ActivityData commentData = new ActivityData(comment);
    activityCaching.put(commentKey, commentData);
    //
    ActivityKey parentKey = new ActivityKey(comment.getParentId());
    ActivityData activityData = activityCaching.get(parentKey);
    
    ExoSocialActivity parent = activityData.build();
    
    List<ActivityKey> comments = commentCaching.get(parentKey);
    if (comments == null) {
      comments = new LinkedList<ActivityKey>();
      commentCaching.put(parentKey, comments);
    }
    comments.add(commentKey);
    
    //update what's hot feature.
    String ownerId = parent.getUserId();
    moveTopStream(ownerId, parent);
    moveTopConnectionsStream(ownerId, parent);
  }
  
  /**
   * 
   * @param activityId
   * @return
   */
  public static List<ExoSocialActivity> getComments(String activityId) {
    ActivityKey parentKey = new ActivityKey(activityId);
    List<ActivityKey> comments = commentCaching.get(parentKey);
    if (comments == null) 
      return Collections.emptyList();
    
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    for(ActivityKey commentKey : comments) {
      ActivityData data = activityCaching.get(commentKey);
      if(data != null) {
        result.add(data.build());
      }
    }
    
    return result;
  }
  
  /**
   * Puts the activity to poster's stream
   * 
   * @param ownerId
   * @param activity
   */
  private static void moveTopStream(String ownerId, ExoSocialActivity activity) {
    moveTopStream(ownerId, activity, ActivityType.FEED);
    moveTopStream(ownerId, activity, ActivityType.USER);
  }
  
  /**
   * Moves the activity to at the top of stream
   * 
   * @param ownerId
   * @param activity
   */
  private static void moveTopConnectionsStream(String ownerId, ExoSocialActivity activity) {
    List<IdentityKey> myConnections = CachedRelationshipData.getConnections(ownerId);
    if (myConnections != null) {
      for(IdentityKey key : myConnections) {
        moveTopStream(key.getId(), activity, ActivityType.FEED);
        moveTopStream(key.getId(), activity, ActivityType.CONNECTION);
      }
    }
  }
  
  /**
   * Moves the activity to at the top of stream
   * 
   * @param ownerId
   * @param activity
   * @param type
   */
  private static void moveTopStream(String ownerId, ExoSocialActivity activity, ActivityType type) {
    StreamKey newKey = StreamKey.init(ownerId).key(type);
    
    ListActivityStreamData data = streamCaching.get(newKey);
    if (data == null) {
      data = new ListActivityStreamData(newKey);
      streamCaching.put(newKey, data);
    }
    
    data.moveTop(activity.getId(), ownerId);
  }
  
  
  /**
   * Puts the activity to poster's stream
   * 
   * @param posterId
   * @param activity
   */
  private static void putStream(String posterId, ExoSocialActivity activity) {
    putToStream(posterId, activity, ActivityType.FEED);
    putToStream(posterId, activity, ActivityType.USER);
  }
  
  /**
   * Puts the activity to poster's connections stream
   * 
   * @param posterId
   * @param activity
   */
  private static void putMyConnectionStream(String posterId, ExoSocialActivity activity) {
    List<IdentityKey> connections = CachedRelationshipData.getConnections(posterId);
    
    if (connections == null) return;
    
    for(IdentityKey key : connections) {
      putToStream(key.getId(), activity, ActivityType.FEED);
      putToStream(key.getId(), activity, ActivityType.CONNECTION);
    }
  }

  /**
   * Puts the activity into the stream
   * 
   * @param posterId
   * @param activity
   * @param type
   */
  private static void putToStream(String posterId, ExoSocialActivity activity, ActivityType type) {
    StreamKey newKey = StreamKey.init(posterId).key(type);
    
    ListActivityStreamData data = streamCaching.get(newKey);
    if (data == null) {
      data = new ListActivityStreamData(newKey);
      streamCaching.put(newKey, data);
    }
    
    data.putAtTop(activity.getId(), posterId);
  }
  
  public static ExoSocialActivity getActivityById(String activityId) {
    return activityCaching.get(new ActivityKey(activityId)).build();
  }
  
  /**
   * Gets the feed stream by the given offset and limit.
   * 
   * @param identity
   * @param offset
   * @param limit
   * @return
   */
  public static List<ExoSocialActivity> feed(Identity identity, int offset, int limit) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.FEED);
    ListActivityStreamData data = streamCaching.get(key);
    
    if (data == null) return Collections.emptyList();
    List<String> got = data.subList(offset, offset + limit);
    
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    
    ActivityData activityData = null;
    
    for(String id : got) {
      activityData = activityCaching.get(new ActivityKey(id));
      if (activityData != null) {
        result.add(activityData.build());
      }
    }
    
    return result;
  }
  
  /**
   * Gets the size of feed stream.
   * 
   * @param identity
   * @return
   */
  public static int feedSize(Identity identity) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.FEED);
    ListActivityStreamData data = streamCaching.get(key);
    
    if (data == null) return 0;
    return data.size();
  }
  
  
  /**
   * Gets the iterator of changes list for feed.
   * 
   * @param identity
   * @return
   */
  public static List<StreamChange<StreamKey, String>> feedChangeList(Identity identity) {
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.FEED);
    return DataChangeMerger.getChangeList(key);
  }
  
  /**
   * Gets the iterator of changes list for feed.
   * 
   * @param identity
   * @return
   */
  public static List<StreamChange<StreamKey, String>> feedChangeList(Identity identity, StreamChange.Kind kind) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.FEED);
    return DataChangeMerger.getChangeList(key, kind);
  }
  
  /**
   * Gets the connection stream by the given offset and limit.
   * 
   * @param identity
   * @param offset
   * @param limit
   * @return
   */
  public static List<ExoSocialActivity> connections(Identity identity, int offset, int limit) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.CONNECTION);
    ListActivityStreamData data = streamCaching.get(key);
    
    if (data == null) return Collections.emptyList();
    List<String> got = data.subList(offset, offset + limit);
    
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    
    ActivityData activityData = null;
    
    for(String id : got) {
      activityData = activityCaching.get(new ActivityKey(id));
      if (activityData != null) {
        result.add(activityData.build());
      }
    }
    
    return result;
  }
  
  /**
   * Gets the size of connections stream.
   * 
   * @param identity
   * @return
   */
  public static int connectionsSize(Identity identity) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.CONNECTION);
    ListActivityStreamData data = streamCaching.get(key);
    
    if (data == null) return 0;
    return data.size();
  }
  
  /**
   * Gets the iterator of changes list for feed.
   * 
   * @param identity
   * @return
   */
  public static List<StreamChange<StreamKey, String>> connectionsChangeList(Identity identity) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.CONNECTION);
    return DataChangeMerger.getChangeList(key);
  }
  
  /**
   * Gets the iterator of changes list for connection.
   * 
   * @param identity.
   * 
   * @return
   */
  public static List<StreamChange<StreamKey, String>> connectionsChangeList(Identity identity, StreamChange.Kind kind) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.CONNECTION);
    return DataChangeMerger.getChangeList(key);
  }
  
  /**
   * Gets the my stream by the given offset and limit.
   * 
   * @param identity
   * @param offset
   * @param limit
   * @return
   */
  public static List<ExoSocialActivity> myActivities(Identity identity, int offset, int limit) {
    
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.USER);
    ListActivityStreamData data = streamCaching.get(key);
    
    if (data == null) return Collections.emptyList();
    List<String> got = data.subList(offset, offset + limit);
    
    List<ExoSocialActivity> result = new LinkedList<ExoSocialActivity>();
    
    ActivityData activityData = null;
    
    for(String id : got) {
      activityData = activityCaching.get(new ActivityKey(id));
      if (activityData != null) {
        result.add(activityData.build());
      }
    }
    
    return result;
  }
  
  /**
   * Gets the size of connections stream.
   * 
   * @param identity
   * @return
   */
  public static int myActivitiesSize(Identity identity) {
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.USER);
    ListActivityStreamData data = streamCaching.get(key);
    
    if (data == null) return 0;
    return data.size();
  }
  
  /**
   * Gets the iterator of changes list for user.
   * 
   * @param identity
   * @return
   */
  public static List<StreamChange<StreamKey, String>> myActivitiesChangeList(Identity identity) {
    StreamKey key = StreamKey.init(identity.getId()).key(ActivityType.USER);
    return DataChangeMerger.getChangeList(key);
  }
  
  /**
   * Clear all caching
   */
  public static void reset() {
    streamCaching.clear();
    activityCaching.clear();
    commentCaching.clear();
    DataChangeMerger.reset();
    
  }
  
  public PersisterScheduler getScheduler() {
    return this.persisterScheduler;
  }
  
  public void commit(boolean forceCommit) {
    persistFixedSize(forceCommit);
  }

  private void persistFixedSize(boolean forcePersist) {
    DataContext<StreamChange<StreamKey, String>> context = DataChangeMerger.getDataContext();
    if (context.getChangesSize() >= 200 || forcePersist) {
      DataChangeQueue<StreamChange<StreamKey, String>> changes = context.popChanges();
      
      if (changes != null && changes.size() > 0) {
        Map<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>>  map = DataChangeMerger.transformToMap(changes);
        //
        for(Map.Entry<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>> e : map.entrySet()) {
          PersisterInvoker.persist(e.getKey(), e.getValue());
        }
      }
    }
  }
}
