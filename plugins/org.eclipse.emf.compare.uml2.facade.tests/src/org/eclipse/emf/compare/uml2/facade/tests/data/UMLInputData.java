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

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * This is the {@code UMLInputData} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@Input({"a1", "m1", "m2", "o1", "o2", "o3", "o4", "u1" }) // Generate resource accessors
public class UMLInputData extends UMLInputDataGen {

	/**
	 * Initializes me.
	 */
	public UMLInputData() {
		super();
	}

	/**
	 * Obtains the UML package contained in a {@code resource}.
	 * 
	 * @param resource
	 *            a resource
	 * @return its UML package, or {@code null} if none
	 */
	public org.eclipse.uml2.uml.Package getPackage(Resource resource) {
		return (org.eclipse.uml2.uml.Package)EcoreUtil.getObjectByType(resource.getContents(),
				UMLPackage.Literals.PACKAGE);
	}

}
