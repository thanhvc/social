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

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.List;

import org.exoplatform.social.core.storage.impl.StorageUtils;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public final class DataContext<M> {
  /** */
  private DataChangeQueue<M> changes;
  
  /**
   * Constructor the DataContext to update 
   * the data model what hold by caching
   * @param listener
   */
  public DataContext() {
  }
  
  public void reset() {
    if (changes != null) {
      this.changes.clear();
    }
  }
  
  public boolean hasChanges() {
    return changes != null && changes.size() > 0;
  }
  
  public boolean contains(M model) {
    if (changes == null) return false;
    
    int index = indexOf(model);
    return index > -1;
  }
  
  public int indexOf(M model) {
    if (changes == null) return -1;
    return this.changes.indexOf(model);
  }
  
  public M get(int index) {
    if (changes == null) return null;
    
    SoftReference<DataChange<M>> got = this.changes.get(index);
    return (got != null) ? got.get().target : null; 
  }
  
  public void remove(int index) {
    if (changes == null) return;
    this.changes.remove(index);
  }
  
  public int getChangesSize() {
    return changes != null ? changes.size() : 0;
  }
  
  private void addChange(DataChange<M> change) {
    if (changes == null) {
      changes = new DataChangeQueue<M>();
    }
    //
    changes.addLast(StorageUtils.softReference(change));
  }
  
  /**
   * Adds the new activity
   * @param target
   */
  public void add(M target) {
    addChange(new DataChange.Add<M>(target));
  }
 
  /**
   * Removes the activity
   * @param target
   */
  public void delete(M target) {
    addChange(new DataChange.Delete<M>(target));
  }
  
  /**
   * Updates the model
   * @param target
   */
  public void update(M target) {
    addChange(new DataChange.Update<M>(target));
  }
  
  public DataChangeQueue<M> getChanges() {
    return changes == null ? new DataChangeQueue<M>() : changes;
  }
  
  public List<SoftReference<DataChange<M>>> peekChanges() {
    if (hasChanges()) {
      return changes;
    } else {
      return Collections.emptyList();
    }
  }
  
  public DataChangeQueue<M> popChanges() {
    if (hasChanges()) {
      DataChangeQueue<M> tmp = changes;
      changes = null;
      return tmp;
    } else {
      return null;
    }
  }
  
  public M pop(M target) {
    int pos = changes.indexOf(target);
    if (pos >= 0) {
      DataChange<M> change = changes.get(pos).get();
      if (change != null) {
        M result = change.target;
        changes.remove(pos);
        return result;
      }
    }
    return null;
  }
  
  public void clearAll() {
    if (changes != null) {
      this.changes.clear();
    }
  }

}
