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
package org.exoplatform.social.core.eviction;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.ActivityManagerTest;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.storage.cache.eviction.CacheEvictionConfig;
import org.exoplatform.social.core.storage.cache.eviction.CacheEvictionManager;
import org.exoplatform.social.core.test.AbstractCoreTest;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 9, 2014  
 */
public class CacheEvictionManagerTest extends AbstractCoreTest {
  private final Log LOG = ExoLogger.getLogger(ActivityManagerTest.class);
  private List<ExoSocialActivity> tearDownActivityList;
  private Identity rootIdentity;
  private Identity johnIdentity;
  private Identity maryIdentity;
  private Identity demoIdentity;
  private Identity ghostIdentity;
  private Identity raulIdentity;
  private Identity jameIdentity;
  private Identity paulIdentity;

  private CacheEvictionManager evictionManager;
  private IdentityManager identityManager;
  private ActivityManager activityManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    evictionManager = (CacheEvictionManager) getContainer().getComponentInstanceOfType(CacheEvictionManager.class);
    identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
    activityManager =  (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
    tearDownActivityList = new ArrayList<ExoSocialActivity>();
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
    ghostIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "ghost", true);
    raulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "raul", true);
    jameIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "jame", true);
    paulIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "paul", true);
    
  }

  @Override
  public void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      try {
        activityManager.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }
    
    identityManager.deleteIdentity(rootIdentity);
    identityManager.deleteIdentity(johnIdentity);
    identityManager.deleteIdentity(maryIdentity);
    identityManager.deleteIdentity(demoIdentity);
    identityManager.deleteIdentity(ghostIdentity);
    identityManager.deleteIdentity(jameIdentity);
    identityManager.deleteIdentity(raulIdentity);
    identityManager.deleteIdentity(paulIdentity);
    super.tearDown();
  }
  
  public void testTaskSize() throws Exception {
    assertTrue(evictionManager.getNumberOfTasks() > 0);
    
    CacheEvictionConfig evictionConfig = evictionManager.getEvictionConfig("ActivityCache");
    assertNotNull(evictionConfig);
    assertEquals(1L, evictionConfig.getWakeupInterval());
  }
  
  public void testEvictionTask() throws Exception {
    this.populateActivityMass(johnIdentity, 10);
    List<ExoSocialActivity> johnActivities = activityManager.getActivities(johnIdentity, 0, 10);
    assertEquals(10, johnActivities.size());
    Thread.sleep(2000);
  }
  
  /**
   * Populates activity.
   * 
   * @param user
   * @param number
   */
  private void populateActivityMass(Identity user, int number) {
    for (int i = 0; i < number; i++) {
      ExoSocialActivity activity = new ExoSocialActivityImpl();;
      activity.setTitle("title " + i);
      activity.setUserId(user.getId());
      try {
        activityManager.saveActivityNoReturn(user, activity);
        tearDownActivityList.add(activity);
      } catch (Exception e) {
        LOG.error("can not save activity.", e);
      }
    }
  }

}
