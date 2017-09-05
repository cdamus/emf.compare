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
package org.eclipse.emf.compare.uml2.facade.tests.framework;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static java.util.stream.StreamSupport.stream;
import static org.eclipse.emf.compare.ConflictKind.REAL;
import static org.eclipse.emf.compare.DifferenceSource.LEFT;
import static org.eclipse.emf.compare.DifferenceSource.RIGHT;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.fromSide;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasConflict;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Conflict;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceSource;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.rcp.EMFCompareRCPPlugin;

/**
 * This is the {@code MergeUtils} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("nls")
public final class MergeUtils {

	/**
	 * Not instantiable by clients.
	 */
	private MergeUtils() {
		super();
	}

	public static IMerger.Registry getMergerRegistry() {
		return EMFCompareRCPPlugin.getDefault().getMergerRegistry();
	}

	public static void acceptAllNonConflicting(Comparison comparison) {
		acceptAllNonConflicting(comparison, getMergerRegistry());
	}

	public static void acceptAllNonConflicting(Comparison comparison, IMerger.Registry mergerRegistry) {
		Monitor monitor = new BasicMonitor();
		Iterable<Diff> nonConflicting = filter(comparison.getDifferences(), not(hasConflict(REAL)));
		IBatchMerger merger = new BatchMerger(mergerRegistry);

		Iterable<Diff> fromRight = filter(nonConflicting, fromSide(RIGHT));
		merger.copyAllRightToLeft(fromRight, monitor);

		Iterable<Diff> fromLeft = filter(nonConflicting, fromSide(LEFT));
		merger.copyAllLeftToRight(fromLeft, monitor);
	}

	public static void accept(DifferenceSource side, Conflict... conflict) {
		accept(side, getMergerRegistry(), conflict);
	}

	public static void accept(DifferenceSource side, IMerger.Registry mergerRegistry, Conflict... conflict) {
		accept(side, mergerRegistry, Arrays.asList(conflict));
	}

	public static void accept(DifferenceSource side, Iterable<? extends Conflict> conflicts) {
		accept(side, getMergerRegistry(), conflicts);
	}

	public static void accept(DifferenceSource side, IMerger.Registry mergerRegistry,
			Iterable<? extends Conflict> conflicts) {
		Monitor monitor = new BasicMonitor();
		List<Diff> conflicting = stream(conflicts.spliterator(), false).map(Conflict::getDifferences)
				.flatMap(Collection::stream).collect(Collectors.toList());

		IBatchMerger merger = new BatchMerger(mergerRegistry);
		switch (side) {
			case LEFT:
				merger.copyAllLeftToRight(conflicting, monitor);
				break;
			case RIGHT:
				merger.copyAllRightToLeft(conflicting, monitor);
				break;
			default:
				fail("Cannot accept changes on side " + side);
				break; // Unreachable
		}
	}

}
