/**
 * Copyright (c) 2017 Christian W. Damus and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    Christian W. Damus - Initial API and implementation
 * 
 */
package org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.operations;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.NamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * <!-- begin-user-doc --> A static utility class that provides operations related to '<em><b>Named
 * Element</b></em>' model objects. <!-- end-user-doc -->
 *
 * <p>
 * The following operations are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.emf.compare.uml2.facade.tests.j2ee.NamedElement#getPackage() <em>Get Package</em>}</li>
 *   <li>{@link org.eclipse.emf.compare.uml2.facade.tests.j2ee.NamedElement#setPackage(org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package) <em>Set Package</em>}</li>
 * </ul>
 *
 * @generated
 */
public class NamedElementOperations {
	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @generated
	 */
	protected NamedElementOperations() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */
	public static org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package getPackage(
			NamedElement namedElement) {

		org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package result = null;
		EObject container = namedElement.eContainer();

		if (container instanceof org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package) {
			result = (org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package)container;
		}

		return result;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated NOT
	 */
	@SuppressWarnings("unchecked")
	public static void setPackage(NamedElement namedElement,
			org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package newPackage) {

		if (newPackage == null) {
			org.eclipse.emf.compare.uml2.facade.tests.j2ee.Package current = namedElement.getPackage();
			if (current != null) {
				EcoreUtil.remove(namedElement);
			}
		} else {
			newPackage.eClass().getEAllContainments().stream()
					.filter(ref -> ref.getEReferenceType().isSuperTypeOf(namedElement.eClass())).findAny()
					.ifPresent(ref -> ((EList<NamedElement>)newPackage.eGet(ref)).add(namedElement));
		}
	}

} // NamedElementOperations
