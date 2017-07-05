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
package org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters;

import org.eclipse.emf.compare.uml2.facade.UMLFacadeResourceAdapter;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.Bean;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Stereotype applications adapter on UML resources for the J2EE façade.
 *
 * @author Christian W. Damus
 */
public class J2EEFacadeResourceAdapter extends UMLFacadeResourceAdapter {

	/**
	 * Initialize me.
	 */
	public J2EEFacadeResourceAdapter() {
		super();
	}

	/**
	 * Gets the existing adapter instance of the given {@code type} attached to a {@coed resource}, or else
	 * creates it.
	 * 
	 * @param resource
	 *            a UML model resource
	 * @param type
	 *            the type of adapter to obtain
	 * @return the adapter, created lazily if necessary
	 * @param <T>
	 *            the adapter type
	 */
	public static J2EEFacadeResourceAdapter getInstance(Resource resource) {
		return getInstance(resource, J2EEFacadeResourceAdapter.class);
	}

	/**
	 * Creates the bean façade, if any, for a class.
	 * 
	 * @param class_
	 *            an UML class
	 * @param bean
	 *            a bean stereotype application attached to it
	 * @return the bean façade
	 */
	public Bean createFacade(org.eclipse.uml2.uml.Class class_,
			org.eclipse.emf.compare.uml2.facade.tests.j2eeprofile.Bean bean) {

		return J2EEFacadeFactory.create(class_);
	}
}
