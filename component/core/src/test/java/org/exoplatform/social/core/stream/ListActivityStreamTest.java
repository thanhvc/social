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

import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.exoplatform.social.core.storage.cache.model.data.ListActivityStreamData;
import org.exoplatform.social.core.storage.cache.model.key.ActivityType;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.streams.event.DataChangeMerger;
import org.exoplatform.social.core.storage.streams.event.StreamChange;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 30, 2014  
 */
public class ListActivityStreamTest extends TestCase {
  private final StreamKey key = StreamKey.init("mary").key(ActivityType.FEED);
  
  @Override
  protected void tearDown() throws Exception {
    DataChangeMerger.reset();
  }

  public void testMoveUp() {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");
    
    ListActivityStreamData data = new ListActivityStreamData(key, list);
    
    data.moveTop("5", "mary");
    String got = data.getList().get(0);
    assertEquals("5", got);
    assertEquals(10, data.getList().size());
    
  }
  
  public void testPutUp() {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");
    
    ListActivityStreamData data = new ListActivityStreamData(key, list);
    
    data.putAtTop("11", "mary");
    String got = data.getList().get(0);
    assertEquals("11", got);
    got = data.getList().get(1);
    assertEquals("9", got);
    assertEquals(11, data.getList().size());
    
  }
  
  public void testDoublePutUp() {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");
    
    ListActivityStreamData data = new ListActivityStreamData(key, list);
    //first
    data.putAtTop("11", "mary");
    String got = data.getList().get(0);
    assertEquals("11", got);
    assertEquals(11, data.getList().size());
    //second
    data.putAtTop("11", "mary");
    assertEquals(1, DataChangeMerger.getSize());
  }
  
  public void testDoubleMoveUp() {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");

    ListActivityStreamData data = new ListActivityStreamData(key, list);

    // first
    data.moveTop("5", "mary");
    String got = data.getList().get(0);
    assertEquals("5", got);
    assertEquals(10, data.getList().size());

    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());

    // second
    data.moveTop("2", "mary");
    got = data.getList().get(0);
    assertEquals("2", got);
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    
    //one more
    data.moveTop("5", "mary");
    got = data.getList().get(0);
    assertEquals("5", got);
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
  }
  
  public void testRemove() {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");

    ListActivityStreamData data = new ListActivityStreamData(key, list);
    

    // first
    data.remove("3", "mary");
    assertEquals(9, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    
    
  }
  
  public void testRemoveTop() {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");

    ListActivityStreamData data = new ListActivityStreamData(key, list);

    // first
    data.remove("9", "mary");
    assertEquals(9, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    String got = data.getList().get(0);
    assertEquals("8", got);
  }
  
  public void testDoubleRemove() throws Exception {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");

    ListActivityStreamData data = new ListActivityStreamData(key, list);

    // first
    data.remove("3", "mary");
    assertEquals(9, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    //second
    data.remove("3", "mary");
    assertEquals(9, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    
  }
  
  public void testMixPutMoveAndRemove() throws Exception {
    List<String> list = new LinkedList<String>();
    list.add("9");
    list.add("8");
    list.add("7");
    list.add("6");
    list.add("5");
    list.add("4");
    list.add("3");
    list.add("2");
    list.add("1");
    list.add("0");

    ListActivityStreamData data = new ListActivityStreamData(key, list);

    // first PUT
    data.putAtTop("11", "mary");
    String got = data.getList().get(0);
    assertEquals("11", got);
    
    data.remove("3", "mary");
    assertEquals(10, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    //second MOVE
    data.remove("3", "mary");
    assertEquals(10, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    //third MOVE
  }
  
  public void testFromBegin() {
    ListActivityStreamData data = new ListActivityStreamData(key);
    
    data.putAtTop("0", "mary");
    data.putAtTop("1", "mary");
    data.putAtTop("2", "mary");
    data.putAtTop("3", "mary");
    data.putAtTop("4", "mary");
    data.putAtTop("5", "mary");
    assertEquals(6, data.getList().size());
    String top = data.getList().get(0);
    assertEquals("5", top);
    //expecting ADD = 6
    assertEquals(6, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    
    //move 0 to top
    data.moveTop("0", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(6, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    
    //move 1 to top
    data.moveTop("1", "mary");
    top = data.getList().get(0);
    assertEquals("1", top);
    assertEquals(6, data.getList().size());
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    
    //move 0 to top >> expecting 0 at the top, MOVE size = 2
    data.moveTop("0", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(6, data.getList().size());
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    assertEquals(6, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    
    //move 5 to top >> expecting 0 at the top, MOVE size = 3
    data.moveTop("5", "mary");
    top = data.getList().get(0);
    assertEquals("5", top);
    assertEquals(6, data.getList().size());
    assertEquals(3, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    assertEquals(6, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    
//    //DONE >> Here is improving point. when removing action is coming.
//    // 2 cases need to consider.
//    //+ Adds already happened recently(temporary status),....., then REMOVE is next >> All Changes must be clear.
//    //+ Pushed to storage, some MOVE ready, then REMOVE, All changes must be clear, just keep REMOVE 
//    
//    //remove 5 to top >> expecting 0 at the top
    data.remove("5", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(5, data.getList().size());
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    assertEquals(5, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    assertEquals(0, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
  }
  
  public void testComplexity() {
    ListActivityStreamData data = new ListActivityStreamData(key);
    
    data.putAtTop("1", "mary");
    data.putAtTop("0", "mary");
   
    assertEquals(2, data.getList().size());
    String top = data.getList().get(0);
    assertEquals("0", top);
    //expecting ADD = 2
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    
    //move 1 to top
    data.moveTop("1", "mary");
    top = data.getList().get(0);
    assertEquals("1", top);
    assertEquals(2, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    
    //move 0 to top >> expecting 0 at the top, MOVE size = 2
    data.moveTop("0", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(2, data.getList().size());
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    
    //DONE >> Here is improving point. when removing action is coming.
    // 2 cases need to consider.
    //+ Adds already happened recently(temporary status),....., then REMOVE is next >> All Changes must be clear. >> NOTHING
    //+ Pushed to storage, some MOVE ready, then REMOVE, All changes must be clear, just keep REMOVE 
    
    //remove 5 to top >> expecting 0 at the top
    data.remove("1", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(1, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    assertEquals(0, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
    
    
  }
  
  public void testComplexity1() {
    List<String> list = new LinkedList<String>();
    list.add("0");
    list.add("1");
    
    ListActivityStreamData data = new ListActivityStreamData(key, list);
    
    assertEquals(2, data.getList().size());
    String top = data.getList().get(0);
    assertEquals("0", top);
    //expecting ADD = 2
    assertEquals(0, DataChangeMerger.getChangeList(StreamChange.Kind.ADD).size());
    
    //move 1 to top
    data.moveTop("1", "mary");
    top = data.getList().get(0);
    assertEquals("1", top);
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    
    //move 0 to top >> expecting 0 at the top, MOVE size = 2
    data.moveTop("0", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(2, data.getList().size());
    assertEquals(2, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    
    //DONE >> Here is improving point. when removing action is coming.
    // 2 cases need to consider.
    //+ Adds already happened recently(temporary status),....., then REMOVE is next >> All Changes must be clear. >> NOTHING
    //+ Pushed to storage, some MOVE ready, then REMOVE, All changes must be clear, just keep REMOVE 
    
    //remove 5 to top >> expecting 0 at the top
    data.remove("1", "mary");
    top = data.getList().get(0);
    assertEquals("0", top);
    assertEquals(1, data.getList().size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.MOVE).size());
    assertEquals(1, DataChangeMerger.getChangeList(StreamChange.Kind.DELETE).size());
  }
}
