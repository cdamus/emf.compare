/*******************************************************************************
 * Copyright (c) 2016, 2017 EclipseSource Services GmbH, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Philip Langer - initial API and implementation
 *     Christian W. Damus - façade providers integration
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.tests.merge.data;

import java.io.IOException;

import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

public class InstanceSpecificationClassifiersMergeInputData extends AbstractUMLInputData {
	public Resource getInstanceSpec1Left() throws IOException {
		return loadFromClassLoader("instancespec1/left.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec1Right() throws IOException {
		return loadFromClassLoader("instancespec1/right.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec2Left() throws IOException {
		return loadFromClassLoader("instancespec2/left.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec2Right() throws IOException {
		return loadFromClassLoader("instancespec2/right.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec3Left() throws IOException {
		return loadFromClassLoader("instancespec3/left.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec3Right() throws IOException {
		return loadFromClassLoader("instancespec3/right.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec4Left() throws IOException {
		return loadFromClassLoader("instancespec4/left.uml", createResourceSet()); //$NON-NLS-1$
	}

	public Resource getInstanceSpec4Right() throws IOException {
		return loadFromClassLoader("instancespec4/right.uml", createResourceSet()); //$NON-NLS-1$
	}

	@Override
	protected ResourceSet createResourceSet() {
		final ResourceSet resourceSet = new ResourceSetImpl();
		UMLResourcesUtil.init(resourceSet);
		return resourceSet;
	}
}
