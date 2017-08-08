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
package org.eclipse.emf.compare.facade.internal.merge;

import static org.eclipse.emf.compare.facade.FacadeProxy.createProxy;

import org.eclipse.emf.compare.facade.FacadeAdapter;
import org.eclipse.emf.compare.facade.FacadeObject;
import org.eclipse.emf.compare.merge.ICopier;
import org.eclipse.emf.ecore.EObject;

/**
 * A copier implementation for <em>Façade Models</em>.
 *
 * @author Christian W. Damus
 */
public class FacadeCopier implements ICopier {

	/**
	 * Initializes me.
	 */
	public FacadeCopier() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public EObject copy(EObject originalObject) {
		return new FacadeCopierImpl().copy(originalObject);
	}

	/**
	 * {@inheritDoc}
	 */
	public final void copyXMIIDs(EObject originalObject, EObject copy) {
		FacadeAdapter originalAdapter = FacadeAdapter.getInstance(originalObject);
		FacadeAdapter copyAdapter = FacadeAdapter.getInstance(copy);

		if ((originalAdapter == null) || (copyAdapter == null)) {
			DEFAULT.copyXMIIDs(originalObject, copy);
			return;
		}

		FacadeObject originalFacade = createProxy(originalAdapter.getFacade());
		FacadeObject copyFacade = createProxy(copyAdapter.getFacade());

		copyXMIIDs(originalFacade, copyFacade);
	}

	/**
	 * Copies the XMI IDs of the objects underlying façades.
	 * 
	 * @param originalObject
	 *            the original façade object
	 * @param copy
	 *            the copy of the original façade object
	 */
	protected void copyXMIIDs(FacadeObject originalObject, FacadeObject copy) {
		delegateCopyXMIIDs(originalObject.getUnderlyingElement(), copy.getUnderlyingElement());
	}

	/**
	 * <p>
	 * Delegates copying of XMIIDs for a pair of objects to the registry.
	 * </p>
	 * <p>
	 * <em>Caution:</em> the caller must ensure that this will not re-enter the current copier.
	 * </p>
	 * 
	 * @param originalObject
	 *            the original obejct
	 * @param copy
	 *            the copy
	 */
	protected void delegateCopyXMIIDs(EObject originalObject, EObject copy) {
		ICopier.Registry.INSTANCE.getCopier(originalObject).copyXMIIDs(originalObject, copy);
	}
}
