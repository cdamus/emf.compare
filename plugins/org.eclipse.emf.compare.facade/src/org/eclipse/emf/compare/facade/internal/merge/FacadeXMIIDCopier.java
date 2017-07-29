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

import org.eclipse.emf.compare.facade.FacadeObject;
import org.eclipse.emf.compare.merge.IXMIIDCopier;
import org.eclipse.emf.ecore.EObject;

/**
 * This is the {@code FacadeXMIIDCopier} type. Enjoy.
 *
 * @author Christian W. Damus
 */
public class FacadeXMIIDCopier implements IXMIIDCopier {

	/**
	 * Initializes me.
	 */
	public FacadeXMIIDCopier() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public void copyXMIIDs(EObject originalObject, EObject copy) {
		if (!(originalObject instanceof FacadeObject) || !(copy instanceof FacadeObject)) {
			DEFAULT.copyXMIIDs(originalObject, copy);
			return;
		}

		FacadeObject originalFacade = (FacadeObject)originalObject;
		FacadeObject copyFacade = (FacadeObject)copy;

		IXMIIDCopier.Registry.INSTANCE.getXMIIDCopier(originalFacade.getUnderlyingElement())
				.copyXMIIDs(originalFacade.getUnderlyingElement(), copyFacade.getUnderlyingElement());
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
		IXMIIDCopier.Registry.INSTANCE.getXMIIDCopier(originalObject).copyXMIIDs(originalObject, copy);
	}
}
