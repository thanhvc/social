/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.social.notification.plugin;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvc@exoplatform.com
 * Aug 27, 2013  
 */
public class AvatarUpdatePlugin extends AbstractNotificationPlugin {
  public static final String ID = "AvatarUpdatePlugin";
  
  public AvatarUpdatePlugin(InitParams initParams) {
    super(initParams);
  }

  

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    try {
      Profile profile = ctx.value(SocialNotificationUtils.PROFILE);
      String remoteId = profile.getIdentity().getRemoteId();
      RelationshipManager manager = Utils.getService(RelationshipManager.class);
      ListAccess<Identity> listAccess = manager.getAllWithListAccess(profile.getIdentity());
      List<String> receivers = new ArrayList<String>();
      Identity[] identities = listAccess.load(0, 100);
      for(Identity id : identities) {
        receivers.add(id.getId());
      }
      
      //
      return NotificationInfo.instance()
             .to(receivers)
             .with(SocialNotificationUtils.REMOTE_ID.getKey(), remoteId)
             .key(getId());
    } catch (Exception e) {
      return null;
    }
    
    
  }

  @Override
  protected MessageInfo makeMessage(NotificationContext ctx) {
MessageInfo messageInfo = new MessageInfo();
    
    NotificationInfo notification = ctx.getNotificationInfo();
    
    String language = getLanguage(notification);
    TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);

    String remoteId = notification.getValueOwnerParameter(SocialNotificationUtils.REMOTE_ID.getKey());
    String toUser = notification.getTo();
    SocialNotificationUtils.addFooterAndFirstName(toUser, templateContext);
    Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
    Profile userProfile = identity.getProfile();
    
    templateContext.put("PORTAL_NAME", System.getProperty("exo.notifications.portalname", "eXo"));
    templateContext.put("USER", userProfile.getFullName());
    String subject = TemplateUtils.processSubject(templateContext);
    
    templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
    templateContext.put("AVATAR", LinkProviderUtils.getUserAvatarUrl(userProfile));
    String body = TemplateUtils.processGroovy(templateContext);

    return messageInfo.subject(subject).body(body).end();
  }

  @Override
  protected boolean makeDigest(NotificationContext ctx, Writer writer) {
    List<NotificationInfo> notifications = ctx.getNotificationInfos();
    NotificationInfo first = notifications.get(0);

    String language = getLanguage(first);
    TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
    
    int count = notifications.size();
    String[] keys = {"USER", "USER_LIST", "LAST3_USERS"};
    String key = "";
    StringBuilder value = new StringBuilder();

    try {
      for (int i = 0; i < count && i < 3; i++) {
        Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notifications.get(i).getValueOwnerParameter(SocialNotificationUtils.REMOTE_ID.getKey()), true);
        if (i > 1 && count == 3) {
          key = keys[i - 1];
        } else {
          key = keys[i];
        }
        value.append(SocialNotificationUtils.buildRedirecUrl("user", identity.getRemoteId(), identity.getProfile().getFullName()));
        if (count > (i + 1) && i < 2) {
          value.append(", ");
        }
      }
      templateContext.put(key, value.toString());
      if(count > 3) {
        templateContext.put("COUNT", SocialNotificationUtils.buildRedirecUrl("user_update_avatar", first.getTo(), String.valueOf((count - 3))));
      }
      
      String digester = TemplateUtils.processDigest(templateContext.digestType(count).end());
      writer.append(digester);
    } catch (IOException e) {
      ctx.setException(e);
      return false;
    }
    
    return true;
  }

}
