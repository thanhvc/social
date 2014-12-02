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
package org.exoplatform.social.core.storage.streams.persister;

import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.service.ProcessContext;
import org.exoplatform.social.common.service.SocialServiceContext;
import org.exoplatform.social.common.service.impl.SocialServiceContextImpl;
import org.exoplatform.social.core.storage.cache.model.key.StreamKey;
import org.exoplatform.social.core.storage.streams.SocialChromatticAsyncProcessor;
import org.exoplatform.social.core.storage.streams.StreamProcessContext;
import org.exoplatform.social.core.storage.streams.event.DataChange;
import org.exoplatform.social.core.storage.streams.event.StreamChange;
import org.exoplatform.social.core.storage.streams.listener.StreamChangeListener;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Oct
 * 30, 2014
 */
public class PersisterInvoker {

  private static final Log LOG = ExoLogger.getLogger(PersisterInvoker.class);

  /** */
  private static StreamChangeListener listener = new StreamChangeListener();

  /**
   * Invokes to records the activity to Stream
   * 
   * @param owner
   * @param entity
   * @param mentioners NULL is empty mentioner.
   * @return
   */
  public static <V, O> void persist(StreamKey key, List<DataChange<StreamChange<StreamKey, String>>> listChanges) {
    if (listChanges == null || listChanges.size() == 0) return;
    SocialServiceContext ctx = SocialServiceContextImpl.getInstance();
    StreamProcessContext processCtx = StreamProcessContext.getIntance(StreamProcessContext.PERSIST_ACTIVITIES_STREAM_PROCESS, SocialServiceContextImpl.getInstance());
    if (ctx.isAsync()) {
      ctx.getServiceExecutor().async(doPersist(key, listChanges), processCtx);
    } else {
      ctx.getServiceExecutor().execute(doPersist(key, listChanges), processCtx);
    }
  }

  private static <V, O> SocialChromatticAsyncProcessor doPersist(final StreamKey key,
                                                                 final List<DataChange<StreamChange<StreamKey, String>>> listChanges) {
    return new SocialChromatticAsyncProcessor(SocialServiceContextImpl.getInstance()) {
      @Override
      protected ProcessContext execute(ProcessContext processContext) throws Exception {
        LOG.debug(Thread.currentThread().getName() + " - persist with " + key.handle());
        for (DataChange<StreamChange<StreamKey, String>> change : listChanges) {
          change.dispatch(listener);
        }
        return processContext;
      }
    };
  }
}
