/*******************************************************************************
 * Copyright (c) 2006, 2007 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.match.metamodel.impl;

import org.eclipse.emf.compare.match.metamodel.Match2Elements;
import org.eclipse.emf.compare.match.metamodel.Match3Element;
import org.eclipse.emf.compare.match.metamodel.MatchFactory;
import org.eclipse.emf.compare.match.metamodel.MatchModel;
import org.eclipse.emf.compare.match.metamodel.MatchPackage;
import org.eclipse.emf.compare.match.metamodel.RemoteUnMatchElement;
import org.eclipse.emf.compare.match.metamodel.UnMatchElement;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc --> An implementation of the model <b>Factory</b>. <!-- end-user-doc -->
 * 
 * @generated
 */
@SuppressWarnings("nls")
public class MatchFactoryImpl extends EFactoryImpl implements MatchFactory {
	/**
	 * Creates an instance of the factory. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public MatchFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static MatchPackage getPackage() {
		return MatchPackage.eINSTANCE;
	}

	/**
	 * Creates the default factory implementation. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public static MatchFactory init() {
		try {
			MatchFactory theMatchFactory = (MatchFactory)EPackage.Registry.INSTANCE
					.getEFactory("http://www.eclipse.org/emf/compare/match/1.1"); //$NON-NLS-1$
			if (theMatchFactory != null) {
				return theMatchFactory;
			}
		} catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new MatchFactoryImpl();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case MatchPackage.MATCH_MODEL:
				return createMatchModel();
			case MatchPackage.MATCH2_ELEMENTS:
				return createMatch2Elements();
			case MatchPackage.MATCH3_ELEMENT:
				return createMatch3Element();
			case MatchPackage.UN_MATCH_ELEMENT:
				return createUnMatchElement();
			case MatchPackage.REMOTE_UN_MATCH_ELEMENT:
				return createRemoteUnMatchElement();
			default:
				throw new IllegalArgumentException(
						"The class '" + eClass.getName() + "' is not a valid classifier"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Match2Elements createMatch2Elements() {
		Match2ElementsImpl match2Elements = new Match2ElementsImpl();
		return match2Elements;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Match3Element createMatch3Element() {
		Match3ElementImpl match3Element = new Match3ElementImpl();
		return match3Element;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public MatchModel createMatchModel() {
		MatchModelImpl matchModel = new MatchModelImpl();
		return matchModel;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public RemoteUnMatchElement createRemoteUnMatchElement() {
		RemoteUnMatchElementImpl remoteUnMatchElement = new RemoteUnMatchElementImpl();
		return remoteUnMatchElement;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public UnMatchElement createUnMatchElement() {
		UnMatchElementImpl unMatchElement = new UnMatchElementImpl();
		return unMatchElement;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public MatchPackage getMatchPackage() {
		return (MatchPackage)getEPackage();
	}

} // MatchFactoryImpl
