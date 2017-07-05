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
package org.eclipse.emf.compare.facade.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The bundle activator (plug-in) class.
 * 
 * @author Christian W. Damus
 */
public class EMFCompareFacadePlugin extends Plugin {

	/** The plug-in ID. */
	public static final String PLUGIN_ID = "org.eclipse.emf.compare.facade"; //$NON-NLS-1$

	/** This plug-in's shared instance. */
	private static EMFCompareFacadePlugin plugin;

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
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
}
