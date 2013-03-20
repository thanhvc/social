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
public class ActivityStorageNewerTest extends AbstractCoreTest {
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
   * Tests {@link ActivityStorage#getNewerOnActivityFeed(Identity, ExoSocialActivity, int)}.
   * {@link ActivityStorage#getNumberOfNewerOnActivityFeed(Identity, ExoSocialActivity)}.
   */
  public void testNewerOnActivityFeed() {
    
    //STEP 1
    createActivities(3, "demo Get Newer On Activity Feed", demoIdentity);
    ExoSocialActivity demoBaseActivity = activityStorage.getActivityFeed(demoIdentity, 0, 10).get(0);
    
    long sinceTime = demoBaseActivity.getPostedTime();
    
    assertEquals(0, activityStorage.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    
    int numberOfNewerActivityFeed = 0;
    
    {
    //getNumberOfNewerForFeed
      numberOfNewerActivityFeed = activityStorage.getNumberOfNewerOnActivityFeed(demoIdentity, demoBaseActivity);
      assertEquals(0, numberOfNewerActivityFeed);
      
      numberOfNewerActivityFeed = activityStorage.getNumberOfNewerOnActivityFeed(demoIdentity, sinceTime);
      assertEquals(0, numberOfNewerActivityFeed);
    }
    
    //STEP 2
    createActivities(1, "demo Get Newer On Activity Feed 1", demoIdentity);
    //
    {
      assertEquals(1, activityStorage.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    }
    
    {
    //getNumberOfNewerForFeed
      numberOfNewerActivityFeed = activityStorage.getNumberOfNewerOnActivityFeed(demoIdentity, demoBaseActivity);
      assertEquals(1, numberOfNewerActivityFeed);
      
      //
      numberOfNewerActivityFeed = activityStorage.getNumberOfNewerOnActivityFeed(demoIdentity, sinceTime);
      assertEquals(1, numberOfNewerActivityFeed);
    }
    
    
    //STEP 3
    createActivities(2, "mary Get Newer On Activity Feed", maryIdentity);
    Relationship demoMaryConnection = relationshipManager.invite(demoIdentity, maryIdentity);
    //
    {
      assertEquals(1, activityStorage.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    }
    
    //
    relationshipManager.confirm(demoMaryConnection);
    
    //STEP 4
    createActivities(2, "mary Get Newer On Activity Feed 1", maryIdentity);
    
    //
    {
      assertEquals(5, activityStorage.getNewerOnActivityFeed(demoIdentity, demoBaseActivity, 10).size());
    }
    
    {
    //getNumberOfNewerForFeed
      numberOfNewerActivityFeed = activityStorage.getNumberOfNewerOnActivityFeed(demoIdentity, demoBaseActivity);
      assertEquals(5, numberOfNewerActivityFeed); 
      
      numberOfNewerActivityFeed = activityStorage.getNumberOfNewerOnActivityFeed(demoIdentity, sinceTime);
      assertEquals(5, numberOfNewerActivityFeed);
    }
    
    relationshipManager.delete(demoMaryConnection);
  }

  
  
  /**
   * Test {@link ActivityStorage#getNewerComments(ExoSocialActivity, ExoSocialActivity, int)}
   * 
   * @since 1.2.0-Beta3
   */
  public void testGetNewerComments() {
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
    
    for (int i = 0; i < totalNumber; i ++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("demo comment " + i);
      comment.setUserId(demoIdentity.getId());
      activityStorage.saveComment(activity, comment);
    }
    
    List<ExoSocialActivity> comments = activityStorage.getComments(activity, 0, 10);
    assertNotNull("comments must not be null", comments);
    assertEquals("comments.size() must return: 10", 10, comments.size());
    
    ExoSocialActivity latestComment = comments.get(0);
    
    List<ExoSocialActivity> newerComments = activityStorage.getNewerComments(activity, latestComment, 10);
    assertNotNull("newerComments must not be null", newerComments);
    assertEquals("newerComments.size() must return: 0", 0, newerComments.size());
    
    ExoSocialActivity baseComment = activityStorage.getComments(activity, 0, 20).get(10);
    newerComments = activityStorage.getNewerComments(activity, baseComment, 20);
    assertNotNull("newerComments must not be null", newerComments);
    assertEquals("newerComments.size() must return: 10", 10, newerComments.size());
    
    baseComment = activityStorage.getComments(activity, 0, 20).get(19);
    newerComments = activityStorage.getNewerComments(activity, baseComment, 20);
    assertNotNull("newerComments must not be null", newerComments);
    assertEquals("newerComments.size() must return: 19", 19, newerComments.size());
  }
  

  /**
   * Test {@link ActivityStorage#getNewerOnUserSpacesActivities(Identity, ExoSocialActivity, int)}, 
   * {@link ActivityStorage#getNumberOfNewerOnUserSpacesActivities(Identity, ExoSocialActivity)}, and
   * {@link  ActivityStorage#getNewerOnUserSpacesActivities(Identity, Long, int)}
   * 
   * @throws Exception
   * @since 1.2.0-Beta3
   */
  public void testNewerOnUserSpacesActivities() throws Exception {
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
      if (i == 0) {
        baseActivity = activity;
      }
    }
    
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(space);

    List<ExoSocialActivity> activities = null;
    int numberOfActivities = 0;
    long sinceTime = baseActivity.getPostedTime();
    
    {
      activities = activityStorage.getNewerOnUserSpacesActivities(demoIdentity, baseActivity, 10);
      assertEquals(4, activities.size());

      //
      numberOfActivities = activityStorage.getNumberOfNewerOnUserSpacesActivities(demoIdentity, baseActivity);
      assertEquals(4, numberOfActivities);
      
      //
      numberOfActivities = activityStorage.getNumberOfNewerOnUserSpacesActivities(demoIdentity, sinceTime);
      assertEquals(4, numberOfActivities);
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
    }
    
    space2 = spaceService.getSpaceByDisplayName(space2.getDisplayName());
    assertNotNull(space2);

    {
      activities = activityStorage.getNewerOnUserSpacesActivities(demoIdentity, baseActivity, 15);
      assertEquals(9, activities.size());
      
      numberOfActivities = activityStorage.getNumberOfNewerOnUserSpacesActivities(demoIdentity, baseActivity);
      assertEquals(9, numberOfActivities);
      
      //
      numberOfActivities = activityStorage.getNumberOfNewerOnUserSpacesActivities(demoIdentity, sinceTime);
      assertEquals(9, numberOfActivities);
      
    }

    spaceService.deleteSpace(space);
    spaceService.deleteSpace(space2);
  }
  
  /**
   * Test {@link ActivityStorage#getNewerOnActivitiesOfConnections(Identity, ExoSocialActivity, int)}
   * {@link ActivityStorage#getNewerOnActivitiesOfConnections(Identity, Long)}
   * 
   * @since 1.2.0-Beta3
   */
  public void testNewerOnActivitiesOfConnections() {
    List<Relationship> relationships = new ArrayList<Relationship> ();
    
    this.createActivities(3, "mary Get Newer On ActivitiesOfConnections", maryIdentity);
    this.createActivities(1, "demo Get Newer On ActivitiesOfConnections", demoIdentity);
    this.createActivities(2, "john Get Newer On ActivitiesOfConnections", johnIdentity);
    this.createActivities(2, "root Get Newer On ActivitiesOfConnections", rootIdentity);
    

    List<ExoSocialActivity> maryActivities = activityStorage.getActivitiesOfIdentity(maryIdentity, 0, 10);
    assertNotNull(maryActivities);
    assertEquals(3, maryActivities.size());
    
    ExoSocialActivity baseActivity = maryActivities.get(2);
    Long sinceTime = maryActivities.get(2).getPostedTime();
    
    List<ExoSocialActivity> activities = null;
    {
      activities = activityStorage.getNewerOnActivitiesOfConnections(johnIdentity, baseActivity, 10);
      assertEquals(2, activities.size());
      
      activities = activityStorage.getNewerOnActivitiesOfConnections(demoIdentity, baseActivity, 10);
      assertEquals(1, activities.size());
      
      activities = activityStorage.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10);
      assertEquals(2, activities.size());
      
      //
      assertEquals(2, activityStorage.getNumberOfNewerOnActivitiesOfConnections(johnIdentity,sinceTime));

      assertEquals(1, activityStorage.getNumberOfNewerOnActivitiesOfConnections(demoIdentity, sinceTime));

      assertEquals(2, activityStorage.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, sinceTime));

    }
    
    RelationshipManager relationshipManager = this.getRelationshipManager();
    Relationship maryDemoRelationship = relationshipManager.invite(maryIdentity, demoIdentity);
    relationshipManager.confirm(maryDemoRelationship);
    relationships.add(maryDemoRelationship);
    
    {
      activities = activityStorage.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10);
      assertEquals(3, activities.size());
      
      activities = activityStorage.getNewerOnActivitiesOfConnections(demoIdentity, baseActivity, 10);
      assertEquals(3, activities.size());
      
      //
      assertEquals(3, activityStorage.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, sinceTime));
      assertEquals(3, activityStorage.getNumberOfNewerOnActivitiesOfConnections(demoIdentity, sinceTime));
      
    }
    
    
    Relationship maryJohnRelationship = relationshipManager.invite(maryIdentity, johnIdentity);
    relationshipManager.confirm(maryJohnRelationship);
    relationships.add(maryJohnRelationship);
    
    {
      activities = activityStorage.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10);
      assertEquals(5, activities.size());
      
      assertEquals(5, activityStorage.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, sinceTime));
      
      
    }
    
    Relationship maryRootRelationship = relationshipManager.invite(maryIdentity, rootIdentity);
    relationshipManager.confirm(maryRootRelationship);
    relationships.add(maryRootRelationship);

    {
      activities = activityStorage.getNewerOnActivitiesOfConnections(maryIdentity, baseActivity, 10);
      assertEquals(7, activities.size());
      
      //
      assertEquals(7, activityStorage.getNumberOfNewerOnActivitiesOfConnections(maryIdentity, sinceTime));
    }

    //
    for (Relationship rel : relationships) {
      relationshipManager.remove(rel);
    }
  }
  
  /**
   * Tests {@link ActivityStorage#getNumberOfNewerOnUserActivities(Identity, ExoSocialActivity)}.
   */
  public void testNewerOnUserActivities() {
    createActivities(2, "demo Get Number Newer User Activities", demoIdentity);
    ExoSocialActivity firstActivity = activityStorage.getUserActivities(demoIdentity, 0, 10).get(0);
    Long sinceTime = firstActivity.getPostedTime();
    
    {
      assertEquals(0, activityStorage.getNumberOfNewerOnUserActivities(demoIdentity, firstActivity));
      assertEquals(0, activityStorage.getNewerOnUserActivities(demoIdentity, firstActivity, 10).size());
      
      assertEquals(0, activityStorage.getNumberOfNewerOnUserActivities(demoIdentity, sinceTime));
    
    }

    createActivities(1, "john Get Number Newer User Activities", johnIdentity);
    
    {
      assertEquals(1, activityStorage.getNewerOnUserActivities(johnIdentity, firstActivity, 10).size());
    }

    createActivities(1, "demo Get Number Newer User Activities 1", demoIdentity);

    {
      assertEquals(1, activityStorage.getNumberOfNewerOnUserActivities(demoIdentity, firstActivity));
      assertEquals(1, activityStorage.getNewerOnUserActivities(demoIdentity, firstActivity, 10).size());
      
      assertEquals(1, activityStorage.getNumberOfNewerOnUserActivities(demoIdentity, sinceTime));
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
  private void createActivities(int number, String title, Identity ownerStream) {
    for (int i = 0; i < number; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(title + i);
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
