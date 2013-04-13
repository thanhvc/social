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
package groovy.social

import org.crsh.cmdline.annotations.Command
import org.crsh.cmdline.annotations.Usage
import org.exoplatform.container.PortalContainer
import org.exoplatform.web.application.javascript.JavascriptConfigService
import org.gatein.portal.controller.resource.script.Module
import org.gatein.portal.controller.resource.script.ScriptResource
import org.gatein.portal.controller.resource.script.Module.Local
import org.gatein.portal.controller.resource.script.Module.Remote

/**
 * Created by The eXo Platform SAS
 * Author : Thanh Vu Cong
 *          thanh_vucong@exoplatform.com
 * Apr 13, 2013  
 */
@Usage("Javascript management")
public class js extends org.crsh.jcr.command.JCRCommand {
  
  private final static String SOCIAL_RESOURCE = "/social-resources";
  private final static String FORUM_RESOURCE = "/forumResources";

  @Usage("Show all of module in JavaScriptManager")
  @Command
  public Object all() throws ScriptException {
    List<ScriptResource> all = getJavascriptConfigService().getAllResources();

    String print = "";

    all.each {
      List<Module> modules = it.getModules();
      print += "${it.getId()} :\n";
      modules.each {
        if (it instanceof Local) {
          Local local = (Local)it;
          print += "ContextPath : ${local.getContextPath()} \n "
        } else if (it instanceof Remote) {
        }
      }
    }
    
    return print;
  }
  
  @Usage("Show SOCIAL of module in JavaScriptManager")
  @Command
  public Object social() throws ScriptException {
    return getResources(SOCIAL_RESOURCE);
  }
  
  @Usage("Show FORUM of module in JavaScriptManager")
  @Command
  public Object forum() throws ScriptException {
    return getResources(FORUM_RESOURCE);
  }
  
  private String getResources(String productName) {
    List<ScriptResource> all = getJavascriptConfigService().getAllResources();

    String print = "";
    String parent = "";

    all.each {
      List<Module> modules = it.getModules();
      parent = "${it.getId()} : {\n";
      modules.each {
        if (it instanceof Local) {
          Local local = (Local)it;
          if (productName.equals(local.getContextPath()) && local.getURI().trim().length() > 0) {
            print += parent;
            print += " - uri : ${local.getURI()} \n "
            print += "}\n";
          }
        } else if (it instanceof Remote) {
        }
      }
    }

    return print;
  }
  
  

  
  private JavascriptConfigService getJavascriptConfigService() {
    PortalContainer container = PortalContainer.getInstance();
    return (JavascriptConfigService) container.getComponentInstanceOfType(JavascriptConfigService.class);
  }
}
