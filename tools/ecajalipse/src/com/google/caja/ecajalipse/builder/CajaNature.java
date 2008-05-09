// Copyright (C) 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.caja.ecajalipse.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Eclipse project nature associates the caja builder with a project
 *  
 * @author jasvir@gmail.com (Jasvir Nagra)
 */
public class CajaNature implements IProjectNature {

  /**
   * ID of this project nature
   */
  public static final String NATURE_ID = "com.google.caja.ecajalipse.cajaNature";

  private IProject project;

  /**
   * Executed when Caja nature is added to a project
   */
  public void configure() throws CoreException {
    IProjectDescription desc = project.getDescription();
    ICommand[] commands = desc.getBuildSpec();

    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(CajaBuilder.BUILDER_ID)) {
        return;
      }
    }

    ICommand[] newCommands = new ICommand[commands.length + 1];
    System.arraycopy(commands, 0, newCommands, 0, commands.length);
    ICommand command = desc.newCommand();
    System.out.println("Setting builder to" + CajaBuilder.BUILDER_ID);
    command.setBuilderName(CajaBuilder.BUILDER_ID);
    System.out.println("Setting buildcommand to" + command);
    newCommands[newCommands.length - 1] = command;
    desc.setBuildSpec(newCommands);
    project.setDescription(desc, null);
  }

  /**
   * Executed when Caja nature is removed from a project
   */
  public void deconfigure() throws CoreException {
    IProjectDescription description = getProject().getDescription();
    ICommand[] commands = description.getBuildSpec();
    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(CajaBuilder.BUILDER_ID)) {
        ICommand[] newCommands = new ICommand[commands.length - 1];
        System.arraycopy(commands, 0, newCommands, 0, i);
        System.arraycopy(commands, i + 1, newCommands, i,
            commands.length - i - 1);
        description.setBuildSpec(newCommands);
        return;
      }
    }
  }

  public IProject getProject() {
    return project;
  }

  public void setProject(IProject project) {
    this.project = project;
  }
}