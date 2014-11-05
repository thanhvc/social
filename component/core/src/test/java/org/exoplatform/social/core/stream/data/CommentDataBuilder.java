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
import java.util.Collections;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 21, 2014  
 */
public class CommentDataBuilder {
  private final Log LOG = ExoLogger.getLogger(CommentDataBuilder.class);
  private String title;
  private Identity commenter;
  private int numberOfActivity;
  private ExoSocialActivity parent;
  
  public static CommentDataBuilder initOne(ExoSocialActivity parent, Identity commenter) {
    return initMore(1, parent, commenter);
  }
  
  public static CommentDataBuilder initOne(ExoSocialActivity parent, String title, Identity commenter) {
    return initMore(1, parent ,title, commenter);
  }
  
  public static CommentDataBuilder initMore(int number, ExoSocialActivity parent, Identity commenter) {
    return initMore(number, parent, null, commenter);
  }
  
  public static CommentDataBuilder initMore(int number, ExoSocialActivity parent, String title, Identity commenter) {
    return new Builder().parent(parent).title(title).commenter(commenter).number(number).build();
  }
  
  public CommentDataBuilder(Builder builder) {
    this.title = builder.title;
    this.commenter = builder.commenter;
    this.numberOfActivity = builder.numberOfComment;
    this.parent = builder.activity;
  }
  
  public List<ExoSocialActivity> inject() {
    if (this.commenter == null) {
      return Collections.emptyList();
    }
    
    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>(numberOfActivity);
    for (int i = 0; i < numberOfActivity; i++) {
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      if (this.title == null) {
        comment.setTitle(commenter.getRemoteId() + " comment " + i);
      } else {
        comment.setTitle(this.title);
      }
      
      comment.isComment(true);
      comment.setUserId(this.commenter.getId());
      comment.setParentId(parent.getId());
      
      try {
        CachedActivityData.saveComment(commenter.getId(), comment);
        list.add(comment);
      } catch (Exception e) {
        LOG.error("can not save comment.", e);
      }
    }
    
    return list;
  }
  
  public ExoSocialActivity injectOne() {
    if (this.commenter == null) {
      return null;
    }

    ExoSocialActivity comment = new ExoSocialActivityImpl();
    if (this.title == null) {
      comment.setTitle(commenter.getRemoteId() + " comment ");
    } else {
      comment.setTitle(this.title);
    }
    
    comment.isComment(true);
    comment.setUserId(this.commenter.getId());
    
    comment.setParentId(parent.getId());
    
    try {
      CachedActivityData.saveComment(commenter.getId(), comment);
    } catch (Exception e) {
      LOG.error("can not save comment.", e);
    }

    return comment;
  }
  
  public ExoSocialActivity createOne() {
    ExoSocialActivity comment = new ExoSocialActivityImpl();
    if (this.title == null) {
      comment.setTitle(commenter.getRemoteId() + " comment ");
    } else {
      comment.setTitle(this.title);
    }
    
    comment.isComment(true);
    comment.setUserId(this.commenter.getId());

    return comment;
  }
  
  public static class Builder {
    public String title = null;
    public Identity commenter;
    public int numberOfComment = 1;
    public ExoSocialActivity activity;
    
    public Builder title(String title) {
      this.title = title;
      return this;
    }
    
    public Builder commenter(Identity poster) {
      this.commenter = poster;
      return this;
    }
    
    public Builder number(int number) {
      this.numberOfComment = number;
      return this;
    }
    
    public Builder parent(ExoSocialActivity parent) {
      this.activity = parent;
      return this;
    }
    
    public CommentDataBuilder build() {
      return new CommentDataBuilder(this);
    }
    
  }
}
