/*******************************************************************************
 * Copyright (c) 2013, 2017 Obeo and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Philip Langer - bug 516520
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.internal.spec;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.ComparePackage;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.uml2.internal.impl.UMLDiffImpl;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * This specialization of the {@link UMLDiffImpl} class allows us to define the derived features and
 * operations implementations.
 * 
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
public class UMLDiffSpec extends UMLDiffImpl {

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.impl.DiffImpl#basicGetMatch()
	 */
	@Override
	public Match basicGetMatch() {
		if (eContainer() instanceof Match) {
			return (Match)eContainer();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.impl.DiffImpl#setMatch(org.eclipse.emf.compare.Match)
	 */
	@Override
	public void setMatch(Match newMatch) {
		Match oldMatch = basicGetMatch();
		if (newMatch != null) {
			EList<Diff> differences = newMatch.getDifferences();
			differences.add(this);
			if (eNotificationRequired()) {
				eNotify(new ENotificationImpl(this, Notification.SET, ComparePackage.DIFF__MATCH, oldMatch,
						newMatch));
			}
		} else if (eContainer() instanceof Match) {
			EList<Diff> differences = ((Match)eContainer()).getDifferences();
			differences.remove(this);
			if (eNotificationRequired()) {
				eNotify(new ENotificationImpl(this, Notification.UNSET, ComparePackage.DIFF__MATCH, oldMatch,
						newMatch));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.uml2.internal.impl.UMLDiffImpl#getPrimeRefining()
	 */
	@Override
	public Diff getPrimeRefining() {
		if (primeRefining == null) {
			for (Diff refBy : this.getRefinedBy()) {
				if (refBy instanceof ReferenceChange) {
					ReferenceChange rc = (ReferenceChange)refBy;
					if (rc.getReference().isContainment() && (this.getDiscriminant() == rc.getValue())) {
						primeRefining = rc;
						break;
					}
				}
			}
		}
		return primeRefining;
	}
}
