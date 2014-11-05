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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
public class ActivityDataBuilder {
  private final Log LOG = ExoLogger.getLogger(ActivityDataBuilder.class);
  private String title;
  private Identity poster;
  private Identity streamOwner;
  private int numberOfActivity;
  
  public static ActivityDataBuilder initOne(Identity poster) {
    return initMore(1, null, poster, null);
  }
  
  public static ActivityDataBuilder initOne(String title, Identity poster) {
    return initMore(1, title, poster, null);
  }
  
  public static ActivityDataBuilder initMore(int number, Identity poster) {
    return initMore(number, null, poster, null);
  }
  
  public static ActivityDataBuilder initMore(int number, String title, Identity poster) {
    return initMore(number, title, poster, null);
  }
  
  public static ActivityDataBuilder initMore(int number, String title, Identity poster, Identity streamOwner) {
    return new Builder().title(title).poster(poster).number(number).build();
  }
  
  public ActivityDataBuilder(Builder builder) {
    this.title = builder.title;
    this.poster = builder.poster;
    this.streamOwner = builder.streamOwner;
    this.numberOfActivity = builder.numberOfActivity;
  }
  
  public List<ExoSocialActivity> inject() {
    if (this.poster == null) {
      return Collections.emptyList();
    }
    
    List<ExoSocialActivity> list = new ArrayList<ExoSocialActivity>(numberOfActivity);
    
    for (int i = 0; i < numberOfActivity; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      if (this.title == null) {
        activity.setTitle(poster.getRemoteId() + " title " + i);
      } else {
        activity.setTitle(this.title);
      }
      
      activity.setUserId(this.poster.getId());
      Map<String, String> templateParams = new LinkedHashMap<String, String>();
      templateParams.put("key1", "value 1");
      templateParams.put("key2", "value 2");
      templateParams.put("key3", "value 3");
      activity.setTemplateParams(templateParams);
      
      try {
        CachedActivityData.saveActivity(this.poster.getId(), activity);
        list.add(activity);
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
    
    return list;
  }
  
  public ExoSocialActivity injectOne() {
    if (this.poster == null) {
      return null;
    }

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    if (this.title == null) {
      activity.setTitle(poster.getRemoteId() + " title ");
    } else {
      activity.setTitle(this.title);
    }

    activity.setUserId(this.poster.getId());
    Map<String, String> templateParams = new LinkedHashMap<String, String>();
    templateParams.put("key1", "value 1");
    templateParams.put("key2", "value 2");
    templateParams.put("key3", "value 3");
    activity.setTemplateParams(templateParams);

    try {
      CachedActivityData.saveActivity(this.poster.getId(), activity);
    } catch (Exception e) {
      LOG.error("can not save activity.", e);
    }

    return activity;
  }
  
  public ExoSocialActivity createOne() {
    if (this.poster == null) {
      return null;
    }

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    if (this.title == null) {
      activity.setTitle(poster.getRemoteId() + " title ");
    } else {
      activity.setTitle(this.title);
    }

    activity.setUserId(this.poster.getId());
    Map<String, String> templateParams = new LinkedHashMap<String, String>();
    templateParams.put("key1", "value 1");
    templateParams.put("key2", "value 2");
    templateParams.put("key3", "value 3");
    activity.setTemplateParams(templateParams);

    return activity;
  }
  
  public static class Builder {
    public String title = null;
    public Identity poster;
    public Identity streamOwner;
    public int numberOfActivity = 1;
    
    public Builder title(String title) {
      this.title = title;
      return this;
    }
    
    public Builder poster(Identity poster) {
      this.poster = poster;
      return this;
    }
    
    public Builder streamOwner(Identity streamOwner) {
      this.streamOwner = streamOwner;
      return this;
    }
    
    public Builder number(int number) {
      this.numberOfActivity = number;
      return this;
    }
    
    public ActivityDataBuilder build() {
      return new ActivityDataBuilder(this);
    }
    
  }
}
