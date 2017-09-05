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
package org.eclipse.emf.compare.uml2.facade.ui.tests.merge;

import static com.google.common.base.Predicates.and;
import static org.eclipse.emf.compare.DifferenceKind.MOVE;
import static org.eclipse.emf.compare.DifferenceSource.LEFT;
import static org.eclipse.emf.compare.DifferenceSource.RIGHT;
import static org.eclipse.emf.compare.ide.ui.tests.git.framework.GitTestSupport.TWO_WAY;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.isProxy;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.isRealConflict;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.matches;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.ofKind;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.onFeature;
import static org.eclipse.emf.ecore.resource.Resource.OPTION_SAVE_ONLY_IF_CHANGED;
import static org.eclipse.emf.ecore.resource.Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.Collections;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.GitMergeStrategyID;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.GitTestRunner;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.GitTestSupport;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.annotations.GitInput;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.annotations.GitMerge;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.annotations.GitMergeStrategy;
import org.eclipse.emf.compare.ide.utils.ResourceUtil;
import org.eclipse.emf.compare.uml2.facade.tests.framework.MergeUtils;
import org.eclipse.emf.compare.uml2.facade.tests.framework.UMLResourceSet;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jgit.api.Status;
import org.eclipse.uml2.uml.Interface;
import org.eclipse.uml2.uml.Property;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.UMLPackage;
import org.junit.runner.RunWith;

/**
 * Tests of basic merge scenarios in façades, of single- and mixed-mode varieties.
 *
 * @author Christian W. Damus
 */
@RunWith(GitTestRunner.class)
@GitMergeStrategy(GitMergeStrategyID.MODEL_ADDITIVE)
@SuppressWarnings({"nls", "unused", "boxing" })
public class BasicMergeTests {

	@GitMerge(local = "master", remote = "aleatory")
	@GitInput("data/add-finder-bothSides-noConflict.zip")
	public void mergeInUML(Status status, GitTestSupport support) throws Exception {
		assertThat(status.hasUncommittedChanges(), is(false));
		assertThat(status.getConflicting(), not(hasItem(anything())));

		Comparison comparison = support.compare("master", "expected", "design.uml", TWO_WAY);

		// If the comparison is empty, then this is trivially ensured.
		// FIXME: We should be able to get an empty comparison without the moves:
		// merge seems to behave differently in Maven execution as in Eclipse execution
		assertThat(comparison.getDifferences(), everyItem(matches(Diff.class, "is move in packagedElement",
				and(ofKind(MOVE), onFeature(UMLPackage.Literals.PACKAGE__PACKAGED_ELEMENT)))));
	}

	@GitMerge(local = "master", remote = "b-side")
	@GitInput("data/add-finder-bothSides-uml-conflict.zip")
	public void mergeConflictInUMLAcceptLocal(Status status, GitTestSupport support) throws Exception {
		mergeConflictInUML(false, status, support);
	}

	private void mergeConflictInUML(boolean acceptRemote, Status status, GitTestSupport support)
			throws Exception {

		assumeThat("Should have uncommitted changes", status.hasUncommittedChanges(), is(true));
		assertThat(status.getConflicting(),
				is(ImmutableSet.of("mixed-uml-j2ee/design.uml", "mixed-uml-j2ee/apps.j2ee.uml")));

		Comparison comparison = support.compare("master", "b-side", "design.uml");
		assertThat(comparison.getConflicts(), hasItem(isRealConflict()));
		Conflict conflict = Iterables
				.getFirst(Iterables.filter(comparison.getConflicts(), isRealConflict()::matches), null);

		MergeUtils.acceptAllNonConflicting(comparison);
		MergeUtils.accept(acceptRemote ? RIGHT : LEFT, conflict);
		ResourceUtil.saveAllResources(comparison.getMatchedResources().get(0).getLeft().getResourceSet(),
				Collections.singletonMap(OPTION_SAVE_ONLY_IF_CHANGED,
						OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER));

		try (UMLResourceSet rset = new UMLResourceSet()) {
			Resource res = rset.getResource("mixed-uml-j2ee/design.uml");
			Property finderRole = rset.requireElement(Property.class, "m1::lookup_thing::finder");
			Type finderType = finderRole.getType();
			Interface finder = rset.requireElement(Interface.class,
					acceptRemote ? "j2ee-app::ThingByChance" : "j2ee-app::ThingByName");
			assertThat(finderType, not(isProxy()));
			assertThat(finderType, is(finder));
		}
	}

	@GitMerge(local = "master", remote = "b-side")
	@GitInput("data/add-finder-bothSides-uml-conflict.zip")
	public void mergeConflictInUMLAcceptRemote(Status status, GitTestSupport support) throws Exception {
		mergeConflictInUML(true, status, support);
	}
}
