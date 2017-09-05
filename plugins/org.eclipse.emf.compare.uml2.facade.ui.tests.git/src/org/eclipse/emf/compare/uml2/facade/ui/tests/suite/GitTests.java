/*******************************************************************************
 * Copyright (c) 2017 Christian W. Damus and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - Initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.facade.ui.tests.suite;

import org.eclipse.emf.compare.uml2.facade.ui.tests.merge.BasicDependencyTests;
import org.eclipse.emf.compare.uml2.facade.ui.tests.merge.BasicMergeTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The Git fragment's test suite.
 *
 * @author Christian W. Damus
 */
@RunWith(Suite.class)
@SuiteClasses({//
		BasicMergeTests.class, //
		BasicDependencyTests.class, //
})
public class GitTests {
	// Specification is all in the annotations
}
