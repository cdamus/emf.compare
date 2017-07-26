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

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.adapters.J2EEFacadeFactory;
import org.eclipse.emf.compare.uml2.facade.tests.j2eeprofile.J2EEProfilePackage;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * The façade provider for J2EE in UML.
 *
 * @author Christian W. Damus
 */
public class J2EEFacadeProvider implements IFacadeProvider {
	private final J2EEFacadeFactory facadeFactory = new J2EEFacadeFactory();

	/**
	 * Initializes me.
	 */
	public J2EEFacadeProvider() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	public EObject createFacade(EObject underlyingObject) {
		return facadeFactory.doSwitch(underlyingObject);
	}

	//
	// Nested types
	//

	public static class Factory extends IFacadeProvider.Factory.AbstractImpl {
		public Factory() {
			super(J2EEFacadeProvider::new);
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isFacadeProviderFactoryFor(Notifier notifier) {
			return hasJ2EEPackage(notifier);
		}

		protected boolean hasJ2EEPackage(Notifier notifier) {
			boolean result = false;

			if (notifier instanceof EObject) {
				result = isJ2EEPackage((EObject)notifier);
			} else if (notifier instanceof Resource) {
				result = ((Resource)notifier).getContents().stream().limit(1L).anyMatch(this::isJ2EEPackage);
			} else if (notifier instanceof ResourceSet) {
				result = ((ResourceSet)notifier).getResources().stream().anyMatch(this::hasJ2EEPackage);
			}

			return result;
		}

		protected boolean isJ2EEPackage(EObject object) {
			return (object instanceof org.eclipse.uml2.uml.Package)
					&& ((org.eclipse.uml2.uml.Package)object).getAllAppliedProfiles().stream()
							.anyMatch(p -> J2EEProfilePackage.eNS_URI.equals(p.getURI()));
		}
	}
}
