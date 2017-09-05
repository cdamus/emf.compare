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
package org.eclipse.emf.compare.facade.internal.merge;

import org.eclipse.emf.compare.facade.FacadeObject;
import org.eclipse.emf.compare.facade.FacadeProxy;
import org.eclipse.emf.compare.utils.EMFCompareCopier;
import org.eclipse.emf.ecore.EObject;

/**
 * A specialized compare copier for façade objects.
 *
 * @author Christian W. Damus
 */
class FacadeCopierImpl extends EMFCompareCopier {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Initializes me.
	 */
	FacadeCopierImpl() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected EObject createCopy(EObject eObject) {
		EObject result = super.createCopy(eObject);

		if ((eObject instanceof FacadeObject) && !(result instanceof FacadeObject)) {
			// Need a proxy
			result = FacadeProxy.createProxy(result);
		}

		return result;
	}
}
