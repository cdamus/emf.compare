/*******************************************************************************
 * Copyright (c) 2014, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Christian W. Damus - support for EMap entry distance calculation
 *******************************************************************************/
package org.eclipse.emf.compare.match.eobject;

import com.google.common.collect.Maps;

import java.util.Map;

import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * The default implementation of {@link org.eclipse.emf.compare.match.eobject.WeightProvider} applicable to
 * all objects.
 * 
 * @since 3.1.0
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
public class DefaultWeightProvider extends AbstractWeightProvider {

	/** The name that the value feature of a map-entry {@link EClass} must have. */
	private static final String VALUE_FEATURE_NAME = "value"; //$NON-NLS-1$

	/**
	 * The list of specific weight to apply on specific Features.
	 */
	protected Map<EStructuralFeature, Integer> weights;

	/**
	 * Weight coefficient of a change on a reference.
	 */
	protected int referenceChangeCoef = NORMAL;

	/**
	 * Weight coefficient of a change on an attribute.
	 */
	protected int attributeChangeCoef = SIGNIFICANT;

	/**
	 * Create the weight provider.
	 */
	public DefaultWeightProvider() {
		weights = Maps.newHashMap();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getWeight(EStructuralFeature feature) {

		if (irrelevant(feature)) {
			return 0;
		}

		Integer found = weights.get(feature);
		if (found == null) {
			found = Integer.valueOf(SMALL);
			/*
			 * This is worst than empirical but it works in many cases, if your feature is a "name" its likely
			 * that it's important for matching the element. At some point I'll have to come up with something
			 * which is more extensible..
			 */
			if ("name".equals(feature.getName()) || "id".equals(feature.getName())) { //$NON-NLS-1$ //$NON-NLS-2$
				found = Integer.valueOf(SIGNIFICANT);
			}
			if (feature instanceof EReference) {
				found = Integer.valueOf(referenceChangeCoef * found.intValue());
			} else {
				found = Integer.valueOf(attributeChangeCoef * found.intValue());
			}
			weights.put(feature, found);
		}
		return found.intValue();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getParentWeight(EObject a) {
		return SIGNIFICANT;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getContainingFeatureWeight(EObject a) {
		return NORMAL;
	}

	/**
	 * return true i the feature is irrelevant for the comparison.
	 * 
	 * @param feat
	 *            any feature.
	 * @return true i the feature is irrelevant for the comparison.
	 */
	protected boolean irrelevant(EStructuralFeature feat) {
		if (feat instanceof EAttribute) {
			return feat.isDerived() || feat.isTransient() || isEMapEntryValue(feat);
		} else {
			EReference ref = (EReference)feat;
			return ref.isDerived() || ref.isTransient() || ref.isContainment() || ref.isContainer()
					|| isEMapEntryValue(feat);
		}
	}

	/**
	 * The value feature of an {@link EMap} entry is irrelevant because the keys in an {@code EMap} must be
	 * unique, so map entries always match by key only.
	 * 
	 * @param feature
	 *            a feature
	 * @return whether it is the value feature of an {@link EMap} entry
	 * @since 3.6
	 */
	protected boolean isEMapEntryValue(EStructuralFeature feature) {
		boolean result = false;

		EClass eClass = feature.getEContainingClass();
		if ((eClass != null) && (eClass.getInstanceClass() == Map.Entry.class)) {
			// Which feature is it?
			result = VALUE_FEATURE_NAME.equals(feature.getName());
		}

		return result;
	}
}
