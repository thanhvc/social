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
package org.exoplatform.social.core.stream;

import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.StreamChange;
import org.exoplatform.social.core.stream.data.ActivityDataBuilder;
import org.exoplatform.social.core.stream.data.CachedActivityData;
import org.exoplatform.social.core.stream.data.CommentDataBuilder;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class ActivityPerfTest extends BaseTest {
  final int number = 1000;
  @Override
  public void initData() {
    if (CachedActivityData.feedSize(demo) == 0) {
      ActivityDataBuilder.initMore(number, demo).inject();
      List<ExoSocialActivity> feed = CachedActivityData.feed(demo, 0, 20);
      ListIterator<ExoSocialActivity> it = feed.listIterator(feed.size() - 1);
      while (it.hasPrevious()) {
        ExoSocialActivity activity = it.previous();
        CommentDataBuilder.initMore(10, activity, demo).inject();
      }
    }
  }
  
  @Override
  public void initConnecions() {}
  
  public void testChanges() throws Exception {
    CachedActivityData activityData = new CachedActivityData();
    
    List<StreamChange<StreamKey, String>> changes = CachedActivityData.feedChangeList(demo);
    assertEquals(number + 19, changes.size());

    //
    changes = CachedActivityData.feedChangeList(demo, StreamChange.Kind.ADD);
    assertEquals(number, changes.size());

    //
    changes = CachedActivityData.feedChangeList(demo, StreamChange.Kind.MOVE);
    assertEquals(19, changes.size());

    //await for finish persistence.
    CountDownLatch lock = activityData.getScheduler().getSynchronizationLock();
    System.out.println("Change size:" + DataChangeMerger.getSize());
    lock.await(2000, TimeUnit.MILLISECONDS);
    
    changes = CachedActivityData.feedChangeList(demo);
    assertEquals(0, changes.size());
    
    changes = CachedActivityData.myActivitiesChangeList(demo);
    assertEquals(0, changes.size());
    //
    clearContext(activityData);
  }
  
  

}
