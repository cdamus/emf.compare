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

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.tryFind;
import static org.eclipse.emf.compare.DifferenceSource.LEFT;
import static org.eclipse.emf.compare.DifferenceSource.RIGHT;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.isProxy;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.matches;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.named;
import static org.eclipse.emf.compare.uml2.tests.AdditionalResourcesKind.REFERENCED_LOCAL;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.addedToReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.attributeValueMatch;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.changedReference;
import static org.eclipse.uml2.uml.util.UMLUtil.findNamedElements;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.eclipse.emf.compare.facade.internal.FacadeProviderRegistryImpl;
import org.eclipse.emf.compare.facade.internal.match.FacadeMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.data.UMLInputData;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.BeanKind;
import org.eclipse.emf.compare.uml2.facade.tests.j2ee.internal.providers.J2EEFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.util.DynamicProxiesRule;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.uml2.tests.AdditionalResources;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.Collaboration;
import org.eclipse.uml2.uml.Dependency;
import org.eclipse.uml2.uml.Type;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for comparison based on pluggable façade model providers with the J2EE façade.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing", "restriction" })
@RunWith(Parameterized.class)
public class FacadeProviderComparisonTest extends AbstractFacadeTest {

	@Rule
	public final DynamicProxiesRule useDynamicProxies;

	private UMLInputData input = new UMLInputData();

	/**
	 * Initializes me.
	 */
	public FacadeProviderComparisonTest(boolean useDynamicProxies, @SuppressWarnings("unused") String label) {

		super();

		this.useDynamicProxies = new DynamicProxiesRule(useDynamicProxies);
	}

	@Test
	public void changeBeanKind_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();
		Comparison comparison = compare(left, right);

		testAB1(TestKind.ADD, comparison);
	}

	private void testAB1(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();

		// This is a very simple delta
		assertThat(differences.size(), is(1));

		Predicate<? super Diff> changeKindDescription;

		switch (kind) {
			case DELETE:
				changeKindDescription = attributeValueMatch("kind", BeanKind.ENTITY, false);
				break;
			case ADD:
				changeKindDescription = attributeValueMatch("kind", BeanKind.ENTITY, false);
				break;
			default:
				fail("Unsupported test kind: " + kind);
				return; // Unreachable
		}

		Diff changeKind = Iterators.find(differences.iterator(), changeKindDescription);
		assertThat(changeKind, notNullValue());
		assertThat(changeKind.getRefinedBy(), not(hasItem(anything())));
		assertThat(changeKind.getRefines(), not(hasItem(anything())));
		assertThat(changeKind.getRequiredBy(), not(hasItem(anything())));
		assertThat(changeKind.getRequires(), not(hasItem(anything())));
		assertThat(changeKind.getEquivalence(), nullValue());
	}

	@Test
	public void mergeBeanKindRL_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeRightToLeft(left, right, null);
	}

	@Test
	public void mergeBeanKindLR_a1() {
		Resource left = input.getA1Left();
		Resource right = input.getA1Right();

		testMergeLeftToRight(left, right, null);
	}

	/**
	 * A control test for the comparison of UML models that do not have a façade.
	 */
	@Test
	public void changeAttributeTypeControl_u1() {
		Resource base = input.getU1Base();
		Resource left = input.getU1Left();
		Resource right = input.getU1Right();
		Comparison comparison = compare(left, right, base);

		List<Conflict> conflicts = comparison.getConflicts();

		// This is a very simple comparison
		assertThat(conflicts.size(), is(1));
		Conflict conflict = conflicts.get(0);
		assertThat(conflict.getKind(), is(ConflictKind.REAL));

		assertThat(conflict.getLeftDifferences().size(), is(1));
		assertThat(conflict.getRightDifferences().size(), is(1));

		Diff leftDiff = Iterables.find(conflict.getLeftDifferences(),
				changedReference("u1.Person.age", "type", "PrimitiveTypes.String", "PrimitiveTypes.Real"));
		assertThat(leftDiff, notNullValue());
		Diff rightDiff = Iterables.find(conflict.getRightDifferences(),
				changedReference("u1.Person.age", "type", "PrimitiveTypes.String", "PrimitiveTypes.Integer"));
		assertThat(rightDiff, notNullValue());
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void basicMixedModeComparison_m1() {
		Resource left = input.getM1Left();
		Resource right = input.getM1Right();
		Comparison comparison = compare(left, right);

		List<Diff> differences = comparison.getDifferences();

		assertThat(differences, hasItem(matches(Diff.class, "Add finder façade",
				addedToReference("j2ee-app", "finder", "j2ee-app.ThingByName"))));

		assertThat(differences, hasItem(matches(Diff.class, "Change UML collaboration role", changedReference(
				"m1.lookup_thing.finder", "type", "j2ee-app.ThingByID", "j2ee-app.ThingByName"))));
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void mixedModeThreeWayConflictInUML_m2() {
		Resource base = input.getM2Base();
		Resource left = input.getM2Left();
		Resource right = input.getM2Right();
		Comparison comparison = compare(left, right, base);

		List<Diff> differences = comparison.getDifferences();

		Diff leftAdd = tryFind(differences, addedToReference("j2ee-app", "finder", "j2ee-app.ThingByName"))
				.orNull();
		assertThat("Left addition of finder not found", leftAdd, notNullValue());
		Diff rightAdd = tryFind(differences, addedToReference("j2ee-app", "finder", "j2ee-app.ThingByChance"))
				.orNull();
		assertThat("Right addition of finder not found", rightAdd, notNullValue());

		List<Conflict> conflicts = comparison.getConflicts();
		assertThat(conflicts.size(), is(1));

		Conflict conflict = conflicts.get(0);

		assertThat(conflict.getLeftDifferences(),
				hasItem(matches(Diff.class, "Change UML collaboration role", changedReference(
						"m2.lookup_thing.finder", "type", "j2ee-app.ThingByID", "j2ee-app.ThingByName"))));
		assertThat(conflict.getRightDifferences(),
				hasItem(matches(Diff.class, "Change UML collaboration role", changedReference(
						"m2.lookup_thing.finder", "type", "j2ee-app.ThingByID", "j2ee-app.ThingByChance"))));
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void mergeMixedModeThreeWayConflictLR_m2() {
		Resource base = input.getM2Base();
		Resource left = input.getM2Left();
		Resource right = input.getM2Right();

		testMergeLeftToRight(left, right, base, true);
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void mergeMixedModeThreeWayConflictRL_m2() {
		Resource base = input.getM2Base();
		Resource left = input.getM2Left();
		Resource right = input.getM2Right();

		testMergeRightToLeft(left, right, base, true);
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void resolveMixedModeConflictAcceptLeft_m2() {
		Resource base = input.getM2Base();
		Resource left = input.getM2Left();
		Resource right = input.getM2Right();

		Comparison comparison = compare(left, right, base);

		List<Conflict> conflicts = comparison.getConflicts();
		// Don't be redundant with the more basic test case
		assumeThat(conflicts.size(), is(1));
		Conflict conflict = conflicts.get(0);

		acceptAllNonConflicting(comparison);

		// Verify the left-side model (conventional merge result)
		org.eclipse.uml2.uml.Package j2eeApp = Iterables.getOnlyElement(filter(
				findNamedElements(left.getResourceSet(), "j2ee-app"), org.eclipse.uml2.uml.Package.class));

		// Left-side addition of a finder
		Type thingByName = j2eeApp.getOwnedType("ThingByName");
		assertThat("Merge lost left side addition", thingByName, notNullValue());
		assertThat(thingByName.getClientDependencies().size(), is(1));
		Dependency usage = thingByName.getClientDependencies().get(0);
		assertThat(usage.getSuppliers(), hasItem(named("Thing")));

		// Right-side addition of a finder
		Type thingByChance = j2eeApp.getOwnedType("ThingByChance");
		assertThat("Merge lost right side addition", thingByChance, notNullValue());
		assertThat(thingByChance.getClientDependencies().size(), is(1));
		usage = thingByChance.getClientDependencies().get(0);
		assertThat(usage.getSuppliers(), hasItem(named("Thing")));

		accept(LEFT, conflict);

		// The left-side collaboration role type is retained
		Collaboration collaboration = Iterables.getOnlyElement(
				filter(findNamedElements(left.getResourceSet(), "m2::lookup_thing"), Collaboration.class));
		Type finderType = collaboration.getCollaborationRole("finder", null).getType();
		assertThat("Collaboration role type lost", finderType, notNullValue());
		assertThat("Collaboration role is a proxy", finderType, not(isProxy()));
		assertThat("Wrong collaboration role type", finderType.getName(), is("ThingByName"));
	}

	@Test
	@AdditionalResources(REFERENCED_LOCAL)
	public void resolveMixedModeConflictAcceptRight_m2() {
		Resource base = input.getM2Base();
		Resource left = input.getM2Left();
		Resource right = input.getM2Right();

		Comparison comparison = compare(left, right, base);

		List<Conflict> conflicts = comparison.getConflicts();
		// Don't be redundant with the more basic test case
		assumeThat(conflicts.size(), is(1));
		Conflict conflict = conflicts.get(0);

		acceptAllNonConflicting(comparison);

		// Verify the left-side model (conventional merge result)
		org.eclipse.uml2.uml.Package j2eeApp = Iterables.getOnlyElement(filter(
				findNamedElements(left.getResourceSet(), "j2ee-app"), org.eclipse.uml2.uml.Package.class));

		// Right-side addition of a finder
		Type thingByChance = j2eeApp.getOwnedType("ThingByChance");
		assertThat("Merge lost right side addition", thingByChance, notNullValue());
		assertThat(thingByChance.getClientDependencies().size(), is(1));
		Dependency usage = thingByChance.getClientDependencies().get(0);
		assertThat(usage.getSuppliers(), hasItem(named("Thing")));

		// Left-side addition of a finder
		Type thingByName = j2eeApp.getOwnedType("ThingByName");
		assertThat("Merge lost right side addition", thingByName, notNullValue());
		assertThat(thingByName.getClientDependencies().size(), is(1));
		usage = thingByName.getClientDependencies().get(0);
		assertThat(usage.getSuppliers(), hasItem(named("Thing")));

		accept(RIGHT, conflict);

		// The right-side collaboration role type is merged
		Collaboration collaboration = Iterables.getOnlyElement(
				filter(findNamedElements(left.getResourceSet(), "m2::lookup_thing"), Collaboration.class));
		Type finderType = collaboration.getCollaborationRole("finder", null).getType();
		assertThat("Collaboration role type lost", finderType, notNullValue());
		assertThat("Collaboration role is a proxy", finderType, not(isProxy()));
		assertThat("Wrong collaboration role type", finderType.getName(), is("ThingByChance"));
	}

	//
	// Test framework
	//

	@Parameters(name = "{1}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList(
				new Object[][] {{Boolean.TRUE, "dynamic proxy" }, {Boolean.FALSE, "plain façade" }, });
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
	protected void fillMatchEngineFactoryRegistry(IMatchEngine.Factory.Registry matchEngineFactoryRegistry) {
		super.fillMatchEngineFactoryRegistry(matchEngineFactoryRegistry);

		IFacadeProvider.Factory.Registry facadeProviderRegistry;
		if (EMFPlugin.IS_ECLIPSE_RUNNING) {
			// Use the extension-based registry
			facadeProviderRegistry = EMFCompareFacadePlugin.getDefault().getFacadeProviderRegistry();
		} else {
			facadeProviderRegistry = FacadeProviderRegistryImpl.createStandaloneInstance();
			facadeProviderRegistry.add(new J2EEFacadeProvider.Factory());
		}

		IMatchEngine.Factory matchEngineFactory = new FacadeMatchEngine.Factory(UseIdentifiers.WHEN_AVAILABLE,
				facadeProviderRegistry);
		matchEngineFactory.setRanking(Integer.MAX_VALUE);

		matchEngineFactoryRegistry.add(matchEngineFactory);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractUMLInputData getInput() {
		return input;
	}

	/**
	 * Assert that, after comparison, the {@code scope} has only collected UML resource URIs.
	 * 
	 * @param scope
	 *            a comparison scope
	 */
	public void verifyComparisonScope(IComparisonScope scope) {
		scope.getResourceURIs().stream().forEach(
				uri -> assertThat("Not an UML URI", uri, endsWith("." + UMLResource.FILE_EXTENSION)));
	}

}
