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

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public interface StreamChange<K, V> {
  
  /**
   * Gets the stream key
   * @return
   */
   K getKey();
  
  /**
   * Gets the data what wrapper by the change
   * @return
   */
  V getHandle();
  
  /**
   * Gets the key what wrapper by the change
   * @return
   */
  String getOwnerId();
  
  /**
   * Gets kind of change
   * @return
   */
  Kind getKind();
  
  /**
   * Gets revision of change.
   * Determines 2 elements have the same priority, 
   *     base on revision which one is higher priority
   * @return the revision
   */
  long getRevision();
  
  /**
   * Sets the new revision
   * 
   * @param newRevision
   */
  void setRevision(long newRevision);
  
  /**
   * This enumeration lists the known kinds of modifications that can be made to an element.
   * 
   */
  enum Kind {
    /**
     * An addition: the element has been added to the list.
     */
    ADD(5),
    /**
     * A deletion: the element has been removed from the list.
     */
    DELETE(10),

    /**
     * A move: the element has been moved in the list.
     */
    MOVE(1);
    
    private final int priority;
    
    private Kind(int priority) {
      this.priority = priority;
    }
    
    /**
     * Gets the priority of change
     * @return
     */
    public int getPriority() {
      return this.priority;
    }
  }
}
