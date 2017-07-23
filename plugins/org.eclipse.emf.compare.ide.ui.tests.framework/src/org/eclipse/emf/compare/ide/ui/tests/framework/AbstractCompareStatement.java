/*******************************************************************************
 * Copyright (c) 2016, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Mathias Schaefer - preferences refactoring
 *     Christian W. Damus - integration of façade providers
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.tests.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareIDEUIPlugin;
import org.eclipse.emf.compare.ide.ui.internal.preferences.EMFCompareUIPreferences;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.internal.preferences.EMFComparePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * EMFCompare specific statements must extends this class.
 * 
 * @author <a href="mailto:mathieu.cartaud@obeo.fr">Mathieu Cartaud</a>
 */
@SuppressWarnings("restriction")
public abstract class AbstractCompareStatement extends Statement {

	/** The class selector for the extension point. */
	private static final String EXTENSION_POINT_CLASS_SELECTOR = "impl"; //$NON-NLS-1$

	/** The id selector for the extension point. */
	private static final String EXTENSION_POINT_ID_SELECTOR = "id"; //$NON-NLS-1$

	/** The separator used to build a string of preferences. */
	private static final String PREFERENCES_SEPARATOR = ";"; //$NON-NLS-1$

	/** Separator used to construct the following IDs. */
	private static final String ID_SEPARATOR = "."; //$NON-NLS-1$

	/** The id of the diff extension point. */
	private static final String DIFF_EXTENSION_POINT_ID = EMFCompareRCPPlugin.PLUGIN_ID + ID_SEPARATOR
			+ EMFCompareRCPPlugin.DIFF_ENGINE_PPID;

	/** The id of the equivalence extension point. */
	private static final String EQ_EXTENSION_POINT_ID = EMFCompareRCPPlugin.PLUGIN_ID + ID_SEPARATOR
			+ EMFCompareRCPPlugin.EQUI_ENGINE_PPID;

	/** The id of the requirement extension point. */
	private static final String REQ_EXTENSION_POINT_ID = EMFCompareRCPPlugin.PLUGIN_ID + ID_SEPARATOR
			+ EMFCompareRCPPlugin.REQ_ENGINE_PPID;

	/** The id of the conflict extension point. */
	private static final String CONFLICT_EXTENSION_POINT_ID = EMFCompareRCPPlugin.PLUGIN_ID + ID_SEPARATOR
			+ EMFCompareRCPPlugin.CONFLICT_DETECTOR_PPID;

	/** The default disabled match engines. */
	private static final List<String> DEFAULT_DISABLED_MATCH_ENGINES = Collections.emptyList();

	/** The default diff engine. */
	private static final String DEFAULT_DIFF_ENGINE = "org.eclipse.emf.compare.rcp.default.diffEngine"; //$NON-NLS-1$

	/** The default eq engine. */
	private static final String DEFAULT_EQ_ENGINE = "org.eclipse.emf.compare.rcp.default.equiEngine"; //$NON-NLS-1$

	/** The default req engine. */
	private static final String DEFAULT_REQ_ENGINE = "org.eclipse.emf.compare.rcp.default.reqEngine"; //$NON-NLS-1$

	/** The default conflict detector. */
	private static final String DEFAULT_CONFLICT_DETECTOR = "org.eclipse.emf.compare.rcp.fast.conflictDetector"; //$NON-NLS-1$

	/** The default disabled post-processors. */
	private static final List<String> DEFAULT_DISABLED_POST_PROCESSORS = Collections.emptyList();

	/** The default disabled façade providers. */
	private static final List<String> DEFAULT_DISABLED_FACADE_PROVIDERS = Collections.emptyList();

	/** The test class. */
	protected final Object testObject;

	/** The test method that will be run. */
	protected final FrameworkMethod test;

	/** The EMFCompare preferences. */
	private final IPreferenceStore rcpPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
			EMFCompareRCPPlugin.PLUGIN_ID);

	/** The EMFCompare UI preferences. */
	private final IPreferenceStore uiPreferenceStore = EMFCompareIDEUIPlugin.getDefault()
			.getPreferenceStore();

	/** The resolution strategy used for this test. */
	private final ResolutionStrategyID resolutionStrategy;

	/** The match engines disabled used for this test. */
	private final Class<?>[] disabledMatchEngines;

	/** The diff engine used for this test. */
	private final Class<?> diffEngine;

	/** The eq engine used for this test. */
	private final Class<?> eqEngine;

	/** The req engine used for this test. */
	private final Class<?> reqEngine;

	/** The conflict detector used for this test. */
	private final Class<?> conflictDetector;

	/** The post-processors disabled for this test. */
	private final Class<?>[] disabledPostProcessors;

	/** The façade providers disabled for this test. */
	private final Class<?>[] disabledFacadeProviders;

	/** The default resolution strategy. */
	private String defaultResolutionStrategy = "WORKSPACE"; //$NON-NLS-1$

	/**
	 * Constructor for the classic (no Git) comparison statement.
	 * 
	 * @param testObject
	 *            The test class
	 * @param test
	 *            The test method
	 * @param resolutionStrategy
	 *            The resolution strategy used for this test
	 * @param configuration
	 *            EMFCompare configurations for this test
	 */
	public AbstractCompareStatement(Object testObject, FrameworkMethod test,
			ResolutionStrategyID resolutionStrategy, EMFCompareTestConfiguration configuration) {
		this.testObject = testObject;
		this.test = test;
		this.resolutionStrategy = resolutionStrategy;
		this.disabledMatchEngines = configuration.getDisabledMatchEngines();
		this.diffEngine = configuration.getDiffEngine();
		this.eqEngine = configuration.getEqEngine();
		this.reqEngine = configuration.getReqEngine();
		this.conflictDetector = configuration.getConflictDetector();
		this.disabledPostProcessors = configuration.getDisabledPostProcessors();
		this.disabledFacadeProviders = configuration.getDisabledFacadeProviders();
		setEMFComparePreferencesDefaults();
	}

	/**
	 * Normalize the given path (remove first "/" and "./" if necessary).
	 * 
	 * @param value
	 *            The given path
	 * @return the normalized path
	 */
	protected String normalizePath(String value) {
		if (value.startsWith("/")) { //$NON-NLS-1$
			return value.substring(1, value.length());
		} else if (value.startsWith("./")) { //$NON-NLS-1$
			return value.substring(2, value.length());
		} else {
			return value;
		}
	}

	/**
	 * Set the default values to use for all test-relevant preference settings.
	 */
	private void setEMFComparePreferencesDefaults() {
		uiPreferenceStore.setDefault(EMFCompareUIPreferences.RESOLUTION_SCOPE_PREFERENCE,
				defaultResolutionStrategy);
		rcpPreferenceStore.setDefault(EMFComparePreferences.MATCH_ENGINE_DISABLE_ENGINES,
				join(DEFAULT_DISABLED_MATCH_ENGINES, PREFERENCES_SEPARATOR));
		rcpPreferenceStore.setDefault(EMFComparePreferences.DIFF_ENGINES, DEFAULT_DIFF_ENGINE);
		rcpPreferenceStore.setDefault(EMFComparePreferences.EQUI_ENGINES, DEFAULT_EQ_ENGINE);
		rcpPreferenceStore.setDefault(EMFComparePreferences.REQ_ENGINES, DEFAULT_REQ_ENGINE);
		rcpPreferenceStore.setDefault(EMFComparePreferences.CONFLICTS_DETECTOR, DEFAULT_CONFLICT_DETECTOR);
		rcpPreferenceStore.setDefault(EMFComparePreferences.DISABLED_POST_PROCESSOR,
				join(DEFAULT_DISABLED_POST_PROCESSORS, PREFERENCES_SEPARATOR));
		rcpPreferenceStore.setDefault(EMFComparePreferences.DISABLED_FACADE_PROVIDER,
				join(DEFAULT_DISABLED_FACADE_PROVIDERS, PREFERENCES_SEPARATOR));
	}

	/**
	 * Restore preferences as if they were unset by the user.
	 */
	protected void restoreEMFComparePreferences() {
		uiPreferenceStore.setToDefault(EMFCompareUIPreferences.RESOLUTION_SCOPE_PREFERENCE);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.MATCH_ENGINE_DISABLE_ENGINES);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.DIFF_ENGINES);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.EQUI_ENGINES);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.REQ_ENGINES);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.CONFLICTS_DETECTOR);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.DISABLED_POST_PROCESSOR);
		rcpPreferenceStore.setToDefault(EMFComparePreferences.DISABLED_FACADE_PROVIDER);
	}

	/**
	 * Set the preferences required to run the test.
	 */
	protected void setEMFComparePreferences() {
		setResolutionStrategyPreference();
		setMatchPreference();
		setDiffPreference();
		setEqPreference();
		setReqPreference();
		setConflictPreference();
		setPostProcessorPreference();
		setFacadeProviderPreference();
	}

	/**
	 * Set the resolution strategy preference.
	 */
	private void setResolutionStrategyPreference() {
		defaultResolutionStrategy = uiPreferenceStore
				.getString(EMFCompareUIPreferences.RESOLUTION_SCOPE_PREFERENCE);
		uiPreferenceStore.setValue(EMFCompareUIPreferences.RESOLUTION_SCOPE_PREFERENCE,
				resolutionStrategy.name());
	}

	/**
	 * Set the match engine preference.
	 */
	private void setMatchPreference() {
		List<String> matchEngineNames = Collections.emptyList();
		for (Class<?> matchEngine : disabledMatchEngines) {
			matchEngineNames.add(matchEngine.getCanonicalName());
		}
		rcpPreferenceStore.setValue(EMFComparePreferences.MATCH_ENGINE_DISABLE_ENGINES,
				join(matchEngineNames, PREFERENCES_SEPARATOR));
	}

	/**
	 * Set the diff engine preference.
	 */
	private void setDiffPreference() {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(DIFF_EXTENSION_POINT_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		String diffEngineId = null;
		for (IExtension iExtension : extensions) {
			for (IConfigurationElement iConfig : iExtension.getConfigurationElements()) {
				if (iConfig.getAttribute(EXTENSION_POINT_CLASS_SELECTOR)
						.equals(diffEngine.getCanonicalName())) {
					diffEngineId = iConfig.getAttribute(EXTENSION_POINT_ID_SELECTOR);
					break;
				}
			}
		}
		if (diffEngineId != null) {
			rcpPreferenceStore.setValue(EMFComparePreferences.DIFF_ENGINES, diffEngineId);
		}
	}

	/**
	 * Set the equivalence engine preference.
	 */
	private void setEqPreference() {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(EQ_EXTENSION_POINT_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		String eqEngineId = null;
		for (IExtension iExtension : extensions) {
			for (IConfigurationElement iConfig : iExtension.getConfigurationElements()) {
				if (iConfig.getAttribute(EXTENSION_POINT_CLASS_SELECTOR)
						.equals(eqEngine.getCanonicalName())) {
					eqEngineId = iConfig.getAttribute(EXTENSION_POINT_ID_SELECTOR);
					break;
				}
			}
		}
		if (eqEngineId != null) {
			rcpPreferenceStore.setValue(EMFComparePreferences.EQUI_ENGINES, eqEngineId);
		}
	}

	/**
	 * Set the requirement engine preference.
	 */
	private void setReqPreference() {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(REQ_EXTENSION_POINT_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		String reqEngineId = null;
		for (IExtension iExtension : extensions) {
			for (IConfigurationElement iConfig : iExtension.getConfigurationElements()) {
				if (iConfig.getAttribute(EXTENSION_POINT_CLASS_SELECTOR)
						.equals(reqEngine.getCanonicalName())) {
					reqEngineId = iConfig.getAttribute(EXTENSION_POINT_ID_SELECTOR);
					break;
				}
			}
		}
		if (reqEngineId != null) {
			rcpPreferenceStore.setValue(EMFComparePreferences.REQ_ENGINES, reqEngineId);
		}
	}

	/**
	 * Set the conflict detector preference.
	 */
	private void setConflictPreference() {
		IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint(CONFLICT_EXTENSION_POINT_ID);
		IExtension[] extensions = extensionPoint.getExtensions();
		String conflictDetectorId = null;
		for (IExtension iExtension : extensions) {
			for (IConfigurationElement iConfig : iExtension.getConfigurationElements()) {
				if (iConfig.getAttribute(EXTENSION_POINT_CLASS_SELECTOR)
						.equals(conflictDetector.getCanonicalName())) {
					conflictDetectorId = iConfig.getAttribute(EXTENSION_POINT_ID_SELECTOR);
					break;
				}
			}
		}
		if (conflictDetectorId != null) {
			rcpPreferenceStore.setValue(EMFComparePreferences.CONFLICTS_DETECTOR, conflictDetectorId);
		}
	}

	/**
	 * Set the post-processors preference.
	 */
	private void setPostProcessorPreference() {
		List<String> postProcessorNames = Collections.emptyList();
		for (Class<?> postProcessor : disabledPostProcessors) {
			postProcessorNames.add(postProcessor.getCanonicalName());
		}
		rcpPreferenceStore.setValue(EMFComparePreferences.DISABLED_POST_PROCESSOR,
				join(postProcessorNames, PREFERENCES_SEPARATOR));
	}

	/**
	 * Set the façade providers preference.
	 */
	private void setFacadeProviderPreference() {
		List<String> facadeProviderFactoryNames = Collections.emptyList();
		for (Class<?> facadeProviderFactory : disabledFacadeProviders) {
			facadeProviderFactoryNames.add(facadeProviderFactory.getCanonicalName());
		}
		rcpPreferenceStore.setValue(EMFComparePreferences.DISABLED_FACADE_PROVIDER,
				join(facadeProviderFactoryNames, PREFERENCES_SEPARATOR));
	}

	/**
	 * Join a collection of string with the given separator.
	 * 
	 * @param parts
	 *            The collection of Strings
	 * @param separator
	 *            The separator
	 * @return the joined string
	 */
	private String join(Collection<String> parts, String separator) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		int lastIndex = parts.size() - 1;
		for (String part : parts) {
			sb.append(part);
			if (i == lastIndex - 1) {
				sb.append(separator);
			} else if (i != lastIndex) {
				sb.append(separator);
			}
			i++;
		}
		return sb.toString();
	}

}
