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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.rcp.extension.AbstractRegistryEventListener;
import org.eclipse.emf.compare.rcp.internal.EMFCompareRCPMessages;
import org.eclipse.emf.compare.rcp.internal.extension.IItemDescriptor;
import org.eclipse.emf.compare.rcp.internal.extension.IItemRegistry;

/**
 * Listener that watches the façade provider extension point.
 * 
 * @author Christian W. Damus
 */
public class FacadeProviderRegistryListener extends AbstractRegistryEventListener {

	/** Attribute name for the factory implementation class. */
	public static final String CLASS = "class"; //$NON-NLS-1$

	/** Element name for the façade provider registration. */
	private static final String FACADE_PROVIDER_FACTORY = "facadeProviderFactory"; //$NON-NLS-1$

	/** Attribute name for the ranking. */
	private static final String RANKING = "ranking"; //$NON-NLS-1$

	/** Attribute name for the user-presentable label. */
	private static final String LABEL = "label"; //$NON-NLS-1$

	/** Attribute name for the user-presentable description. */
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$

	/** The façade provider registry to which extensions will be registered. */
	private final IItemRegistry<IFacadeProvider.Factory> facadeProviderRegistry;

	/**
	 * Initializes me with the façade provider registry that I maintain.
	 * 
	 * @param pluginID
	 *            the plugin ID of the extension point to be monitored
	 * @param extensionPointID
	 *            the extension point ID to be monitored
	 * @param log
	 *            the log to which to report problems
	 * @param registry
	 *            the façade provider registry to populate and maintain
	 */
	public FacadeProviderRegistryListener(String pluginID, String extensionPointID, ILog log,
			IItemRegistry<IFacadeProvider.Factory> registry) {

		super(pluginID, extensionPointID, log);

		this.facadeProviderRegistry = registry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean validateExtensionElement(IConfigurationElement element) {
		boolean result = true;

		switch (element.getName()) {
			case FACADE_PROVIDER_FACTORY:
				if (element.getAttribute(CLASS) == null) {
					logMissingAttribute(element, CLASS);
					result = false;
				} else if (element.getAttribute(RANKING) == null) {
					logMissingAttribute(element, RANKING);
					result = false;
				} else {
					String rankingStr = element.getAttribute(RANKING);
					try {
						Integer.parseInt(rankingStr);
					} catch (NumberFormatException nfe) {
						log(IStatus.ERROR, element,
								EMFCompareRCPMessages.getString("malformed.extension.attribute", //$NON-NLS-1$
										RANKING));
						result = false;
					}
				}
				break;
			default:
				result = false;
				break;
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean addedValid(IConfigurationElement element) {
		String label = element.getAttribute(LABEL);
		String description = element.getAttribute(DESCRIPTION);
		int rank = Integer.parseInt(element.getAttribute(RANKING));
		FacadeProviderFactoryDescriptor descriptor = new FacadeProviderFactoryDescriptor(label, description,
				rank, element, element.getAttribute(CLASS));

		IItemDescriptor<IFacadeProvider.Factory> previous = facadeProviderRegistry.add(descriptor);
		if (previous != null) {
			log(IStatus.WARNING, element, EMFCompareRCPMessages.getString("duplicate.extension", //$NON-NLS-1$
					facadeProviderRegistry.getClass().getName()));
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean removedValid(IConfigurationElement element) {
		facadeProviderRegistry.remove(element.getAttribute(CLASS));
		return true;
	}
}
