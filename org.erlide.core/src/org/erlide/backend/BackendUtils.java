/*******************************************************************************
 * Copyright (c) 2008 Vlad Dumitrescu and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution.
 * 
 * Contributors:
 *     Vlad Dumitrescu
 *******************************************************************************/
package org.erlide.backend;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

public class BackendUtils {
	public static IProject[] getProjects(final String attribute) {
		final String[] projectNames = attribute.split(";");
		return getProjects(projectNames);
	}

	public static IProject[] getProjects(final String[] projectNames) {
		final List<IProject> projects = new ArrayList<IProject>();
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (final String s : projectNames) {
			if (s != null && s.length() > 0) {
				final IProject p = root.getProject(s);
				if (p != null) {
					projects.add(p);
				}
			}
		}
		return projects.toArray(new IProject[projects.size()]);
	}

	public static String getErlideNameSuffix() {
		String fUniqueId;
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final String location = root.getLocation().toPortableString();
		fUniqueId = Long.toHexString(location.hashCode() & 0xFFFFFFF);
		return fUniqueId;
	}

}