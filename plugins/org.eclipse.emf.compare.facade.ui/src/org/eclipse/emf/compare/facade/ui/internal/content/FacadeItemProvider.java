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

import com.google.common.collect.Collections2;

import java.util.Collection;

import org.eclipse.emf.compare.facade.FacadeAdapter;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.util.FacadeUtil;
import org.eclipse.emf.compare.provider.ExtendedItemProviderDecorator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemColorProvider;
import org.eclipse.emf.edit.provider.IItemFontProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;

/**
 * Item-provider decorator for façades.
 * 
 * @author Christian W. Damus
 */
public class FacadeItemProvider extends ExtendedItemProviderDecorator implements IEditingDomainItemProvider, ITreeItemContentProvider, IItemLabelProvider, IItemPropertySource, IItemColorProvider, IItemFontProvider {

	/** My façade provider. */
	private final IFacadeProvider facadeProvider;

	/**
	 * Initializes me with my factory and façade provider.
	 * 
	 * @param factory
	 *            my factory
	 * @param facadeProvider
	 *            my façade provider
	 */
	public FacadeItemProvider(ComposeableAdapterFactory factory, IFacadeProvider facadeProvider) {
		super(factory);

		this.facadeProvider = facadeProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<?> getChildren(Object object) {
		return Collections2.transform(super.getChildren(object), this::facadeElse);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean hasChildren(Object object) {
		return !getChildren(object).isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getParent(Object object) {
		Object result = super.getParent(object);

		if (object instanceof EObject) {
			if ((result == null)
					|| ((result instanceof Resource) && !((Resource)result).getContents().contains(object))) {

				EObject underlying = FacadeAdapter.getUnderlyingObject((EObject)object);
				if (underlying != null) {
					ITreeItemContentProvider tree = (ITreeItemContentProvider)getAdapterFactory()
							.adapt(underlying, ITreeItemContentProvider.class);
					if (tree != null) {
						result = tree.getParent(underlying);
					}
				}
			}
		}

		return result;
	}

	/**
	 * Obtains the façade view of an {@code object}.
	 * 
	 * @param object
	 *            an object
	 * @return its corresponding façade, or just the same {@code object} if there is no façade
	 */
	private Object facadeElse(Object object) {
		if (object instanceof EObject) {
			return FacadeUtil.facadeElse(facadeProvider, (EObject)object);
		} else {
			return object;
		}
	}

}
