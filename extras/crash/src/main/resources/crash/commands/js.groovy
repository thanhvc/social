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

import org.crsh.command.CRaSHCommand
import org.exoplatform.container.PortalContainer
import org.exoplatform.web.application.javascript.JavascriptConfigService
import org.gatein.common.io.IOTools
import org.gatein.portal.controller.resource.ResourceId
import org.gatein.portal.controller.resource.ResourceScope
import org.gatein.portal.controller.resource.script.Module
import org.gatein.portal.controller.resource.script.ScriptResource
import org.gatein.portal.controller.resource.script.Module.Local
import org.gatein.portal.controller.resource.script.Module.Remote
import org.crsh.cmdline.annotations.Usage
import org.crsh.cmdline.annotations.Command
import org.crsh.cmdline.annotations.Argument
import org.crsh.cmdline.annotations.Required

/**
 * Created by The eXo Platform SAS
 * Author : Thanh Vu Cong
 *          thanh_vucong@exoplatform.com
 * Apr 13, 2013  
 */
@Usage("Javascript management")
class js extends CRaSHCommand {
  
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
  
  @Usage("Show content of module in JavaScriptManager by uri such as: 'UIActivityUpdates.js' or '/social-resources/javascript/eXo/social/webui/UIActivityUpdates.js'")
  @Command
  public Object show(@Argument String uri) throws ScriptException {
    Locale local = Locale.ENGLISH;
    Module module = getModule(uri);
    Reader reader = getJavascriptConfigService().getJavascript(module, local);
    
    //
    StringWriter buffer = new StringWriter();
    IOTools.copy(reader, buffer, 1);
    return buffer.toString();
  }
  
  @Usage("Show resource by resourceName such as: 'social-ui-activity-updates'")
  @Command
  public Object resource(@Argument String resourceName) throws ScriptException {
    ResourceId resourceId = new ResourceId(ResourceScope.SHARED, resourceName);
    ScriptResource scriptResource = getJavascriptConfigService().getResource(resourceId);
    String print = "";
    List<Module> modules = scriptResource.getModules();

    //
    print = "${resourceId.toString()} : {\n";
    modules.each {
      if (it instanceof Local) {
        Local local = (Local)it;
        print += " - uri : ${local.getURI()} \n ";
      } else if (it instanceof Remote) {
      }
      print += "}\n";
    }

    return print;
  }
  
  /**
   * Gets all of resource by Product name
   * 
   * @param productName
   * @return
   */
  private String getResources(String productName) {
    List<ScriptResource> all = getJavascriptConfigService().getAllResources();

    String print = "";
    String parent = "";

    all.each {
      List<Module> modules = it.getModules();

      //
      parent = "${it.getId().toString()} : {\n";
      modules.each {
        if (it instanceof Local) {
          Local local = (Local)it;
          if (productName.equals(local.getContextPath()) && local.getURI().trim().length() > 0) {
            print += parent;
            print += " - uri : ${local.getURI()} \n ";
            print += "}\n";
          }
        } else if (it instanceof Remote) {
        }
      }
    }

    return print;
  }
  
  /**
   * Gets module by uri
   * @param uri
   * @return
   */
  private Module getModule(String uri) {
    List<ScriptResource> all = getJavascriptConfigService().getAllResources();

    Module got = null;

    for(ScriptResource resource : all) {
      List<Module> modules = resource.getModules();
      for(Module module : modules) {
        if (module instanceof Local) {
          Local local = (Local)module;
          if (local.getURI().indexOf(uri) >= 0) {
            got = local;
            break;
          }
        }
      }
    }
    
    return got;
  }
    
  private JavascriptConfigService getJavascriptConfigService() {
    PortalContainer container = PortalContainer.getInstance();
    return (JavascriptConfigService) container.getComponentInstanceOfType(JavascriptConfigService.class);
  }
}
