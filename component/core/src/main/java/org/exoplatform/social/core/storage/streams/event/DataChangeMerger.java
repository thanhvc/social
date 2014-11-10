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
package org.exoplatform.social.core.storage.streams.event;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.impl.ActivityStreamStorageImpl.ActivityRefType;
import org.exoplatform.social.core.storage.impl.StorageUtils;
import org.exoplatform.social.core.storage.streams.StreamContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class DataChangeMerger {
  
  private static DataContext<StreamChange<StreamKey, String>> dataContext = new DataContext<StreamChange<StreamKey, String>>();

  public static void reset() {
    dataContext.reset();
  }
  
  public static DataContext<StreamChange<StreamKey, String>> getDataContext() {
    return dataContext;
  }
  
  public static long getSize() {
    return dataContext.getChangesSize();
  }
  
  /**
   * Gets the changes list by DataChange.Kind
   * 
   * @param kind
   * @return
   */
  public static List<StreamChange<StreamKey, String>> getChangeList(final StreamChange.Kind kind) {
    List<StreamChange<StreamKey, String>> filted = new LinkedList<StreamChange<StreamKey, String>>();
    Iterator<DataChange<StreamChange<StreamKey, String>>> it = dataContext.getChanges().iterator();
    while(it.hasNext()) {
      DataChange<StreamChange<StreamKey, String>> got = it.next();
      if (got != null) {
        StreamChange<StreamKey, String> change = got.target;
        if (change.getKind().equals(kind)) {
          filted.add(change);
        }
      }
    }
    return filted;
  }
  
  /**
   * Gets the changes list by DataChange.Kind
   * 
   * @param kind
   * @return
   */
  public static List<StreamChange<StreamKey, String>> getChangeList(final StreamKey key, final StreamChange.Kind kind) {
    List<StreamChange<StreamKey, String>> filted = new LinkedList<StreamChange<StreamKey, String>>();
    Iterator<DataChange<StreamChange<StreamKey, String>>> it = dataContext.getChanges().iterator();
    while(it.hasNext()) {
      DataChange<StreamChange<StreamKey, String>> got = it.next();
      if (got != null) {
        StreamChange<StreamKey, String> change = got.target;
        if (change.getKind().equals(kind) && change.getKey().equals(key)) {
          filted.add(change);
        }
      }
    }
    return filted;
  }
  
  /**
   * Gets the changes list by DataChange.Kind
   * 
   * @param kind
   * @return
   */
  public static List<StreamChange<StreamKey, String>> getChangeList(final StreamKey key) {
    List<StreamChange<StreamKey, String>> filted = new LinkedList<StreamChange<StreamKey, String>>();
    Iterator<DataChange<StreamChange<StreamKey, String>>> it = dataContext.getChanges().iterator();
    while(it.hasNext()) {
      DataChange<StreamChange<StreamKey, String>> got = it.next();
      if (got != null) {
        StreamChange<StreamKey, String> change = got.target;
        if (change.getKey().equals(key)) {
          filted.add(change);
        }
      }
    }
    return filted;
  }
  
  /**
   * The method is optimizing the changes list.
   * The reducing change list makes the storage less stress with a lot of transactions in short time.
   * 
   * For example: In 10s, there are 2 changes occurs during time.
   * 
   * - First: Add action
   * - Second: Move action >> due to add comment.
   * 
   * Poster: Merger makes sure that there is only one ADD action for the given stream.
   * Connecter:Merger makes sure that there is only one ADD_REF action for the given stream.
   * 
   * This means there is saving an action here(reduce 1 action per stream) then making less stress storage.
   * 
   * Case 1: ADD, MOVE, and REMOVE >> removes all of changes for the given activity.
   * Case 2: MOVE, and REMOVE >> only REMOVE action is keeping.
   * 
   * @param kind
   * @param streamKey
   * @param value
   * @param ownerId
   */
  public static void merge(StreamChange.Kind kind, StreamKey streamKey, String id, String ownerId) {
    //2 cases need to consider.
    //+ Adds already happened recently(temporary status),....., then REMOVE is next >> All Changes must be clear.
    //+ Pushed to storage, some MOVE ready, then REMOVE, All changes must be clear, just keep REMOVE
    
    //don't add change for Viewer stream
    if (ActivityType.VIEWER == streamKey.getType()) return;
    
    if (kind == StreamChange.Kind.DELETE) {
      //test case: ListActivityDataTest#testComplex
      StreamChange<StreamKey, String> move = SimpleStreamDataChange.create(StreamChange.Kind.MOVE, streamKey, id, ownerId).build();
      int index = dataContext.indexOf(move);
      if (index >-1) {
        dataContext.remove(index);
      }
      //test case: ListActivityDataTest#testComplex1
      StreamChange<StreamKey, String> add = SimpleStreamDataChange.create(StreamChange.Kind.ADD, streamKey, id, ownerId).build();
      index = dataContext.indexOf(add);
      if (index >-1) {
        dataContext.remove(index);
        StreamContext.decreaseCount(ownerId, streamKey.getType());
        return;//don't add remove
      }
      
    }
    StreamChange<StreamKey, String> change = SimpleStreamDataChange.create(kind, streamKey, id, ownerId).build();
    int indexOfFirstOccurrence = dataContext.indexOf(change);
    boolean isExisting = indexOfFirstOccurrence > -1;
    if (!isExisting) {
      switch (kind) {
      case ADD:
        dataContext.add(SimpleStreamDataChange.create(kind, streamKey, id, ownerId).build());
        StreamContext.increaseCount(ownerId, streamKey.getType());
        break;
      case MOVE:
        dataContext.update(SimpleStreamDataChange.create(kind, streamKey, id, ownerId).build());
        break;
      case DELETE:
        dataContext.delete(SimpleStreamDataChange.create(kind, streamKey, id, ownerId).build());
        StreamContext.decreaseCount(ownerId, streamKey.getType());
        break;
      }
    } else {
      StreamChange<StreamKey, String> existing = dataContext.get(indexOfFirstOccurrence);
      existing.setRevision(change.getRevision());
    }
  }
  
  /**
   * Transform the changes list to map with key = streamKey
   * @param changes
   * @return
   */
  public static Map<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>> transformToMap(DataChangeQueue<StreamChange<StreamKey, String>> changes) {
    Map<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>> map = new HashMap<StreamKey, List<DataChange<StreamChange<StreamKey, String>>>>();
    
    Iterator<DataChange<StreamChange<StreamKey, String>>>  it = changes.iterator();
    while(it.hasNext()) {
      DataChange<StreamChange<StreamKey, String>> change = it.next();
      //
      if (change != null && change.target != null) {
        StreamKey key = change.target.getKey();
        if (key != null) {
          List<DataChange<StreamChange<StreamKey, String>>> list = map.get(key);
          
          if (list == null) {
            list = new LinkedList<DataChange<StreamChange<StreamKey, String>>>();
            map.put(key, list);
          }
          
          list.add(change);
        }
      }
    }
    
    return map;
  }
  
  /**
   * Checks the activity occurs the deleted list keeping in memory
   * 
   * @param owner the identity
   * @param activityId the activityId
   * @param type the stream type
   * @return TRUE/FALSE
   */
  public static boolean isDeleted(Identity owner, String activityId, ActivityRefType type) {
    ActivityType activityType = StorageUtils.translateToActivityType(type);
    if (activityType == null) return true;
    //
    StreamKey streamKey = StreamKey.init(owner.getId()).key(activityType);
    StreamChange<StreamKey, String> deleted = SimpleStreamDataChange.create(StreamChange.Kind.DELETE, streamKey, activityId, owner.getId()).build();
    int index = dataContext.indexOf(deleted);
    
    return index > -1;
  }
}
