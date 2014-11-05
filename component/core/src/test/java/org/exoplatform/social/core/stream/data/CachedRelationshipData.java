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

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.storage.cache.model.key.IdentityKey;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 21, 2014  
 */
public class CachedRelationshipData {

  public final static SimpleCache<IdentityKey, List<IdentityKey>> relationshipCaching = new SimpleCache<IdentityKey, List<IdentityKey>>();
  
  public static void addRelationship(Identity identity1, Identity identity2) {
    IdentityKey key1 = new IdentityKey(identity1);
    List<IdentityKey> relationships1 = relationshipCaching.get(key1);
    if (relationships1 == null) {
      relationships1 = new ArrayList<IdentityKey>();
      
      relationshipCaching.put(key1, relationships1);
    }
    IdentityKey key2 = new IdentityKey(identity2.getId());
    relationships1.add(key2);
    //
    List<IdentityKey> relationships2 = relationshipCaching.get(key2);
    if (relationships2 == null) {
      relationships2 = new ArrayList<IdentityKey>();
      relationshipCaching.put(key2, relationships2);
    }
    relationships2.add(key1);
    
  }
  
  
  public static void removeRelationship(Identity identity1, Identity identity2) {
    IdentityKey key1 = new IdentityKey(identity1);
    IdentityKey key2 = new IdentityKey(identity1);
    List<IdentityKey> relationships1 = relationshipCaching.get(key1);
    if (relationships1 != null) {
      relationships1.remove(key2);
    }
    
    relationships1.add(key2);
    //
    List<IdentityKey> relationships2 = relationshipCaching.get(key2);
    if (relationships2 != null) {
      relationships1.remove(key1);
    }
  }
  
  
  public static List<IdentityKey> getConnections(Identity identity) {
    IdentityKey key = new IdentityKey(identity.getId());
    return relationshipCaching.get(key);
  }
  
  public static List<IdentityKey> getConnections(String identityId) {
    IdentityKey key = new IdentityKey(identityId);
    return relationshipCaching.get(key);
  }
  
}
