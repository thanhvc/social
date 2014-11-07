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

import java.util.Collections;
import java.util.List;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public final class DataContext<M> {
  /** */
  private DataChangeQueue<M> changes = new DataChangeQueue<M>();
  
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
    return changes.size() > 0;
  }
  
  public boolean contains(M model) {
    if (changes == null) return false;
    DataChange<M> change = new DataChange<M>(model);
    return changes.contains(change);
  }
  
  public int indexOf(M model) {
    if (changes == null) return -1;
    DataChange<M> change = new DataChange<M>(model);
    return this.changes.indexOf(change);
  }
  
  public M get(int index) {
    if (changes == null) return null;
    
    DataChange<M> got = this.changes.get(index);
    return (got != null) ? got.target : null; 
  }
  
  public void remove(int index) {
    if (changes == null) return;
    this.changes.remove(index);
  }
  
  public int getChangesSize() {
    return changes.size();
  }
  
  private void addChange(DataChange<M> change) {
    changes.addLast(change);
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
    DataChange.Update<M> update = new DataChange.Update<M>(target);
    if (changes.indexOf(update) == -1) {
      addChange(new DataChange.Update<M>(target));
    }
  }
  
  public DataChangeQueue<M> getChanges() {
    return changes;
  }
  
  public List<DataChange<M>> peekChanges() {
    if (hasChanges()) {
      return changes;
    } else {
      return Collections.emptyList();
    }
  }
  
  public DataChangeQueue<M> popChanges() {
    if (hasChanges()) {
      DataChangeQueue<M> tmp = changes;
      changes = new DataChangeQueue<M>();
      return tmp;
    } else {
      return null;
    }
  }
  
  public M pop(M target) {
    int pos = changes.indexOf(target);
    if (pos >= 0) {
      DataChange<M> change = changes.get(pos);
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
