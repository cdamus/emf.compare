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
package org.eclipse.emf.compare.facade.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.rcp.internal.extension.impl.LazyItemDescriptor;

/**
 * Descriptor for {@link IFacadeProvider.Factory} instances registered on the extension point.
 * 
 * @author Christian W. Damus
 */
public class FacadeProviderFactoryDescriptor extends LazyItemDescriptor<IFacadeProvider.Factory> {

	/**
	 * Initializes me.
	 * 
	 * @param label
	 *            my user-presentable label
	 * @param description
	 *            my user-presentable description
	 * @param rank
	 *            my rank
	 * @param config
	 *            my configuration element from the extension point
	 * @param id
	 *            my unique identifier
	 */
	public FacadeProviderFactoryDescriptor(String label, String description, int rank,
			IConfigurationElement config, String id) {
		super(label, description, rank, config, id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IFacadeProvider.Factory getItem() {
		IFacadeProvider.Factory result = null;

		try {
			result = (IFacadeProvider.Factory)getConfig()
					.createExecutableExtension(FacadeProviderRegistryListener.CLASS);
			result.setRanking(getRank());
		} catch (CoreException e) {
			EMFCompareFacadePlugin.getDefault().getLog().log(e.getStatus());
			result = IFacadeProvider.Factory.NULL_FACTORY;
		}

		return result;
	}

}
