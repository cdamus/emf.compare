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
package org.eclipse.emf.compare.uml2.facade.tests.suite;

import org.eclipse.emf.compare.uml2.facade.tests.BasicFacadeComparisonTest;
import org.eclipse.emf.compare.uml2.facade.tests.BasicFacadeTest;
import org.eclipse.emf.compare.uml2.facade.tests.BasicOpaqexprFacadeTest;
import org.eclipse.emf.compare.uml2.facade.tests.FacadeComparisonPerformanceTest;
import org.eclipse.emf.compare.uml2.facade.tests.FacadeComparisonScopeTest;
import org.eclipse.emf.compare.uml2.facade.tests.FacadePropertyTesterTest;
import org.eclipse.emf.compare.uml2.facade.tests.FacadeProviderComparisonTest;
import org.eclipse.emf.compare.uml2.facade.tests.FacadeProxyTest;
import org.eclipse.emf.compare.uml2.facade.tests.OpaqexprFacadeProviderComparisonTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * The master test suite for the bundle.
 *
 * @author Christian W. Damus
 */
@RunWith(Suite.class)
@SuiteClasses({ //
		BasicFacadeTest.class, //
		FacadePropertyTesterTest.class, //
		FacadeProxyTest.class, //
		FacadeComparisonScopeTest.class, //
		BasicFacadeComparisonTest.class, //
		FacadeProviderComparisonTest.class, //
		BasicOpaqexprFacadeTest.class, //
		OpaqexprFacadeProviderComparisonTest.class, //
		FacadeComparisonPerformanceTest.class, //
})

public class AllTests {

	/**
	 * Initializes me.
	 */
	public AllTests() {
		super();
	}

	/**
	 * Standalone launcher for all of compare's tests.
	 * 
	 * @generated
	 */
	public static void main(String[] args) {
		TestRunner.run(suite());
	}

	/**
	 * This will return a suite populated with all tests available through this class.
	 * 
	 * @generated
	 */
	public static Test suite() {
		return new JUnit4TestAdapter(AllTests.class);
	}

}
