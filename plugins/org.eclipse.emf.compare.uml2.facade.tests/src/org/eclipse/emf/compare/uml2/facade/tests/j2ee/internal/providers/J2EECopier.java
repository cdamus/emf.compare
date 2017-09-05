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
package org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers;

import java.util.stream.Stream;

import org.eclipse.emf.compare.uml2.facade.merge.UMLFacadeCopier;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.Finder;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.HomeInterface;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.FinderAdapter;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.HomeInterfaceAdapter;
import org.eclipse.uml2.uml.Interface;

/**
 * Copier for the J2EE façade that accounts for additional related objects just as the home and finder
 * interfaces' usages of their bean classes.
 *
 * @author Christian W. Damus
 */
public class J2EECopier extends UMLFacadeCopier {

	/**
	 * Initializes me.
	 */
	public J2EECopier() {
		super();
	}

	/**
	 * Gets the elements related to an UML interface that complete the definition of a J2EE home interface.
	 * 
	 * @param original
	 *            the original façade (source of the merge)
	 * @param originalUML
	 *            the original façade's underlying UML element
	 * @param copy
	 *            the copy façade (target of the merge)
	 * @param copyUML
	 *            the copy façade's underlying UML element
	 * @return pairs of related elements
	 */
	public Stream<Pair> getRelatedElements(HomeInterface original, Interface originalUML, HomeInterface copy,
			Interface copyUML) {

		return streamOf(pair(HomeInterfaceAdapter.get(original).getBeanRelationship(originalUML),
				HomeInterfaceAdapter.get(copy).getBeanRelationship(copyUML)));
	}

	/**
	 * Gets the elements related to an UML interface that complete the definition of a J2EE finder.
	 * 
	 * @param original
	 *            the original façade (source of the merge)
	 * @param originalUML
	 *            the original façade's underlying UML element
	 * @param copy
	 *            the copy façade (target of the merge)
	 * @param copyUML
	 *            the copy façade's underlying UML element
	 * @return pairs of related elements
	 */
	protected Stream<Pair> getRelatedElements(Finder original, Interface originalUML, Finder copy,
			Interface copyUML) {

		return streamOf(pair(FinderAdapter.get(original).getBeanRelationship(originalUML),
				FinderAdapter.get(copy).getBeanRelationship(copyUML)));
	}

}
