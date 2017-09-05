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

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.rcp.internal.extension.IItemRegistry;
import org.eclipse.emf.compare.rcp.internal.extension.impl.ItemRegistry;
import org.osgi.framework.BundleContext;

/**
 * The bundle activator (plug-in) class.
 * 
 * @author Christian W. Damus
 */
public class EMFCompareFacadePlugin extends Plugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = "org.eclipse.emf.compare.facade"; //$NON-NLS-1$

	/** The id of the façade provider extension point. */
	public static final String FACADE_PROVIDER_PPID = "facadeProvider"; //$NON-NLS-1$

	/** This plug-in's shared instance. */
	private static EMFCompareFacadePlugin plugin;

	/** Whether to use dynamic proxies, which by default is {@code true}. */
	private static boolean useDynamicProxies = true;

	/** The registry that keeps references to façade provider factories. */
	private IItemRegistry<IFacadeProvider.Factory> facadeProviderRegistry;

	/** The API registry that keeps references to façade provider factories. */
	private FacadeProviderRegistryWrapper facadeProviderRegistryWrapper;

	/** A registry listener that will be used to watch the façade provider extension point. */
	private FacadeProviderRegistryListener facadeProviderRegistryListener;

	/**
	 * Obtains the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static EMFCompareFacadePlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		IExtensionRegistry registry = Platform.getExtensionRegistry();

		createFacadeProviderRegistry(registry);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		IExtensionRegistry registry = Platform.getExtensionRegistry();

		disposeFacadeProviderRegistry(registry);

		plugin = null;
		super.stop(context);
	}

	public IItemRegistry<IFacadeProvider.Factory> getFacadeProviderItemRegistry() {
		return facadeProviderRegistry;
	}

	public IFacadeProvider.Factory.Registry getFacadeProviderRegistry() {
		return facadeProviderRegistryWrapper;
	}

	/**
	 * Initialize the extension-based façade provider registry.
	 * 
	 * @param registry
	 *            {@link IExtensionRegistry} to listen to in order to fill the registry
	 */
	private void createFacadeProviderRegistry(IExtensionRegistry registry) {
		facadeProviderRegistry = new ItemRegistry<IFacadeProvider.Factory>();
		facadeProviderRegistryListener = new FacadeProviderRegistryListener(PLUGIN_ID, FACADE_PROVIDER_PPID,
				getLog(), facadeProviderRegistry);
		facadeProviderRegistryListener.readRegistry(registry);
		facadeProviderRegistryWrapper = new FacadeProviderRegistryWrapper(facadeProviderRegistry);
	}

	/**
	 * Discard the extension-based façade provider registry.
	 * 
	 * @param registry
	 *            IExtensionRegistry to remove listener(s) from
	 */
	private void disposeFacadeProviderRegistry(final IExtensionRegistry registry) {
		registry.removeListener(facadeProviderRegistryListener);
		facadeProviderRegistryListener = null;
		facadeProviderRegistry = null;
		facadeProviderRegistryWrapper = null;
	}

	/**
	 * Queries whether dynamic proxies are supplied for façade models that do not implement the
	 * {@code FacadeObject} interface.
	 * 
	 * @return whether dynamic proxies are provided on behalf of {@link IFacadeProvider}s
	 */
	public static boolean isUseDynamicProxies() {
		return useDynamicProxies;
	}

	/**
	 * Sets whether dynamic proxies are supplied for façade models that do not implement the
	 * {@code FacadeObject} interface.
	 * 
	 * @param useDynamicProxies
	 *            whether dynamic proxies are provided on behalf of {@link IFacadeProvider}s
	 */
	public static void setUseDynamicProxies(boolean useDynamicProxies) {
		EMFCompareFacadePlugin.useDynamicProxies = useDynamicProxies;
	}
}
