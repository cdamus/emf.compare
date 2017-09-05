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
package org.eclipse.emf.compare.uml2.facade.tests;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.google.common.base.Supplier;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.eclipse.emf.compare.facade.internal.FacadeProviderRegistryImpl;
import org.eclipse.emf.compare.facade.internal.match.FacadeMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.postprocessor.IPostProcessor;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.uml2.facade.tests.data.PerformanceInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EEFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.internal.providers.OpaqexprFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.util.DynamicProxiesRule;
import org.eclipse.emf.compare.uml2.facade.tests.util.Pair;
import org.eclipse.emf.compare.uml2.facade.tests.util.PerformanceStats;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for measumrement of the relative performance of comparison based on pluggable façade models
 * versus the raw input models.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing", "restriction" })
@RunWith(Parameterized.class)
public class FacadeComparisonPerformanceTest extends AbstractFacadeTest {

	/** Number of iterations to track for statistics. */
	public static final int ITERATIONS = 10;

	/** Number of iterations to warm up. */
	public static final int WARMUP = 1;

	/** Number of iterations to run, including warm-up and recorded. */
	public static final int RUNS = 10;

	/** The separator used int the preferences for lists of class names. */
	private static final String PREFERENCES_SEPARATOR = ";"; //$NON-NLS-1$

	/** Disabled façade providers preference key. */
	private static final String DISABLED_FACADE_PROVIDER = "org.eclipse.emf.compare.preference.facade.provider"; //$NON-NLS-1$

	@Rule
	public final TestName name = new TestName();

	@Rule
	public final DynamicProxiesRule useDynamicProxies;

	private final boolean umlPost;

	/** Value of the disabled façade providers preference to restore. */
	private String disabledFacadeProvidersPreference;

	private IMatchEngine.Factory.Registry matchRegistry;

	private IMatchEngine.Factory facadeMatchEngineFactory;

	private PerformanceInputData input = new PerformanceInputData();

	private PrintStream output;

	/**
	 * Initializes me.
	 */
	public FacadeComparisonPerformanceTest(boolean useDynamicProxies,
			@SuppressWarnings("unused") String label1, boolean umlPost,
			@SuppressWarnings("unused") String label2) {

		super();

		this.useDynamicProxies = new DynamicProxiesRule(useDynamicProxies);
		this.umlPost = umlPost;
	}

	@Test
	public void mergeAddBodiesLR_p1() {
		PerformanceStats facadeStats = new PerformanceStats(WARMUP);
		runMerge(input::getP1Left, input::getP1Right, false, facadeStats);

		printStats(facadeStats, "With façades");

		PerformanceStats rawStats = new PerformanceStats(WARMUP);
		disableFacades();
		runMerge(input::getP1Left, input::getP1Right, false, rawStats);

		printStats(rawStats, "Raw UML");

		double factor;
		if (umlPost) {
			factor = 3.0; // Not worse than a 3x factor
		} else {
			factor = 3.5; // Not worse than a 3.5x factor
		}
		assertDelta(facadeStats, rawStats, factor);
	}

	void runMerge(Supplier<? extends Resource> left, Supplier<? extends Resource> right, boolean rightToLeft,
			PerformanceStats stats) {

		if (rightToLeft) {
			stats.run(RUNS, //
					Pair.supplier(left, right), //
					(l, r) -> testMergeRightToLeft(l, r, null), //
					getInput()::close);
		} else {
			stats.run(RUNS, //
					Pair.supplier(left, right), //
					(l, r) -> testMergeLeftToRight(l, r, null), //
					getInput()::close);
		}
	}

	@Test
	public void mergeAddBodiesRL_p1() {
		PerformanceStats facadeStats = new PerformanceStats(WARMUP);
		runMerge(input::getP1Left, input::getP1Right, true, facadeStats);

		printStats(facadeStats, "With façades");

		PerformanceStats rawStats = new PerformanceStats(WARMUP);
		disableFacades();
		runMerge(input::getP1Left, input::getP1Right, true, rawStats);

		printStats(rawStats, "Raw UML");

		double factor;
		if (umlPost) {
			factor = 3.0; // Not worse than a 3x factor
		} else {
			factor = 3.5; // Not worse than a 3.5x factor
		}
		assertDelta(facadeStats, rawStats, factor);
	}

	//
	// Test framework
	//

	@Parameters(name = "{1}, {3}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList(new Object[][] { //
				{Boolean.TRUE, "dynamic proxy", Boolean.TRUE, "uml hooks" }, //
				{Boolean.FALSE, "plain façade", Boolean.TRUE, "uml hooks" }, //
				{Boolean.TRUE, "dynamic proxy", Boolean.FALSE, "no uml hooks" }, //
				{Boolean.FALSE, "plain façade", Boolean.FALSE, "no uml hooks" }, //
		});
	}

	@BeforeClass
	public static void setupClass() {
		fillRegistries();
	}

	@AfterClass
	public static void teardownClass() {
		resetRegistries();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerPostProcessors(IPostProcessor.Descriptor.Registry<String> postProcessorRegistry) {
		if (umlPost) {
			super.registerPostProcessors(postProcessorRegistry);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fillMergerRegistry(IMerger.Registry registry) {
		if (umlPost) {
			super.fillMergerRegistry(registry);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void fillMatchEngineFactoryRegistry(IMatchEngine.Factory.Registry matchEngineFactoryRegistry) {
		super.fillMatchEngineFactoryRegistry(matchEngineFactoryRegistry);

		this.matchRegistry = matchEngineFactoryRegistry;

		IFacadeProvider.Factory.Registry facadeProviderRegistry;
		if (EMFPlugin.IS_ECLIPSE_RUNNING) {
			// Use the extension-based registry
			facadeProviderRegistry = EMFCompareFacadePlugin.getDefault().getFacadeProviderRegistry();
		} else {
			facadeProviderRegistry = FacadeProviderRegistryImpl.createStandaloneInstance();
			facadeProviderRegistry.add(new J2EEFacadeProvider.Factory());
			facadeProviderRegistry.add(new OpaqexprFacadeProvider.Factory());
		}

		this.facadeMatchEngineFactory = new FacadeMatchEngine.Factory(UseIdentifiers.WHEN_AVAILABLE,
				facadeProviderRegistry);
		facadeMatchEngineFactory.setRanking(Integer.MAX_VALUE);

		matchEngineFactoryRegistry.add(facadeMatchEngineFactory);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractUMLInputData getInput() {
		return input;
	}

	protected void disableFacades() {
		List<Class<? extends IFacadeProvider.Factory>> facadeProviders = Arrays
				.asList(J2EEFacadeProvider.Factory.class, OpaqexprFacadeProvider.Factory.class);

		if (EMFPlugin.IS_ECLIPSE_RUNNING) {
			// The main EMF Compare preferences
			IPreferenceStore rcpPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
					EMFCompareRCPPlugin.PLUGIN_ID);

			disabledFacadeProvidersPreference = rcpPreferenceStore.getDefaultString(DISABLED_FACADE_PROVIDER);

			String value = facadeProviders.stream().map(Class::getName)
					.collect(Collectors.joining(PREFERENCES_SEPARATOR));
			rcpPreferenceStore.setValue(DISABLED_FACADE_PROVIDER, value);
		} else {
			matchRegistry.remove(facadeMatchEngineFactory.getClass().getName());
		}
	}

	@After
	public void restoreFacades() {
		if (EMFPlugin.IS_ECLIPSE_RUNNING) {
			if (disabledFacadeProvidersPreference != null) {
				// The main EMF Compare preferences
				IPreferenceStore rcpPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
						EMFCompareRCPPlugin.PLUGIN_ID);

				rcpPreferenceStore.setValue(DISABLED_FACADE_PROVIDER, disabledFacadeProvidersPreference);
			}
		} else {
			matchRegistry.add(facadeMatchEngineFactory);
		}

		// Forget these
		disabledFacadeProvidersPreference = null;
		matchRegistry = null;
		facadeMatchEngineFactory = null;
	}

	@After
	public void endOutput() {
		if (output != null) {
			output.println();
			output = null;
		}
	}

	final void printf(String format, Object... arg) {
		if (output == null) {
			output = System.out;
			output.println(name.getMethodName());
			output.println("---------------------------------");
		}

		output.printf(format, arg);
	}

	void printStats(PerformanceStats stats, String which) {
		printf("%-13s: %5d ms total, %4d ms avg (± %d)%n", which, stats.total(MILLISECONDS),
				stats.average(MILLISECONDS), stats.stddev(MILLISECONDS));
	}

	void assertDelta(PerformanceStats a, PerformanceStats b, double factor) {
		long deltaPctTotal = (a.total(MILLISECONDS) - b.total(MILLISECONDS)) * 100 / b.total(MILLISECONDS);
		long deltaPctAvg = (a.average(MILLISECONDS) - b.average(MILLISECONDS)) * 100
				/ b.average(MILLISECONDS);

		// It would be weird if they were different
		if (deltaPctTotal == deltaPctAvg) {
			printf("Delta : %d%%%n", deltaPctTotal);
		} else {
			printf("Delta : %d%% total, %d%% avg%n", deltaPctTotal, deltaPctAvg);
		}

		// Make an allowance for the overhead of dynamic proxies
		double acceptableFactor = useDynamicProxies.getAsBoolean() ? factor * 200.0 : factor * 100.0;
		double test = max(abs(deltaPctTotal), abs(deltaPctAvg));

		assertThat("Performance impact of façades is too great", test, lessThanOrEqualTo(acceptableFactor));
	}
}
