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

import java.util.concurrent.CountDownLatch;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public interface PersistAlgorithm {
  
  /** **/
  boolean shoudldPersist(int changedSize);
  
  /**
   * Gets the CountDownLatch lock of persister service
   * 
   * @return
   */
  CountDownLatch getSynchronizationLock();
  
  /**
   * Starts persister component
   */
  void start();
  
  /**
   * Stop persister component
   */
  void stop();

}
