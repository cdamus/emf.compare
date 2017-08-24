/*******************************************************************************
 * Copyright (c) 2015, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Christian W. Damus - façade providers integration
 *******************************************************************************/
package org.eclipse.emf.compare.uml2.facade.ui.tests;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.eclipse.emf.compare.tests.framework.CompareMatchers.regexMatches;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareIDEUIPlugin;
import org.eclipse.emf.compare.ide.ui.internal.configuration.EMFCompareConfiguration;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.tree.provider.DelegatingTreeMergeViewerItemContentProvider;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.tree.provider.MergeViewerItemProviderConfiguration;
import org.eclipse.emf.compare.ide.ui.internal.contentmergeviewer.tree.provider.TreeContentMergeViewerItemLabelProvider;
import org.eclipse.emf.compare.ide.ui.internal.logical.ComparisonScopeBuilder;
import org.eclipse.emf.compare.ide.ui.internal.logical.StorageTypedElement;
import org.eclipse.emf.compare.ide.ui.internal.logical.StreamAccessorStorage;
import org.eclipse.emf.compare.ide.ui.internal.logical.resolver.ThreadedModelResolver;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.CompareInputAdapter;
import org.eclipse.emf.compare.ide.ui.internal.structuremergeviewer.provider.TreeCompareInputAdapterFactory;
import org.eclipse.emf.compare.ide.ui.internal.util.PlatformElementUtil;
import org.eclipse.emf.compare.ide.ui.logical.IModelResolver;
import org.eclipse.emf.compare.ide.ui.tests.framework.annotations.Compare;
import org.eclipse.emf.compare.provider.spec.CompareItemProviderAdapterFactorySpec;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;
import org.eclipse.emf.compare.rcp.ui.internal.structuremergeviewer.groups.provider.TreeItemProviderAdapterFactorySpec;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.rcp.ui.mergeviewer.item.provider.IMergeViewerItemProviderConfiguration;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.compare.tests.framework.junit.AnnotationRule;
import org.eclipse.emf.compare.tests.framework.junit.AutoCloseRule;
import org.eclipse.emf.compare.tests.framework.junit.ProjectFixture;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.provider.AdapterFactoryItemDelegator;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.tree.TreeFactory;
import org.eclipse.emf.edit.tree.TreeNode;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test cases for provider delegation in presentation of façades in the merge viewer.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"restriction", "nls", "boxing" })
public class MergeViewerTreeTest {

	private static final String CONFLICTS = "Conflicts";

	private static final String CONFLICT = "> Conflict";

	@Rule
	public final AnnotationRule<Compare, String> leftRule = AnnotationRule.create(Compare.class, String.class,
			Compare::left);

	@Rule
	public final AnnotationRule<Compare, String> rightRule = AnnotationRule.create(Compare.class,
			String.class, Compare::right);

	@Rule
	public final AnnotationRule<Compare, String> baseRule = AnnotationRule.create(Compare.class, String.class,
			Compare::ancestor);

	@Rule
	public final AutoCloseRule cleanup = new AutoCloseRule();

	private final ProjectFixture project = new ProjectFixture();

	private AdapterFactoryItemDelegator itemDelegator;

	/**
	 * Initializes me.
	 */
	public MergeViewerTreeTest() {
		super();
	}

	@Test
	@Compare(left = "data/basic/left.uml", right = "data/basic/right.uml", ancestor = "data/basic/base.uml")
	public void mergeViewerFacadeWithConflicts() throws IOException, CoreException {
		EMFCompareConfiguration config = initComparison("Facade");
		Comparison comparison = config.getComparison();

		TreeNode rootTreeNode = TreeFactory.eINSTANCE.createTreeNode();
		rootTreeNode.setData(comparison);
		rootTreeNode.eAdapters().add(config.getStructureMergeViewerGrouper().getProvider());

		List<?> children = children(rootTreeNode);

		// First level starts with the "Conflicts" node
		assertThat(children.size(), greaterThanOrEqualTo(1));
		Object conflictsNode = children.get(0);
		assertThat(label(conflictsNode), is(CONFLICTS));

		children = children(conflictsNode);

		// Second level starts with the "Conflict" node
		assertThat(children.size(), greaterThanOrEqualTo(1));
		Object conflictNode = children.get(0);
		assertThat(label(conflictNode), startsWith(CONFLICT));

		children = children(conflictNode);

		// Third level has our conflicted matches and within
		// those are the actual conflicting diffs
		assertThat(children.size(), is(4));
		Set<String> missingDiffs = newLinkedHashSet(asList( //
				"Home Interface WhatsitHome [homeInterface set]", //
				"Bean Whatsit [bean changed]", //
				"Bean Thing [bean changed]", //
				"Home Interface WhatsitHome [homeInterface changed]", //
				"Bean Thing [bean unset]"));
		for (Object next : children) {
			for (Object diff : children(next)) {
				missingDiffs.remove(label(diff));
			}
		}

		assertNoDiffsMissed(missingDiffs);
	}

	@Test
	@Compare(left = "data/partialFacade/left.uml", right = "data/partialFacade/right.uml", ancestor = "data/partialFacade/base.uml")
	public void mergeViewerPartialFacadeWithConflicts() throws IOException, CoreException {
		EMFCompareConfiguration config = initComparison("PartialFacade");
		Comparison comparison = config.getComparison();

		TreeNode rootTreeNode = TreeFactory.eINSTANCE.createTreeNode();
		rootTreeNode.setData(comparison);
		rootTreeNode.eAdapters().add(config.getStructureMergeViewerGrouper().getProvider());

		List<?> children = children(rootTreeNode);

		// First level starts with the "Conflicts" node
		assertThat(children.size(), greaterThanOrEqualTo(1));
		Object conflictsNode = children.get(0);
		assertThat(label(conflictsNode), is(CONFLICTS));

		children = children(conflictsNode);

		// Second level starts with the "Conflict" node
		assertThat(children.size(), greaterThanOrEqualTo(1));
		Object conflictNode = children.get(0);
		assertThat(label(conflictNode), startsWith(CONFLICT));

		children = children(conflictNode);

		// Third level has our conflicted matches and within
		// those are the actual conflicting diffs
		assertThat(children.size(), is(1));
		assertThat(label(children.get(0)), is("Smalltalk -> self things notEmpty"));
		Set<String> missingDiffs = newLinkedHashSet(asList( //
				"self things notEmpty [value changed]", //
				"things notEmpty [value changed]"));
		for (Object next : children) {
			for (Object diff : children(next)) {
				missingDiffs.remove(label(diff));
			}
		}

		assertNoDiffsMissed(missingDiffs);
	}

	@Test
	@Compare(left = "data/basic/left.uml", right = "data/basic/right.uml", ancestor = "data/basic/base.uml")
	public void leftViewerFacade() throws IOException, CoreException {
		EMFCompareConfiguration config = initComparison("Facade");
		Comparison comparison = config.getComparison();

		TreeNode rootTreeNode = TreeFactory.eINSTANCE.createTreeNode();
		rootTreeNode.setData(comparison);
		rootTreeNode.eAdapters().add(config.getStructureMergeViewerGrouper().getProvider());

		// Find the first diff
		TreeNode diff = requireDiff(rootTreeNode);

		// Get the compare input adapter
		CompareInputAdapter input = requireCompareInputAdapter(diff, config);

		// Check the tree
		IMergeViewerItemProviderConfiguration itemProviderConfig = new MergeViewerItemProviderConfiguration(
				config.getAdapterFactory(), config.getStructureMergeViewerGrouper().getProvider(),
				config.getStructureMergeViewerFilter().getAggregatedPredicate(), config.getComparison(),
				MergeViewerSide.LEFT);
		ITreeContentProvider treeProvider = new DelegatingTreeMergeViewerItemContentProvider(
				config.getComparison(), itemProviderConfig);
		ILabelProvider labelProvider = new TreeContentMergeViewerItemLabelProvider(null,
				config.getAdapterFactory(), MergeViewerSide.LEFT);

		// Get the root match
		EObject root = input.getComparisonObject();
		for (EObject next = root; !(next instanceof Comparison);) {
			next = (EObject)treeProvider.getParent(next);
			if (!(next instanceof Comparison)) {
				root = next;
			}
		}

		// First level starts with the Package match
		assertThat(labelProvider.getText(root), is("Package model"));

		Object[] children = treeProvider.getChildren(root);

		// Second level has our matches and within
		// those are the actual diffs
		assertThat(children.length, greaterThanOrEqualTo(5));
		Set<String> missingDiffs = newLinkedHashSet(asList( //
				"Home Interface WhatsitHome [homeInterface set]", //
				"Bean Whatsit [bean changed]", //
				"Bean Thing [bean changed]", //
				"Home Interface WhatsitHome [homeInterface changed]", //
				"Bean Thing [bean unset]"));
		for (Object child : children) {
			for (Object next : children(child)) {
				missingDiffs.remove(label(next));
			}
		}

		assertNoDiffsMissed(missingDiffs);
	}

	@Test
	@Compare(left = "data/partialFacade/left.uml", right = "data/partialFacade/right.uml", ancestor = "data/partialFacade/base.uml")
	public void leftViewerPartialFacade() throws IOException, CoreException {
		EMFCompareConfiguration config = initComparison("PartialFacade");
		Comparison comparison = config.getComparison();

		TreeNode rootTreeNode = TreeFactory.eINSTANCE.createTreeNode();
		rootTreeNode.setData(comparison);
		rootTreeNode.eAdapters().add(config.getStructureMergeViewerGrouper().getProvider());

		// Find the first diff
		TreeNode diff = requireDiff(rootTreeNode);

		// Get the compare input adapter
		CompareInputAdapter input = requireCompareInputAdapter(diff, config);

		// Check the tree
		IMergeViewerItemProviderConfiguration itemProviderConfig = new MergeViewerItemProviderConfiguration(
				config.getAdapterFactory(), config.getStructureMergeViewerGrouper().getProvider(),
				config.getStructureMergeViewerFilter().getAggregatedPredicate(), config.getComparison(),
				MergeViewerSide.LEFT);
		ITreeContentProvider treeProvider = new DelegatingTreeMergeViewerItemContentProvider(
				config.getComparison(), itemProviderConfig);
		ILabelProvider labelProvider = new TreeContentMergeViewerItemLabelProvider(null,
				config.getAdapterFactory(), MergeViewerSide.LEFT);

		// Get the root match
		EObject root = input.getComparisonObject();
		for (EObject next = root; !(next instanceof Comparison);) {
			next = (EObject)treeProvider.getParent(next);
			if (!(next instanceof Comparison)) {
				root = next;
			}
		}

		// First level starts with the Package match. Note that this
		// uses the UML label
		assertThat(labelProvider.getText(root), is("<Package> root"));

		Object[] children = treeProvider.getChildren(root);

		// Second level has the opaque expression containing bodies.
		// Note that this is not an UML label, but from the façade
		assertThat(children.length, is(1));
		assertThat(labelProvider.getText(children[0]), is("Opaque Expression expr"));

		children = treeProvider.getChildren(children[0]);

		// Third level has our matches and within
		// those are the actual diffs
		assertThat(children.length, greaterThanOrEqualTo(4));
		Set<String> missingDiffs = newLinkedHashSet(asList( //
				"self things notEmpty [value changed]", //
				"things notEmpty [value changed]"));
		for (Object child : children) {
			assertThat("Wrong body label", labelProvider.getText(child),
					regexMatches("^(OCL|Smalltalk|Java|English) -> "));

			for (Object next : children(child)) {
				missingDiffs.remove(label(next));
			}
		}

		assertNoDiffsMissed(missingDiffs);
	}

	//
	// Test framework
	//

	private EMFCompareConfiguration initComparison(String projectName) throws IOException, CoreException {
		URL leftURL = getClass().getResource(leftRule.get());
		URL rightURL = getClass().getResource(rightRule.get());
		URL baseURL = null;
		if (!Strings.isNullOrEmpty(baseRule.get())) {
			baseURL = getClass().getResource(baseRule.get());
		}

		project.create(projectName);
		IFile leftFile = project.createFile("left.uml", leftURL);
		IFile rightFile = project.createFile("right.uml", rightURL);
		IFile baseFile = null;
		if (baseURL != null) {
			baseFile = project.createFile("base.uml", baseURL);
		}

		ITypedElement left = new StorageTypedElement(leftFile, leftFile.getFullPath().toOSString());
		ITypedElement right = new StorageTypedElement(rightFile, rightFile.getFullPath().toOSString());
		ITypedElement base = null;
		if (baseFile != null) {
			base = new StorageTypedElement(baseFile, baseFile.getFullPath().toOSString());
		}

		IStorage leftStorage = PlatformElementUtil.findFile(left);
		if (leftStorage == null) {
			leftStorage = StreamAccessorStorage.fromTypedElement(left);
		}
		IModelResolver resolver = EMFCompareIDEUIPlugin.getDefault().getModelResolverRegistry()
				.getBestResolverFor(leftStorage);

		assertThat(resolver, instanceOf(ThreadedModelResolver.class));

		CompareConfiguration config = new CompareConfiguration();
		if (baseURL == null) {
			config.setLeftEditable(true);
			config.setRightEditable(true);
		} else {
			config.setLeftEditable(false);
			config.setRightEditable(false);
		}
		EMFCompareConfiguration emfConfig = new EMFCompareConfiguration(config);

		ComposedAdapterFactory composedAdapterFactory = new ComposedAdapterFactory(
				ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		composedAdapterFactory.addAdapterFactory(new CompareItemProviderAdapterFactorySpec());
		composedAdapterFactory.addAdapterFactory(new TreeCompareInputAdapterFactory());
		emfConfig.setAdapterFactory(composedAdapterFactory);

		ComparisonScopeBuilder scopeBuilder = new ComparisonScopeBuilder(resolver,
				EMFCompareIDEUIPlugin.getDefault().getModelMinimizerRegistry().getCompoundMinimizer(), null);
		IComparisonScope scope = scopeBuilder.build(left, right, base, new NullProgressMonitor());

		EMFCompareRCPPlugin plugin = EMFCompareRCPPlugin.getDefault();
		Comparison comparison = EMFCompare.builder() //
				.setMatchEngineFactoryRegistry(plugin.getMatchEngineFactoryRegistry()) //
				.setPostProcessorRegistry(plugin.getPostProcessorRegistry()) //
				.build().compare(scope);

		emfConfig.setComparisonAndScope(comparison, scope);

		composedAdapterFactory.addAdapterFactory(
				new TreeItemProviderAdapterFactorySpec(emfConfig.getStructureMergeViewerFilter()));
		itemDelegator = new AdapterFactoryItemDelegator(composedAdapterFactory);

		return emfConfig;
	}

	List<?> children(Object node) {
		Collection<?> result = itemDelegator.getChildren(node);

		if (result instanceof List<?>) {
			return (List<?>)result;
		} else {
			return Lists.newArrayList(result);
		}
	}

	String label(Object node) {
		return itemDelegator.getText(node);
	}

	void assertNoDiffsMissed(Collection<?> diffs) {
		assertThat(diffs.stream().map(String::valueOf) //
				.collect(joining(", ", "Missing diffs: ", "")), //
				diffs, empty());

	}

	TreeNode findDiff(Object root) {
		TreeNode result;

		if ((root instanceof TreeNode) && (((TreeNode)root).getData() instanceof Diff)) {
			result = (TreeNode)root;
		} else {
			List<?> children = children(root);
			result = null;

			for (Object next : children) {
				result = findDiff(next);
				if (result != null) {
					break;
				}
			}
		}

		return result;
	}

	TreeNode requireDiff(Object root) {
		TreeNode result = findDiff(root);

		assertThat("No diff found in merge tree", result, notNullValue());

		return result;
	}

	CompareInputAdapter requireCompareInputAdapter(TreeNode node, EMFCompareConfiguration config) {
		Object result = config.getAdapterFactory().adapt(node, ICompareInput.class);

		assertThat("No compare input adapter", result, notNullValue());
		assertThat("Wrong compare input adapter", result, instanceOf(CompareInputAdapter.class));

		return (CompareInputAdapter)result;
	}
}
