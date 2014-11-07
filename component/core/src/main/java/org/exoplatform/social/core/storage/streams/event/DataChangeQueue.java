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

import java.util.LinkedList;
import java.util.ListIterator;

import org.exoplatform.social.core.storage.streams.listener.DataChangeListener;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
@SuppressWarnings("serial")
public class DataChangeQueue<M> extends LinkedList<DataChange<M>> implements DataChangeListener<M> {

  public DataChangeQueue() {}
  
  /**
   * 1. Implements PersisterListener works with JCR
   * 2. Implements CachingListener works with eXo Caching
   * 
   * @param listener
   */
  public void broadcast(DataChangeListener<M> listener) {
    ListIterator<DataChange<M>> it = this.listIterator();
    while(it.hasNext()) {
      it.next().dispatch(listener);
    }
  }
  
  public void onAdd(M target) {
    DataChange<M> change = new DataChange.Add<M>(target);
    addLast(change);
    
  }

  public void onDelete(M target) {
    DataChange<M> change = new DataChange.Delete<M>(target);
    addLast(change);
  }

  public void onUpdate(M target) {
    DataChange<M> change = new DataChange.Update<M>(target);
    addLast(change);
  }
  
}
