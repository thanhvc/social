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

import org.exoplatform.social.core.storage.streams.listener.DataChangeListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class DataChange<M> {
  
  final M target;
  
  public DataChange(M target) throws IllegalArgumentException {
    if (target == null) {
      throw new IllegalArgumentException("The target must not be null.");
    }
    
    this.target = target;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataChange)) {
      return false;
    }

    DataChange<?> that = (DataChange<?>) o;

    if (target != null ? !target.equals(that.target) : that.target != null) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public String toString() {
    return target.toString();
  }
  
  @Override
  public int hashCode() {
    return target.hashCode();
  }
  
  /**
   * Dispatch the data change to the listener for handling
   * 
   * @param listener the listener handling
   */
  public void dispatch(DataChangeListener<M> listener) {}
  
  public static final class Add<M> extends DataChange<M> {
    
    public Add(M model) {
      super(model);
    }

    @Override
    public void dispatch(DataChangeListener<M> listener) {
      listener.onAdd(this.target);
    }
    
    @Override
    public String toString() {
      return "DataChange.Add[target:" + target + "]";
    }
    
  }
  
  public static final class Delete<M> extends DataChange<M> {

    public Delete(M model) {
      super(model);
    }

    @Override
    public void dispatch(DataChangeListener<M> listener) {
      listener.onDelete(this.target);
    }

    @Override
    public String toString() {
      return "DataChange.Delete[target: " + target + " ]";
    }

  }
  
  public static final class Update<M> extends DataChange<M> {

    public Update(M model) {
      super(model);
    }

    @Override
    public void dispatch(DataChangeListener<M> listener) {
      listener.onUpdate(this.target);
    }

    @Override
    public String toString() {
      return "DataChange.Update[target:" + target + "]";
    }
  }
}
