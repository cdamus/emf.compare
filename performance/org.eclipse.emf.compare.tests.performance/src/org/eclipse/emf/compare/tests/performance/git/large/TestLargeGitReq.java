/*******************************************************************************
 * Copyright (c) 2015 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.tests.performance.git.large;

import org.eclipse.emf.compare.tests.performance.AbstractEMFComparePerformanceTest;
import org.eclipse.emf.compare.tests.performance.TestReq;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import data.models.DataGit;
import data.models.LargeGitInputData;
import data.models.LargeSplitGitInputData;
import fr.obeo.performance.api.PerformanceMonitor;

/**
 * @author <a href="mailto:axel.richard@obeo.fr">Axel Richard</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestLargeGitReq extends AbstractEMFComparePerformanceTest {

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.tests.performance.AbstractEMFComparePerformanceTest#setSUTName()
	 */
	@Override
	protected void setSUTName() {
		getPerformance().getSystemUnderTest().setName(TestReq.class.getSimpleName());
	}

	@Test
	public void a_reqUMLLarge() {
		PerformanceMonitor monitor = getPerformance().createMonitor("reqUMLLarge");

		final DataGit data = new LargeGitInputData();
		data.match();
		data.diff();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.req();
			}
		});
		data.dispose();
	}

	@Test
	public void b_reqUMLLargeSplit() {
		PerformanceMonitor monitor = getPerformance().createMonitor("reqUMLLargeSplit");

		final DataGit data = new LargeSplitGitInputData();
		data.match();
		data.diff();
		monitor.measure(warmup(), getStepsNumber(), new Runnable() {
			public void run() {
				data.req();
			}
		});
		data.dispose();
	}
}
