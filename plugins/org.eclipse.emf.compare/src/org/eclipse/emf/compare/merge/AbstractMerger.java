/*******************************************************************************
 * Copyright (c) 2012, 2017 Obeo, Christian W. Damus, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Stefan Dirix - bugs 441172, 452147 and 454579
 *     Alexandra Buzila - Fixes for Bug 446252
 *     Martin Fleck - bug 507177
 *     Christian W. Damus - integration of façade providers
 *******************************************************************************/
package org.eclipse.emf.compare.merge;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static org.eclipse.emf.compare.ConflictKind.PSEUDO;
import static org.eclipse.emf.compare.DifferenceSource.RIGHT;
import static org.eclipse.emf.compare.DifferenceState.DISCARDED;
import static org.eclipse.emf.compare.DifferenceState.MERGED;
import static org.eclipse.emf.compare.DifferenceState.MERGING;
import static org.eclipse.emf.compare.DifferenceState.UNRESOLVED;
import static org.eclipse.emf.compare.merge.IMergeCriterion.NONE;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasConflict;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasSameReferenceAs;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.isDiffOnEOppositeOf;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.sameSideAs;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.ConflictKind;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.FeatureMapChange;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.internal.utils.ComparisonUtil;
import org.eclipse.emf.compare.utils.EMFCompareCopier;
import org.eclipse.emf.compare.utils.EMFComparePredicates;
import org.eclipse.emf.compare.utils.ReferenceUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * Abstract implementation of an {@link IMerger}. This can be used as a base implementation to avoid
 * re-implementing the whole contract.
 * 
 * @author <a href="mailto:laurent.goubet@obeo.fr">Laurent Goubet</a>
 * @since 3.0
 */
public abstract class AbstractMerger implements IMerger2, IMergeOptionAware, IMergeCriterionAware {

	/** The key of the merge option that allows to the mergers to consider sub-diffs of a diff as a whole. */
	public static final String SUB_DIFF_AWARE_OPTION = "subDiffAwareOption"; //$NON-NLS-1$

	/** The logger. */
	private static final Logger LOGGER = Logger.getLogger(AbstractMerger.class);

	/**
	 * The map of all merge options that this merger should be aware of.
	 * 
	 * @since 3.4
	 */
	protected Map<Object, Object> mergeOptions;

	/** Ranking of this merger. */
	private int ranking;

	/** Registry from which this merger has been created. */
	private Registry2 registry;

	/**
	 * Default constructor.
	 */
	public AbstractMerger() {
		this.mergeOptions = Maps.newHashMap();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMerger#getRanking()
	 */
	public int getRanking() {
		return ranking;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMerger#setRanking(int)
	 */
	public void setRanking(int r) {
		ranking = r;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMerger#getRegistry()
	 */
	public Registry getRegistry() {
		return registry;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMerger#setRegistry(org.eclipse.emf.compare.merge.IMerger.Registry)
	 */
	public void setRegistry(Registry registry) {
		if (this.registry != null && registry != null) {
			throw new IllegalStateException("The registry has to be set only once."); //$NON-NLS-1$
		}
		if (!(registry instanceof Registry2)) {
			throw new IllegalArgumentException("The registry must implement Registry2"); //$NON-NLS-1$
		}
		this.registry = (Registry2)registry;
	}

	/**
	 * Default implementation of apply for mergers that extends this class. Will accept <code>null</code> or
	 * AdditiveMergeCriterion.INSTANCE.
	 * 
	 * @param criterion
	 *            The merge criterion
	 * @return <code>true</code> if the given criterion is null or is AdditiveMergeCriterion.INSTANCE.
	 * @since 3.4
	 */
	public boolean apply(IMergeCriterion criterion) {
		return criterion == null || criterion == NONE || criterion == AdditiveMergeCriterion.INSTANCE;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see org.eclipse.emf.compare.merge.IMergeOptionAware#getMergeOptions()
	 */
	public Map<Object, Object> getMergeOptions() {
		return this.mergeOptions;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMergeOptionAware#setMergeOptions(java.util.Map)
	 * @since 3.3
	 */
	public void setMergeOptions(Map<Object, Object> options) {
		this.mergeOptions = options;
	}

	/**
	 * Check the SUB_DIFF_AWARE_OPTION state.
	 * 
	 * @return true if the SUB_DIFF_AWARE_OPTION of the merge options is set to true, false otherwise.
	 */
	private boolean isHandleSubDiffs() {
		if (this.mergeOptions != null) {
			Object subDiffs = this.mergeOptions.get(SUB_DIFF_AWARE_OPTION);
			return subDiffs == Boolean.TRUE;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.2
	 */
	public Set<Diff> getDirectMergeDependencies(Diff diff, boolean mergeRightToLeft) {
		long start = System.currentTimeMillis();

		final Set<Diff> dependencies = new LinkedHashSet<Diff>();
		if (isAccepting(diff, mergeRightToLeft)) {
			dependencies.addAll(diff.getRequires());
			dependencies.addAll(diff.getImpliedBy());
		} else {
			dependencies.addAll(diff.getImplies());
			dependencies.addAll(diff.getRequiredBy());
		}
		dependencies.addAll(diff.getRefinedBy());
		if (diff.getEquivalence() != null) {
			final Diff masterEquivalence = findMasterEquivalence(diff, mergeRightToLeft);
			if (masterEquivalence != null && masterEquivalence != diff) {
				dependencies.add(masterEquivalence);
			}
		}

		if (LOGGER.isDebugEnabled()) {
			Long duration = new Long(System.currentTimeMillis() - start);
			String log = String.format(
					"getDirectMergeDependencies(Diff, boolean) - %d dependencies found in %d ms for diff %d", //$NON-NLS-1$
					new Integer(dependencies.size()), duration, new Integer(diff.hashCode()));
			LOGGER.debug(log);
		}

		return dependencies;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.2
	 */
	public Set<Diff> getDirectResultingMerges(Diff target, boolean mergeRightToLeft) {
		long start = System.currentTimeMillis();

		final Set<Diff> resulting = new LinkedHashSet<Diff>();
		resulting.addAll(getImpliedMerges(target, mergeRightToLeft));
		resulting.addAll(getLogicallyResultingMerges(target, mergeRightToLeft));

		if (LOGGER.isDebugEnabled()) {
			Long duration = new Long(System.currentTimeMillis() - start);
			String log = String.format(
					"getDirectResultingMerges(Diff, boolean) - %d resulting merges found in %d ms for diff %d", //$NON-NLS-1$
					new Integer(resulting.size()), duration, new Integer(target.hashCode()));
			LOGGER.debug(log);
		}

		return resulting;

	}

	/**
	 * Interlocked differences only occur in special cases: When both ends of a one-to-one feature have the
	 * same type and are actually set to the container object in an instance model.
	 * <p>
	 * For each end of the feature usually two differences are determined: Setting the feature in object A and
	 * in object B. Each pair of differences is equivalent. But when the value of the feature is set to its
	 * containing object, those differences may ALL act as equivalent depending on the merge direction.
	 * <p>
	 * These interlocked differences are therefore indirectly equivalent and need special treatment to avoid
	 * merging the same effects twice. These differences are determined by this method.
	 * 
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=452147">Bugzilla #452147</a> for more
	 *      information.
	 * @param referenceChange
	 *            The diff for which interlocked differences are determined.
	 * @param mergeRightToLeft
	 *            The direction in which we're considering a merge.
	 * @return All interlocked differences in regards to the given {@code referenceChange} and
	 *         {@code mergeDirection}.
	 */
	private Collection<? extends Diff> findInterlockedOneToOneDiffs(ReferenceChange referenceChange,
			boolean mergeRightToLeft) {
		final boolean sanityChecks = referenceChange.getKind() != DifferenceKind.CHANGE
				|| referenceChange.getReference().isMany()
				|| referenceChange.getReference().getEOpposite().isMany();

		// check if value to be set is the container itself
		final EObject sourceContainer = ComparisonUtil.getExpectedSide(referenceChange.getMatch(),
				referenceChange.getSource(), mergeRightToLeft);

		if (!sanityChecks && sourceContainer != null) {
			final Object sourceValue = ReferenceUtil.safeEGet(sourceContainer,
					referenceChange.getReference());

			if (sourceValue == sourceContainer) {
				// collect all diffs which might be "equal"
				final Match match = referenceChange.getMatch();
				final Set<Diff> candidates = new LinkedHashSet<Diff>();
				for (Diff diff : match.getDifferences()) {
					candidates.add(diff);
					if (diff.getEquivalence() != null) {
						candidates.addAll(diff.getEquivalence().getDifferences());
					}
				}

				// special case - check for interlocked diffs and return them as result
				return filterInterlockedOneToOneDiffs(candidates, referenceChange, mergeRightToLeft);
			}
		}

		return Collections.emptyList();
	}

	/**
	 * Checks for interlocked differences from a list of candidates. See
	 * {@link #findInterlockedOneToOneDiffs(ReferenceChange, boolean)} for more information.
	 *
	 * @param diffsToCheck
	 *            The differences to be checked for indirect equivalence.
	 * @param referenceChange
	 *            The diff to which the determined differences are indirectly equivalent.
	 * @param mergeRightToLeft
	 *            The direction in which we're considering a merge.
	 * @return All differences (and their equivalents) from {@code diffsToCheck} which are indirectly
	 *         equivalent to {@code referenceChange}. Does not modify the given collection.
	 */
	private Collection<? extends Diff> filterInterlockedOneToOneDiffs(Collection<? extends Diff> diffsToCheck,
			ReferenceChange referenceChange, boolean mergeRightToLeft) {

		final Object sourceContainer = ComparisonUtil.getExpectedSide(referenceChange.getMatch(),
				referenceChange.getSource(), mergeRightToLeft);
		final EReference sourceReference = referenceChange.getReference();

		final Set<Diff> result = new LinkedHashSet<Diff>();

		for (Diff candidate : diffsToCheck) {
			if (candidate instanceof ReferenceChange) {
				// check if container & reference(-opposite) are the same as from the given referenceChange
				final Object candidateContainer = ComparisonUtil.getExpectedSide(candidate.getMatch(),
						candidate.getSource(), mergeRightToLeft);
				final EReference candidateReference = ((ReferenceChange)candidate).getReference();

				if (sourceContainer == candidateContainer
						&& sourceReference.getEOpposite() == candidateReference) {
					result.add(candidate);
					if (candidate.getEquivalence() != null) {
						result.addAll(candidate.getEquivalence().getDifferences());
					}
				}
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 3.2
	 */
	public Set<Diff> getDirectResultingRejections(Diff target, boolean rightToLeft) {
		long start = System.currentTimeMillis();

		final Set<Diff> directlyImpliedRejections = new LinkedHashSet<Diff>();
		final Conflict conflict = target.getConflict();
		if (conflict != null && conflict.getKind() == ConflictKind.REAL) {
			if (isAccepting(target, rightToLeft)) {
				Iterables.addAll(directlyImpliedRejections,
						Iterables.filter(conflict.getDifferences(), not(sameSideAs(target))));
			}
		}

		if (LOGGER.isDebugEnabled()) {
			Long duration = new Long(System.currentTimeMillis() - start);
			String log = String.format(
					"getDirectResultingMerges(Diff, boolean) - %d implied rejections found in %d ms for diff %d", //$NON-NLS-1$
					new Integer(directlyImpliedRejections.size()), duration, new Integer(target.hashCode()));
			LOGGER.debug(log);
		}

		return directlyImpliedRejections;
	}

	/**
	 * Even within 'equivalent' differences, there might be one that we need to consider as the "master", one
	 * part of the equivalence that should take precedence over the others when merging.
	 * <p>
	 * There are four main cases in which this happens :
	 * <ol>
	 * <li>Equivalent differences regarding two "eOpposite" sides, with one side being a single-valued
	 * reference while the other side is a multi-valued reference (one-to-many). In such a case, we need the
	 * 'many' side of that equivalence to be merged over the 'single' side, so as to avoid potential ordering
	 * issues. Additionally, to avoid losing information, equivalent differences with
	 * {@link DifferenceKind.ADD} instead of {@link DifferenceKind.REMOVE} must be merged first.</li>
	 * <li>Equivalent differences regarding two "eOpposite" sides, with both sides being a single-valued
	 * reference (one-to-one). In such a case, we need to merge the difference that results in setting a
	 * feature value over the difference unsetting a feature. This is needed to prevent information loss.</li>
	 * <li>Equivalent differences with conflicts: basically, if one of the diffs of an equivalence relation is
	 * in conflict while the others are not, then none of the equivalent differences can be automatically
	 * merged. We need to consider the conflict to be taking precedence over the others to make sure that the
	 * conflict is resolved before even trying to merge anything.</li>
	 * <li>Equivalent {@link ReferenceChange} and {@link FeatureMapChange} differences: in this case the
	 * {@link FeatureMapChange} difference will take precedence over the {@link ReferenceChange} when the the
	 * resulting operation actively modifies a FeatureMap. The {@link ReferenceChange} will take precedence
	 * when a FeatureMap is only modified implicitly. This happens in order to prevent special cases in which
	 * the {@link ReferenceChangeMerger} cannot ensure the correct order of the feature map attribute.</li>
	 * </ol>
	 * </p>
	 * 
	 * @param diff
	 *            The diff we need to check the equivalence for a 'master' difference.
	 * @param mergeRightToLeft
	 *            Direction of the merge operation.
	 * @return The master difference of this equivalence relation. May be <code>null</code> if there are none.
	 */
	private Diff findMasterEquivalence(Diff diff, boolean mergeRightToLeft) {
		final List<Diff> equivalentDiffs = diff.getEquivalence().getDifferences();
		final Optional<Diff> firstConflicting = Iterables.tryFind(equivalentDiffs,
				hasConflict(ConflictKind.REAL));

		final Diff idealMasterDiff;

		if (diff instanceof ReferenceChange) {
			final ReferenceChange referenceChange = (ReferenceChange)diff;
			idealMasterDiff = getMasterEquivalenceForReferenceChange(referenceChange, mergeRightToLeft);
		} else if (diff instanceof FeatureMapChange) {
			final FeatureMapChange featureMapChange = (FeatureMapChange)diff;
			idealMasterDiff = getMasterEquivalenceForFeatureMapChange(featureMapChange, mergeRightToLeft);
		} else {
			idealMasterDiff = null;
		}

		final Diff masterDiff;
		// conflicting equivalents take precedence over the ideal master equivalence
		if (firstConflicting.isPresent() && !hasRealConflict(idealMasterDiff)) {
			if (hasRealConflict(diff)) {
				masterDiff = null;
			} else {
				masterDiff = firstConflicting.get();
			}
		} else {
			masterDiff = idealMasterDiff;
		}

		return masterDiff;
	}

	/**
	 * Determines if the given {@link Diff} has a conflict of kind {@link ConflictKind#REAL}.
	 *
	 * @param diff
	 *            The {@link Diff} to check.
	 * @return {@code true} if the diff exists and has a conflict of kind {@link ConflictKind#REAL},
	 *         {@code false} otherwise.
	 * @since 3.4
	 */
	private boolean hasRealConflict(Diff diff) {
		return diff != null && diff.getConflict() != null
				&& diff.getConflict().getKind() == ConflictKind.REAL;
	}

	/**
	 * Returns the master equivalence for a {@link FeatureMapChange}.
	 * 
	 * @see AbstractMerger#findMasterEquivalence(Diff, boolean)
	 * @param diff
	 *            The {@link Diff} we need to check the equivalence for a 'master' difference.
	 * @param mergeRightToLeft
	 *            Direction of the current merging.
	 * @return The master difference of {@code diff} and its equivalent diffs. This method may return
	 *         <code>null</code> if there is no master diff.
	 */
	private Diff getMasterEquivalenceForFeatureMapChange(FeatureMapChange diff, boolean mergeRightToLeft) {
		if (diff.getKind() == DifferenceKind.MOVE) {
			final Comparison comparison = diff.getMatch().getComparison();
			final FeatureMap.Entry entry = (FeatureMap.Entry)diff.getValue();

			if (entry.getValue() instanceof EObject) {
				final Match valueMatch = comparison.getMatch((EObject)entry.getValue());

				final EObject expectedValue = ComparisonUtil.getExpectedSide(valueMatch, diff.getSource(),
						mergeRightToLeft);

				// Try to find the ReferenceChange-MasterEquivalence when the expected value will not be
				// contained in a FeatureMap
				if (!ComparisonUtil.isContainedInFeatureMap(expectedValue)) {
					return Iterators.tryFind(diff.getEquivalence().getDifferences().iterator(),
							Predicates.instanceOf(ReferenceChange.class)).orNull();
				}
			}

		}
		return null;
	}

	/**
	 * Returns the master equivalence for a {@link ReferenceChange}.
	 * 
	 * @see AbstractMerger#findMasterEquivalence(Diff, boolean)
	 * @param diff
	 *            The {@link Diff} we need to check the equivalence for a 'master' difference.
	 * @param mergeRightToLeft
	 *            Direction of the current merging.
	 * @return The master difference of {@code diff} and its equivalent diffs. This method may return
	 *         <code>null</code> if there is no master diff.
	 */
	private Diff getMasterEquivalenceForReferenceChange(ReferenceChange diff, boolean mergeRightToLeft) {
		Diff masterDiff = getMasterEquivalenceOnReference(diff, mergeRightToLeft);
		if (masterDiff == null) {
			masterDiff = getMasterEquivalenceOnFeatureMap(diff, mergeRightToLeft);
		}
		return masterDiff;
	}

	/**
	 * Returns the master equivalence for a {@link ReferenceChange} from among its equivalents with the same
	 * or {@code eOpposite} reference.
	 * 
	 * @see AbstractMerger#findMasterEquivalence(Diff, boolean)
	 * @param diff
	 *            The {@link Diff} we need to check the equivalence for a 'master' difference.
	 * @param mergeRightToLeft
	 *            Direction of the current merging.
	 * @return The master difference of {@code diff} and its equivalent diffs. This method may return
	 *         <code>null</code> if there is no master diff.
	 */
	private Diff getMasterEquivalenceOnReference(ReferenceChange diff, final boolean mergeRightToLeft) {
		Diff masterDiff = null;
		/*
		 * For the following, we'll only consider diffs that are either on the same reference as "diff", or on
		 * its eopposite.
		 */
		final Predicate<Diff> candidateFilter = or(isDiffOnEOppositeOf(diff), hasSameReferenceAs(diff));
		final List<Diff> equivalentDiffs = diff.getEquivalence().getDifferences();

		// We need to lookup the first multi-valued addition
		final Optional<Diff> multiValuedAddition = Iterators.tryFind(
				Iterators.filter(equivalentDiffs.iterator(), candidateFilter), new Predicate<Diff>() {
					public boolean apply(Diff input) {
						return input instanceof ReferenceChange
								&& ((ReferenceChange)input).getReference().isMany()
								&& isAdd((ReferenceChange)input, mergeRightToLeft);
					}
				});

		final Iterator<Diff> candidateDiffs = Iterators.filter(equivalentDiffs.iterator(), candidateFilter);
		if (multiValuedAddition.isPresent()) {
			// We have at least one multi-valued addition. It will take precedence if there is any
			// single-valued reference change or multi-valued deletion
			while (masterDiff == null && candidateDiffs.hasNext()) {
				final ReferenceChange next = (ReferenceChange)candidateDiffs.next();
				if (!next.getReference().isMany() || !isAdd(next, mergeRightToLeft)) {
					masterDiff = multiValuedAddition.get();
				}
			}
		} else {
			// The only diff that could take precedence is a single-valued set, _if_ there is any multi-valued
			// deletion or single-valued unset in the list.
			ReferenceChange candidate = null;
			if (candidateDiffs.hasNext()) {
				candidate = (ReferenceChange)candidateDiffs.next();
			}
			while (masterDiff == null && candidateDiffs.hasNext()) {
				assert candidate != null;
				final ReferenceChange next = (ReferenceChange)candidateDiffs.next();
				if (candidate.getReference().isMany() || isUnset(candidate, mergeRightToLeft)) {
					// candidate is a multi-valued deletion or an unset. Do we have a single-valued set in the
					// list?
					if (!next.getReference().isMany() && isSet(next, mergeRightToLeft)) {
						masterDiff = next;
					}
				} else if (isSet(candidate, mergeRightToLeft)) {
					// candidate is a set. Is it our master diff?
					if (next.getReference().isMany() || isUnset(next, mergeRightToLeft)) {
						masterDiff = candidate;
					}
				} else {
					// candidate is a change on a single-valued reference. This has no influence over the
					// 'master' lookup. Go on to the next.
					candidate = next;
				}
			}
		}

		return masterDiff;
	}

	/**
	 * Returns the master equivalence of type {@link FeatureMapChange}, for a {@link ReferenceChange}.
	 * 
	 * @see AbstractMerger#findMasterEquivalence(Diff, boolean)
	 * @param diff
	 *            The {@link Diff} we need to check the equivalence for a 'master' difference.
	 * @param mergeRightToLeft
	 *            Direction of the current merging.
	 * @return The master difference of {@code diff} and its equivalent diffs. This method may return
	 *         <code>null</code> if there is no master diff.
	 */
	private Diff getMasterEquivalenceOnFeatureMap(ReferenceChange diff, boolean mergeRightToLeft) {
		if (diff.getKind() == DifferenceKind.MOVE) {

			Comparison comparison = diff.getMatch().getComparison();
			Match valueMatch = comparison.getMatch(diff.getValue());

			EObject sourceValue = ComparisonUtil.getExpectedSide(valueMatch, diff.getSource(),
					mergeRightToLeft);

			// No FeatureMap-MasterEquivalence when the resulting destination is not a FeatureMap
			if (!ComparisonUtil.isContainedInFeatureMap(sourceValue)) {
				return null;
			}
		}

		return Iterators.tryFind(diff.getEquivalence().getDifferences().iterator(),
				Predicates.instanceOf(FeatureMapChange.class)).orNull();
	}

	/**
	 * Checks whether the given {@code diff} is of kind {@link DifferenceKind#CHANGE} and its reference is
	 * one-to-one.
	 * 
	 * @param diff
	 *            The ReferenceChange to check.
	 * @return {@code true} if the given {@code diff} is of kind {@link DifferenceKind#CHANGE} and describes a
	 *         one-to-one reference, {@code false} otherwise.
	 */
	private boolean isOneToOneAndChange(ReferenceChange diff) {
		final boolean oppositeReferenceExists = diff.getReference() != null
				&& diff.getReference().getEOpposite() != null;
		return diff.getKind() == DifferenceKind.CHANGE && oppositeReferenceExists
				&& !diff.getReference().isMany() && !diff.getReference().getEOpposite().isMany();
	}

	/**
	 * Returns a set of differences that should be logically merged with the given diff. As opposed to
	 * {@link #getDirectMergeDependencies(Diff, boolean) merge dependencies}, it is not structurally necessary
	 * to merge these diffs before the given diff. This may include the diff's {@link Diff#getImpliedBy()
	 * implications}, the diff's {@link Diff#getRefines() refinement} or any other diff that should be
	 * logically merged with the given diff.
	 * 
	 * @param diff
	 *            The difference we're considering merging.
	 * @param mergeRightToLeft
	 *            The direction in which we're considering a merge.
	 * @return The Set of all differences that will be merged because we've merged <code>diff</code>.
	 * @since 3.5
	 */
	protected Set<Diff> getLogicallyResultingMerges(Diff diff, boolean mergeRightToLeft) {
		long start = System.currentTimeMillis();

		final Set<Diff> resulting = new LinkedHashSet<Diff>();

		// we need to add the implication side that actually needs merging
		if (isAccepting(diff, mergeRightToLeft)) {
			resulting.addAll(diff.getImpliedBy());
		} else {
			resulting.addAll(diff.getImplies());
		}

		// refinements should only be merged together
		resulting.addAll(diff.getRefines());

		// any logically linked sub diffs should be merged as well
		if (isHandleSubDiffs()) {
			Set<Diff> subDiffs = Sets
					.newLinkedHashSet(ComparisonUtil.getDirectSubDiffs(!mergeRightToLeft).apply(diff));
			resulting.addAll(subDiffs);
		}

		if (LOGGER.isDebugEnabled()) {
			Long duration = new Long(System.currentTimeMillis() - start);
			String log = String.format(
					"getLogicallyResultingMerges(Diff, boolean) - %d merges found in %d ms for diff %d", //$NON-NLS-1$
					new Integer(resulting.size()), duration, new Integer(diff.hashCode()));
			LOGGER.debug(log);
		}

		return resulting;
	}

	/**
	 * Returns all differences that are automatically set to the targets {@link Diff#getState() state} if the
	 * given target diff is merged. This may include the diff's {@link Diff#getImplies() implications}, the
	 * diff's {@link Diff#getEquivalence() equivalences} or any other diff that requires no merging by itself.
	 * 
	 * @param target
	 *            The difference we're considering merging.
	 * @param mergeRightToLeft
	 *            The direction in which we're considering a merge.
	 * @return The Set of all differences that will be set to <code>MERGED</code> because we've merged
	 *         <code>target</code>.
	 * @since 3.5
	 */
	protected Set<Diff> getImpliedMerges(Diff target, boolean mergeRightToLeft) {
		long start = System.currentTimeMillis();
		final Set<Diff> impliedMerges = new LinkedHashSet<Diff>();

		if (isAccepting(target, mergeRightToLeft)) {
			impliedMerges.addAll(target.getImplies());
		} else {
			impliedMerges.addAll(target.getImpliedBy());
		}

		if (target.getEquivalence() != null) {
			impliedMerges.addAll(target.getEquivalence().getDifferences());
			impliedMerges.remove(target);
		}

		if (target.getConflict() != null && target.getConflict().getKind() == PSEUDO) {
			impliedMerges.addAll(target.getConflict().getDifferences());
			impliedMerges.remove(target);
		}

		// If a diff refines another, we have to check if the "macro" diff has to be merged with it. It is the
		// case when the unresolved diffs that refine the "macro" diff are all contained by the set
		// (target + resulting) (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=458961)
		for (Diff refine : target.getRefines()) {
			Set<Diff> tmp = Sets.newHashSet(impliedMerges);
			tmp.add(target);
			Collection<Diff> unresolvedRefinedDiffs = Collections2.filter(refine.getRefinedBy(),
					EMFComparePredicates.hasState(UNRESOLVED));
			if (tmp.containsAll(unresolvedRefinedDiffs)) {
				impliedMerges.add(refine);
			}
		}

		// Bug 452147:
		// Add interlocked differences to the resulting merges to avoid merging redundant differences with
		// undefined consequences.
		if (target instanceof ReferenceChange) {
			final ReferenceChange refTarget = (ReferenceChange)target;
			if (isOneToOneAndChange(refTarget)) {
				impliedMerges.addAll(findInterlockedOneToOneDiffs(refTarget, mergeRightToLeft));
			}
		}

		if (LOGGER.isDebugEnabled()) {
			Long duration = new Long(System.currentTimeMillis() - start);
			String log = String.format(
					"getImpliedMerges(Diff, boolean) - %d implied merges found in %d ms for diff %d", //$NON-NLS-1$
					new Integer(impliedMerges.size()), duration, new Integer(target.hashCode()));
			LOGGER.debug(log);
		}
		return impliedMerges;
	}

	/**
	 * Executes a copy in the given merge direction. This method is a generalization of
	 * {@link #copyLeftToRight(Diff, Monitor)} and {@link #copyRightToLeft(Diff, Monitor)}.
	 * 
	 * @param target
	 *            The difference to handle.
	 * @param monitor
	 *            Monitor.
	 * @param rightToLeft
	 *            Merge direction.
	 * @since 3.5
	 */
	protected void copyDiff(Diff target, Monitor monitor, boolean rightToLeft) {
		if (isInTerminalState(target)) {
			return;
		}

		long start = System.currentTimeMillis();

		// Change the diff's state before we actually merge it : this allows us to avoid requirement cycles.
		target.setState(MERGING);

		// Mark all implied diffs as merged before actually merging this one
		Set<Diff> impliedMerges = getImpliedMerges(target, rightToLeft);
		while (!impliedMerges.isEmpty()) {
			Diff impliedMerge = impliedMerges.iterator().next();
			// avoid implication circles
			if (impliedMerge != target && !isInTerminalState(impliedMerge)) {
				if (isAccepting(impliedMerge, rightToLeft)) {
					impliedMerge.setState(MERGED);
				} else {
					impliedMerge.setState(DISCARDED);
				}
				impliedMerges.addAll(getImpliedMerges(impliedMerge, rightToLeft));
			}
			impliedMerges.remove(impliedMerge);
			impliedMerges.remove(target);
		}
		if (isAccepting(target, rightToLeft)) {
			accept(target, rightToLeft);
			target.setState(MERGED);
		} else {
			reject(target, rightToLeft);
			target.setState(DISCARDED);
		}

		if (LOGGER.isDebugEnabled()) {
			long duration = System.currentTimeMillis() - start;
			LOGGER.debug("copyDiff(Diff, Monitor, Boolean) - diff " + target.hashCode() //$NON-NLS-1$
					+ " merged (rightToLeft: " + rightToLeft + ") in " //$NON-NLS-1$ //$NON-NLS-2$
					+ duration + "ms"); //$NON-NLS-1$
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMerger#copyLeftToRight(org.eclipse.emf.compare.Diff,
	 *      org.eclipse.emf.common.util.Monitor)
	 * @since 3.1
	 */
	public void copyLeftToRight(Diff target, Monitor monitor) {
		copyDiff(target, monitor, false);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.merge.IMerger#copyRightToLeft(org.eclipse.emf.compare.Diff,
	 *      org.eclipse.emf.common.util.Monitor)
	 * @since 3.1
	 */
	public void copyRightToLeft(Diff target, Monitor monitor) {
		copyDiff(target, monitor, true);
	}

	/**
	 * Accept the given difference. This may be overridden by clients.
	 * 
	 * @param diff
	 *            the difference to merge
	 * @param rightToLeft
	 *            the direction of the merge
	 * @since 3.1
	 */
	protected void accept(final Diff diff, boolean rightToLeft) {
		// Empty default implementation
	}

	/**
	 * Reject the given difference. This may be overridden by clients.
	 * 
	 * @param diff
	 *            the difference to merge
	 * @param rightToLeft
	 *            the direction of the merge
	 * @since 3.1
	 */
	protected void reject(final Diff diff, boolean rightToLeft) {
		// Empty default implementation
	}

	/**
	 * This can be used by mergers to merge another (required, equivalent...) difference using the right
	 * merger for that diff.
	 * 
	 * @param diff
	 *            The diff we need to merge.
	 * @param rightToLeft
	 *            Direction of that merge.
	 * @param monitor
	 *            The monitor we should use to report progress.
	 */
	protected void mergeDiff(Diff diff, boolean rightToLeft, Monitor monitor) {
		final DelegatingMerger delegate = getMergerDelegate(diff);
		if (rightToLeft) {
			delegate.copyRightToLeft(diff, monitor);
		} else {
			delegate.copyLeftToRight(diff, monitor);
		}
	}

	/**
	 * Find the best merger for diff and wrap it in a delegate that will take the current merge criterion into
	 * account. The current merge criterion should be stored in the merger's mergeOptions map using
	 * IMergeCriterion.OPTION_MERGE_CRITERION as a key.
	 * 
	 * @param diff
	 *            The diff
	 * @return the best merger to use for merging the diff
	 * @since 3.4
	 */
	protected DelegatingMerger getMergerDelegate(Diff diff) {
		IMergeCriterion criterion = (IMergeCriterion)getMergeOptions()
				.get(IMergeCriterion.OPTION_MERGE_CRITERION);
		return getMergerDelegate(diff, (Registry2)getRegistry(), criterion);
	}

	/**
	 * Find the best merger for diff and wrap it in a delegate that will take the given merge criterion into
	 * account. This is NOT Thread-safe!
	 * 
	 * @param diff
	 *            The diff
	 * @param registry
	 *            The registry of mergers where to look for mergers
	 * @param criterion
	 *            The merge criterion to use
	 * @return The best merger for diff and criterion, wrapped in a delegate to deal with setting/restoring
	 *         the criterion in the merger used.
	 * @since 3.4
	 */
	public static DelegatingMerger getMergerDelegate(Diff diff, Registry2 registry,
			IMergeCriterion criterion) {
		Iterator<IMerger> it = registry.getMergersByRankDescending(diff, criterion);
		if (!it.hasNext()) {
			throw new IllegalStateException("No merger found for diff " + diff.getClass().getSimpleName()); //$NON-NLS-1$
		}
		IMerger merger = it.next();
		return new DelegatingMerger(merger, criterion);
	}

	/**
	 * Returns whether the given difference is in a terminal state or not. Differences that are in a terminal
	 * state, i.e., either MERGED or DISCARDED, do not need to be handled by the merger.
	 * 
	 * @param target
	 *            difference
	 * @return true if the target should be merged, false otherwise.
	 * @since 3.5
	 */
	public static boolean isInTerminalState(Diff target) {
		switch (target.getState()) {
			case MERGED:
			case DISCARDED:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Specifies whether the given {@code diff} will add a value in the target model for the current merging.
	 * <p>
	 * To check whether the {@code diff} is an addition, we have to check the direction of the merge,
	 * specified in {@code rightToLeft} and the {@link Diff#getSource() source of the diff}. Therefore, this
	 * method delegates to {@link #isLeftAddOrRightDelete(ReferenceChange)} and
	 * {@link #isLeftDeleteOrRightAdd(ReferenceChange)}.
	 * </p>
	 * 
	 * @param diff
	 *            The difference to check.
	 * @param rightToLeft
	 *            Direction of the merge.
	 * @return <code>true</code> if {@code diff} will add a value with this merge, <code>false</code>
	 *         otherwise.
	 * @since 3.2
	 */
	protected boolean isAdd(ReferenceChange diff, boolean rightToLeft) {
		if (rightToLeft) {
			return isLeftDeleteOrRightAdd(diff);
		} else {
			return isLeftAddOrRightDelete(diff);
		}
	}

	/**
	 * Specifies whether the given {@code diff} is either an addition on the left-hand side or a deletion on
	 * the right-hand side.
	 * 
	 * @param diff
	 *            The difference to check.
	 * @return <code>true</code> if it is a left addition or a right deletion.
	 */
	private boolean isLeftAddOrRightDelete(ReferenceChange diff) {
		if (diff.getSource() == DifferenceSource.LEFT) {
			return diff.getKind() == DifferenceKind.ADD;
		} else {
			return diff.getKind() == DifferenceKind.DELETE;
		}
	}

	/**
	 * Specifies whether the given {@code diff} is either a deletion on the left-hand side or an addition on
	 * the right-hand side.
	 * 
	 * @param diff
	 *            The difference to check.
	 * @return <code>true</code> if it is a left deletion or a right addition.
	 */
	private boolean isLeftDeleteOrRightAdd(ReferenceChange diff) {
		if (diff.getSource() == DifferenceSource.LEFT) {
			return diff.getKind() == DifferenceKind.DELETE;
		} else {
			return diff.getKind() == DifferenceKind.ADD;
		}
	}

	/**
	 * Checks whether the given diff will result in the unsetting of a reference in the given merge direction.
	 *
	 * @param diff
	 *            The difference to check.
	 * @param mergeRightToLeft
	 *            Direction of the merge.
	 * @return <code>true</code> if {@code diff} will unset a value with this merge, <code>false</code> if
	 *         this will either "set" or "change" values... or if the given diff is affecting a multi-valued
	 *         reference.
	 */
	protected boolean isUnset(ReferenceChange diff, boolean mergeRightToLeft) {
		if (diff.getKind() != DifferenceKind.CHANGE) {
			return false;
		}

		boolean isUnset = false;
		final Match match = diff.getMatch();
		final EObject container;
		if (diff.getSource() == DifferenceSource.LEFT) {
			container = match.getLeft();
		} else {
			container = match.getRight();
		}

		if (container == null) {
			// This is an unset diff. However, if we're merging towards the source, we're actually "rejecting"
			// the unset, and the merge operation will be a "set"
			isUnset = isAccepting(diff, mergeRightToLeft);
		} else {
			if (!ReferenceUtil.safeEIsSet(container, diff.getReference())) {
				// No value on the source side, this is an unset
				// Same case as above, if we are rejecting the diff, it is a "set" operation
				isUnset = isAccepting(diff, mergeRightToLeft);
			} else {
				// The feature is set on the source side. If we're merging towards the other side, this cannot
				// be an unset.
				// Otherwise we're going to reset this reference to its previous value. That will end as an
				// "unset" if the "previous value" is unset itself.
				if (isRejecting(diff, mergeRightToLeft)) {
					final EObject originContainer;
					if (match.getComparison().isThreeWay()) {
						originContainer = match.getOrigin();
					} else if (mergeRightToLeft) {
						originContainer = match.getRight();
					} else {
						originContainer = match.getLeft();
					}

					isUnset = originContainer == null
							|| !ReferenceUtil.safeEIsSet(originContainer, diff.getReference());
				}
			}
		}

		return isUnset;
	}

	/**
	 * Checks whether the given diff will result in the setting of a reference in the given merge direction.
	 *
	 * @param diff
	 *            The difference to check.
	 * @param mergeRightToLeft
	 *            Direction of the merge.
	 * @return <code>true</code> if {@code diff} will set a value with this merge, <code>false</code> if this
	 *         will either "unset" or "change" values... or if the given diff is affecting a multi-valued
	 *         reference.
	 * @since 3.5
	 */
	protected boolean isSet(ReferenceChange diff, boolean mergeRightToLeft) {
		if (diff.getKind() != DifferenceKind.CHANGE) {
			return false;
		}

		boolean isSet = false;
		final Match match = diff.getMatch();
		final EObject container;
		if (diff.getSource() == DifferenceSource.LEFT) {
			container = match.getLeft();
		} else {
			container = match.getRight();
		}

		if (container == null) {
			// This is an unset diff. However, if we're merging towards the source, we're actually "rejecting"
			// the unset, and the merge operation will be a "set"
			isSet = isRejecting(diff, mergeRightToLeft);
		} else {
			if (!ReferenceUtil.safeEIsSet(container, diff.getReference())) {
				// No value on the source side, this is an unset
				// Same case as above, if we are rejecting the diff, it is a "set" operation
				isSet = isRejecting(diff, mergeRightToLeft);
			} else {
				// The feature is set on the source side. If we're merging towards the other side, this is a
				// "set" operation if the feature is not set on the target side.
				// Otherwise we're going to reset this reference to its previous value. That will end as an
				// "unset" if the "previous value" is unset itself.
				if (isRejecting(diff, mergeRightToLeft)) {
					final EObject originContainer;
					if (match.getComparison().isThreeWay()) {
						originContainer = match.getOrigin();
					} else if (mergeRightToLeft) {
						originContainer = match.getRight();
					} else {
						originContainer = match.getLeft();
					}

					isSet = originContainer != null
							&& ReferenceUtil.safeEIsSet(originContainer, diff.getReference());
				} else {
					final EObject targetContainer;
					if (mergeRightToLeft) {
						targetContainer = match.getLeft();
					} else {
						targetContainer = match.getRight();
					}

					isSet = targetContainer == null
							|| !ReferenceUtil.safeEIsSet(targetContainer, diff.getReference());
				}
			}
		}

		return isSet;
	}

	/**
	 * Checks whether the given merge direction will result in accepting this difference based on the
	 * difference's {@link Diff#getSource() source}.
	 * 
	 * <pre>
	 *                     | LEFT  | RIGHT
	 * --------------------+-------+-------
	 * Merge Left to Right | true  | false
	 * Merge Right to Left | false | true
	 * </pre>
	 * 
	 * @param diff
	 *            difference
	 * @param mergeRightToLeft
	 *            merge direction
	 * @return true if the merge source direction matches the difference source, false otherwise.
	 * @see #isRejecting(Diff, boolean)
	 * @since 3.5
	 */
	public static boolean isAccepting(Diff diff, boolean mergeRightToLeft) {
		if (diff.getSource() == RIGHT) {
			return mergeRightToLeft;
		} else {
			return !mergeRightToLeft;
		}
	}

	/**
	 * Checks whether the given merge direction will result in rejecting this difference.
	 *
	 * @param diff
	 *            The difference we're merging.
	 * @param mergeRightToLeft
	 *            Direction of the merge operation.
	 * @return <code>true</code> if we're rejecting this diff.
	 * @see #isAccepting(Diff, boolean)
	 */
	private boolean isRejecting(Diff diff, boolean mergeRightToLeft) {
		return !isAccepting(diff, mergeRightToLeft);
	}

	/**
	 * This will create a copy of the given EObject that can be used as the target of an addition (or the
	 * reverting of a deletion).
	 * <p>
	 * The target will be self-contained and will have no reference towards any other EObject set (neither
	 * containment nor "classic" references). All of its attributes' values will match the given
	 * {@code referenceObject}'s.
	 * </p>
	 * 
	 * @param referenceObject
	 *            The EObject for which we'll create a copy.
	 * @return A self-contained copy of {@code referenceObject}.
	 * @see EMFCompareCopier#copy(EObject)
	 */
	protected EObject createCopy(EObject referenceObject) {
		/*
		 * We can't simply use EcoreUtil.copy. References will have their own diffs and will thus be merged
		 * later on.
		 */
		return getCopier(referenceObject).copy(referenceObject);
	}

	/**
	 * Gets the most appropriate copier available for an object that was merged, using a
	 * {@linkplain ICopier.Registry registry} provided in the {@linkplain #getMergeOptions() merge options},
	 * if any.
	 * 
	 * @param originalObject
	 *            a merged object
	 * @return the most appropriate copier for it (never {@code null})
	 * @since 3.6
	 */
	protected ICopier getCopier(EObject originalObject) {
		ICopier.Registry copierRegistry = (ICopier.Registry)getMergeOptions()
				.get(ICopier.OPTION_COPIER_REGISTRY);
		if (copierRegistry == null) {
			copierRegistry = ICopier.Registry.INSTANCE;
		}

		return copierRegistry.getCopier(originalObject);
	}

	/**
	 * Adds the given {@code value} into the given {@code list} at the given {@code index}. An {@code index}
	 * under than zero or above the list's size will mean that the value should be appended at the end of the
	 * list.
	 * 
	 * @param list
	 *            The list into which {@code value} should be added.
	 * @param value
	 *            The value we need to add to {@code list}.
	 * @param <E>
	 *            Type of objects contained in the list.
	 * @param insertionIndex
	 *            The index at which {@code value} should be inserted into {@code list}. {@code -1} if it
	 *            should be appended at the end of the list.
	 */
	@SuppressWarnings("unchecked")
	protected <E> void addAt(List<E> list, E value, int insertionIndex) {
		if (list instanceof InternalEList<?>) {
			if (insertionIndex < 0 || insertionIndex > list.size()) {
				((InternalEList<Object>)list).addUnique(value);
			} else {
				((InternalEList<Object>)list).addUnique(insertionIndex, value);
			}
		} else {
			if (insertionIndex < 0 || insertionIndex > list.size()) {
				list.add(value);
			} else {
				list.add(insertionIndex, value);
			}
		}
	}

}
