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
package org.eclipse.emf.compare.facade.internal.post;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import static org.eclipse.emf.compare.DifferenceSource.LEFT;
import static org.eclipse.emf.compare.DifferenceSource.RIGHT;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.fromSide;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.ofKind;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.valueIs;
import static org.eclipse.emf.compare.utils.MatchUtil.findAddOrDeleteContainmentDiffs;
import static org.eclipse.emf.compare.utils.MatchUtil.getMatchedObject;

import com.google.common.collect.Iterables;

import java.util.Collections;

import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.ComparisonCanceledException;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceKind;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.ResourceAttachmentChange;
import org.eclipse.emf.compare.facade.FacadeAdapter;
import org.eclipse.emf.compare.internal.utils.ComparisonUtil;
import org.eclipse.emf.compare.postprocessor.IPostProcessor;
import org.eclipse.emf.compare.utils.MatchUtil;
import org.eclipse.emf.ecore.EObject;

/**
 * Generic façade post-processor. It modifies the comparison model to
 * <ul>
 * <li>add diff {@link Diff#getRequires() requires} dependencies based on the underlying model</li>
 * </ul>
 *
 * @author Christian W. Damus
 */
public class FacadePostProcessor implements IPostProcessor {

	/**
	 * Initializes me.
	 */
	public FacadePostProcessor() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postRequirements(Comparison comparison, Monitor monitor) {
		for (Diff next : comparison.getDifferences()) {
			if (next instanceof ReferenceChange) {
				if (monitor.isCanceled()) {
					throw new ComparisonCanceledException();
				}

				if (tryAddReferenceToFacade(comparison, next)) {
					continue;
				}
				if (tryRemoveReferenceToDeletedFacade(comparison, next)) {
					continue;
				}
			}
		}
	}

	/**
	 * Looks for dependencies of a change that adds a reference to an object underlying an added façade
	 * object.
	 * 
	 * @param comparison
	 *            the current comparison
	 * @param diff
	 *            a diff to examing
	 * @return whether it was an addition of a reference to an added façade
	 */
	private boolean tryAddReferenceToFacade(Comparison comparison, Diff diff) {
		boolean result = false;

		out: switch (diff.getKind()) {
			case ADD:
			case CHANGE:
				ReferenceChange refChange = (ReferenceChange)diff;
				EObject referenced = refChange.getValue();
				if (FacadeAdapter.isFacade(referenced)) {
					// But not if it's of the same façade as the subject of the diff
					// (or if the subject of the diff is deleted)
					EObject subject = MatchUtil.getMatchedObject(refChange.getMatch(), refChange.getSource());
					if ((subject == null)
							|| (subject.eClass().getEPackage() == referenced.eClass().getEPackage())) {
						break out;
					}
					referenced = FacadeAdapter.getUnderlyingObject(referenced);
				}

				Iterable<Diff> dependencies = getRequiredFacadeAdds(comparison, referenced);
				result = addAll(refChange.getRequires(), dependencies);
				break;
			default:
				// Pass
				break;
		}

		return result;
	}

	/**
	 * Looks for dependents of a change that removes a reference to an object underlying a deleted façade
	 * object.
	 * 
	 * @param comparison
	 *            the current comparison
	 * @param diff
	 *            a diff to examing
	 * @return whether it was an removal of a reference to a deleted façade
	 */
	private boolean tryRemoveReferenceToDeletedFacade(Comparison comparison, Diff diff) {
		boolean result = false;

		out: switch (diff.getKind()) {
			case DELETE:
			case CHANGE:
				ReferenceChange refChange = (ReferenceChange)diff;
				if (!ComparisonUtil.isDeleteOrUnsetDiff(refChange)) {
					break out;
				}

				EObject referenced = refChange.getValue();
				if ((referenced == null) && (refChange.getKind() == DifferenceKind.CHANGE)) {
					// It's an unset of a scalar reference
					referenced = MatchUtil.getOriginValue(comparison, refChange);
				}

				if (referenced == null) {
					// Can't determine the removed object. Give up
					break out;
				}

				if (FacadeAdapter.isFacade(referenced)) {
					// But not if it's of the same façade as the subject of the diff
					// (or if the subject of the diff is deleted)
					EObject subject = MatchUtil.getMatchedObject(refChange.getMatch(), refChange.getSource());
					if ((subject == null)
							|| (subject.eClass().getEPackage() == referenced.eClass().getEPackage())) {
						break out;
					}
					referenced = FacadeAdapter.getUnderlyingObject(referenced);
				}

				Iterable<Diff> dependents = getRequiringFacadeDeletes(comparison, referenced,
						refChange.getSource());
				result = addAll(refChange.getRequiredBy(), dependents);
				break;
			default:
				// Pass
				break;
		}

		return result;
	}

	/**
	 * Finds diffs that relate to the addition of a façade that represents the given object {@link referenced}
	 * by some other object that didn't previously reference it.
	 * 
	 * @param comparison
	 *            the comparison being analyzed
	 * @param referenced
	 *            an object that is added to a reference in some diff
	 * @return diffs pertaining to the addition of the object
	 */
	private Iterable<Diff> getRequiredFacadeAdds(Comparison comparison, EObject referenced) {
		Iterable<Diff> result;

		EObject facade = FacadeAdapter.getFacade(referenced);
		if (facade == null) {
			// If it doesn't have a façade, then there's nothing to look for
			result = Collections.emptyList();
		} else {
			Match match = comparison.getMatch(facade);
			if (match == null) {
				result = Collections.emptyList();
			} else {
				// Is this object new?
				EObject origin = getOriginObject(match, facade);
				if (origin == null) {
					// It's new. On which side?
					if (match.getLeft() == facade) {
						result = getAddDiff(match, facade, LEFT);
					} else if (match.getRight() == facade) {
						result = getAddDiff(match, facade, RIGHT);
					} else {
						// Deleted in both sides, it would seem
						result = Collections.emptyList();
					}
				} else {
					// It's not new
					result = Collections.emptyList();
				}
			}
		}

		return result;
	}

	/**
	 * Obtains the original-side counterpart of an {@code object} in a {@code match}.
	 * 
	 * @param match
	 *            a match
	 * @param object
	 *            an object on some side of the {@code match}
	 * @return the counterpart on the origin side, depending on whether it's a three-way or a two-way
	 *         comparison
	 */
	private static EObject getOriginObject(Match match, EObject object) {
		if (match.getComparison().isThreeWay()) {
			return match.getOrigin();
		} else if (match.getLeft() == object) {
			return match.getRight();
		} else {
			return match.getLeft();
		}
	}

	/**
	 * Obtains the (usually at most one) diffs that are the addition of the given {@code added} object on the
	 * specified side of a {@code match}.
	 * 
	 * @param match
	 *            a one-sided match of an {@code added} object
	 * @param added
	 *            the added object
	 * @param onSide
	 *            the side on which the {@code added} object was added
	 * @return the diffs, if any, that are its addition diffs
	 */
	@SuppressWarnings("unchecked")
	Iterable<Diff> getAddDiff(Match match, EObject added, DifferenceSource onSide) {
		Iterable<Diff> addToContainment = filter(orEmpty(findAddOrDeleteContainmentDiffs(match)),
				and(ofKind(DifferenceKind.ADD), valueIs(added), fromSide(onSide)));

		Iterable<Diff> addToResource = filter(match.getDifferences(), and(
				instanceOf(ResourceAttachmentChange.class), ofKind(DifferenceKind.ADD), fromSide(onSide)));

		return Iterables.concat(addToContainment, addToResource);
	}

	/**
	 * Cases a {@code null} iterable as an empty iterable.
	 * 
	 * @param maybeNull
	 *            an iterable or {@code null}
	 * @return the iterable or an empty iterable if it was {@code null}
	 * @param <T>
	 *            the element type of the iterable
	 */
	static <T> Iterable<T> orEmpty(Iterable<T> maybeNull) {
		if (maybeNull == null) {
			return Collections.emptyList();
		} else {
			return maybeNull;
		}
	}

	/**
	 * Finds diffs that relate to the deletion of a façade that represents the given object {@link referenced}
	 * by some other object that previously referenced it.
	 * 
	 * @param comparison
	 *            the comparison being analyzed
	 * @param referenced
	 *            an object that is removed from a reference in some diff
	 * @param onSide
	 *            the side of the comparison in which to look for deletions of the {@code reference} object
	 * @return diffs pertaining to the deletion of the object
	 */
	private Iterable<Diff> getRequiringFacadeDeletes(Comparison comparison, EObject referenced,
			DifferenceSource onSide) {

		Iterable<Diff> result;

		EObject facade = FacadeAdapter.getFacade(referenced);
		if (facade == null) {
			// If it doesn't have a façade, then there's nothing to look for
			result = Collections.emptyList();
		} else {
			Match match = comparison.getMatch(facade);
			if (match == null) {
				result = Collections.emptyList();
			} else {
				// Is this object deleted?
				EObject origin = getOriginObject(match, facade);
				if ((origin != null) && (getMatchedObject(match, onSide) == null)) {
					// It's deleted
					result = getDeleteDiff(match, facade, onSide);
				} else {
					// It's not deleted
					result = Collections.emptyList();
				}
			}
		}

		return result;
	}

	/**
	 * Obtains the (usually at most one) diffs that are the deletion of the given {@code removed} object on
	 * the specified side of a {@code match}.
	 * 
	 * @param match
	 *            an empty-sided match of a {@code removed} object
	 * @param removed
	 *            the removed object
	 * @param onSide
	 *            the side on which the {@code removed} object was removed
	 * @return the diffs, if any, that are its deletion diffs
	 */
	@SuppressWarnings("unchecked")
	Iterable<Diff> getDeleteDiff(Match match, EObject removed, DifferenceSource onSide) {
		Iterable<Diff> removeFromContainment = filter(orEmpty(findAddOrDeleteContainmentDiffs(match)),
				and(ofKind(DifferenceKind.DELETE), valueIs(removed), fromSide(onSide)));

		Iterable<Diff> removeFromResource = filter(match.getDifferences(), and(
				instanceOf(ResourceAttachmentChange.class), ofKind(DifferenceKind.DELETE), fromSide(onSide)));

		return Iterables.concat(removeFromContainment, removeFromResource);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postMatch(Comparison comparison, Monitor monitor) {
		// Pass
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postDiff(Comparison comparison, Monitor monitor) {
		// Pass
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postEquivalences(Comparison comparison, Monitor monitor) {
		// Pass
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postConflicts(Comparison comparison, Monitor monitor) {
		// Pass
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void postComparison(Comparison comparison, Monitor monitor) {
		// Pass
	}

}
