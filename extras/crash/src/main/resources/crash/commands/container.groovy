package crash.commands

import org.crsh.cli.Argument
import org.crsh.cli.Command
import org.crsh.cli.Usage
import org.crsh.command.CRaSHCommand
import org.exoplatform.commons.utils.PropertyManager
import org.exoplatform.container.PortalContainer
import org.exoplatform.container.configuration.ConfigurationManager
import org.exoplatform.container.configuration.ConfigurationManagerImpl
import org.exoplatform.container.xml.Component
import org.exoplatform.container.xml.ComponentLifecyclePlugin
import org.exoplatform.container.xml.Configuration
import org.exoplatform.services.naming.InitialContextInitializer
import org.gatein.common.io.IOTools
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

/**
 * Created by The eXo Platform SAS
 * Author : Thanh Vu Cong
 *          thanh_vucong@exoplatform.com
 * Apr 13, 2013  
 */
@Usage("Exo Container Utilities")
public class container extends CRaSHCommand {

  @Usage("Using to show all of configuration files for eXo Container")
  @Command
  public Object configs() throws ScriptException {
    return getContainer().getConfigurationXML();
  }
  
  @Usage("Show all of lifecycle plugins")
  @Command
  public Object plugins() throws ScriptException {
    Configuration config = getContainer().getConfiguration();
    Collection<?> plugins = config.componentLifecyclePlugins;
    String print = "";
    
    //
    plugins.each {
      if (it instanceof ComponentLifecyclePlugin) {
        ComponentLifecyclePlugin lifecycle = (ComponentLifecyclePlugin) it;
        print += "${comp.name} : ${comp.documentURL.toString()} \n";
      }
    }
    
    return print;
  }
  
  @Usage("Show all of components")
  @Command
  public Object components() throws ScriptException {
    Configuration config = getContainer().getConfiguration();
    Collection<?> components = config.components;
    String print = "";

    components.each {
      if (it instanceof Component) {
        Component comp = (Component) it;
        print += "${comp.type} : ${comp.documentURL.toString()} \n";
      }
      
    }
    
    return print;
  }
  
  @Usage("Show all of components in given product such as forum, social, ecm, platform, and calendar")
  @Command
  public Object components(@Usage("given produce name such as forum, social, ecm, platform, and calendar") @Argument String productName) throws ScriptException {
    Configuration config = getContainer().getConfiguration();
    Collection<?> components = config.components;
    String print = "";

    components.each {
      if (it instanceof Component) {
        Component comp = (Component) it;
        if (comp.documentURL.toString().indexOf(productName) > 0) {
          print += "${comp.type} : ${comp.documentURL.toString()} \n";
        }
        
      }
      
    }
    
    return print;
  }
  
  @Usage("Find component in configuration file")
  @Command
  public Object find(@Usage("given component name what needs to find") @Argument String componentName) throws ScriptException {
    Configuration config = getContainer().getConfiguration();
    Collection<?> components = config.components;
    String print = "";

    //the documentURL = jndi:/localhost/social-extension/WEB-INF/conf/social-extension/social/core-configuration.xml 
    //it will be managed under Tomcat's servlet context.
    //more detail: ConfigurationManagerImpl#getURL() method at line 359
    components.each {
      if (it instanceof Component) {
        Component comp = (Component) it;
        
        if (comp.type.indexOf(componentName) > 0) {
          print += "${comp.type} : ${comp.documentURL.path} \n";
        }
      }
      
    }
    
    return print;
  }
  
  @Usage("show component in configuration file: example: container config SpaceService")
  @Command
  public Object config(@Usage("given component name what need to show configuration") @Argument String componentName) throws ScriptException {
    Configuration config = getContainer().getConfiguration();
    Collection<Component> components = (Collection<Component>) config.components;
    Component got = null;
    
    //
    for(Component component : components) {
      if (component.type.indexOf(componentName) > 0) {
        got = component;
        break;
      }
    }
    
    //
    if (got == null) {
      return "Component " + componentName + " not found.";
    }
    
    //
    
    ConfigurationManagerImpl manager = getConfigurationManager();
    
    BufferedReader is = new BufferedReader(
      new InputStreamReader(got.documentURL.openStream()));
    
    if (is == null) {
      return "Failed to get InputStream: " + got.documentURL.path;
    }
    
    StringWriter buffer = new StringWriter();
    
    IOTools.copy(is, buffer, 1);
    IOTools.safeClose(is);
    
    return buffer.toString();
  }
  
  @Usage("Hack to set developing mode in Exo")
  @Command
  public Object hack() throws ScriptException {
    PropertyManager.setProperty(PropertyManager.DEVELOPING, "true");
    if (PropertyManager.isDevelopping()) {
      return "Done";
    } else {
      return "Failed";
    }
  }

  private InitialContextInitializer getInitialContext() {
    return (InitialContextInitializer) getContainer().getComponentInstanceOfType(InitialContextInitializer.class);
  }
  
  private ConfigurationManagerImpl getConfigurationManager() {
    return (ConfigurationManagerImpl) getContainer().getComponentInstanceOfType(ConfigurationManager.class);
  }
  
  private PortalContainer getContainer() {
    return PortalContainer.getInstance();
  }
}
