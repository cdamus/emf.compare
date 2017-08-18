/*
 * Copyright (c) 2017 Christian W. Damus and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Christian W. Damus - initial API and implementation
 *
 */
package org.eclipse.emf.compare.uml2.facade.ui.tests.merge;

import static com.google.common.collect.Iterables.tryFind;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.isPresent;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.added;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.changedReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.removed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assume.assumeThat;

import com.google.common.base.Optional;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.GitMergeStrategyID;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.GitTestRunner;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.annotations.GitCompare;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.annotations.GitInput;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.annotations.GitMergeStrategy;
import org.junit.runner.RunWith;

/**
 * Tests of basic {@link Diff#getRequires() diff dependency} scenarios in façades, of ssingle- and mixed-mode
 * varieties.
 *
 * @author Christian W. Damus
 */
@RunWith(GitTestRunner.class)
@GitMergeStrategy(GitMergeStrategyID.MODEL_ADDITIVE)
@SuppressWarnings({"nls" })
public class BasicDependencyTests {

	@GitCompare(local = "master", remote = "aleatory", file = "design.uml")
	@GitInput("data/add-finder-bothSides-noConflict.zip")
	public void umlDependencyOnAddedFacadeObject(Comparison comparison) throws Exception {
		assumeThat("There were unexpected conflicts", comparison.getConflicts(), empty());

		Optional<Diff> setCollaborationRoleTypeOpt = tryFind(comparison.getDifferences(),
				changedReference("m1.lookup_thing.random", "type", null, "j2ee-app.ThingByChance"));
		assertThat("Set of collaboration role type not found", setCollaborationRoleTypeOpt, isPresent());

		Diff setCollaborationRoleType = setCollaborationRoleTypeOpt.get();
		Optional<Diff> addFinder = tryFind(setCollaborationRoleType.getRequires(),
				added("j2ee-app.ThingByChance"));

		assertThat("Changed collaboration role type does not require addition of finder", addFinder,
				isPresent());
	}

	@GitCompare(local = "master", remote = "modify-finder", file = "design.uml")
	@GitInput("data/delete-conflict-with-modify.zip")
	public void umlDependencyFromDeletedFacadeObject(Comparison comparison) throws Exception {
		Optional<Diff> deleteFinderOpt = tryFind(comparison.getDifferences(),
				removed("j2ee-app.ThingByChance"));
		assertThat("Deletion of Finder façade not found", deleteFinderOpt, isPresent());

		Diff deleteFinder = deleteFinderOpt.get();
		Optional<Diff> unsetCollaborationRoleType = tryFind(deleteFinder.getRequires(),
				changedReference("m1.lookup_thing.random", "type", "j2ee-app.ThingByChance", null));

		assertThat("Deletion of finder does not require unset of collaboration role type",
				unsetCollaborationRoleType, isPresent());
	}
}
