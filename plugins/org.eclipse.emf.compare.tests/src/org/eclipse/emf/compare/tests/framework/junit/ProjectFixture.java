/*
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *
 */
package org.eclipse.emf.compare.tests.framework.junit;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

/**
 * A test fixture that manages a workspace project for a test case.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("nls")
public class ProjectFixture implements AutoCloseable {

	private IProject project;

	/**
	 * Initializes me.
	 */
	public ProjectFixture() {
		super();
	}

	public IProject create(String name) {
		if (project != null) {
			throw new IllegalStateException("project exists");
		}

		project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (project.exists()) {
			// Clean it out, first
			try {
				project.delete(true, null);
			} catch (Exception e) {
				e.printStackTrace();
				fail("Failed to prepare test project: " + e.getMessage());
			}
		}

		try {
			project.create(null);
			project.open(null);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create test project: " + e.getMessage());
		}

		return project;
	}

	public IProject getProject() {
		return project;
	}

	public IFile createFile(String path, URL source) {
		IFile file = getFile(path);

		try (InputStream contents = source.openStream()) {
			if (file.exists()) {
				file.setContents(contents, IResource.FORCE, null);
			} else {
				file.create(contents, true, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create test file: " + e.getMessage());
		}

		return file;
	}

	public IFile getFile(String path) {
		return project.getFile(new Path(path));
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() throws Exception {
		if (project != null) {
			try {
				project.getWorkspace().run(new IWorkspaceRunnable() {

					public void run(IProgressMonitor monitor) throws CoreException {
						SubMonitor sub = SubMonitor.convert(monitor, 2);
						project.delete(true, sub.newChild(1));

						// Refresh the workspace to ensure that the state is synchronized
						// before the next test in case it tries to create the same project
						project.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE,
								sub.newChild(1));
						sub.done();
					}
				}, new NullProgressMonitor());
			} finally {
				project = null;
			}
		}
	}

}
