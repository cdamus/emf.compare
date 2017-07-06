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
package org.eclipse.emf.compare.uml2.facade.tests.data;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * This is the {@code BasicFacadeInputData} type. Enjoy.
 *
 * @author Christian W. Damus
 */
public class BasicFacadeInputData extends AbstractFacadeInputData {

	/**
	 * Initializes me.
	 */
	public BasicFacadeInputData() {
		super();
	}

	public Resource getA1Left() {
		return loadFacadeFromClassLoader("a1/left.uml"); //$NON-NLS-1$
	}

	public Resource getA1Right() {
		return loadFacadeFromClassLoader("a1/right.uml"); //$NON-NLS-1$
	}

	public Resource getA2Left() {
		return loadFacadeFromClassLoader("a2/left.uml"); //$NON-NLS-1$
	}

	public Resource getA2Right() {
		return loadFacadeFromClassLoader("a2/right.uml"); //$NON-NLS-1$
	}

}
