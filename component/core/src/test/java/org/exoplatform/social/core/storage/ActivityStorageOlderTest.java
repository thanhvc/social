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
package org.exoplatform.social.core.storage;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.relationship.model.Relationship;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.test.AbstractCoreTest;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          thanhvc@exoplatform.com
 * Mar 20, 2013  
 */
public class ActivityStorageOlderTest extends AbstractCoreTest {
  private IdentityStorage identityStorage;
  private ActivityStorage activityStorage;
  private IdentityManager identityManager;
  private RelationshipManager relationshipManager;
  private List<ExoSocialActivity> tearDownActivityList;

  private Identity rootIdentity;
  private Identity johnIdentity;
  private Identity maryIdentity;
  private Identity demoIdentity;
 
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    activityStorage = (ActivityStorage) getContainer().getComponentInstanceOfType(ActivityStorage.class);
    identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
    relationshipManager = (RelationshipManager) getContainer().getComponentInstanceOfType(RelationshipManager.class);
    assertNotNull("identityManager must not be null", identityStorage);
    assertNotNull("activityStorage must not be null", activityStorage);
    //
    rootIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, "root");
    if (rootIdentity == null) {
      rootIdentity = new Identity(OrganizationIdentityProvider.NAME, "root");
      identityStorage.saveIdentity(rootIdentity);
    }
    
    //
    johnIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, "john");
    if (johnIdentity == null) {
      johnIdentity = new Identity(OrganizationIdentityProvider.NAME, "john");
      identityStorage.saveIdentity(johnIdentity);
    }
    
    //
    maryIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, "mary");
    if (maryIdentity == null) {
      maryIdentity = new Identity(OrganizationIdentityProvider.NAME, "mary");
      identityStorage.saveIdentity(maryIdentity);
    }
    
    //
    demoIdentity = identityStorage.findIdentity(OrganizationIdentityProvider.NAME, "demo");
    if (demoIdentity == null) {
      demoIdentity = new Identity(OrganizationIdentityProvider.NAME, "demo");
      identityStorage.saveIdentity(demoIdentity);
    }
    tearDownActivityList = new ArrayList<ExoSocialActivity>();
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      activityStorage.deleteActivity(activity.getId());
    }
    super.tearDown();
  }
  
  /**
   * Test {@link ActivityStorage#getOlderComments(ExoSocialActivity, ExoSocialActivity, int)}
   * {@link ActivityStorage#getNumberOfOlderComments(ExoSocialActivity, ExoSocialActivity)}
   * 
   * @since 1.2.0-Beta3
   */
  public void testOlderComments() {
    int totalNumber = 10;
    String activityTitle = "activity title";
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(activityTitle);
    activity.setUserId(rootIdentity.getId());
    activityStorage.saveActivity(rootIdentity, activity);
    tearDownActivityList.add(activity);
    
    for (int i = 0; i < totalNumber; i ++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("john comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorage.saveComment(activity, comment);
    }
    
    List<ExoSocialActivity> comments = activityStorage.getComments(activity, 0, 10);
    assertEquals(10, comments.size());
    
    
    ExoSocialActivity baseComment = comments.get(0);
    
    List<ExoSocialActivity> olderComments = null;
    int number = 0;
    
    {
      olderComments = activityStorage.getOlderComments(activity, baseComment, 10);
      assertEquals(9, olderComments.size());
      number = activityStorage.getNumberOfOlderComments(activity, baseComment);
      assertEquals(9, number);
    
    }
    
    baseComment = comments.get(9);
    
    {
      olderComments = activityStorage.getOlderComments(activity, baseComment, 10);
      assertEquals(0, olderComments.size());
      
      //
      number = activityStorage.getNumberOfOlderComments(activity, baseComment);
      assertEquals(0, number);
    }
    
    baseComment = comments.get(5);
    
    {
      olderComments = activityStorage.getOlderComments(activity, baseComment, 10);
      assertEquals(4, olderComments.size());
      //
      number = activityStorage.getNumberOfOlderComments(activity, baseComment);
      assertEquals(4, number);
    }
    
  }
  
  /**
   * Tests {@link ActivityStorage#getNumberOfOlderOnActivityFeed(Identity, ExoSocialActivity)}.
   * {@link ActivityStorage#getNumberOfNewerComments(ExoSocialActivity, ExoSocialActivity)}
   */
  public void testOlderOnActivityFeed() {
    createActivities(3, demoIdentity);
    createActivities(2, maryIdentity);
    Relationship maryDemoConnection = relationshipManager.invite(maryIdentity, demoIdentity);
    relationshipManager.confirm(maryDemoConnection);
    
    
    List<ExoSocialActivity> demoActivityFeed = activityStorage.getActivityFeed(demoIdentity, 0, 10);
    ExoSocialActivity lastDemoActivity = demoActivityFeed.get(4);
    
    int oldDemoActivityFeed = 0;
    
    {
      oldDemoActivityFeed = activityStorage.getNumberOfOlderOnActivityFeed(demoIdentity, lastDemoActivity);
      assertEquals(0, oldDemoActivityFeed);
      //
      assertEquals(0, activityStorage.getOlderOnActivityFeed(demoIdentity, lastDemoActivity, 10).size());
    }
    
    createActivities(1, johnIdentity);
    
    {
      oldDemoActivityFeed = activityStorage.getNumberOfOlderOnActivityFeed(demoIdentity, lastDemoActivity);
      assertEquals(0, oldDemoActivityFeed);
      //
      assertEquals(0, activityStorage.getOlderOnActivityFeed(demoIdentity, lastDemoActivity, 10).size());
    }
    
    ExoSocialActivity nextLastDemoActivity = demoActivityFeed.get(3);
    
    {
      oldDemoActivityFeed = activityStorage.getNumberOfOlderOnActivityFeed(demoIdentity, nextLastDemoActivity);
      assertEquals(1, oldDemoActivityFeed); 
      //
      assertEquals(1, activityStorage.getOlderOnActivityFeed(demoIdentity, nextLastDemoActivity, 10).size());
    }
    
    
    //
    relationshipManager.delete(maryDemoConnection);
    
  }

  /**
   * Tests {@link ActivityStorage#getOlderOnUserActivities(Identity, int, int)}.
   * {@link ActivityStorage#getNumberOfOlderOnUserActivities(Identity, ExoSocialActivity)}.
   */
  public void testOlderOnUserActivities() {
    createActivities(2, demoIdentity);
    ExoSocialActivity firstActivity = activityStorage.getUserActivities(demoIdentity, 0, 10).get(0);
    
    {
      assertEquals(1, activityStorage.getOlderOnUserActivities(demoIdentity, firstActivity, 10).size());
      assertEquals(1, activityStorage.getNumberOfOlderOnUserActivities(demoIdentity, firstActivity));
    }

    //
    createActivities(2, maryIdentity);
    
    {
      assertEquals(1, activityStorage.getOlderOnUserActivities(demoIdentity, firstActivity, 10).size());
      assertEquals(1, activityStorage.getNumberOfOlderOnUserActivities(demoIdentity, firstActivity));
    }
    
    //
    createActivities(2, demoIdentity);
    {
      assertEquals(1, activityStorage.getOlderOnUserActivities(demoIdentity, firstActivity, 10).size());
      assertEquals(1, activityStorage.getNumberOfOlderOnUserActivities(demoIdentity, firstActivity));
    }
    
    firstActivity = activityStorage.getUserActivities(demoIdentity, 0, 10).get(0);
    {
      assertEquals(3, activityStorage.getOlderOnUserActivities(demoIdentity, firstActivity, 10).size());
      assertEquals(3, activityStorage.getNumberOfOlderOnUserActivities(demoIdentity, firstActivity));
    }
  }
  
  /**
   * Test {@link ActivityStorage#getNumberOfOlderOnUserSpacesActivities(Identity, ExoSocialActivity)}
   * {@link ActivityStorage#getOlderOnUserSpacesActivities(Identity, ExoSocialActivity, int)}
   * 
   * @throws Exception
   * @since 1.2.0-Beta3
   */
  public void testOlderOnUserSpacesActivities() throws Exception {
    SpaceService spaceService = this.getSpaceService();
    Space space = this.getSpaceInstance(spaceService, 0);
    Identity spaceIdentity = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    
    int totalNumber = 5;
    
    ExoSocialActivity baseActivity = null;
    
    //demo posts activities to space
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorage.saveActivity(spaceIdentity, activity);
      tearDownActivityList.add(activity);
      if (i == totalNumber - 1) {
        baseActivity = activity;
      }
    }
    
    int number = 0;
    List<ExoSocialActivity> activities = null;
    
    {
      number = activityStorage.getNumberOfOlderOnUserSpacesActivities(demoIdentity, baseActivity);
      activities = activityStorage.getOlderOnUserSpacesActivities(demoIdentity, baseActivity, 10);
      
      assertEquals(4, number);
      assertEquals(4, activities.size());
    }
    
    
    Space space2 = this.getSpaceInstance(spaceService, 1);
    Identity spaceIdentity2 = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space2.getPrettyName(), false);
    
    //demo posts activities to space2
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorage.saveActivity(spaceIdentity2, activity);
      tearDownActivityList.add(activity);
      if (i == totalNumber - 1) {
        baseActivity = activity;
      }
    }
    
    {
      number = activityStorage.getNumberOfOlderOnUserSpacesActivities(demoIdentity, baseActivity);
      assertEquals(9, number);
      
      activities = activityStorage.getOlderOnUserSpacesActivities(demoIdentity, baseActivity, 10);
      assertEquals(9, activities.size());
    }
    
    
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    spaceService.deleteSpace(space);
    space2 = spaceService.getSpaceByDisplayName(space2.getDisplayName());
    spaceService.deleteSpace(space2);
  }

  /**
   * Test {@link ActivityStorage#getOlderOnActivitiesOfConnections(Identity, ExoSocialActivity, int)}
   * {@link ActivityStorage#getNumberOfOlderOnActivitiesOfConnections(Identity, ExoSocialActivity)}
   * 
   * @since 1.2.0-Beta3
   */
  public void testOlderOnActivitiesOfConnections() {
    List<Relationship> relationships = new ArrayList<Relationship> ();
    
    this.createActivities(3, maryIdentity);
    this.createActivities(1, demoIdentity);
    this.createActivities(2, johnIdentity);
    this.createActivities(2, rootIdentity);
    
    List<ExoSocialActivity> rootActivities = activityStorage.getActivitiesOfIdentity(rootIdentity, 0, 10);
    assertEquals(2, rootActivities.size());
    
    ExoSocialActivity baseActivity = rootActivities.get(1);
    
    List<ExoSocialActivity> activities;
    
    int number = 0;
    {
      activities = activityStorage.getOlderOnActivitiesOfConnections(rootIdentity, baseActivity, 10);
      assertEquals(0, activities.size());
      
      //
      number = activityStorage.getNumberOfOlderOnActivitiesOfConnections(rootIdentity, baseActivity);
      assertEquals(0, number);
    }
    
    {
      activities = activityStorage.getOlderOnActivitiesOfConnections(johnIdentity, baseActivity, 10);
      assertEquals(2, activities.size());
    
      //
      number = activityStorage.getNumberOfOlderOnActivitiesOfConnections(johnIdentity, baseActivity);
      assertEquals(2, number);
    }
    
    RelationshipManager relationshipManager = this.getRelationshipManager();
    
    Relationship rootJohnRelationship = relationshipManager.invite(rootIdentity, johnIdentity);
    relationshipManager.confirm(rootJohnRelationship);
    relationships.add(rootJohnRelationship);
    
    {
      activities = activityStorage.getOlderOnActivitiesOfConnections(rootIdentity, baseActivity, 10);
      assertEquals(2, activities.size());
      
      //
      number = activityStorage.getNumberOfOlderOnActivitiesOfConnections(rootIdentity, baseActivity);
      assertEquals(2, number);
    }
    
    Relationship rootDemoRelationship = relationshipManager.invite(rootIdentity, demoIdentity);
    relationshipManager.confirm(rootDemoRelationship);
    relationships.add(rootDemoRelationship);
    
    {
      activities = activityStorage.getOlderOnActivitiesOfConnections(rootIdentity, baseActivity, 10);
      assertEquals(3, activities.size());
      //
      number = activityStorage.getNumberOfOlderOnActivitiesOfConnections(rootIdentity, baseActivity);
      assertEquals(3, number);
    }
    
    
    Relationship rootMaryRelationship = relationshipManager.invite(rootIdentity, maryIdentity);
    relationshipManager.confirm(rootMaryRelationship);
    relationships.add(rootMaryRelationship);
    
    {
      activities = activityStorage.getOlderOnActivitiesOfConnections(rootIdentity, baseActivity, 10);
      assertEquals(6, activities.size());
      //
      number = activityStorage.getNumberOfOlderOnActivitiesOfConnections(rootIdentity, baseActivity);
      assertEquals(6, number);
    }
    
    for (Relationship rel : relationships) {
      relationshipManager.remove(rel);
    }
  }
  
  /**
   * Gets the relationship manager.
   * 
   * @return
   * @since 1.2.0-Beta3
   */
  private RelationshipManager getRelationshipManager() {
    return (RelationshipManager) getContainer().getComponentInstanceOfType(RelationshipManager.class);
  }
  
  /**
   * Gets the space service.
   * 
   * @return the space service
   */
  private SpaceService getSpaceService() {
    return (SpaceService) getContainer().getComponentInstanceOfType(SpaceService.class);
  }
  
  /**
   * Creates activities.
   * 
   * @param number
   * @param ownerStream
   * @since 1.2.0-Beta3
   */
  private void createActivities(int number, Identity ownerStream) {
    for (int i = 0; i < number; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activityStorage.saveActivity(ownerStream, activity);
      tearDownActivityList.add(activity);
    }
  }
  
  /**
   * Gets an instance of the space.
   * 
   * @param spaceService
   * @param number
   * @return
   * @throws Exception
   * @since 1.2.0-GA
   */
  private Space getSpaceInstance(SpaceService spaceService, int number) throws Exception {
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] {"demo"};
    String[] members = new String[] {"demo"};
    String[] invitedUsers = new String[] {"mary"};
    String[] pendingUsers = new String[] {"john",};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    spaceService.saveSpace(space, true);
    return space;
  }
}
