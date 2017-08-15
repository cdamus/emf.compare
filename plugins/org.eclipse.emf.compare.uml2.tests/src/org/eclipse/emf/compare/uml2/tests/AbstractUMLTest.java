/**
 * Copyright (c) 2012, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Stefan Dirix - update priority value for UML merger
 *     Philip Langer - bug 501864
 *     Martin Fleck - bug 507177
 *     Christian W. Damus - customize the matching process in tests
 */
package org.eclipse.emf.compare.uml2.tests;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterators.all;
import static org.eclipse.emf.compare.merge.AbstractMerger.SUB_DIFF_AWARE_OPTION;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasDirectOrIndirectConflict;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.AttributeChange;
import org.eclipse.emf.compare.ComparePackage;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.EMFCompare.Builder;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.match.IMatchEngine;
import org.eclipse.emf.compare.match.impl.MatchEngineFactoryRegistryImpl;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMergeOptionAware;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.postprocessor.IPostProcessor;
import org.eclipse.emf.compare.postprocessor.PostProcessorDescriptorRegistryImpl;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.tests.framework.junit.AnnotationRule;
import org.eclipse.emf.compare.tests.postprocess.data.TestPostProcessor;
import org.eclipse.emf.compare.uml2.internal.StereotypedElementChange;
import org.eclipse.emf.compare.uml2.internal.UMLDiff;
import org.eclipse.emf.compare.uml2.internal.merge.OpaqueElementBodyChangeMerger;
import org.eclipse.emf.compare.uml2.internal.merge.UMLMerger;
import org.eclipse.emf.compare.uml2.internal.merge.UMLReferenceChangeMerger;
import org.eclipse.emf.compare.uml2.internal.postprocessor.OpaqueElementBodyChangePostProcessor;
import org.eclipse.emf.compare.uml2.internal.postprocessor.UMLPostProcessor;
import org.eclipse.emf.compare.uml2.profile.test.uml2comparetestprofile.UML2CompareTestProfilePackage;
import org.eclipse.emf.compare.utils.ReferenceUtil;
import org.eclipse.emf.compare.utils.ReflectiveDispatch;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

/**
 * This class provides hooks in the comparison process that subclasses may choose to implement by providing
 * public methods:
 * <ul>
 * <li><tt>verifyComparisonScope(<i>scope-type</i> scope)</tt>: check details of the scope input to the
 * comparison. The <i>scope-type</i> should be {@link IComparisonScope} or some type conforming to it if only
 * specific a implementation of the scope should be verified</li>
 * </ul>
 * 
 * @author <a href="mailto:cedric.notot@obeo.fr">Cedric Notot</a>
 */
@SuppressWarnings({"nls", "restriction" })
public abstract class AbstractUMLTest {

	@Rule
	public final AnnotationRule<AdditionalResources, AdditionalResourcesKind> additionalResourcesKind = AnnotationRule
			.create(AdditionalResources.class, AdditionalResourcesKind.NONE);

	protected EMFCompare emfCompare;

	private IMerger.Registry mergerRegistry;

	/** Cached cascading options before the last time the filter was enabled or disabled. */
	private static final Map<IMergeOptionAware, Object> CACHED_OPTIONS = Maps.newHashMap();

	/**
	 * Each sublass of AbstractUMLTest have to call this method in a @BeforeClass annotated method. This allow
	 * each test to customize its context.
	 */
	public static void fillRegistries() {
		if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
			EPackage.Registry.INSTANCE.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
			EPackage.Registry.INSTANCE.put(ComparePackage.eNS_URI, ComparePackage.eINSTANCE);
			EPackage.Registry.INSTANCE.put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);

			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore",
					new EcoreResourceFactoryImpl());
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("uml", //$NON-NLS-1$
					new UMLResourceFactoryImpl());
		}
	}

	/**
	 * Each sublass of AbstractUMLTest have to call this method in a @BeforeClass annotated method. This allow
	 * each test to safely delete its context.
	 */
	public static void resetRegistries() {
		if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("uml");
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("ecore");

			EPackage.Registry.INSTANCE.remove(UML2CompareTestProfilePackage.eNS_URI);
			EPackage.Registry.INSTANCE.remove(ComparePackage.eNS_URI);
			EPackage.Registry.INSTANCE.remove(EcorePackage.eNS_URI);
		}
	}

	@Before
	public void before() {
		Builder builder = EMFCompare.builder();
		// post-processor and merger registry is not filled in runtime (org.eclipse.emf.compare.rcp not
		// loaded)
		final IPostProcessor.Descriptor.Registry<String> postProcessorRegistry = new PostProcessorDescriptorRegistryImpl<String>();
		registerPostProcessors(postProcessorRegistry);
		builder.setPostProcessorRegistry(postProcessorRegistry);
		mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		final IMerger umlMerger = new UMLMerger();
		final IMerger umlReferenceChangeMerger = new UMLReferenceChangeMerger();
		final IMerger opaqueElementBodyChangeMerger = new OpaqueElementBodyChangeMerger();
		umlMerger.setRanking(20);
		umlReferenceChangeMerger.setRanking(25);
		opaqueElementBodyChangeMerger.setRanking(25);
		mergerRegistry.add(umlMerger);
		mergerRegistry.add(umlReferenceChangeMerger);
		mergerRegistry.add(opaqueElementBodyChangeMerger);

		IMatchEngine.Factory.Registry matchEngineFactoryRegistry = MatchEngineFactoryRegistryImpl
				.createStandaloneInstance();
		fillMatchEngineFactoryRegistry(matchEngineFactoryRegistry);
		builder.setMatchEngineFactoryRegistry(matchEngineFactoryRegistry);

		emfCompare = builder.build();
	}

	/**
	 * Overridden by subclasses that need to customize the matching step.
	 * 
	 * @param matchEngineFactoryRegistry
	 *            a registry of match-engine factories to tweak
	 */
	protected void fillMatchEngineFactoryRegistry(IMatchEngine.Factory.Registry matchEngineFactoryRegistry) {
		// Pass
	}

	/**
	 * Used to register new post processors.
	 * 
	 * @param postProcessorRegistry
	 */
	protected void registerPostProcessors(
			final IPostProcessor.Descriptor.Registry<String> postProcessorRegistry) {
		postProcessorRegistry.put(UMLPostProcessor.class.getName(),
				new TestPostProcessor.TestPostProcessorDescriptor(
						Pattern.compile("http://www.eclipse.org/uml2/\\d\\.0\\.0/UML"), null,
						new UMLPostProcessor(), 20));
		postProcessorRegistry.put(OpaqueElementBodyChangePostProcessor.class.getName(),
				new TestPostProcessor.TestPostProcessorDescriptor(
						Pattern.compile("http://www.eclipse.org/uml2/\\d\\.0\\.0/UML"), null,
						new OpaqueElementBodyChangePostProcessor(), 25));
	}

	@After
	public void cleanup() {
		getInput().close();
	}

	protected EMFCompare getCompare() {
		return emfCompare;
	}

	protected Comparison compare(Notifier left, Notifier right) {
		return compare(left, right, null);
	}

	protected Comparison compare(Notifier left, Notifier right, Notifier origin) {
		IComparisonScope scope;

		switch (additionalResourcesKind.get()) {
			case NONE:
				scope = new DefaultComparisonScope(left, right, origin);
				break;
			case REFERENCED_LOCAL:
			case REFERENCED_ALL:
				if (!((left instanceof Resource) || (right instanceof Resource)
						|| (origin instanceof Resource))) {
					scope = new DefaultComparisonScope(left, right, origin);
				} else {
					ResourceSet leftSet = null;
					ResourceSet rightSet = null;
					ResourceSet originSet = null;
					if (left != null) {
						leftSet = ((Resource)left).getResourceSet();
					}
					if (right != null) {
						rightSet = ((Resource)right).getResourceSet();
					}
					if (origin != null) {
						originSet = ((Resource)origin).getResourceSet();
					}
					DefaultComparisonScope theScope = new DefaultComparisonScope(leftSet, rightSet,
							originSet);
					scope = theScope;
					theScope.setResourceSetContentFilter(
							reachedFromAny(additionalResourcesKind.get().isLocal(), (Resource)left,
									(Resource)right, (Resource)origin));
				}
				break;
			default:
				fail("Unsupported additional-resources kind: " + additionalResourcesKind.get());
				scope = null; // Unreachable
				break;
		}

		Comparison result = getCompare().compare(scope);

		ReflectiveDispatch.safeInvoke(this, "verifyComparisonScope", AssertionError.class, scope);

		return result;
	}

	/**
	 * A predicate matching resources that are transitively reachable from any of a set of starting resources
	 * (such as inputs of the comparison).
	 * 
	 * @param localOnly
	 *            whether to consider reachability only locally in each resource's storage domain
	 * @param startingResource
	 *            the starting resources for reachability analysis
	 * @return the reachability predicate
	 */
	Predicate<Resource> reachedFromAny(boolean localOnly, Resource... startingResource) {
		ImmutableSet.Builder<Resource> builder = ImmutableSet.builder();
		for (Resource next : startingResource) {
			if (next != null) {
				builder.add(next);
			}
		}
		final Set<Resource> startingResources = builder.build();

		final ResourceGraph resourceGraph = new ResourceGraph(localOnly);
		for (Resource next : startingResources) {
			resourceGraph.addAdapter(next.getResourceSet());
			EcoreUtil.resolveAll(next); // Discover the cross-reference graph
		}

		return new Predicate<Resource>() {
			/**
			 * {@inheritDoc}
			 */
			public boolean apply(Resource input) {
				return resourceGraph.anyReaches(startingResources, input);
			}
		};

	}

	protected IMerger.Registry getMergerRegistry() {
		return mergerRegistry;
	}

	protected enum TestKind {
		ADD, DELETE;
	}

	protected static int count(List<Diff> differences, Predicate<Object> p) {
		int count = 0;
		final Iterator<Diff> result = Iterators.filter(differences.iterator(), p);
		while (result.hasNext()) {
			count++;
			result.next();
		}
		return count;
	}

	public static Predicate<? super Diff> onRealFeature(final EStructuralFeature feature) {
		return new Predicate<Diff>() {
			public boolean apply(Diff input) {
				final EStructuralFeature affectedFeature;
				if (input instanceof AttributeChange) {
					affectedFeature = ((AttributeChange)input).getAttribute();
				} else if (input instanceof ReferenceChange) {
					affectedFeature = ((ReferenceChange)input).getReference();
				} else {
					return false;
				}
				return feature == affectedFeature;
			}
		};
	}

	public static Predicate<? super Diff> isChangeAdd() {
		return new Predicate<Diff>() {
			public boolean apply(Diff input) {
				if (input instanceof ReferenceChange) {
					return ReferenceUtil
							.getAsList(input.getMatch().getLeft(), ((ReferenceChange)input).getReference())
							.contains(((ReferenceChange)input).getValue());
				} else if (input instanceof AttributeChange) {
					return ReferenceUtil
							.getAsList(input.getMatch().getLeft(), ((AttributeChange)input).getAttribute())
							.contains(((AttributeChange)input).getValue());
				}
				return false;
			}
		};
	}

	protected static Predicate<Diff> discriminantInstanceOf(final EClass clazz) {
		return new Predicate<Diff>() {
			public boolean apply(Diff input) {
				return input instanceof UMLDiff && clazz.isInstance(((UMLDiff)input).getDiscriminant());
			}

		};
	}

	protected abstract AbstractUMLInputData getInput();

	protected void testMergeLeftToRight(Notifier left, Notifier right, Notifier origin) {
		testMergeLeftToRight(left, right, origin, false);
	}

	protected void testMergeRightToLeft(Notifier left, Notifier right, Notifier origin) {
		testMergeRightToLeft(left, right, origin, false);
	}

	protected void testMergeLeftToRight(Notifier left, Notifier right, Notifier origin,
			boolean pseudoAllowed) {
		final IComparisonScope scope = new DefaultComparisonScope(left, right, origin);
		final Comparison comparisonBefore = getCompare().compare(scope);
		EList<Diff> differencesBefore = comparisonBefore.getDifferences();
		final IBatchMerger merger = new BatchMerger(mergerRegistry);
		merger.copyAllLeftToRight(differencesBefore, new BasicMonitor());
		final Comparison comparisonAfter = getCompare().compare(scope);
		EList<Diff> differencesAfter = comparisonAfter.getDifferences();
		final boolean diffs;
		if (pseudoAllowed) {
			diffs = all(differencesAfter.iterator(), hasDirectOrIndirectConflict(ConflictKind.PSEUDO));
		} else {
			diffs = differencesAfter.isEmpty();
		}
		assertTrue("Comparison#getDifferences() must be empty after copyAllLeftToRight", diffs);
	}

	protected void testMergeRightToLeft(Notifier left, Notifier right, Notifier origin,
			boolean pseudoAllowed) {
		final IComparisonScope scope = new DefaultComparisonScope(left, right, origin);
		final Comparison comparisonBefore = getCompare().compare(scope);
		EList<Diff> differencesBefore = comparisonBefore.getDifferences();
		final IBatchMerger merger = new BatchMerger(mergerRegistry);
		merger.copyAllRightToLeft(differencesBefore, new BasicMonitor());
		final Comparison comparisonAfter = getCompare().compare(scope);
		EList<Diff> differencesAfter = comparisonAfter.getDifferences();
		final boolean diffs;
		if (pseudoAllowed) {
			diffs = all(differencesAfter.iterator(), hasDirectOrIndirectConflict(ConflictKind.PSEUDO));
		} else {
			diffs = differencesAfter.isEmpty();
		}
		assertTrue("Comparison#getDifferences() must be empty after copyAllRightToLeft", diffs);
	}

	protected void testIntersections(Comparison comparison) {
		for (Diff diff : comparison.getDifferences()) {
			int realRefinesSize = Iterables.size(
					Iterables.filter(diff.getRefines(), not(instanceOf(StereotypedElementChange.class))));
			assertFalse("Wrong number of refines (without StereotypedElementChange) on" + diff,
					realRefinesSize > 1);
			int stereotypedElementChangeRefines = Iterables
					.size(Iterables.filter(diff.getRefines(), instanceOf(StereotypedElementChange.class)));
			assertFalse("Wrong number of refines (of type StereotypedElementChange) on " + diff,
					stereotypedElementChangeRefines > 1);

		}
	}

	/**
	 * Enables the cascading filter by setting the filter option in all mergers to true. Any changes done by
	 * this method can be restored by calling {@link #restoreCascadingFilter()}.
	 */
	protected void enableCascadingFilter() {
		setCascadingFilter(true);
	}

	/**
	 * Disables the cascading filter by setting the filter option in all mergers to false. Any changes done by
	 * this method can be restored by calling {@link #restoreCascadingFilter()}.
	 */
	protected void disableCascadingFilter() {
		setCascadingFilter(false);
	}

	/**
	 * Sets the cascading filter option (subdiff-awareness) of all mergers to the given state. Any changes
	 * done by this method can be restored by calling {@link #restoreCascadingFilter()}.
	 * 
	 * @param enabled
	 *            filter state
	 */
	private void setCascadingFilter(boolean enabled) {
		for (IMergeOptionAware merger : Iterables.filter(mergerRegistry.getMergers(null),
				IMergeOptionAware.class)) {
			Map<Object, Object> mergeOptions = merger.getMergeOptions();
			Object previousValue = mergeOptions.get(SUB_DIFF_AWARE_OPTION);
			CACHED_OPTIONS.put(merger, previousValue);
			mergeOptions.put(SUB_DIFF_AWARE_OPTION, Boolean.valueOf(enabled));
		}
	}

	/**
	 * Restores the cascading filter options changed by the last call to {@link #enableCascadingFilter()},
	 * {@link #disableCascadingFilter()}, or {@link #setCascadingFilter(boolean)}.
	 */
	protected void restoreCascadingFilter() {
		// restore previous values
		for (Entry<IMergeOptionAware, Object> entry : CACHED_OPTIONS.entrySet()) {
			IMergeOptionAware merger = entry.getKey();
			merger.getMergeOptions().put(SUB_DIFF_AWARE_OPTION, entry.getValue());
		}
	}
}
