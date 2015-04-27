/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;

/**
 * Immutable activity data.
 *
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public class ActivityData implements CacheData<ExoSocialActivity> {

  
  public final static ActivityData NULL = new ActivityData(new ExoSocialActivityImpl());
  
  private final String id;
  private final String title;
  private final String body;
  private final List<String> likes;
  private final boolean isComment;
  private final boolean isHidden;
  private final boolean isLocked;
  private final Long postedTime;
  private final Long lastUpdated;
  private final List<String> replyIds;
  private final String userId;
  private final String appId;
  private final String titleId;
  private final String bodyId;
  private final String type;
  private final Map templateParams;
  private final String externalId;
  private final String url;
  private final String streamId;
  private final String streamOwner;
  private final String streamFaviconUrl;
  private final String streamSourceUrl;
  private final String streamTitle;
  private final String streamUrl;
  private final List<String> mentioners;
  private final List<String> commenters;
  private final ActivityStream.Type streamType;
  private final String posterId;
  private final String parentId;

  public ActivityData(final ExoSocialActivity activity) {

    this.id = activity.getId();
    this.title = activity.getTitle();
    this.body = activity.getBody();
    this.likes = activity.getLikeIdentityIds() != null && activity.getLikeIdentityIds().length > 0 ? Arrays.asList(activity.getLikeIdentityIds()) : null;
    this.isComment = activity.isComment();
    this.isHidden = activity.isHidden();
    this.isLocked = activity.isLocked();
    this.postedTime = activity.getPostedTime();
    this.lastUpdated = activity.getUpdated().getTime();
    this.replyIds = activity.getReplyToId() != null && activity.getReplyToId().length > 0 ? Arrays.asList(activity.getReplyToId()) : null;
    this.userId = activity.getUserId();
    this.appId = activity.getAppId();
    this.titleId = activity.getTitleId();
    this.bodyId = activity.getBodyId();
    this.type = activity.getType();
    this.externalId = activity.getExternalId();
    this.url = activity.getUrl();
    this.streamId = activity.getStreamId();
    this.streamOwner = activity.getStreamOwner();
    this.streamFaviconUrl = activity.getStreamFaviconUrl();
    this.streamSourceUrl = activity.getStreamSourceUrl();
    this.streamTitle = activity.getStreamTitle();
    this.streamUrl = activity.getStreamUrl();
    this.mentioners =  activity.getMentionedIds() != null && activity.getMentionedIds().length > 0 ? Arrays.asList(activity.getMentionedIds()) : null;
    this.commenters =  activity.getCommentedIds() != null && activity.getCommentedIds().length > 0 ? Arrays.asList(activity.getCommentedIds()) : null;
    this.streamType = activity.getActivityStream().getType();
    this.posterId = activity.getPosterId();
    this.parentId = activity.getParentId();

    if (activity.getTemplateParams() != null) {
      this.templateParams = activity.getTemplateParams();
    }
    else {
      this.templateParams = null;
    }

  }

  public ExoSocialActivity build() {

    //
    if (this == NULL) {
      return null;
    }
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setId(id);
    activity.setTitle(title);
    activity.setBody(body);
    if (likes != null) { 
      activity.setLikeIdentityIds(likes.toArray(new String[0])); 
    }
    if (replyIds != null) {
      activity.setReplyToId(replyIds.toArray(new String[0]));
    } else {
      activity.setReplyToId(new String[0]);
    }
    activity.isComment(isComment);
    activity.isHidden(isHidden);
    activity.isLocked(isLocked);
    activity.setPostedTime(postedTime);
    activity.setUpdated(new Date(lastUpdated));
    activity.setUserId(userId);
    activity.setAppId(appId);
    activity.setTitleId(titleId);
    activity.setBodyId(bodyId);
    activity.setType(type);
    if (templateParams != null) activity.setTemplateParams(new LinkedHashMap<String, String>(templateParams));
    activity.setExternalId(externalId);
    activity.setUrl(url);
    if (mentioners != null) {
      activity.setMentionedIds(mentioners.toArray(new String[0]));
    }
    if (commenters != null) {
      activity.setCommentedIds(commenters.toArray(new String[0]));
    }
    activity.setPosterId(posterId);
    activity.setParentId(parentId);

    ActivityStream activityStream = activity.getActivityStream();
    activityStream.setId(streamId);
    activityStream.setPrettyId(streamOwner);
    activityStream.setFaviconUrl(streamFaviconUrl);
    activityStream.setPermaLink(streamSourceUrl);
    activityStream.setTitle(streamTitle);
    activityStream.setType(streamType);

    activity.setActivityStream(activityStream);

    return activity;

  }

  public String getUserId() {
    return userId;
  }

  public String getStreamOwner() {
    return streamOwner;
  }
}
