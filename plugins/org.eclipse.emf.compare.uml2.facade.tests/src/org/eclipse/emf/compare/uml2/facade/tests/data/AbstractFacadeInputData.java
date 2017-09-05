/*******************************************************************************
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.facade.tests.data;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.emf.compare.uml2.facade.tests.j2ee.J2EEPackage;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.J2EEFacadeFactory;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.J2EEFacadeResourceAdapter;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.util.J2EEResource;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * This is the {@code AbstractFacadeInputData} type. Enjoy.
 *
 * @author Christian W. Damus
 */
public class AbstractFacadeInputData extends AbstractUMLInputData {

	/**
	 * Initializes me.
	 */
	public AbstractFacadeInputData() {
		super();
	}

	/**
	 * Loads the resource containing a façade model from the underlying UML model identified by a relative
	 * {@code path}.
	 * 
	 * @param path
	 *            the relative path to the UML model
	 * @return the J2EE façade model resource
	 * @throws IOException
	 *             on failure to load the UML model
	 */
	protected Resource loadFacadeFromClassLoader(String path) {
		Resource umlResource = null;

		try {
			umlResource = loadFromClassLoader(path);
		} catch (IOException e) {
			e.printStackTrace();
			fail(String.format("Failed to load test façade %s: %s", path, e.getMessage())); //$NON-NLS-1$
			return null; // Unreachable
		}

		// Ensure the adapter for stereotype applications
		J2EEFacadeResourceAdapter.getInstance(umlResource);

		Resource result = umlResource.getResourceSet().createResource(
				umlResource.getURI().trimFileExtension().appendFileExtension(J2EEResource.FILE_EXTENSION));

		org.eclipse.uml2.uml.Package uml = (org.eclipse.uml2.uml.Package)EcoreUtil
				.getObjectByType(umlResource.getContents(), UMLPackage.Literals.PACKAGE);
		org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package facade = J2EEFacadeFactory.create(uml);
		result.getContents().add(facade);

		return result;
	}

	/**
	 * Obtains the already-loaded resource in any of my resource sets that is loaded from the given
	 * {@code path}.
	 * 
	 * @param path
	 *            the resource's relative path
	 * @return the loaded resource
	 * @throws AssertionError
	 *             to fail the test if the resource is not found to have been loaded
	 */
	protected Resource getLoadedResource(String path) {
		return getSets().stream().map(ResourceSet::getResources).flatMap(Collection::stream)
				.filter(r -> r.getURI().toString().endsWith(path)).findAny()
				.orElseThrow(() -> new AssertionError("No such resource: " + path)); //$NON-NLS-1$
	}

	/**
	 * Obtains the J2EE façade package contained in a {@code resource}.
	 * 
	 * @param resource
	 *            a resource
	 * @return its J2EE façade package, or {@code null} if none
	 */
	public org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package getPackage(Resource resource) {
		return (org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package)EcoreUtil
				.getObjectByType(resource.getContents(), J2EEPackage.Literals.PACKAGE);
	}
}
