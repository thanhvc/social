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
package org.exoplatform.social.core.storage.streams;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.RelationshipStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.storage.cache.model.data.ListActivityStreamData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.impl.StorageUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 31, 2014  
 */
public class StreamHelper {
  /**
   * Gets the RelationshipStorage component
   * @return
   */
  private static RelationshipStorage getRelationshipStorage() {
    return CommonsUtils.getService(RelationshipStorage.class);
  }
  
 
  /**
   * Gets the IdentityStorage component
   * @return
   */
  private static IdentityStorage getIdentityStorage() {
    return CommonsUtils.getService(IdentityStorage.class);
  }
  
  /**
   * Gets the SpaceStorage component
   * @return
   */
  private static SpaceStorage getSpaceStorage() {
    return CommonsUtils.getService(SpaceStorage.class);
  }
  
 
  
  private static boolean isSpaceActivity(String type) {
    return SpaceIdentityProvider.NAME.toString().equals(type);
  }
  
  private static List<Identity> getMemberIdentities(Space space) {
    List<Identity> identities = new ArrayList<Identity>();
    for(String remoteId : space.getMembers()) {
      //improves performance here just load identity data without profile
      identities.add(getIdentityStorage().findIdentity(OrganizationIdentityProvider.NAME, remoteId));
    }
    
    return identities;
  }
  
  /**
   * Using when post new activity
   *
   */
  public static class ADD {
    
    public static void addActivity(ExoSocialActivity activity) {
      String posterId = activity.getPosterId();
      String streamOwnerId = activity.getActivityStream().getId();
      
      boolean isSpaceOwner = isSpaceActivity(activity.getActivityStream().getType().toString());
      
      if (isSpaceOwner) {
        putSpaceMembersAndStreamOwner(streamOwnerId, activity);
      } else {
        putFeedAndUser(posterId, activity);
        putViewer(posterId, activity);
        if (!posterId.equals(streamOwnerId)) {
          putViewer(streamOwnerId, activity);
        }
        putConnections(posterId, activity);
      }
      List<String> mentioners = StorageUtils.getIdentityIds(activity.getMentionedIds());
      //create or move to top for mentioners
      MOVE.createOrMove(mentioners, activity);
    }
    
    public static void addMentioners(ExoSocialActivity activity) {
      List<String> mentioners = StorageUtils.getIdentityIds(activity.getMentionedIds());
      putMentioners(mentioners, activity);
    }

    /**
     * Puts the activity to poster's stream
     * 
     * @param posterId
     * @param activity
     */
    private static void putFeedAndUser(String posterId, ExoSocialActivity activity) {
      putToStream(posterId, activity, ActivityType.FEED);
      putToStream(posterId, activity, ActivityType.USER);
    }
    
    /**
     * Puts the activity to streamOwner's viewer stream
     * 
     * @param posterId
     * @param activity
     */
    private static void putViewer(String streamOwnerId, ExoSocialActivity activity) {
      putToStream(streamOwnerId, activity, ActivityType.VIEWER);
    }
    
    /**
     * Puts the activity ref to mentioners 
     * 
     * @param identityIds
     * @param activity
     */
    private static void putMentioners(List<String> identityIds, ExoSocialActivity activity) {
      if (identityIds == null || identityIds.size() <= 0)
        return;
      
      String posterId = activity.getPosterId();
      String streamOwnerId = activity.getActivityStream().getId();
      identityIds.remove(posterId);
      identityIds.remove(streamOwnerId);

      for (String identityId : identityIds) {
        //handle the case A post B's wall and mention B >> duplicate B's Feed
        if (!identityId.equals(streamOwnerId)) {
          putToStream(identityId, activity, ActivityType.FEED);
        }
        putToStream(identityId, activity, ActivityType.USER);
      }
    }

    /**
     * Puts the activity to poster's connections stream
     * 
     * @param posterId
     * @param activity
     */
    private static void putConnections(String identityId, ExoSocialActivity activity) {
      Identity identity = getIdentityStorage().findIdentityById(identityId);
      List<Identity> connections = getRelationshipStorage().getConnections(identity);

      if (connections == null)
        return;

      for (Identity key : connections) {
        putToStream(key.getId(), activity, ActivityType.FEED);
        putToStream(key.getId(), activity, ActivityType.CONNECTION);
      }
    }
    
    /**
     * Puts the activity to space member
     * 
     * @param posterId
     * @param activity
     */
    private static void putSpaceMembersAndStreamOwner(String spaceOwnerId, ExoSocialActivity activity) {
      putToStream(spaceOwnerId, activity, ActivityType.SPACE);
      
      Identity identity = getIdentityStorage().findIdentityById(spaceOwnerId);
      Space space = getSpaceStorage().getSpaceByPrettyName(identity.getRemoteId());
      
      if (space != null) {
        List<Identity> spaceMembers = getMemberIdentities(space);

        for (Identity key : spaceMembers) {
          putToStream(key.getId(), activity, ActivityType.FEED);
          putToStream(key.getId(), activity, ActivityType.SPACES);
        }
      }
      
    }
    
   
    
    /**
     * Puts the activity into the stream
     * 
     * @param posterId
     * @param activity
     * @param type
     */
    private static void putToStream(String posterId, ExoSocialActivity activity, ActivityType type) {
      StreamKey newKey = StreamKey.init(posterId).key(type);
      ExoCache<StreamKey, ListActivityStreamData> streamCache = StreamContext.getStreamCache();
      
      ListActivityStreamData data = streamCache.get(newKey);
      if (data == null) {
        data = new ListActivityStreamData(newKey);
        streamCache.put(newKey, data);
      }
      
      data.putAtTop(activity.getId(), posterId);
    }
    
    
  }
  
  public static class MOVE {
    
    public static void addComment(String commenterId, ExoSocialActivity activity) {
      String streamOwnerId = activity.getActivityStream().getId();
      
     boolean isSpaceOwner = isSpaceActivity(activity.getActivityStream().getType().toString());
     
      if (isSpaceOwner) {
        moveSpaceMembersAndStreamOwner(streamOwnerId, activity);
      } else {
        Identity commenter = getIdentityStorage().findIdentityById(commenterId);
        Identity streamOwner = getIdentityStorage().findIdentityById(streamOwnerId);
        if (getRelationshipStorage().getRelationship(commenter, streamOwner) == null) {
          movePosterStream(commenterId, activity);
        } else {
          movePosterStream(commenterId, activity);
          moveViewer(commenterId, activity);
          if (!commenterId.equals(streamOwnerId)) {
            moveViewer(streamOwnerId, activity);
          }
          moveConnection(commenterId, activity);
        }
      }
    }
    /**
     * Handles to like the activity
     * @param likerId
     * @param activity
     */
    public static void addLike(String likerId, ExoSocialActivity activity) {
      String posterId = activity.getPosterId();
      String streamOwnerId = activity.getActivityStream().getId();
      
     boolean isSpaceOwner = isSpaceActivity(activity.getActivityStream().getType().toString());
     
      if (isSpaceOwner) {
        moveSpaceMembersAndStreamOwner(streamOwnerId, activity);
      } else {
        Identity liker = getIdentityStorage().findIdentityById(likerId);
        Identity streamOwner = getIdentityStorage().findIdentityById(streamOwnerId);
        if (getRelationshipStorage().getRelationship(liker, streamOwner) == null) {
          movePosterStream(likerId, activity);
        } else {
          movePosterStream(posterId, activity);
          moveViewer(posterId, activity);
          if (!posterId.equals(streamOwnerId)) {
            moveViewer(streamOwnerId, activity);
          }
          moveConnection(posterId, activity);
        }
      }
      
      if (!posterId.equals(likerId)) {
        moveTopStream(posterId, activity, ActivityType.USER);
      }
    }
    
    /**
     * Moves the activity to space member
     * 
     * @param posterId
     * @param activity
     */
    private static void moveSpaceMembersAndStreamOwner(String spaceOwnerId, ExoSocialActivity activity) {
      moveTopStream(spaceOwnerId, activity, ActivityType.SPACE);
      
      Identity identity = getIdentityStorage().findIdentityById(spaceOwnerId);
      Space space = getSpaceStorage().getSpaceByPrettyName(identity.getRemoteId());
      
      if (space != null) {
        List<Identity> spaceMembers = getMemberIdentities(space);

        for (Identity key : spaceMembers) {
          moveTopStream(key.getId(), activity, ActivityType.FEED);
          moveTopStream(key.getId(), activity, ActivityType.SPACE);
        }
      }
      
    }
    
    /**
     * Add or move the activity ref to mentioners 
     * 
     * @param identityIds
     * @param activity
     */
    public static void addOrMoveMentioners(List<String> identityIds, ExoSocialActivity activity) {
      if (identityIds == null || identityIds.size() <= 0)
        return;

      for (String identityId : identityIds) {
        moveTopStream(identityId, activity, ActivityType.FEED);
        moveTopStream(identityId, activity, ActivityType.USER);
      }
    }
    
    /**
     * Moves the activity to poster's stream
     * 
     * @param posterId
     * @param activity
     */
    private static void movePosterStream(String posterId, ExoSocialActivity activity) {
      moveTopStream(posterId, activity, ActivityType.FEED);
      moveTopStream(posterId, activity, ActivityType.USER);
    }
    
    /**
     * Moves the activity for streamOwner's viewer stream
     * 
     * @param posterId
     * @param activity
     */
    private static void moveViewer(String streamOwnerId, ExoSocialActivity activity) {
      moveTopStream(streamOwnerId, activity, ActivityType.VIEWER);
    }
    
    /**
     * Moves the activity to poster's connections
     * 
     * @param posterId
     * @param activity
     */
    private static void moveConnection(String identityId, ExoSocialActivity activity) {
      Identity got = getIdentityStorage().findIdentityById(identityId);
      List<Identity> connections = getRelationshipStorage().getConnections(got);
      
      if (connections == null) return;
      
      for(Identity identity : connections) {
        moveTopStream(identity.getId(), activity, ActivityType.FEED);
        moveTopStream(identity.getId(), activity, ActivityType.CONNECTION);
      }
    }
    
    /**
     * Puts the activity ref to mentioners
     * 
     * @param identityIds
     * @param activity
     */
    private static void createOrMove(List<String> identityIds, ExoSocialActivity activity) {
      if (identityIds == null || identityIds.size() <= 0)
        return;

      for (String identityId : identityIds) {
        moveTopStream(identityId, activity, ActivityType.FEED);
        moveTopStream(identityId, activity, ActivityType.USER);
      }
    }
    
    /**
     * Moves the activity to at the top of stream for relationship in What's hot functional
     * 
     * @param ownerId
     * @param activity
     * @param type
     */
    private static void moveTopStream(String identityId, ExoSocialActivity activity, ActivityType type) {
      StreamKey newKey = StreamKey.init(identityId).key(type);
      
      ExoCache<StreamKey, ListActivityStreamData> streamCache = StreamContext.getStreamCache();
      
      ListActivityStreamData data = streamCache.get(newKey);
      if (data == null) {
        data = new ListActivityStreamData(newKey);
        streamCache.put(newKey, data);
      }
      
      data.moveTop(activity.getId(), identityId);
    }
  }
  
  public static class REMOVE {
    /**
     * Deleted the activity and updates the stream
     * @param activity
     */
    public static void removeActivity(ExoSocialActivity activity) {
      String posterId = activity.getPosterId();
      String streamOwnerId = activity.getActivityStream().getId();
      
      boolean isSpaceOwner = isSpaceActivity(activity.getActivityStream().getType().toString());
      
      if (isSpaceOwner) {
        removeSpaceMembersAndStreamOwner(streamOwnerId, activity);
      } else {
        removePosterStream(posterId, activity);
        removeViewer(posterId, activity);
        if (!posterId.equals(streamOwnerId)) {
          removeViewer(streamOwnerId, activity);
        }
        removeConnection(posterId, activity);
      }
    }
    
    /**
     * Removes the activity to space member
     * 
     * @param posterId
     * @param activity
     */
    private static void removeSpaceMembersAndStreamOwner(String spaceOwnerId, ExoSocialActivity activity) {
      removeFromStream(spaceOwnerId, activity, ActivityType.SPACE);
      
      Identity identity = getIdentityStorage().findIdentityById(spaceOwnerId);
      Space space = getSpaceStorage().getSpaceByPrettyName(identity.getRemoteId());
      
      if (space != null) {
        List<Identity> spaceMembers = getMemberIdentities(space);

        for (Identity key : spaceMembers) {
          removeFromStream(key.getId(), activity, ActivityType.FEED);
          removeFromStream(key.getId(), activity, ActivityType.SPACE);
        }
      }
      
    }
    /**
     * Updates the stream when the comment has been deleted
     * the changes will be created to synch with activity reference
     * 
     * @param commenterId
     * @param activity
     */
    public static void removeComment(String commenterId, ExoSocialActivity activity) {
      removeFromStream(commenterId, activity, ActivityType.USER);
      removeFromStream(commenterId, activity, ActivityType.VIEWER);
      Identity commenter = getIdentityStorage().findIdentityById(commenterId);
      Identity streamOwner = getIdentityStorage().findIdentityById(activity.getActivityStream().getId());
      //case commenter not connected streamOwner, Feed removes the activity
      if (getRelationshipStorage().getRelationship(commenter, streamOwner) == null) {
        removeFromStream(commenterId, activity, ActivityType.FEED);
      }
    }
    
    /**
     * Removes the activity like
     * 
     * @param likerId
     * @param activity
     */
    public static void removeLike(String likerId, ExoSocialActivity activity) {
      removeFromStream(likerId, activity, ActivityType.USER);
      removeFromStream(likerId, activity, ActivityType.VIEWER);
      Identity commenter = getIdentityStorage().findIdentityById(likerId);
      Identity streamOwner = getIdentityStorage().findIdentityById(activity.getActivityStream().getId());
      //case commenter not connected streamOwner, Feed removes the activity
      if (getRelationshipStorage().getRelationship(commenter, streamOwner) == null) {
        removeFromStream(likerId, activity, ActivityType.FEED);
      }
    }
    
    
    /**
     * Add or move the activity ref to mentioners 
     * 
     * @param identityIds
     * @param activity
     */
    public static void removeMentioners(List<String> identityIds, ExoSocialActivity activity) {
      if (identityIds == null || identityIds.size() <= 0)
        return;
      //
      String posterId = activity.getPosterId();
      Identity poster = getIdentityStorage().findIdentityById(posterId);

      for (String identityId : identityIds) {
        removeFromStream(identityId, activity, ActivityType.USER);
        Identity removedIdentity = getIdentityStorage().findIdentityById(identityId);
        //just remove feed when don't have relationshi with poster
        if (getRelationshipStorage().getRelationship(removedIdentity, poster) == null) {
          removeFromStream(identityId, activity, ActivityType.FEED);
        }
      }
    }
    
    /**
     * Removes the activity to poster's stream
     * 
     * @param posterId
     * @param activity
     */
    private static void removePosterStream(String posterId, ExoSocialActivity activity) {
      removeFromStream(posterId, activity, ActivityType.FEED);
      removeFromStream(posterId, activity, ActivityType.USER);
    }
    
    /**
     * Removes the activity for streamOwner's viewer stream
     * 
     * @param posterId
     * @param activity
     */
    private static void removeViewer(String streamOwnerId, ExoSocialActivity activity) {
      removeFromStream(streamOwnerId, activity, ActivityType.VIEWER);
    }
    
    /**
     * Removes the activity to poster's connections stream
     * 
     * @param posterId
     * @param activity
     */
    private static void removeConnection(String posterId, ExoSocialActivity activity) {
      Identity got = getIdentityStorage().findIdentityById(posterId);
      List<Identity> connections = getRelationshipStorage().getConnections(got);
      
      if (connections == null) return;
      
      for(Identity identity : connections) {
        removeFromStream(identity.getId(), activity, ActivityType.FEED);
        removeFromStream(identity.getId(), activity, ActivityType.CONNECTION);
      }
    }
    
    /**
     * Puts the activity into the stream
     * 
     * @param posterId
     * @param activity
     * @param type
     */
    private static void removeFromStream(String identityId, ExoSocialActivity activity, ActivityType type) {
      StreamKey newKey = StreamKey.init(identityId).key(type);
      
      ExoCache<StreamKey, ListActivityStreamData> streamCache = StreamContext.getStreamCache();
      
      ListActivityStreamData data = streamCache.get(newKey);
      if (data == null) return;
      
      data.remove(activity.getId(), identityId);
    }
    
   
    
    
  }
  
  public static class CACHED {
    /**
     * clear caching when removes the connection
     * @param streamOwner
     */
    public static void clearConnection(String identityId1, String identityId2) {
      clearStream(identityId1, ActivityType.FEED);
      clearStream(identityId1, ActivityType.CONNECTION);
      clearStream(identityId1, ActivityType.VIEWER);
      clearStream(identityId2, ActivityType.FEED);
      clearStream(identityId2, ActivityType.CONNECTION);
      clearStream(identityId2, ActivityType.VIEWER);
      //
      StreamContext.clearConnectionCountCache(identityId1);
      StreamContext.clearConnectionCountCache(identityId2);
    }
    
    /**
     * clear caching when hidden/unhidden the activity
     * @param posterId
     */
    public static void clearPoster(String posterId) {
      clearStream(posterId, ActivityType.FEED);
      clearStream(posterId, ActivityType.USER);
      clearStream(posterId, ActivityType.VIEWER);
    }
    
    /**
     * clear caching when hidden/unhidden the activity of poster's connection
     * @param posterId
     */
    public static void clearPosterConnections(String posterId) {
      clearStream(posterId, ActivityType.FEED);
      clearStream(posterId, ActivityType.USER);
      clearStream(posterId, ActivityType.VIEWER);
    }
    
    /**
     * clear caching when removes the connection
     * @param streamOwner
     */
    public static void clearSpace(String removedSpaceMemberId) {
      clearStream(removedSpaceMemberId, ActivityType.FEED);
      clearStream(removedSpaceMemberId, ActivityType.SPACE);
    }
    
    private static void clearStream(String identityId, ActivityType type) {
      StreamKey newKey = StreamKey.init(identityId).key(type);
      
      ExoCache<StreamKey, ListActivityStreamData> streamCache = StreamContext.getStreamCache();
      
      ListActivityStreamData data = streamCache.get(newKey);
      if (data == null) return;
      
      data.clear();
      
      StreamContext.clearMySpacesCountCache(identityId);
    }
  }
}
