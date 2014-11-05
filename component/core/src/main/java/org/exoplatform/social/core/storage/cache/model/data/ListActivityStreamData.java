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
package org.exoplatform.social.core.storage.cache.model.data;

import java.util.List;

import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.StreamChange;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class ListActivityStreamData extends AbstractStreamListData<StreamKey, String> {
  public ListActivityStreamData(StreamKey streamKey, List<String> list) {
    super(streamKey, list);
  }
  
  public ListActivityStreamData(StreamKey streamKey) {
    super(streamKey);
  }
  
  
  protected void addChange(StreamChange.Kind kind, String value, String ownerId) {
    DataChangeMerger.merge(kind, this.key, value, ownerId);
  }

}
