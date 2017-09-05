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
	@Override
	public EObject copy(EObject originalObject) {
		return new FacadeCopierImpl().copy(originalObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void copyXMIIDs(EObject originalObject, EObject copy) {
		FacadeAdapter originalAdapter = FacadeAdapter.getInstance(originalObject);
		FacadeAdapter copyAdapter = FacadeAdapter.getInstance(copy);

		if ((originalAdapter == null) || (copyAdapter == null)) {
			DEFAULT.copyXMIIDs(originalObject, copy);
			return;
		}

		copyXMIIDs(originalAdapter, copyAdapter);
	}

	/**
	 * Copies the XMI IDs of the objects underlying façades.
	 * 
	 * @param originalAdapter
	 *            the original façade object's adapter
	 * @param copy
	 *            the adapter of the copy of the original façade object
	 */
	protected void copyXMIIDs(FacadeAdapter originalAdapter, FacadeAdapter copy) {
		delegateCopyXMIIDs(originalAdapter.getUnderlyingElement(), copy.getUnderlyingElement());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIdentifier(EObject object) {
		String result = DEFAULT.getIdentifier(object);

		if (result == null) {
			// Maybe it's a façade? Access the underlying object
			EObject underlyingObject;

			if (object instanceof FacadeObject) {
				underlyingObject = ((FacadeObject)object).getUnderlyingElement();
			} else {
				underlyingObject = FacadeAdapter.getUnderlyingObject(object);
			}

			// Delegate to the underlying model
			ICopier delegate = ICopier.Registry.INSTANCE.getCopier(underlyingObject);
			result = delegate.getIdentifier(underlyingObject);
		}

		return result;
	}
}
