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

import org.crsh.cmdline.annotations.Argument
import org.crsh.cmdline.annotations.Command
import org.crsh.cmdline.annotations.Usage
import org.crsh.command.CRaSHCommand
import org.exoplatform.commons.utils.PropertyManager
import org.exoplatform.container.PortalContainer
import org.exoplatform.groovyscript.text.TemplateService

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Apr 29, 2013  
 */
@Usage("Exo groovy template management")
class gtmpl extends CRaSHCommand {
  private final static String pathKey = "exo.social.template.path";

  @Usage("Show all groovy template in caching.")
  @Command
  public Object cache() throws ScriptException {
    String[] list = getTemplateService.listCachedTemplates();

    //
    String print = "";
    list.each {print += it + " \n";}
    //
    return print;
  }
  
  @Usage("Clear groovy template caching.")
  @Command
  public Object clear() throws ScriptException {
    String[] list = getTemplateService.getTemplatesCache().clearCache();
    return "Done";
  }
  
  @Usage("Set path to social groovy template. Ex: gtmpl path /Users/thanhvc/java/eXoProjects/feature-relook/social40-feature/component/webui/src/main/resources/groovy/social/webui")
  @Command
  public Object path(@Argument String templatePath) throws ScriptException {
    PropertyManager.setProperty(pathKey, templatePath);
    if (PropertyManager.getProperty(pathKey).length() > 0) {
      return "Added";
    } else {
      return "Clear";
    }
  }
  
  private TemplateService getTemplateService() {
    return (TemplateService) getContainer().getComponentInstanceOfType(TemplateService.class);
  }
  
  private PortalContainer getContainer() {
    return PortalContainer.getInstance();
  }
}
