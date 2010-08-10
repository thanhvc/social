/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.opensocial.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.AnonymousSecurityToken;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.model.FilterOperation;
import org.apache.shindig.protocol.model.SortOrder;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.social.common.jcr.QuerySpec;
import org.exoplatform.social.common.jcr.QuerySpec.Operation;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.opensocial.auth.ExoBlobCrypterSecurityToken;

import com.google.common.collect.Sets;


/**
 * Created by IntelliJ IDEA.
 * User: jeremi
 * Date: Oct 13, 2008
 * Time: 7:58:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExoService {

        /**
         * Get the set of user id's from a user and group.
         *
         * @param user the user
         * @param group the group
         * @param token the token
         * @return the id set
         * @throws Exception the exception
         */
    protected Set<Identity> getIdSet(UserId user, GroupId group, SecurityToken token) throws Exception {

      if(token instanceof AnonymousSecurityToken) {
  		throw new Exception(Integer.toString(HttpServletResponse.SC_FORBIDDEN));
  	  }

      String userId = user.getUserId(token);

      Identity id = getIdentity(userId, token);
      Set<Identity> returnVal = Sets.newLinkedHashSet();

      if (group == null) {
        returnVal.add(id);
      }
      else {
        switch (group.getType()) {
        case all:
        case friends:
        case groupId:
          returnVal.addAll(getFriendsList(id, token));
          break;
        case self:
          returnVal.add(id);
          break;
        }
      }
      return returnVal;
    }

    /**
     * Gets the friends list.
     *
     * @param id the id
     * @return the friends list
     * @throws Exception the exception
     */
    protected List<Identity> getFriendsList(Identity id, SecurityToken token) throws Exception {
      PortalContainer pc = (PortalContainer) getPortalContainer(token);
      RelationshipManager rm = (RelationshipManager) pc.getComponentInstanceOfType(RelationshipManager.class);
      List<Relationship> rels = rm.getContacts(id);
      List<Identity> ids = new ArrayList<Identity>();

      for(Relationship rel : rels) {
        if (rel.getSender().getId().equals(id.getId()))
          ids.add(rel.getReceiver());
        else
          ids.add(rel.getSender());
      }

      return ids;

    }

    /**
     * Get the set of user id's for a set of users and a group.
     *
     * @param users the users
     * @param group the group
     * @param token the token
     * @return the id set
     * @throws Exception the exception
     */
    protected Set<Identity> getIdSet(Set<UserId> users, GroupId group, SecurityToken token)
            throws Exception {

	  if(token instanceof AnonymousSecurityToken) {
		  throw new Exception(Integer.toString(HttpServletResponse.SC_FORBIDDEN));
	  }
      Set<Identity> ids = Sets.newLinkedHashSet();
      for (UserId user : users) {
        ids.addAll(getIdSet(user, group, token));
      }
      return ids;
    }

    protected Identity getIdentity(String id, boolean loadProfile, SecurityToken st) throws Exception {

      PortalContainer pc = (PortalContainer) getPortalContainer(st);
      IdentityManager im = (IdentityManager) pc.getComponentInstanceOfType(IdentityManager.class);

      Identity identity = im.getIdentity(id, loadProfile);

      if(identity == null) throw new Exception("\n\n\n can't find identity \n\n\n");

      return identity;
    }

    /**
     * Gets the identity.
     *
     * @param id the id
     * @return the identity
     * @throws Exception the exception
     */
    protected Identity getIdentity(String id, SecurityToken st) throws Exception {

      return getIdentity(id, false, st);
    }

    protected PortalContainer getPortalContainer(SecurityToken st) {
    	String portalName = PortalContainer.getCurrentPortalContainerName();
    	if(st instanceof ExoBlobCrypterSecurityToken) {
        final ExoBlobCrypterSecurityToken crypterSecurityToken = (ExoBlobCrypterSecurityToken) st;
        final String container = crypterSecurityToken.getPortalContainer();
        portalName = container;    		
    	}
    	PortalContainer portalContainer = (PortalContainer) ExoContainerContext.getContainerByName(portalName);
    	ExoContainerContext.setCurrentContainer(portalContainer);
    	return portalContainer;
    }

    protected String getHost(SecurityToken st) {
    	String host = null;
    	if(st instanceof ExoBlobCrypterSecurityToken) {
    		host = ((ExoBlobCrypterSecurityToken)st).getHostName();
    	}
    	return host;
    }

    protected QuerySpec toQuerySpec(CollectionOptions options) {
      QuerySpec query = new QuerySpec();
      query.setFirst(options.getFirst());
      query.setMax(options.getMax());


      if (options.getFilter() != null) {
        Operation operation = toOperation(options.getFilterOperation());
        if (operation != null) {
        query.addCondition(options.getFilter(), operation, options.getFilterValue());
        }
      }


      if (options.getSortBy() != null) {
        QuerySpec.SortOrder sortOrder = (options.getSortOrder() == SortOrder.ascending) ? QuerySpec.SortOrder.asc : QuerySpec.SortOrder.desc;
        query.addSort(options.getSortBy(), sortOrder);
      }



      if (options.getUpdatedSince() != null) {
        query.addCondition("updated", QuerySpec.Operation.greaterThan, ""+  options.getUpdatedSince().getTime());
      }


      return query;

    }


    private Operation toOperation(FilterOperation filterOperation) {
      try {
      Operation.valueOf(Operation.class, filterOperation.name());
      }
      catch (IllegalArgumentException iae) {
        ;
      }
      return null;
    }

}
