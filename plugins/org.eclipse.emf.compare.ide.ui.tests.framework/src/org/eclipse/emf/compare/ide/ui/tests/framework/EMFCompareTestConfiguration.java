/*******************************************************************************
 * Copyright (c) 2016, 2017 Obeo, Christian W. Damus, and otherw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Christian W. Damus - integration of façade providers
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.tests.framework;

import static org.eclipse.emf.compare.ide.ui.tests.framework.AbstractCompareTestRunner.DEFAULT_DISABLED_FACADE_PROVIDERS;

/**
 * This class is a wrapper for EMFCompare configurations.
 * 
 * @author <a href="mailto:mathieu.cartaud@obeo.fr">Mathieu Cartaud</a>
 */
public class EMFCompareTestConfiguration {

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

	/**
	 * The constructor.
	 * 
	 * @param disabledMatchEngineFactory
	 *            The match engines disabled for the test
	 * @param diffEngine
	 *            The diff engine used for the test
	 * @param eqEngine
	 *            The eq engine used for the test
	 * @param reqEngine
	 *            The req engine used for the test
	 * @param conflictDetector
	 *            The conflict detector used for the test
	 * @param disabledPostProcessors
	 *            The post processors disabled for the test
	 */
	public EMFCompareTestConfiguration(Class<?>[] disabledMatchEngineFactory, Class<?> diffEngine,
			Class<?> eqEngine, Class<?> reqEngine, Class<?> conflictDetector,
			Class<?>[] disabledPostProcessors) {
		this(disabledMatchEngineFactory, diffEngine, eqEngine, reqEngine, conflictDetector,
				disabledPostProcessors, DEFAULT_DISABLED_FACADE_PROVIDERS);
	}

	/**
	 * The constructor.
	 * 
	 * @param disabledMatchEngineFactory
	 *            The match engines disabled for the test
	 * @param diffEngine
	 *            The diff engine used for the test
	 * @param eqEngine
	 *            The eq engine used for the test
	 * @param reqEngine
	 *            The req engine used for the test
	 * @param conflictDetector
	 *            The conflict detector used for the test
	 * @param disabledPostProcessors
	 *            The post processors disabled for the test
	 * @param disabledFacadeProviders
	 *            The façade providers disabled for the test
	 * @since 1.1
	 */
	public EMFCompareTestConfiguration(Class<?>[] disabledMatchEngineFactory, Class<?> diffEngine,
			Class<?> eqEngine, Class<?> reqEngine, Class<?> conflictDetector,
			Class<?>[] disabledPostProcessors, Class<?>[] disabledFacadeProviders) {
		this.disabledMatchEngines = disabledMatchEngineFactory;
		this.diffEngine = diffEngine;
		this.eqEngine = eqEngine;
		this.reqEngine = reqEngine;
		this.conflictDetector = conflictDetector;
		this.disabledPostProcessors = disabledPostProcessors;
		this.disabledFacadeProviders = disabledFacadeProviders;
	}

	public Class<?>[] getDisabledMatchEngines() {
		return disabledMatchEngines;
	}

	public Class<?> getDiffEngine() {
		return diffEngine;
	}

	public Class<?> getEqEngine() {
		return eqEngine;
	}

	public Class<?> getReqEngine() {
		return reqEngine;
	}

	public Class<?> getConflictDetector() {
		return conflictDetector;
	}

	public Class<?>[] getDisabledPostProcessors() {
		return disabledPostProcessors;
	}

	/**
	 * Obtains the façade provider classes to disable for the test scope.
	 * 
	 * @return the disabled façade provider classes
	 * @since 1.1
	 */
	public Class<?>[] getDisabledFacadeProviders() {
		return disabledFacadeProviders;
	}
}
