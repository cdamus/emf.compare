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
package org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers;

import java.util.stream.Stream;

import org.eclipse.emf.compare.uml2.facade.merge.UMLFacadeXMIIDCopier;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.J2EEPackage;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.FinderAdapter;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.HomeInterfaceAdapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Interface;

/**
 * XMI ID copier for the J2EE façade that accounts for additional related objects just as the home and finder
 * interfaces' usages of their bean classes.
 *
 * @author Christian W. Damus
 */
public class J2EEXMIIDCopier extends UMLFacadeXMIIDCopier {

	/**
	 * Initializes me.
	 */
	public J2EEXMIIDCopier() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Stream<Pair> getRelatedElements(EObject original, Element originalUML, EObject copy,
			Element copyUML) {

		// The XML enablement expression ensured that we have the right package
		switch (original.eClass().getClassifierID()) {
			case J2EEPackage.HOME_INTERFACE:
				Interface originalHome = (Interface)originalUML;
				Interface copyHome = (Interface)copyUML;

				return streamOf(pair(HomeInterfaceAdapter.get(originalHome).getBeanRelationship(originalHome),
						HomeInterfaceAdapter.get(copyHome).getBeanRelationship(copyHome)));
			case J2EEPackage.FINDER:
				Interface originalFinder = (Interface)originalUML;
				Interface copyFinder = (Interface)copyUML;

				return streamOf(pair(FinderAdapter.get(originalFinder).getBeanRelationship(originalFinder),
						FinderAdapter.get(copyFinder).getBeanRelationship(copyFinder)));
			default: // BEAN etc.
				return Stream.empty();
		}
	}

}
