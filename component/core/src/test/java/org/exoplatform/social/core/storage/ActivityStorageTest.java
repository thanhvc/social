/*
* Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.social.core.storage;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ActivityStream;
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
import org.exoplatform.social.core.test.MaxQueryNumber;
import org.exoplatform.social.core.test.QueryNumberTest;

/**
 * Unit Test for {@link org.exoplatform.social.core.storage.ActivityStorage}
 *
 * @author hoat_le
 *
 */
@QueryNumberTest
public class ActivityStorageTest extends AbstractCoreTest {
  private final Log LOG = ExoLogger.getLogger(ActivityStorageTest.class);
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
    rootIdentity = new Identity(OrganizationIdentityProvider.NAME, "root");
    johnIdentity = new Identity(OrganizationIdentityProvider.NAME, "john");
    maryIdentity = new Identity(OrganizationIdentityProvider.NAME, "mary");
    demoIdentity = new Identity(OrganizationIdentityProvider.NAME, "demo");
    
    identityStorage.saveIdentity(rootIdentity);
    identityStorage.saveIdentity(johnIdentity);
    identityStorage.saveIdentity(maryIdentity);
    identityStorage.saveIdentity(demoIdentity);

    assertNotNull("rootIdentity.getId() must not be null", rootIdentity.getId());
    assertNotNull("johnIdentity.getId() must not be null", johnIdentity.getId());
    assertNotNull("maryIdentity.getId() must not be null", maryIdentity.getId());
    assertNotNull("demoIdentity.getId() must not be null", demoIdentity.getId());

    tearDownActivityList = new ArrayList<ExoSocialActivity>();
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      activityStorage.deleteActivity(activity.getId());
    }
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(johnIdentity);
    identityStorage.deleteIdentity(maryIdentity);
    identityStorage.deleteIdentity(demoIdentity);
    super.tearDown();
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.ActivityStorage#saveActivity(org.exoplatform.social.core.identity.model.Identity, org.exoplatform.social.core.activity.model.ExoSocialActivity)}
   */
  @MaxQueryNumber(100)
  public void testSaveActivity() throws ActivityStorageException {
    final String activityTitle = "activity Title";
    //test wrong
    {
      ExoSocialActivity wrongActivity = new ExoSocialActivityImpl();
      try {
        activityStorage.saveActivity(demoIdentity, null);
        activityStorage.saveActivity(null, wrongActivity);
      } catch (ActivityStorageException e) {
        LOG.info("wrong argument tests passed.");
      }
    }
    //test with only mandatory fields
    {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(activityTitle);
      activityStorage.saveActivity(rootIdentity, activity);

      assertNotNull("activity.getId() must not be null", activity.getId());

      tearDownActivityList.addAll(activityStorage.getUserActivities(rootIdentity, 0, 1));

      ExoSocialActivity johnActivity = new ExoSocialActivityImpl();
      johnActivity.setTitle(activityTitle);
      activityStorage.saveActivity(johnIdentity, johnActivity);
      assertNotNull("johnActivity.getId() must not be null", johnActivity.getId());

      tearDownActivityList.addAll(activityStorage.getUserActivities(johnIdentity, 0, 1));
    }
    //Test with full fields.
    {

    }

    //Test mail-formed activityId
    {

    }

  }

  /**
   * Test {@link org.exoplatform.social.core.storage.ActivityStorage#deleteActivity(String)}
   */
  @MaxQueryNumber(200)
  public void testDeleteActivity() throws ActivityStorageException {
    final String activityTitle = "activity Title";

    //Test deleteActivity(String)
    {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(activityTitle);
      activityStorage.saveActivity(maryIdentity, activity);

      assertNotNull("activity.getId() must not be null", activity.getId());

      activityStorage.deleteActivity(activity.getId());
      try {
        assertEquals(null, activityStorage.getActivity(activity.getId()));
      } catch (Exception ase) {
        // ok
      }
    }
    //Test deleteActivity(Activity)
    {
      ExoSocialActivity activity2 = new ExoSocialActivityImpl();
      activity2.setTitle(activityTitle);
      activityStorage.saveActivity(demoIdentity, activity2);

      assertNotNull("activity2.getId() must not be null", activity2.getId());
      activityStorage.deleteActivity(activity2.getId());
    }

  }

  /**
   * Test {@link org.exoplatform.social.core.storage.ActivityStorage#saveComment(org.exoplatform.social.core.activity.model.ExoSocialActivity , org.exoplatform.social.core.activity.model.ExoSocialActivity)}
   */
  @MaxQueryNumber(100)
  public void testSaveComment() throws ActivityStorageException {

    //comment on his own activity
    {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("blah blah");
      activityStorage.saveActivity(rootIdentity, activity);

      assertNotNull(activity.getReplyToId());

      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("comment blah");
      comment.setUserId(rootIdentity.getId());

      activityStorage.saveComment(activity, comment);

      assertNotNull(activity.getReplyToId());
      assertEquals(1, activity.getReplyToId().length);

      comment = activityStorage.getActivity(comment.getId());
      assertTrue(comment.isComment());
      
      tearDownActivityList.add(activity);
    }

    // comment on other users' activity
    {

    }

  }

  /**
   * Test {@link org.exoplatform.social.core.storage.ActivityStorage#deleteComment(String, String)}
   */
  @MaxQueryNumber(112)
  public void testDeleteComment() throws ActivityStorageException {

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("blah blah");
    activityStorage.saveActivity(rootIdentity, activity);

    ExoSocialActivity comment = new ExoSocialActivityImpl();
    comment.setTitle("coment blah blah");
    comment.setUserId(rootIdentity.getId());

    activityStorage.saveComment(activity, comment);

    assertNotNull("comment.getId() must not be null", comment.getId());

    activityStorage.deleteComment(activity.getId(), comment.getId());

    tearDownActivityList.add(activity);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.ActivityStorage#getActivity(String)}
   */
  @MaxQueryNumber(50)
  public void testGetActivity() throws ActivityStorageException {
    final String activityTitle = "activity title";
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(activityTitle);
    activityStorage.saveActivity(demoIdentity, activity);
    tearDownActivityList.add(activity);

    assertNotNull("activity.getId() must not be null", activity.getId());

    assertEquals("demoIdentity.getRemoteId() must return: " + demoIdentity.getRemoteId(), demoIdentity.getRemoteId(), activity.getStreamOwner());

    ExoSocialActivity gotActivity = activityStorage.getActivity(activity.getId());

    assertNotNull("gotActivity.getId() must not be null", gotActivity.getId());

    assertEquals("activity.getId() must return: " + activity.getId(), activity.getId(), gotActivity.getId());

    assertEquals("gotActivity.getTitle() must return: " + gotActivity.getTitle(), activityTitle, gotActivity.getTitle());


    ActivityStream activityStream = activity.getActivityStream();
    assertNotNull("activityStream.getId() must not be null", activityStream.getId());
    assertEquals("activityStream.getPrettyId() must return: " + demoIdentity.getRemoteId(), demoIdentity.getRemoteId(), activityStream.getPrettyId());
    assertEquals(ActivityStream.Type.USER, activityStream.getType());
    assertNotNull("activityStream.getPermaLink() must not be null", activityStream.getPermaLink());

  }

  /**
   * Test {@link org.exoplatform.social.core.storage.ActivityStorage#getUserActivities(org.exoplatform.social.core.identity.model.Identity, long, long)}
   *
   * and {@link org.exoplatform.social.core.storage.ActivityStorage#getUserActivities(org.exoplatform.social.core.identity.model.Identity)}
   *
   */
  @MaxQueryNumber(5000)
  public void testGetActivitiesAndCount() throws ActivityStorageException {
    final int totalNumber = 5;
    final String activityTitle = "activity title";
    //John posts activity to root's activity stream
    for (int i = 0; i < totalNumber; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(activityTitle + i);

      activityStorage.saveActivity(rootIdentity, activity);
      tearDownActivityList.add(activity);
    }

    //Till now Root's activity stream has 10 activities posted by John
    assertEquals(0, activityStorage.getUserActivities(johnIdentity, 0, 100).size());
    assertEquals(totalNumber, activityStorage.getUserActivities(rootIdentity, 0, 100).size());

    //Root posts activities to his stream
    for (int i = 0; i < totalNumber; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle(activityTitle + i);
      activity.setUserId(rootIdentity.getId());
      activityStorage.saveActivity(rootIdentity, activity);

      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("Comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorage.saveComment(activity, comment);
      tearDownActivityList.add(activity);
    }
    //Till now Root's activity stream has 10 activities: 5 posted by John and 5 posted by Root
    //, each of those activities posted by Root has 1 comment by John.
    assertEquals(totalNumber, activityStorage.getUserActivities(johnIdentity).size());
    assertEquals(totalNumber*2, activityStorage.getUserActivities(rootIdentity).size());


    // Test ActivityStorage#getActivities(Identity, long, long)
    {
      final int limit = 8;
      assertTrue(activityStorage.getUserActivities(rootIdentity).size() > limit);
      List<ExoSocialActivity> gotRootActivityList = activityStorage.getUserActivities(rootIdentity, 0, limit);
      assertEquals(limit, gotRootActivityList.size());
    }
    
    //count testing
    {
      assertEquals(totalNumber*2, activityStorage.getNumberOfUserActivities(rootIdentity));
      assertEquals(0, activityStorage.getNumberOfUserActivities(johnIdentity)); 
    }

  }

  /**
   * Tests {@link ActivityStorage#getActivityFeed(Identity, int, int)}.
   */
  @MaxQueryNumber(650)
  public void testGetActivityFeed() {
    createActivities(3, demoIdentity);
    createActivities(3, maryIdentity);
    createActivities(2, johnIdentity);

    List<ExoSocialActivity> demoActivityFeed = activityStorage.getActivityFeed(demoIdentity, 0, 10);
    assertEquals("demoActivityFeed.size() must be 3", 3, demoActivityFeed.size());

    Relationship demoMaryConnection = relationshipManager.invite(demoIdentity, maryIdentity);
    assertEquals(3, activityStorage.getActivityFeed(demoIdentity, 0, 10).size());

    relationshipManager.confirm(demoMaryConnection);
    List<ExoSocialActivity> demoActivityFeed2 = activityStorage.getActivityFeed(demoIdentity, 0, 10);
    assertEquals("demoActivityFeed2.size() must return 6", 6, demoActivityFeed2.size());
    List<ExoSocialActivity> maryActivityFeed = activityStorage.getActivityFeed(maryIdentity, 0, 10);
    assertEquals("maryActivityFeed.size() must return 6", 6, maryActivityFeed.size());
  }

  /**
   * Tests {@link ActivityStorage#getNumberOfActivitesOnActivityFeed(Identity)}.
   */
  @MaxQueryNumber(500)
  public void testGetNumberOfActivitesOnActivityFeed() {
    createActivities(3, demoIdentity);
    createActivities(2, maryIdentity);
    createActivities(1, johnIdentity);
    int demoActivityCount = activityStorage.getNumberOfActivitesOnActivityFeed(demoIdentity);
    assertEquals("demoActivityCount must be 3", 3, demoActivityCount);
    int maryActivityCount = activityStorage.getNumberOfActivitesOnActivityFeed(maryIdentity);
    assertEquals("maryActivityCount must be 2", 2, maryActivityCount);
    Relationship demoMaryConnection = relationshipManager.invite(demoIdentity, maryIdentity);
    int demoActivityCount2 = activityStorage.getNumberOfActivitesOnActivityFeed(demoIdentity);
    assertEquals("demoActivityCount2 must be 3", 3, demoActivityCount2);
    relationshipManager.confirm(demoMaryConnection);
    int demoActivityCount3 = activityStorage.getNumberOfActivitesOnActivityFeed(demoIdentity);
    assertEquals("demoActivityCount3 must be 5", 5, demoActivityCount3);
    int maryActivityCount2 = activityStorage.getNumberOfActivitesOnActivityFeed(maryIdentity);
    assertEquals("maryActivityCount2 must be 5", 5, maryActivityCount2);
  }

  

  /**
   * Test {@link ActivityStorage#getActivitiesOfConnections(Identity, int, int)}
   */
  @MaxQueryNumber(2172)
  public void testGetActivitiesOfConnections() {
    List<Relationship> relationships = new ArrayList<Relationship> ();
    
    this.createActivities(2, rootIdentity);
    this.createActivities(1, demoIdentity);
    this.createActivities(2, johnIdentity);
    this.createActivities(3, maryIdentity);
    
    List<ExoSocialActivity> activities = activityStorage.getActivitiesOfConnections(demoIdentity, 0, 10);
    assertNotNull("activities must not be null", activities);
    assertEquals("activities.size() must return: 1", 1, activities.size());
    
    RelationshipManager relationshipManager = this.getRelationshipManager();
    
    Relationship rootDemoRelationship = relationshipManager.invite(rootIdentity, demoIdentity);
    relationshipManager.confirm(rootDemoRelationship);
    relationships.add(rootDemoRelationship);
    
    activities = activityStorage.getActivitiesOfConnections(rootIdentity, 0, 10);
    assertNotNull("activities must not be null", activities);
    assertEquals("activities.size() must return: 3", 3, activities.size());
    
    Relationship rootMaryRelationship = relationshipManager.invite(rootIdentity, maryIdentity);
    relationshipManager.confirm(rootMaryRelationship);
    relationships.add(rootMaryRelationship);
    
    activities = activityStorage.getActivitiesOfConnections(rootIdentity, 0, 10);
    assertNotNull("activities must not be null", activities);
    assertEquals("activities.size() must return: 6", 6, activities.size());
    
    Relationship rootJohnRelationship = relationshipManager.invite(rootIdentity, johnIdentity);
    relationshipManager.confirm(rootJohnRelationship);
    relationships.add(rootJohnRelationship);
    
    activities = activityStorage.getActivitiesOfConnections(rootIdentity, 0, 10);
    assertNotNull("activities must not be null", activities);
    assertEquals("activities.size() must return: 8", 8, activities.size());
    
    for (Relationship rel : relationships) {
      relationshipManager.remove(rel);
    }
  }

  /**
   * Test {@link ActivityStorage#getActivitiesOfConnections(Identity, int, int)} for issue SOC-1995
   * 
   * @throws Exception
   * @since 1.2.2
   */
  @MaxQueryNumber(550)
  public void testGetActivitiesOfConnectionsWithPosterIdentity() throws Exception {
    RelationshipManager relationshipManager = this.getRelationshipManager();
    List<Relationship> relationships = new ArrayList<Relationship>();
    
    Relationship johnDemoIdentity = relationshipManager.inviteToConnect(johnIdentity, demoIdentity);
    relationshipManager.confirm(demoIdentity, johnIdentity);
    johnDemoIdentity = relationshipManager.get(johnDemoIdentity.getId());
    relationships.add(johnDemoIdentity);
    
    Relationship demoMaryIdentity = relationshipManager.inviteToConnect(demoIdentity, maryIdentity);
    relationshipManager.confirm(maryIdentity, demoIdentity);
    johnDemoIdentity = relationshipManager.get(demoMaryIdentity.getId());
    relationships.add(demoMaryIdentity);
   
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("Hello Demo from Mary");
    activity.setUserId(maryIdentity.getId());
    activityStorage.saveActivity(demoIdentity, activity);
    tearDownActivityList.add(activity);
    
    activity = activityStorage.getActivity(activity.getId());
    assertNotNull("activity must not be null", activity);
    assertEquals("activity.getStreamOwner() must return: demo", "demo", activity.getStreamOwner());
    assertEquals("activity.getUserId() must return: " + maryIdentity.getId(), maryIdentity.getId(), activity.getUserId());
    
    List<ExoSocialActivity> johnConnectionActivities = activityStorage.getActivitiesOfConnections(johnIdentity, 0, 10);
    assertNotNull("johnConnectionActivities must not be null", johnConnectionActivities);
    assertEquals("johnConnectionActivities.size() must return: 1", 1, johnConnectionActivities.size());
    
    List<ExoSocialActivity> demoConnectionActivities = activityStorage.getActivitiesOfConnections(demoIdentity, 0, 10);
    assertNotNull("demoConnectionActivities must not be null", demoConnectionActivities);
    assertEquals("demoConnectionActivities.size() must return: 1", 1, demoConnectionActivities.size());
    
    List<ExoSocialActivity> maryConnectionActivities = activityStorage.getActivitiesOfConnections(maryIdentity, 0, 10);
    assertNotNull("maryConnectionActivities must not be null", maryConnectionActivities);
    assertEquals("maryConnectionActivities.size() must return: 1", 1, maryConnectionActivities.size());
    
    for (Relationship rel : relationships) {
      relationshipManager.delete(rel);
    }
  }
  
  /**
   * Test {@link ActivityStorage#getNumberOfActivitiesOfConnections(Identity)}
   * 
   * @since 1.2.0-Beta3
   */
  @MaxQueryNumber(2172)
  public void testGetNumberOfActivitiesOfConnections() {
    List<Relationship> relationships = new ArrayList<Relationship> ();
    
    this.createActivities(2, rootIdentity);
    this.createActivities(1, demoIdentity);
    this.createActivities(2, johnIdentity);
    this.createActivities(3, maryIdentity);
    
    int count = activityStorage.getNumberOfActivitiesOfConnections(demoIdentity);
    assertEquals("count must be: 1", 1, count);
    
    RelationshipManager relationshipManager = this.getRelationshipManager();
    
    Relationship rootDemoRelationship = relationshipManager.invite(rootIdentity, demoIdentity);
    relationshipManager.confirm(rootDemoRelationship);
    relationships.add(rootDemoRelationship);
    
    count = activityStorage.getNumberOfActivitiesOfConnections(rootIdentity);
    assertEquals("count must be: 3", 3, count);
    
    Relationship rootMaryRelationship = relationshipManager.invite(rootIdentity, maryIdentity);
    relationshipManager.confirm(rootMaryRelationship);
    relationships.add(rootMaryRelationship);
    
    count = activityStorage.getNumberOfActivitiesOfConnections(rootIdentity);
    assertEquals("count must be: 6", 6, count);
    
    Relationship rootJohnRelationship = relationshipManager.invite(rootIdentity, johnIdentity);
    relationshipManager.confirm(rootJohnRelationship);
    relationships.add(rootJohnRelationship);
    
    count = activityStorage.getNumberOfActivitiesOfConnections(rootIdentity);
    assertEquals("count must be: 8", 8, count);
    
    for (Relationship rel : relationships) {
      relationshipManager.remove(rel);
    }
  }

  

 

  /**
   * Test {@link ActivityStorage#getUserSpacesActivities(Identity, int, int)}
   * 
   * @throws Exception
   * @since 1.2.0-Beta3
   */
  @MaxQueryNumber(1800)
  public void testGetUserSpacesActivities() throws Exception {
    SpaceService spaceService = this.getSpaceService();
    Space space = this.getSpaceInstance(spaceService, 0);
    Identity spaceIdentity = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    
    int totalNumber = 10;
    
    //demo posts activities to space
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorage.saveActivity(spaceIdentity, activity);
      tearDownActivityList.add(activity);
    }
    
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull("space must not be null", space);
    assertEquals("space.getDisplayName() must return: my space 0", "my space 0", space.getDisplayName());
    assertEquals("space.getDescription() must return: add new space 0", "add new space 0", space.getDescription());
    
    List<ExoSocialActivity> demoActivities = activityStorage.getUserSpacesActivities(demoIdentity, 0, 10);
    assertNotNull("demoActivities must not be null", demoActivities);
    assertEquals("demoActivities.size() must return: 10", 10, demoActivities.size());
    
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
    assertNotNull("space2 must not be null", space2);
    assertEquals("space2.getDisplayName() must return: my space 1", "my space 1", space2.getDisplayName());
    assertEquals("space2.getDescription() must return: add new space 1", "add new space 1", space2.getDescription());
    
    demoActivities = activityStorage.getUserSpacesActivities(demoIdentity, 0, 20);
    assertNotNull("demoActivities must not be null", demoActivities);
    assertEquals("demoActivities.size() must return: 20", 20, demoActivities.size());
    
    demoActivities = activityStorage.getUserSpacesActivities(demoIdentity, 0, 10);
    assertNotNull("demoActivities must not be null", demoActivities);
    assertEquals("demoActivities.size() must return: 10", 10, demoActivities.size());
    
    demoActivities = activityStorage.getUserSpacesActivities(johnIdentity, 0, 10);
    assertNotNull("demoActivities must not be null", demoActivities);
    assertEquals("demoActivities.size() must return: 0", 0, demoActivities.size());
    
    spaceService.deleteSpace(space);
    spaceService.deleteSpace(space2);
  }

  /**
   * Test {@link ActivityStorage#getNumberOfUserSpacesActivities(Identity)}
   * 
   * @throws Exception
   * @since 1.2.0-Beta3
   */
  @MaxQueryNumber(1700)
  public void testGetNumberOfUserSpacesActivities() throws Exception {
    SpaceService spaceService = this.getSpaceService();
    Space space = this.getSpaceInstance(spaceService, 0);
    Identity spaceIdentity = this.identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    
    int totalNumber = 10;
    
    //demo posts activities to space
    for (int i = 0; i < totalNumber; i ++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("activity title " + i);
      activity.setUserId(demoIdentity.getId());
      activityStorage.saveActivity(spaceIdentity, activity);
      tearDownActivityList.add(activity);
    }
    
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull("space must not be null", space);
    assertEquals("space.getDisplayName() must return: my space 0", "my space 0", space.getDisplayName());
    assertEquals("space.getDescription() must return: add new space 0", "add new space 0", space.getDescription());
    
    int number = activityStorage.getNumberOfUserSpacesActivities(demoIdentity);
    assertEquals("number must be: 10", 10, number);
    
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
    assertNotNull("space2 must not be null", space2);
    assertEquals("space2.getDisplayName() must return: my space 1", "my space 1", space2.getDisplayName());
    assertEquals("space2.getDescription() must return: add new space 1", "add new space 1", space2.getDescription());
    
    number = activityStorage.getNumberOfUserSpacesActivities(demoIdentity);
    assertEquals("number must be: 20", 20, number);
    
    spaceService.deleteSpace(space);
    spaceService.deleteSpace(space2);
  }

  

  
  
 
  
  /**
   * Test {@link ActivityStorage#getComments(ExoSocialActivity, int, int)}
   * 
   * @since 1.2.0-Beta3
   */
  @MaxQueryNumber(1644)
  public void testGetComments() {
    int totalNumber = 5;
    String activityTitle = "activity title";
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(activityTitle);
    activity.setUserId(rootIdentity.getId());
    activityStorage.saveActivity(rootIdentity, activity);
    tearDownActivityList.add(activity);
    
    for (int i = 0; i < totalNumber; i++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("Comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorage.saveComment(activity, comment);
    }
    
    List<ExoSocialActivity> comments = activityStorage.getComments(activity, 0, 40);
    assertNotNull("comments must not be null", comments);
    assertEquals(totalNumber, comments.size());
  }

  /**
   * Test {@link ActivityStorage#getNumberOfComments(ExoSocialActivity)}
   * 
   * @since 1.2.0-Beta3
   */
  @MaxQueryNumber(1644)
  public void testGetNumberOfComments() {
    int totalNumber = 5;
    String activityTitle = "activity title";
    
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle(activityTitle);
    activity.setUserId(rootIdentity.getId());
    activityStorage.saveActivity(rootIdentity, activity);
    tearDownActivityList.add(activity);
    
    for (int i = 0; i < totalNumber; i++) {
      //John comments on Root's activity
      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("Comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorage.saveComment(activity, comment);
    }
    
    List<ExoSocialActivity> comments = activityStorage.getComments(activity, 0, 40);
    assertNotNull("comments must not be null", comments);
    assertEquals(totalNumber, comments.size());
    
    int number = activityStorage.getNumberOfComments(activity);
    assertEquals(totalNumber, number);
  }

  /**
   *
   * 
   * @throws ActivityStorageException
   */
  @MaxQueryNumber(100)
  public void testGetStreamInfo() throws ActivityStorageException {
    checkCleanData();
    // root save on root's stream
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("blabla");
    activity.setUpdated(new Date());
    activity.setUserId(demoIdentity.getId());
    activityStorage.saveActivity(demoIdentity, activity);

    String streamId = activity.getStreamId();
    assertNotNull("streamId must not be null", streamId);
    assertEquals(activity.getStreamOwner(), demoIdentity.getRemoteId());

    ActivityStream activityStream = activity.getActivityStream();

    assertEquals("activityStream.getId() must return: " + streamId, streamId, activityStream.getId());

    assertEquals("activityStream.getPrettyId() must return: " + demoIdentity.getRemoteId(), demoIdentity.getRemoteId(), activityStream.getPrettyId());

    assertNotNull(activityStream.getPermaLink());

    List<ExoSocialActivity> activities = activityStorage.getUserActivities(demoIdentity, 0, 100);
    assertEquals(1, activities.size());
    assertEquals(demoIdentity.getRemoteId(), activities.get(0).getStreamOwner());
    assertEquals(streamId, activities.get(0).getStreamId());

    ExoSocialActivity loaded = activityStorage.getActivity(activity.getId());
    assertEquals(demoIdentity.getRemoteId(), loaded.getStreamOwner());
    assertEquals(streamId, loaded.getStreamId());

    tearDownActivityList.add(activity);
  }

  /**
   * Test {@link ActivityStorage#getUserActivities(Identity, long, long)}
   * 
   * @throws ActivityStorageException
   */
  @MaxQueryNumber(700)
  public void testGetActivitiesByPagingWithoutCreatingComments() throws ActivityStorageException {
    checkCleanData();
    final int totalActivityCount = 9;
    final int retrievedCount = 7;

    for (int i = 0; i < totalActivityCount; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("blabla");
      activityStorage.saveActivity(demoIdentity, activity);
      tearDownActivityList.add(activity);
    }

    List<ExoSocialActivity> activities = activityStorage.getUserActivities(demoIdentity, 0, retrievedCount);
    assertEquals(retrievedCount, activities.size());
  }

  /**
   * Test {@link ActivityStorage#getUserActivities(Identity, long, long) and 
   * ActivityStorage#saveComment(ExoSocialActivity, ExoSocialActivity)}
   * 
   * @throws ActivityStorageException
   */
  @MaxQueryNumber(200)
  public void testGetActivitiesByPagingWithCreatingComments() throws ActivityStorageException {
    checkCleanData();

    final int totalActivityCount = 2;
    final int retrievedCount = 1;

    for (int i = 0; i < totalActivityCount; i++) {
      // root save on john's stream
      ExoSocialActivity activity = new ExoSocialActivityImpl();
      activity.setTitle("blabla");
      activity.setUserId(johnIdentity.getId());

      activityStorage.saveActivity(johnIdentity, activity);
      activity = activityStorage.getUserActivities(johnIdentity, 0, 1).get(0);
      //for teardown cleanup
      tearDownActivityList.add(activity);

      //test activity has been created
      String streamId = activity.getStreamId();
      assertNotNull(streamId);
      assertEquals(activity.getStreamOwner(), johnIdentity.getRemoteId());

      ExoSocialActivity comment = new ExoSocialActivityImpl();
      comment.setTitle("this is comment " + i);
      comment.setUserId(johnIdentity.getId());
      activityStorage.saveComment(activity, comment);

    }

    List<ExoSocialActivity> activities = activityStorage.getUserActivities(johnIdentity, 0, retrievedCount);
    assertEquals(retrievedCount, activities.size());
  }

  /**
   * 
   * 
   * @throws ActivityStorageException
   */
  @MaxQueryNumber(100)
  public void testTemplateParams() throws ActivityStorageException {
    checkCleanData();
    final String URL_PARAMS = "URL";
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("blabla");
    activity.setUserId(rootIdentity.getId());
    activity.setUpdated(new Date());

    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put(URL_PARAMS, "http://xxxxxxxxxxxxxxxx/xxxx=xxxxx");
    activity.setTemplateParams(templateParams);

    activityStorage.saveActivity(rootIdentity, activity);

    tearDownActivityList.add(activity);

    activity = activityStorage.getUserActivities(rootIdentity, 0, 100).get(0);
    assertNotNull("activity must not be null", activity);
    assertNotNull("activity.getTemplateParams() must not be null", activity.getTemplateParams());
    assertEquals("http://xxxxxxxxxxxxxxxx/xxxx=xxxxx", activity.getTemplateParams().get(URL_PARAMS));
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
   * Checks clean data.
   * 
   * @since 1.2.0-Beta3
   */
  private void checkCleanData() {
    assertEquals("assertEquals(activityStorage.getActivities(rootIdentity).size() must be 0",
            0, activityStorage.getUserActivities(rootIdentity, 0,
            activityStorage.getNumberOfUserActivities(rootIdentity)).size());
    assertEquals("assertEquals(activityStorage.getActivities(johnIdentity).size() must be 0",
            0, activityStorage.getUserActivities(johnIdentity, 0,
            activityStorage.getNumberOfUserActivities(johnIdentity)).size());
    assertEquals("assertEquals(activityStorage.getActivities(maryIdentity).size() must be 0",
            0, activityStorage.getUserActivities(maryIdentity, 0,
            activityStorage.getNumberOfUserActivities(maryIdentity)).size());
    assertEquals("assertEquals(activityStorage.getActivities(demoIdentity).size() must be 0",
            0, activityStorage.getUserActivities(demoIdentity, 0,
            activityStorage.getNumberOfUserActivities(demoIdentity)).size());
  }

  /**
   * Deletes connections of identity.
   * 
   * @param existingIdentity
   * @since 1.2.0-Beta3
   */
  private void deleteConnections(Identity existingIdentity) {
    List<Relationship> allConnections = relationshipManager.getAll(existingIdentity);
    for (Relationship relationship : allConnections) {
      relationshipManager.remove(relationship);
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
