/*******************************************************************************
 * Copyright (c) 2005 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.ui.launch;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.erlide.backend.ErlLaunchAttributes;
import org.erlide.backend.ErlLaunchData;
import org.erlide.backend.ErtsProcess;
import org.erlide.common.CommonUtils;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.editors.erl.ErlangEditor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ErlangNodeLaunchShortcut implements ILaunchShortcut {

    public void launch(final ISelection selection, final String mode) {
        ErlLogger.debug("** Launch:: " + selection.toString());
        if (selection.isEmpty()) {
            return;
        }
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }
        final List<IErlProject> projects = Lists.newArrayList();
        final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        for (final Object element : structuredSelection.toArray()) {
            if (!(element instanceof IResource)) {
                return;
            }
            final IErlElement erlElement = ErlangCore.getModel().findElement(
                    (IResource) element);
            final IErlProject project = erlElement.getErlProject();
            if (project != null && !projects.contains(project)) {
                projects.add(project);
            }
        }
        if (projects.isEmpty()) {
            return;
        }
        try {
            doLaunch(mode, projects);
        } catch (final CoreException e) {
            final IWorkbench workbench = PlatformUI.getWorkbench();
            final Shell shell = workbench.getActiveWorkbenchWindow().getShell();
            MessageDialog.openError(shell, "Error", e.getStatus().getMessage());
        }
    }

    public void launch(final IEditorPart editor, final String mode) {
        ErlLogger.debug("** Launch :: " + editor.getTitle());
        if (editor instanceof ErlangEditor) {
            final ErlangEditor erlangEditor = (ErlangEditor) editor;
            final IErlModule module = erlangEditor.getModule();
            if (module != null) {
                final IErlProject project = module.getProject();
                if (project != null) {
                    try {
                        doLaunch(mode, Lists.newArrayList(project));
                    } catch (final CoreException e) {
                        final IWorkbench workbench = PlatformUI.getWorkbench();
                        final Shell shell = workbench
                                .getActiveWorkbenchWindow().getShell();
                        MessageDialog.openError(shell, "Error", e.getStatus()
                                .getMessage());
                    }
                }
            }
        }
    }

    private void doLaunch(final String mode, final List<IErlProject> projects)
            throws CoreException {
        final ILaunchConfiguration launchConfiguration = getLaunchConfiguration(
                projects, mode);
        try {
            bringConsoleViewToFront();
            mode.equals(ILaunchManager.DEBUG_MODE);

            launchConfiguration.launch(mode, null);
        } catch (final CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void bringConsoleViewToFront() throws PartInitException {
        final IWorkbenchWindow[] windows = PlatformUI.getWorkbench()
                .getWorkbenchWindows();
        for (final IWorkbenchWindow window : windows) {
            final IWorkbenchPage[] pages = window.getPages();
            for (final IWorkbenchPage page : pages) {
                final IViewPart view = page
                        .findView("org.eclipse.ui.console.ConsoleView");
                if (view != null) {
                    page.showView("org.eclipse.ui.console.ConsoleView");
                }
            }
        }
    }

    private ILaunchConfiguration getLaunchConfiguration(
            final List<IErlProject> projects, final String mode)
            throws CoreException {
        final ILaunchManager launchManager = DebugPlugin.getDefault()
                .getLaunchManager();
        final List<String> projectNames = getProjectNames(projects);
        final String name = CommonUtils.packList(projectNames, "_");
        // try and find one
        final ILaunchConfiguration[] launchConfigurations = launchManager
                .getLaunchConfigurations();
        for (final ILaunchConfiguration launchConfiguration : launchConfigurations) {
            if (launchConfiguration.getName().equals(name)) {
                if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                    return addInterpretedModules(projects, launchConfiguration);
                }
                return launchConfiguration;
            }
        }
        // try and make one
        final ILaunchConfigurationType launchConfigurationType = launchManager
                .getLaunchConfigurationType(ErtsProcess.CONFIGURATION_TYPE);
        ILaunchConfigurationWorkingCopy wc = null;
        wc = launchConfigurationType.newInstance(null, name);
        wc.setAttribute(ErlLaunchAttributes.PROJECTS, CommonUtils.packList(
                projectNames, ErlLaunchData.PROJECT_NAME_SEPARATOR));
        wc.setAttribute(ErlLaunchAttributes.RUNTIME_NAME, projects.get(0)
                .getRuntimeInfo().getName());
        wc.setAttribute(ErlLaunchAttributes.NODE_NAME, name);
        wc.setAttribute(ErlLaunchAttributes.CONSOLE, true);
        wc.setAttribute(ErlLaunchAttributes.INTERNAL, false);
        wc.setAttribute(ErlLaunchAttributes.LOAD_ALL_NODES, false);
        wc.setAttribute("org.eclipse.debug.core.environmentVariables",
                Maps.newHashMap());
        if (mode.equals("debug")) {
            final List<String> moduleNames = getProjectAndModuleNames(projects);
            wc.setAttribute(ErlLaunchAttributes.DEBUG_INTERPRET_MODULES,
                    moduleNames);
        }
        wc.setMappedResources(getProjectResources(projects));
        return wc.doSave();
    }

    private IResource[] getProjectResources(final List<IErlProject> projects) {
        final List<IResource> result = Lists.newArrayListWithCapacity(projects
                .size());
        for (final IErlProject project : projects) {
            result.add(project.getResource());
        }
        return result.toArray(new IResource[0]);
    }

    private List<String> getProjectNames(final List<IErlProject> projects) {
        final List<String> result = Lists.newArrayListWithCapacity(projects
                .size());
        for (final IErlProject project : projects) {
            result.add(project.getName());
        }
        return result;
    }

    private List<String> getProjectAndModuleNames(
            final List<IErlProject> projects) throws ErlModelException {
        final List<String> moduleNames = Lists.newArrayList();
        for (final IErlProject project : projects) {
            final Collection<IErlModule> modules = project.getModules();
            final String projectNameColon = project.getName() + ":";
            for (final IErlModule module : modules) {
                moduleNames.add(projectNameColon + module.getName());
            }
        }
        return moduleNames;
    }

    private ILaunchConfiguration addInterpretedModules(
            final List<IErlProject> projects,
            final ILaunchConfiguration launchConfiguration)
            throws CoreException {
        final List<String> moduleNames = getProjectAndModuleNames(projects);
        final ILaunchConfigurationWorkingCopy wc = launchConfiguration
                .getWorkingCopy();
        wc.setAttribute(ErlLaunchAttributes.DEBUG_INTERPRET_MODULES,
                moduleNames);
        return wc.doSave();
    }

}
