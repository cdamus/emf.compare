/*****************************************************************************
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
 *****************************************************************************/
package org.eclipse.emf.compare.uml2.facade.ui.tests;

import static org.eclipse.emf.compare.DifferenceState.UNRESOLVED;
import static org.eclipse.emf.compare.uml2.facade.tests.CompareMatchers.isRealConflict;
import static org.eclipse.emf.compare.uml2.facade.tests.CompareMatchers.matches;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasState;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.Iterables;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.actions.MergeRunnableImpl;
import org.eclipse.emf.compare.ide.ui.tests.framework.RuntimeTestRunner;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.Compare;
import org.eclipse.emf.compare.internal.merge.MergeMode;
import org.eclipse.emf.compare.merge.DiffRelationshipComputer;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Dependency;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.NamedElement;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.util.UMLUtil;
import org.junit.runner.RunWith;

/**
 * Basic test case for conflict resolution in a three-way comparison using the pluggable façade providers.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing", "restriction" })
@RunWith(RuntimeTestRunner.class)
public class BasicConflictMergeWithFacadeTest {

	private static final boolean MERGE_RIGHT_TO_LEFT = false;

	private IMerger.Registry mergerRegistry = EMFCompareRCPPlugin.getDefault().getMergerRegistry();

	@Compare(left = "data/basic/left.uml", right = "data/basic/right.uml", ancestor = "data/basic/base.uml")
	public void mergeHomeInterfaceConflict(final Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();
		List<Conflict> conflicts = comparison.getConflicts();

		assertThat(conflicts, hasItem(isRealConflict()));
		assertThat(differences, everyItem(matches(Diff.class, "is unresolved", hasState(UNRESOLVED))));

		MergeRunnableImpl merge = new MergeRunnableImpl(true, true, MergeMode.RIGHT_TO_LEFT,
				new DiffRelationshipComputer(mergerRegistry));
		merge.merge(differences, MERGE_RIGHT_TO_LEFT, mergerRegistry);

		Collection<Interface> homeInterfaces = UMLUtil.findNamedElements(getResource(comparison, "left.uml"),
				"model::WhatsitHome", false, UMLPackage.Literals.INTERFACE);
		assertThat("WhatsitHome not unique", homeInterfaces.size(), is(1));

		Interface whatsitHome = Iterables.getOnlyElement(homeInterfaces);

		assertThat("Wrong number of usages", whatsitHome.getClientDependencies().size(), is(1));

		Dependency usage = Iterables.getOnlyElement(whatsitHome.getClientDependencies());

		assertThat("Wrong number of suppliers", usage.getSuppliers().size(), is(1));
		assertThat(usage.getSuppliers(), everyItem(instanceOf(org.eclipse.uml2.uml.Class.class)));
		assertThat(usage.getSuppliers(),
				everyItem(matches(NamedElement.class, "named Thing", e -> "Thing".equals(e.getName()))));
	}

	//
	// Test framework
	//

	Resource getResource(Comparison comparison, String name) {
		return comparison.getMatchedResources().stream() //
				.flatMap(match -> Stream.of(match.getOrigin(), match.getLeft(), match.getRight())) //
				.filter(Objects::nonNull) //
				.filter(res -> res.getURI().lastSegment().equals(name)) //
				.findAny() //
				.orElseThrow(() -> new AssertionError("No such resource in the comparison: " + name));
	}
}
