/*
 * Copyright (c) 2017 Christian W. Damus and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Christian W. Damus - Initial API and implementation
 *   
 */
package org.eclipse.emf.compare.uml2.facade.ui.tests.suite;

import org.eclipse.emf.compare.uml2.facade.ui.tests.BasicConflictMergeJustUMLControlTest;
import org.eclipse.emf.compare.uml2.facade.ui.tests.BasicConflictMergeWithFacadeTest;
import org.eclipse.emf.compare.uml2.facade.ui.tests.FacadeItemProviderTest;
import org.eclipse.emf.compare.uml2.facade.ui.tests.MergeViewerTreeTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * The master test suite for the project.
 *
 * @author Christian W. Damus
 */
@RunWith(Suite.class)
@SuiteClasses({//
		BasicConflictMergeWithFacadeTest.class, //
		BasicConflictMergeJustUMLControlTest.class, //
		FacadeItemProviderTest.class, //
		MergeViewerTreeTest.class, //
})
public class AllTests {
	// Specification is all in the annotations
}
