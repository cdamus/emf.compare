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

import static com.google.common.base.Predicates.and;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.isRealConflict;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.matches;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.addedToReference;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.attributeValueMatch;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.onEObject;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.onFeature;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.removedFromReference;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.facade.FacadeObject;
import org.eclipse.emf.compare.facade.IFacadeProvider;
import org.eclipse.emf.compare.facade.internal.EMFCompareFacadePlugin;
import org.eclipse.emf.compare.facade.internal.FacadeProviderRegistryImpl;
import org.eclipse.emf.compare.facade.internal.match.FacadeMatchEngine;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.uml2.facade.tests.data.UMLInputData;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.OpaqexprPackage;
import org.eclipse.emf.compare.uml2.facade.tests.opaqexpr.internal.providers.OpaqexprFacadeProvider;
import org.eclipse.emf.compare.uml2.facade.tests.util.DynamicProxiesRule;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.utils.UseIdentifiers;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for comparison based on pluggable façade model providers with the {@link OpaqexprPackage
 * opaqexpr} façade.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing", "restriction" })
@RunWith(Parameterized.class)
public class OpaqexprFacadeProviderComparisonTest extends AbstractFacadeTest {

	@Rule
	public final DynamicProxiesRule useDynamicProxies;

	private UMLInputData input = new UMLInputData();

	/**
	 * Initializes me.
	 */
	public OpaqexprFacadeProviderComparisonTest(boolean useDynamicProxies,
			@SuppressWarnings("unused") String label) {

		super();

		this.useDynamicProxies = new DynamicProxiesRule(useDynamicProxies);
	}

	@Test
	public void addBody_o1() {
		Resource left = input.getO1Left();
		Resource right = input.getO1Right();
		Comparison comparison = compare(left, right);

		testO1(TestKind.ADD, comparison);
	}

	private void testO1(TestKind kind, Comparison comparison) {
		List<Diff> differences = comparison.getDifferences();
		EAttribute keyFeature = OpaqexprPackage.Literals.BODY_ENTRY__KEY;

		assertThat(differences.size(), is(1));

		Predicate<? super Diff> addBodyEntryDescription;

		switch (kind) {
			case DELETE:
				addBodyEntryDescription = removedFromReference("expr", "body", "expr.Smalltalk", keyFeature);
				break;
			case ADD:
				addBodyEntryDescription = addedToReference("expr", "body", "expr.Smalltalk", keyFeature);
				break;
			default:
				fail("Unsupported test kind: " + kind);
				return; // Unreachable
		}

		Diff addBodyEntry = Iterators.find(differences.iterator(), addBodyEntryDescription);
		assertThat(addBodyEntry, notNullValue());
		assertThat(addBodyEntry.getRefinedBy(), not(hasItem(anything())));
		assertThat(addBodyEntry.getRefines(), not(hasItem(anything())));
		assertThat(addBodyEntry.getRequires(), not(hasItem(anything())));
		assertThat(addBodyEntry.getRequiredBy(), not(hasItem(anything())));

		Matcher<EObject> proxyAssertion;
		if (useDynamicProxies.getAsBoolean()) {
			// And we did, in fact, use the façade
			proxyAssertion = instanceOf(FacadeObject.class);
		} else {
			proxyAssertion = not(instanceOf(FacadeObject.class));
		}

		assertThat(addBodyEntry.getMatch().getLeft(), proxyAssertion);
		EObject left = addBodyEntry.getMatch().getLeft();
		assertThat(left.eClass(), is(OpaqexprPackage.Literals.OPAQUE_EXPRESSION));
		assertThat(addBodyEntry.getMatch().getRight(), proxyAssertion);
		EObject right = addBodyEntry.getMatch().getRight();
		assertThat(right.eClass(), is(OpaqexprPackage.Literals.OPAQUE_EXPRESSION));
	}

	@Test
	public void removeBody_o1() {
		// Reverse comparison
		Resource left = input.getO1Right();
		Resource right = input.getO1Left();
		Comparison comparison = compare(left, right);

		testO1(TestKind.DELETE, comparison);
	}

	@Test
	public void mergeAddBodyRL_o1() {
		Resource left = input.getO1Left();
		Resource right = input.getO1Right();

		testMergeRightToLeft(left, right, null);
	}

	@Test
	public void mergeBAddBodyLR_o1() {
		Resource left = input.getO1Left();
		Resource right = input.getO1Right();

		testMergeLeftToRight(left, right, null);
	}

	@Test
	public void bodyValueConflict_o2() {
		Resource base = input.getO2Base();
		Resource left = input.getO2Left();
		Resource right = input.getO2Right();
		Comparison comparison = compare(left, right, base);

		List<Conflict> conflicts = comparison.getConflicts();

		assertThat(conflicts.size(), is(1));

		Conflict conflict = conflicts.get(0);
		assertThat(conflict, isRealConflict());

		Diff setBodyEntry1 = Iterables.find(conflict.getDifferences(),
				changedBodyValue("o2.expr.Smalltalk", "self things notEmpty"));
		Diff setBodyEntry2 = Iterables.find(conflict.getDifferences(),
				changedBodyValue("o2.expr.Smalltalk", "things notEmpty"));

		assertThat(setBodyEntry1, notNullValue());
		assertThat(setBodyEntry2, notNullValue());
	}

	@Test
	public void bodyAddAddConflictDifferentIndex_o3() {
		Resource base = input.getO3Base();
		Resource left = input.getO3Left();
		Resource right = input.getO3Right();
		Comparison comparison = compare(left, right, base);
		EAttribute keyFeature = OpaqexprPackage.Literals.BODY_ENTRY__KEY;

		List<Conflict> conflicts = comparison.getConflicts();

		assertThat(conflicts.size(), is(1));

		Conflict conflict = conflicts.get(0);
		assertThat(conflict, isRealConflict());

		Predicate<? super Diff> addBodyDescription = addedToReference("o3.expr", "body", "o3.expr.Smalltalk",
				keyFeature);

		assertThat(conflict.getDifferences(),
				everyItem(matches(Diff.class, "is add Smalltalk body", addBodyDescription)));
	}

	@Test
	public void bodyAddAddConflictSameIndex_o4() {
		Resource base = input.getO4Base();
		Resource left = input.getO4Left();
		Resource right = input.getO4Right();
		Comparison comparison = compare(left, right, base);
		EAttribute keyFeature = OpaqexprPackage.Literals.BODY_ENTRY__KEY;

		List<Conflict> conflicts = comparison.getConflicts();

		assertThat(conflicts.size(), is(1));

		Conflict conflict = conflicts.get(0);
		assertThat(conflict, isRealConflict());

		Predicate<? super Diff> addBodyDescription = addedToReference("o4.expr", "body", "o4.expr.Smalltalk",
				keyFeature);

		assertThat(conflict.getDifferences(),
				everyItem(matches(Diff.class, "is add Smalltalk body", addBodyDescription)));
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
			facadeProviderRegistry.add(new OpaqexprFacadeProvider.Factory());
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

	@SuppressWarnings("unchecked")
	Predicate<Diff> changedBodyValue(String qualifiedName, String value) {
		return and(onEObject(qualifiedName, OpaqexprPackage.Literals.BODY_ENTRY__KEY),
				onFeature(OpaqexprPackage.Literals.BODY_ENTRY__VALUE),
				attributeValueMatch("value", value, false));

	}
}
