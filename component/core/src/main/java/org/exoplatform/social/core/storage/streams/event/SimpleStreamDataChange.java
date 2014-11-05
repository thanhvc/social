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
public class SimpleStreamDataChange<K, V> implements StreamChange<K, V> {
  
  private final K streamKey;
  
  private final V handle;
  
  private final String ownerId;
  
  private final Kind kind;
  
  private long revision; 
  
  public static <K, V> Builder<K, V> create(Kind kind, K streamKey, V handle, String ownerId) {
    return new Builder<K, V>(kind, streamKey, handle, ownerId);
  }
  
  public SimpleStreamDataChange(Builder<K, V> builder) {
    this.streamKey = builder.streamKey;
    this.kind = builder.kind;
    this.handle = builder.id;
    this.ownerId = builder.ownerId;
    this.revision = builder.revision;
  }

  public K getKey() {
    return this.streamKey;
  }
  
  public V getHandle() {
    return this.handle;
  }
  
  public String getOwnerId() {
    return this.ownerId;
  }
  
  public StreamChange.Kind getKind() {
    return this.kind;
  }
  
  public long getRevision() {
    return this.revision;
  }
  
  public void setRevision(long newRevision) {
    this.revision = newRevision;
  }
  
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimpleStreamDataChange)) {
      return false;
    }

    SimpleStreamDataChange<?, ?> that = (SimpleStreamDataChange<?, ?>) o;

    if (handle != null ? !handle.equals(that.handle) : that.handle != null) {
      return false;
    }
    
    if (kind != null ? !kind.equals(that.kind) : that.kind != null) {
      return false;
    }
    
    if (streamKey != null ? !streamKey.equals(that.streamKey) : that.streamKey != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = 31 * (handle != null ? handle.hashCode() : 0);
    result = 31 * (kind != null ? kind.hashCode() : 0); 
    return result;
  }
  
  @Override
  public String toString() {
    return "SimpleStreamDataChange[" + streamKey.toString() + "id = " + this.handle + ", kind = "
        + this.kind.toString() + ", revision = " + this.revision + "]";
  }
  
  public static class Builder<K, V> {
    public final K streamKey;
    
    public final V id;
    
    public final String ownerId;
    
    public final Kind kind;
    
    public long revision; 
    
    public Builder(Kind kind, K streamKey, V id, String ownerId) {
      this.streamKey = streamKey;
      this.kind = kind;
      this.id = id;
      this.ownerId = ownerId;
      this.revision = System.currentTimeMillis();
    }
    
    public Builder<K, V> revision(long revision) {
      this.revision = revision;
      return this;
    }
    
    public StreamChange<K, V> build() {
      return new SimpleStreamDataChange<K, V>(this);
    }
  }
  
}
