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
package org.eclipse.emf.compare.facade.ui.internal.content;

import static org.eclipse.emf.compare.facade.util.FacadeUtil.getFacadeProvider;

import com.google.common.base.Predicate;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.tree.provider.TreeMergeViewerItemContentProvider;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.item.provider.IMergeViewerItemProviderConfiguration;
import org.eclipse.emf.compare.rcp.ui.structuremergeviewer.groups.IDifferenceGroupProvider;
import org.eclipse.emf.ecore.EObject;

/**
 * A specialized tree merge viewer item content provider that can weave children from façade providers into
 * the tree.
 *
 * @author Christian W. Damus
 */
public class FacadeMergeViewerItemContentProvider extends TreeMergeViewerItemContentProvider {

	/**
	 * Initializes me.
	 */
	public FacadeMergeViewerItemContentProvider() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object[] getChildren(Object object, IMergeViewerItemProviderConfiguration configuration) {
		return super.getChildren(object, getFacadeConfiguration(configuration));
	}

	/**
	 * Obtains the configuration most appropriate for working with façades in the comparison.
	 * 
	 * @param configuration
	 *            the real configuration
	 * @return a delegating configuration that handles façades, or else the original {@code configuration} if
	 *         it already handles or does not need to handle façades
	 */
	protected IMergeViewerItemProviderConfiguration getFacadeConfiguration(
			IMergeViewerItemProviderConfiguration configuration) {

		IMergeViewerItemProviderConfiguration result = configuration;

		IFacadeProvider facadeProvider = getFacadeProvider(configuration.getComparison());

		if ((facadeProvider != null) && (facadeProvider != IFacadeProvider.NULL_PROVIDER)) {
			AdapterFactory adapterFactory = new FacadeAdapterFactory(configuration.getAdapterFactory(),
					facadeProvider);

			result = new IMergeViewerItemProviderConfiguration() {

				@Override
				public AdapterFactory getAdapterFactory() {
					return adapterFactory;
				}

				//
				// Delegation
				//

				@Override
				public MergeViewerSide getSide() {
					return configuration.getSide();
				}

				@Override
				public IDifferenceGroupProvider getDifferenceGroupProvider() {
					return configuration.getDifferenceGroupProvider();
				}

				@Override
				public Predicate<? super EObject> getDifferenceFilterPredicate() {
					return configuration.getDifferenceFilterPredicate();
				}

				@Override
				public Comparison getComparison() {
					return configuration.getComparison();
				}
			};
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getParent(Object object, IMergeViewerItemProviderConfiguration configuration) {
		return super.getParent(object, getFacadeConfiguration(configuration));
	}
}
