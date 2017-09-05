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
package org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.internal.providers;

import java.util.Map;

import org.eclipse.emf.compare.uml2.facade.merge.UMLFacadeCopier;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.OpaqexprPackage;
import org.eclipse.emf.ecore.EObject;

/**
 * Copier for the {@link OpaqexprPackage opaqexpr} façade that accounts for façade-side objects that do not
 * map to distinct objects in the underlying UML model and so need to have external identifiers synthesized
 * for them.
 *
 * @author Christian W. Damus
 */
public class OpaqexprCopier extends UMLFacadeCopier {

	/**
	 * Initializes me.
	 */
	public OpaqexprCopier() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIdentifier(EObject object) {
		String result = super.getIdentifier(object);

		if ((result == null) && OpaqexprPackage.Literals.BODY_ENTRY.isInstance(object)) {
			String containerID = getIdentifier(object.eContainer());
			if (containerID != null) {
				result = containerID + "$." + ((Map.Entry<?, ?>)object).getKey(); //$NON-NLS-1$
			}
		}

		return result;
	}

}
