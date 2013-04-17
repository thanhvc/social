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
package crash.commands

import org.crsh.cli.Argument
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.exoplatform.container.PortalContainer
import org.exoplatform.services.cache.CacheService
import org.exoplatform.services.cache.ExoCache

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
@Usage("ExoCache management")
class cache {

  @Usage("Clean the specified cache.")
  @Command
  public Object clear(@Argument List<String> cacheNames) throws ScriptException {
    cacheNames.each {
      getCache(it).clearCache();
    }
    return "Done";
  }

  @Usage("Get the cache size.")
  @Command
  public Object size(@Argument List<String> cacheNames) throws ScriptException {
    String print = "";
    cacheNames.each { print += "$it : ${getCache(it).cacheSize}\n"; }
    return print;
  }

  @Usage("Get the cache capacity.")
  @Command
  public Object capacity(@Argument List<String> cacheNames) throws ScriptException {
    String print = "";
    cacheNames.each { print += "$it : ${getCache(it).maxSize}\n"; }
    return print;
  }

  @Usage("Get the cache live time.")
  @Command
  public Object time(@Argument List<String> cacheNames) throws ScriptException {
    String print = "";
    cacheNames.each { print += "$it : ${getCache(it).liveTime}\n"; }
    return print;
  }

  @Usage("Display all cache names")
  @Command
  public Object all() throws ScriptException {
    Collection caches = getCacheService().getAllCacheInstances();
    caches.each {
      ExoCache cache = ((ExoCache)it);
      print += "${cache.name}(${cache.maxSize}) : ${cache.cacheSize}\n";
    }
    return print;
  }

  private CacheService getCacheService() {
    PortalContainer container = PortalContainer.getInstance();
    return (CacheService) container.getComponentInstanceOfType(CacheService.class);
  }

  private ExoCache getCache(String cacheName) {
    return getCacheService().getCacheInstance(cacheName);
  }
}
