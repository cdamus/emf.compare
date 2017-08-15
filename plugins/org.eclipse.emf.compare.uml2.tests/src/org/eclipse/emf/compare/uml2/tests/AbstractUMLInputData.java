/**
 * Copyright (c) 2013, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Christian W. Damus - façade providers integration
 */
package org.eclipse.emf.compare.uml2.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.tests.framework.AbstractInputData;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.ResourcesPlugin;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

/**
 * Implementations of this class can be used to load models from the class' class loader.
 * 
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
@SuppressWarnings("nls")
public abstract class AbstractUMLInputData extends AbstractInputData implements AutoCloseable {
	/** Store the set of the resource sets of the input data. */
	private Set<ResourceSet> sets = new LinkedHashSet<ResourceSet>();

	public Set<ResourceSet> getSets() {
		return sets;
	}

	@Override
	protected Resource loadFromClassLoader(String string) throws IOException {

		final URL fileURL = getClass().getResource(string);
		final InputStream str = fileURL.openStream();
		final URI uri = URI.createURI(fileURL.toString());

		ResourceSet resourceSet = createResourceSet();

		Resource resource = resourceSet.createResource(uri);
		resource.load(str, Collections.emptyMap());
		str.close();

		return resource;
	}

	/**
	 * Creates a resource set and adds it to the {@linkplain #getSets() resource sets} tracked for later
	 * clean-up.
	 * 
	 * @return a new resource set suitable for working with UML models
	 */
	protected ResourceSet createResourceSet() {
		ResourceSet result = new ResourceSetImpl();
		getSets().add(result);

		// Standalone
		if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
			UMLResourcesUtil.init(result);

			// override wrong pathmap mapping in UMLResourcesUtil
			final URL UMLJarredFileLocation = ResourcesPlugin.class.getResource("ResourcesPlugin.class");
			String UMLJarPath = UMLJarredFileLocation.toString();
			UMLJarPath = UMLJarPath.substring(0, UMLJarPath.indexOf('!'));

			final Map<URI, URI> uriMap = URIConverter.URI_MAP;
			uriMap.put(URI.createURI(UMLResource.LIBRARIES_PATHMAP),
					URI.createURI(UMLJarPath + "!/libraries/"));
			uriMap.put(URI.createURI(UMLResource.METAMODELS_PATHMAP),
					URI.createURI(UMLJarPath + "!/metamodels/"));
			uriMap.put(URI.createURI(UMLResource.PROFILES_PATHMAP),
					URI.createURI(UMLJarPath + "!/profiles/"));

			// And add some missing registrations
			@SuppressWarnings("restriction")
			Resource.Factory resourceFactory = new org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl();
			Resource.Factory.Registry resourceFactoryReg = result.getResourceFactoryRegistry();
			resourceFactoryReg.getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, resourceFactory);
			resourceFactoryReg.getContentTypeToFactoryMap().put(UMLPackage.eCONTENT_TYPE, resourceFactory);
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		for (ResourceSet set : getSets()) {
			for (Resource res : set.getResources()) {
				res.unload();
			}
			set.getResources().clear();
		}
		getSets().clear();
	}
}
