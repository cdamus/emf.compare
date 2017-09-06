/*******************************************************************************
 * Copyright (c) 2016, 2017 Obeo, Christian W. Damus, and others.
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

import static org.eclipse.emf.compare.utils.ReflectiveDispatch.safeInvoke;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.compare.conflict.DefaultConflictDetector;
import org.eclipse.emf.compare.conflict.MatchBasedConflictDetector;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.equi.DefaultEquiEngine;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.ConflictDetectors;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.DiffEngines;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.DisabledFacadeProviders;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.DisabledMatchEngines;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.DisabledPostProcessors;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.EqEngines;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.ReqEngines;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.ResolutionStrategies;
import org.eclipse.emf.compare.req.DefaultReqEngine;
import org.junit.Assert;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

/**
 * EMFCompare specific runners must extends this class.
 * 
 * @author <a href="mailto:mathieu.cartaud@obeo.fr">Mathieu Cartaud</a>
 */
public abstract class AbstractCompareTestRunner extends ParentRunner<Runner> {

	/** The default disabled façade provider classes. */
	static final Class<?>[] DEFAULT_DISABLED_FACADE_PROVIDERS = new Class<?>[0];

	/** The list of all runners. */
	protected final List<Runner> runners;

	/** Default list of resolution strategies used if the @ResolutionStrategies annotation is not used. */
	private final ResolutionStrategyID[] defaultResolutionStrategies = new ResolutionStrategyID[] {
			ResolutionStrategyID.WORKSPACE, };

	/** Default list of match engines disabled if the @MatchEngines annotation is not used. */
	private final Class<?>[] defaultDisabledMatchEngines = new Class<?>[] {};

	/** Default list of diff engines used if the @DiffEngines annotation is not used. */
	private final Class<?>[] defaultDiffEngines = new Class<?>[] {DefaultDiffEngine.class };

	/** Default list of eq engines used if the @EqEngines annotation is not used. */
	private final Class<?>[] defaultEqEngines = new Class<?>[] {DefaultEquiEngine.class };

	/** Default list of req engines used if the @ReqEngines annotation is not used. */
	private final Class<?>[] defaultReqEngines = new Class<?>[] {DefaultReqEngine.class };

	/** Default list of conflict detector used if the @ConflictEngines annotation is not used. */
	private final Class<?>[] defaultConflictDetectors = new Class<?>[] {DefaultConflictDetector.class,
			MatchBasedConflictDetector.class, };

	/** Default list of resolution strategies disabled if the @PostProcessors annotation is not used. */
	private final Class<?>[] defaultDisabledPostProcessors = new Class<?>[] {};

	/**
	 * Default list of façade providers disabled if the
	 * {@link DisabledFacadeProviders @DisabledFacadeProviders} annotation is not used.
	 */
	private final Class<?>[] defaultDisabledFacadeProviders = DEFAULT_DISABLED_FACADE_PROVIDERS;

	/**
	 * The constructor.
	 * 
	 * @param testClass
	 *            The given test class
	 * @throws InitializationError
	 *             If the test cannot be created
	 */
	public AbstractCompareTestRunner(Class<?> testClass) throws InitializationError {
		super(testClass);

		runners = new ArrayList<Runner>();
		prepareRunnersForTests();
	}

	/**
	 * Create a runner for each configuration. The configurations are a matrix of all association
	 * possibilities between match, diff, eq, req and conflict engines.
	 */
	private void prepareRunnersForTests() {
		final ResolutionStrategyID[] resolutionStrategies = getAnnotation(ResolutionStrategies.class,
				ResolutionStrategyID[].class, defaultResolutionStrategies);
		final Class<?>[] disabledMatchEngines = getAnnotation(DisabledMatchEngines.class, Class[].class,
				defaultDisabledMatchEngines);
		final Class<?>[] diffEngines = getAnnotation(DiffEngines.class, Class[].class, defaultDiffEngines);
		final Class<?>[] eqEngines = getAnnotation(EqEngines.class, Class[].class, defaultEqEngines);
		final Class<?>[] reqEngines = getAnnotation(ReqEngines.class, Class[].class, defaultReqEngines);
		final Class<?>[] conflictDetectors = getAnnotation(ConflictDetectors.class, Class[].class,
				defaultConflictDetectors);
		final Class<?>[] disabledPostProcessors = getAnnotation(DisabledPostProcessors.class, Class[].class,
				defaultDisabledPostProcessors);
		final Class<?>[] disabledFacadeProviders = getAnnotation(DisabledFacadeProviders.class, Class[].class,
				defaultDisabledFacadeProviders);

		// CHECKSTYLE:OFF those embedded fors are necessary to create all the test possibilities
		for (ResolutionStrategyID resolutionStrategy : resolutionStrategies) {
			for (Class<?> diffEngine : diffEngines) {
				for (Class<?> eqEngine : eqEngines) {
					for (Class<?> reqEngine : reqEngines) {
						for (Class<?> conflictDetector : conflictDetectors) {
							// CHECKSTYLE:ON
							try {
								EMFCompareTestConfiguration configuration = new EMFCompareTestConfiguration(
										disabledMatchEngines, diffEngine, eqEngine, reqEngine,
										conflictDetector, disabledPostProcessors, disabledFacadeProviders);
								createRunner(getTestClass().getJavaClass(), resolutionStrategy,
										configuration);
							} catch (InitializationError e) {
								e.printStackTrace();
								Assert.fail(e.getMessage());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Extracts an array of values from an annotation on the test class, or the given default values if it is
	 * not present.
	 * 
	 * @param annotationType
	 *            the type of annotation to extract values from
	 * @param valueArrayType
	 *            the type of value array provided by the annotation
	 * @param defaultValues
	 *            the default values in case the annotation is not present
	 * @return the annotation values (explicit or implicit)
	 * @param <A>
	 *            the annotation type
	 * @param <T>
	 *            the value type
	 */
	private <A extends Annotation, T> T[] getAnnotation(Class<? extends A> annotationType,
			Class<? extends T[]> valueArrayType, T[] defaultValues) {
		A annotation = getTestClass().getAnnotation(annotationType);

		T[] result;
		if (annotation == null) {
			result = defaultValues;
		} else {
			result = valueArrayType.cast(safeInvoke(annotation, "value")); //$NON-NLS-1$
		}

		return result;
	}

	/**
	 * Create the specific runner for the given parameters.
	 * 
	 * @param testClass
	 *            The class to test
	 * @param resolutionStrategy
	 *            The resolution strategy used for this runner
	 * @param configuration
	 *            EMFCompare configurations for the test
	 * @throws InitializationError
	 *             If the creation of the runner goes wrong
	 */
	public abstract void createRunner(Class<?> testClass, ResolutionStrategyID resolutionStrategy,
			EMFCompareTestConfiguration configuration) throws InitializationError;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.junit.runners.BlockJUnit4ClassRunner#getChildren()
	 */
	@Override
	protected List<Runner> getChildren() {
		return runners;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.junit.runners.ParentRunner#describeChild(java.lang.Object)
	 */
	@Override
	protected Description describeChild(Runner child) {
		return child.getDescription();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.junit.runners.ParentRunner#runChild(java.lang.Object,
	 *      org.junit.runner.notification.RunNotifier)
	 */
	@Override
	protected void runChild(Runner child, RunNotifier notifier) {
		child.run(notifier);
	}

}
