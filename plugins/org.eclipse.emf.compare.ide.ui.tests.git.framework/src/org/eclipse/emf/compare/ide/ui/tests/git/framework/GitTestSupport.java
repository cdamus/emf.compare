/*******************************************************************************
 * Copyright (c) 2016, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Philip Langer - add convenience methods
 *     Martin Fleck - bug 512562
 *     Christian W. Damus - integration of façade providers
 *******************************************************************************/
package org.eclipse.emf.compare.ide.ui.tests.git.framework;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.RemoteResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.egit.core.Activator;
import org.eclipse.egit.core.internal.util.ResourceUtil;
import org.eclipse.egit.core.op.BranchOperation;
import org.eclipse.egit.core.op.CherryPickOperation;
import org.eclipse.egit.core.op.MergeOperation;
import org.eclipse.egit.core.op.RebaseOperation;
import org.eclipse.egit.core.op.ResetOperation;
import org.eclipse.egit.core.synchronize.GitResourceVariantTreeSubscriber;
import org.eclipse.egit.core.synchronize.GitSubscriberResourceMappingContext;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeData;
import org.eclipse.egit.core.synchronize.dto.GitSynchronizeDataSet;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.EMFCompare.Builder;
import org.eclipse.emf.compare.ide.ui.internal.EMFCompareIDEUIPlugin;
import org.eclipse.emf.compare.ide.ui.internal.logical.ComparisonScopeBuilder;
import org.eclipse.emf.compare.ide.ui.internal.logical.StorageTypedElement;
import org.eclipse.emf.compare.ide.ui.internal.logical.SubscriberStorageAccessor;
import org.eclipse.emf.compare.ide.ui.logical.IModelResolver;
import org.eclipse.emf.compare.ide.ui.logical.IStorageProvider;
import org.eclipse.emf.compare.ide.ui.logical.IStorageProviderAccessor;
import org.eclipse.emf.compare.ide.ui.tests.git.framework.internal.statements.InternalGitTestSupport;
import org.eclipse.emf.compare.rcp.internal.extension.impl.EMFCompareBuilderConfigurator;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;

/**
 * This class contains methods to perform git operations in the context of an EMFCompare test. This class may
 * be injected in client.
 * 
 * @author <a href="mailto:mathieu.cartaud@obeo.fr">Mathieu Cartaud</a>
 */
@SuppressWarnings({"restriction" })
public class GitTestSupport extends InternalGitTestSupport {

	/**
	 * Used to specify that there is only one project in the repository. The tester does not have to specify
	 * it.
	 */
	public static final String COMPARE_NO_PROJECT_SELECTED = "noProject"; //$NON-NLS-1$

	/** Convenience for the boolean {@code threeWay} parameter to specify a two-way comparison. */
	public static final boolean TWO_WAY = false;

	/** Convenience for the boolean {@code threeWay} parameter to specify a three-way comparison. */
	public static final boolean THREE_WAY = true;

	/** The result of the merge operation. */
	private MergeResult mergeResult;

	/** The result of the rebase operation. */
	private RebaseResult rebaseResult;

	/** The result of the cherry-pick operation. */
	private CherryPickResult cherryPickResult;

	public Repository getRepository() {
		return repository;
	}

	/**
	 * Get the list of projects in the workspace. All projects are refreshed before being returned.
	 * 
	 * @return the list of all projects
	 * @throws CoreException
	 *             Thrown if the refresh operation went wrong
	 */
	public List<IProject> getProjects() throws CoreException {
		for (IProject project : projects) {
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
		return Lists.newArrayList(projects);
	}

	/**
	 * Get The JGit Status Object from the repository.
	 * 
	 * @return the status Object of the Git repository
	 * @throws Exception
	 *             If the status cannot be retrieved
	 */
	public Status getStatus() throws Exception {
		Git git = new Git(repository);
		try {
			return git.status().call();
		} finally {
			git.close();
		}
	}

	public MergeResult getMergeResult() {
		return mergeResult;
	}

	public RebaseResult getRebaseResult() {
		return rebaseResult;
	}

	public CherryPickResult getCherryPickResult() {
		return cherryPickResult;
	}

	/**
	 * Merge two branches with the given merge strategy.
	 * 
	 * @param local
	 *            The checkouted branch (for example "master" or "refs/for/master", both syntaxes are
	 *            accepted)
	 * @param remote
	 *            The branch to merge with (for example "master" or "refs/for/master", both syntaxes are
	 *            accepted)
	 * @throws CoreException
	 *             Thrown if the merge operation or the refresh of projects went wrong
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws InterruptedException
	 *             Thrown if the checkout operation went wrong
	 */
	public void merge(String local, String remote) throws CoreException, IOException, InterruptedException {
		checkoutBranch(normalizeBranch(local));
		MergeOperation op = new MergeOperation(repository, normalizeBranch(remote));
		op.execute(new NullProgressMonitor());
		mergeResult = op.getResult();
		for (IProject iProject : projects) {
			iProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
	}

	/**
	 * Cherry-pick the commit located on the given "from" branch to the given "to" branch.
	 * 
	 * @param local
	 *            The branch on with the commit will be cherry-picked (for example "master" or
	 *            "refs/for/master", both syntaxes are accepted)
	 * @param remote
	 *            The branch where the commit will be cherry-picked (for example "master" or
	 *            "refs/for/master", both syntaxes are accepted)
	 * @throws CoreException
	 *             Thrown if the cherry-pick operation or the refresh of projects went wrong
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws InterruptedException
	 *             Thrown if the checkout operation went wrong
	 */
	public void cherryPick(String local, String remote)
			throws CoreException, IOException, InterruptedException {
		checkoutBranch(normalizeBranch(local));
		RevWalk revWalk = new RevWalk(repository);
		try {
			RevCommit commitId = revWalk.parseCommit(repository.findRef(remote).getObjectId());
			CherryPickOperation op = new CherryPickOperation(repository, commitId);
			op.execute(new NullProgressMonitor());
			cherryPickResult = op.getResult();
			for (IProject iProject : projects) {
				iProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			}
		} finally {
			revWalk.close();
		}
	}

	/**
	 * Rebase the given from branch on the to branch.
	 * 
	 * @param local
	 *            The checkouted branch (for example "master" or "refs/for/master", both syntaxes are
	 *            accepted)
	 * @param remote
	 *            The branch to rebase on (for example "master" or "refs/for/master", both syntaxes are
	 *            accepted)
	 * @throws CoreException
	 *             Thrown if the rebase operation or the refresh of projects went wrong
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws InterruptedException
	 *             Thrown if the checkout operation went wrong
	 */
	public void rebase(String local, String remote) throws CoreException, IOException, InterruptedException {
		checkoutBranch(normalizeBranch(local));
		RebaseOperation op = new RebaseOperation(repository, repository.findRef(remote));
		op.execute(new NullProgressMonitor());
		rebaseResult = op.getResult();
		for (IProject iProject : projects) {
			iProject.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		}
	}

	/**
	 * <p>
	 * Do a three-way comparison of the file with the given path between the two given branches. This method
	 * is intended to be used when there is only one project in the repository.
	 * </p>
	 * <p>
	 * If there are multiple projects, then use {@link #compare(String, String, String, String)} with the name
	 * of the containing project, instead.
	 * </p>
	 * 
	 * @param from
	 *            The branch to check out (for example "master" or "refs/heads/master", both syntaxes are
	 *            accepted)
	 * @param to
	 *            The branch to compare with (for example "mybranch" or "refs/heads/mybranch", both syntaxes
	 *            are accepted)
	 * @param fileName
	 *            The file to compare (the relative path to the file from the project)
	 * @return the comparison
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws CoreException
	 *             Thrown if the checkout operation went wrong
	 */
	public Comparison compare(String from, String to, String fileName) throws IOException, CoreException {
		return compare(from, to, fileName, COMPARE_NO_PROJECT_SELECTED);
	}

	/**
	 * <p>
	 * Compare a file with the given path between the two given branches. This method is intended to be used
	 * when there is only one project in the repository.
	 * </p>
	 * <p>
	 * If there are multiple projects, then use {@link #compare(String, String, String, String, boolean)} with
	 * the name of the containing project, instead.
	 * </p>
	 * 
	 * @param from
	 *            The branch to check out (for example "master" or "refs/heads/master", both syntaxes are
	 *            accepted)
	 * @param to
	 *            The branch to compare with (for example "mybranch" or "refs/heads/mybranch", both syntaxes
	 *            are accepted)
	 * @param fileName
	 *            The file to compare (the relative path to the file from the project)
	 * @param threeWay
	 *            {@code true} to perform a three-way comparison; {@code false} for a two-way comparison
	 * @return the comparison
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws CoreException
	 *             Thrown if the checkout operation went wrong
	 */
	public Comparison compare(String from, String to, String fileName, boolean threeWay)
			throws IOException, CoreException {
		return compare(from, to, fileName, COMPARE_NO_PROJECT_SELECTED, threeWay);
	}

	/**
	 * <p>
	 * Do a three-way compare on the file with the given path between the two given branches, accounting for
	 * their common ancestor. This method is intended to be used when there are multiple projects in the
	 * repository.
	 * </p>
	 * <p>
	 * If there is only one project, then use {@link #compare(String, String, String)} instead or pass
	 * {@link GitTestSupport#COMPARE_NO_PROJECT_SELECTED} as the {@code containerProject}.
	 * </p>
	 * 
	 * @param from
	 *            The branch to check out (for example "master" or "refs/heads/master", both syntaxes are
	 *            accepted)
	 * @param to
	 *            The branch to compare with (for example "mybranch" or "refs/heads/mybranch", both syntaxes
	 *            are accepted)
	 * @param fileName
	 *            The file to compare (the relative path to the file from the project)
	 * @param containerProject
	 *            The project containing the file to compare. If
	 *            {@link GitTestSupport#COMPARE_NO_PROJECT_SELECTED} is used, the first correct file will be
	 *            used (use this when their is only one project)
	 * @return the comparison
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws CoreException
	 *             Thrown if the checkout operation went wrong
	 */
	public Comparison compare(String from, String to, String fileName, String containerProject)
			throws IOException, CoreException {
		return compare(from, to, fileName, containerProject, true);
	}

	/**
	 * <p>
	 * Compare the file with the given path between the two given branches. This method is intended to be used
	 * when there are multiple projects in the repository.
	 * </p>
	 * <p>
	 * If there is only one project, then use {@link #compare(String, String, String, boolean)} instead or
	 * pass {@link GitTestSupport#COMPARE_NO_PROJECT_SELECTED} as the {@code containerProject}.
	 * </p>
	 * 
	 * @param from
	 *            The branch to check out (for example "master" or "refs/heads/master", both syntaxes are
	 *            accepted)
	 * @param to
	 *            The branch to compare with (for example "mybranch" or "refs/heads/mybranch", both syntaxes
	 *            are accepted)
	 * @param fileName
	 *            The file to compare (the relative path to the file from the project)
	 * @param containerProject
	 *            The project containing the file to compare. If
	 *            {@link GitTestSupport#COMPARE_NO_PROJECT_SELECTED} is used, the first correct file will be
	 *            used (use this when their is only one project)
	 * @param threeWay
	 *            {@code true} to perform a three-way comparison; {@code false} for a two-way comparison
	 * @return the comparison
	 * @throws IOException
	 *             Thrown if the checkout operation went wrong
	 * @throws CoreException
	 *             Thrown if the checkout operation went wrong
	 */
	public Comparison compare(String from, String to, String fileName, String containerProject,
			boolean threeWay) throws IOException, CoreException {

		String normalizedFrom = normalizeBranch(from);
		String normalizedTo = normalizeBranch(to);
		IFile file = null;
		for (IProject project : projects) {
			if (!containerProject.equals(COMPARE_NO_PROJECT_SELECTED)) {
				if (project.getName().equals(containerProject)) {
					file = project.getFile(fileName);
					break;
				}
			} else {
				file = project.getFile(fileName);
				break;
			}
		}

		if (file == null || !file.exists()) {
			throw new IllegalArgumentException("Could not find file " + fileName + ": wrong test set-up?"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		checkoutBranch(normalizedFrom);
		final String fullPath = file.getFullPath().toString();
		final GitSynchronizeData data = new GitSynchronizeData(getRepository(), normalizedFrom, normalizedTo,
				true);
		final GitSynchronizeDataSet gsds = new GitSynchronizeDataSet(data);
		final GitResourceVariantTreeSubscriber subscriber = new GitResourceVariantTreeSubscriber(gsds);
		subscriber.init(new NullProgressMonitor());
		final GitSubscriberResourceMappingContext context = new GitSubscriberResourceMappingContext(
				subscriber, gsds);

		final Set<IResource> includedResources = new HashSet<IResource>(Arrays.asList(file));
		final Set<ResourceMapping> allMappings = new HashSet<ResourceMapping>();

		Set<IResource> newResources = new HashSet<IResource>(includedResources);
		do {
			final Set<IResource> copy = newResources;
			newResources = new HashSet<IResource>();
			for (IResource resource : copy) {
				ResourceMapping[] mappings = ResourceUtil.getResourceMappings(resource, context);
				allMappings.addAll(Arrays.asList(mappings));
				newResources.addAll(collectResources(mappings, context));
			}
		} while (includedResources.addAll(newResources));

		// Launch the comparison now that the logical model is computed
		// and can be provided to a new GitSynchronizeData object
		final ResourceMapping[] mappings = allMappings.toArray(new ResourceMapping[allMappings.size()]);
		final GitSynchronizeData gsdThatCoverLogicalModel = new GitSynchronizeData(repository, normalizedFrom,
				normalizedTo, true, includedResources);
		final GitSynchronizeDataSet gsds2 = new GitSynchronizeDataSet(gsdThatCoverLogicalModel);
		final GitResourceVariantTreeSubscriber subscriber2 = new GitResourceVariantTreeSubscriber(gsds2);
		RemoteResourceMappingContext remoteContext = new GitSubscriberResourceMappingContext(subscriber2,
				gsds2);
		final SubscriberScopeManager subscriberScopeManager = new SubscriberScopeManager(
				subscriber2.getName(), mappings, subscriber2, remoteContext, true);
		subscriber2.init(new NullProgressMonitor());
		disposers.add(new Runnable() {
			public void run() {
				subscriber.dispose();
				gsds.dispose();
				subscriber2.dispose();
				gsds2.dispose();
				subscriberScopeManager.dispose();
			}
		});

		final IStorageProviderAccessor accessor = new SubscriberStorageAccessor(subscriber2);
		final IStorageProvider remoteProvider = accessor.getStorageProvider(file,
				IStorageProviderAccessor.DiffSide.REMOTE);
		final IStorageProvider ancestorProvider = accessor.getStorageProvider(file,
				IStorageProviderAccessor.DiffSide.ORIGIN);
		assertNotNull(remoteProvider);
		assertNotNull(ancestorProvider);

		final IProgressMonitor monitor = new NullProgressMonitor();
		final ITypedElement left = new StorageTypedElement(file, fullPath);
		final ITypedElement right = new StorageTypedElement(remoteProvider.getStorage(monitor), fullPath);
		final ITypedElement origin = new StorageTypedElement(ancestorProvider.getStorage(monitor), fullPath);

		EMFCompareIDEUIPlugin p = EMFCompareIDEUIPlugin.getDefault();
		IModelResolver resolver = p.getModelResolverRegistry().getBestResolverFor(file);

		final ComparisonScopeBuilder scopeBuilder = new ComparisonScopeBuilder(resolver,
				EMFCompareIDEUIPlugin.getDefault().getModelMinimizerRegistry().getCompoundMinimizer(),
				accessor);
		final IComparisonScope scope;
		if (threeWay) {
			scope = scopeBuilder.build(left, right, origin, monitor);
		} else {
			scope = scopeBuilder.build(left, right, monitor);
		}

		final ResourceSet leftResourceSet = (ResourceSet)scope.getLeft();
		final ResourceSet rightResourceSet = (ResourceSet)scope.getRight();
		final ResourceSet originResourceSet = (ResourceSet)scope.getOrigin();

		// It would be very odd, indeed, for the three-way comparison not to find pseudoconflicts
		// at least, which means that the comparison will have resources to compare. But we use
		// two-way comparisons in the tests to verify that a merge and some expected result are
		// not different which, in the best base, actually resources in no resources being
		// compared because they are indistinguishable
		assertFalse(threeWay && leftResourceSet.getResources().isEmpty());
		assertFalse(threeWay && rightResourceSet.getResources().isEmpty());
		assertFalse(threeWay && originResourceSet.getResources().isEmpty());

		final Builder comparisonBuilder = EMFCompare.builder();
		EMFCompareBuilderConfigurator.createDefault().configure(comparisonBuilder);
		return comparisonBuilder.build().compare(scope, new BasicMonitor());
	}

	/**
	 * Get the resources associated with the given mappings.
	 * 
	 * @param mappings
	 *            An array of resourceMappings
	 * @param context
	 *            The context of resourceMappings
	 * @return a list of resources
	 */
	private Set<IResource> collectResources(ResourceMapping[] mappings, ResourceMappingContext context) {
		final Set<IResource> resources = new HashSet<IResource>();
		for (ResourceMapping mapping : mappings) {
			try {
				ResourceTraversal[] traversals = mapping.getTraversals(context, new NullProgressMonitor());
				for (ResourceTraversal traversal : traversals) {
					resources.addAll(Arrays.asList(traversal.getResources()));
				}
			} catch (CoreException e) {
				Activator.logError(e.getMessage(), e);
			}
		}
		return resources;
	}

	/**
	 * Checkout repository to the given branch.
	 * 
	 * @param refName
	 *            The branch to checkout (for example "master" or "refs/heads/master", both syntaxes are
	 *            accepted)
	 * @throws CoreException
	 *             Thrown if the merge operation or the refresh of projects went wrong
	 * @throws IOException
	 *             Thrown if the cannot retrieve the current branch
	 */
	public void checkoutBranch(String refName) throws CoreException, IOException {
		ResetOperation reset = new ResetOperation(repository, repository.getBranch(), ResetType.HARD);
		reset.execute(null);
		BranchOperation op = new BranchOperation(getRepository(), normalizeBranch(refName));
		op.execute(null);
		CheckoutResult result = op.getResult();
		if (result.getStatus() != CheckoutResult.Status.OK) {
			throw new IllegalStateException("Unable to checkout branch " + refName + " result:" + result); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Specifies whether the repository is in conflict state or not.
	 * <p>
	 * This is a convenience method for <code>getStatus().getConflicting().isEmpty()</code>.
	 * </p>
	 * 
	 * @return <code>true</code> if the repository is in a conflict state, <code>false</code> otherwise.
	 * @throws Exception
	 *             If the status of the repository could not be created queried.
	 */
	public boolean noConflict() throws Exception {
		return getStatus().getConflicting().isEmpty();
	}

	/**
	 * Specifies whether the file given in <code>path</code> currently exists in the working tree of the
	 * repository.
	 * 
	 * @param path
	 *            The path to the file in question, such as <code>dir1/dir2/file1.txt</code>.
	 * @return <code>true</code> if the file in the given <code>path</code> exists, <code>false</code>
	 *         otherwise.
	 */
	public boolean fileExists(String path) {
		return new File(repository.getWorkTree(), path).exists();
	}
}
