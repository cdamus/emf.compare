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

/**
 * This is the {@code BasicFacadeInputData} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@FacadeInput({"a1", "a2", "a3", "b1", "b2", "f1" }) // Generate resource accessors
public class BasicFacadeInputData extends BasicFacadeInputDataGen {

	/**
	 * Initializes me.
	 */
	public BasicFacadeInputData() {
		super();
	}

}
