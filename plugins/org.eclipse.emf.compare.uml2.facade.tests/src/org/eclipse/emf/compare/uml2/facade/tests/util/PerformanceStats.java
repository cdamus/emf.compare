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
package org.eclipse.emf.compare.uml2.facade.tests.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;

/**
 * A very simple performance statistics tracker.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings("nls")
public class PerformanceStats {
	/** A start-time token indicated clock not started. */
	private static final long NOT_STARTED = -1L;

	/** Current interval start time. */
	private long startNanos = NOT_STARTED;

	/** Samples of time measurement intervals. */
	private final List<Long> samples = Lists.newArrayList();

	private final int warmupIterations;

	/** Implementation of the current running summary statistics. */
	private Stats stats;

	/**
	 * Initializes me without any warm-up iterations.
	 */
	public PerformanceStats() {
		this(0);
	}

	/**
	 * Initializes me with the number of warm-up iterations that are expected to have outlying measurements
	 * that should be discarded.
	 * 
	 * @param warmupIterations
	 *            the number of iterations of warm-up to account for
	 */
	public PerformanceStats(int warmupIterations) {
		super();

		this.warmupIterations = warmupIterations;
	}

	/**
	 * Initializes me as a copy of another performance {@code stats} collection.
	 * 
	 * @param stats
	 *            performance stats to copy
	 */
	public PerformanceStats(PerformanceStats stats) {
		this(stats.warmupIterations);

		this.samples.addAll(stats.samples);
	}

	/**
	 * Start clocking a time interval for a performance run.
	 * 
	 * @throws IllegalStateException
	 *             if the timer was already running
	 */
	public void start() {
		Preconditions.checkState(startNanos == NOT_STARTED, "Timing already started");
		stats = null;
		startNanos = System.nanoTime();
	}

	/**
	 * Stop the current time time interval and add it to the summary statistics.
	 * 
	 * @throws IllegalStateException
	 *             if the timer was not running
	 */
	public void stop() {
		Preconditions.checkState(startNanos != NOT_STARTED, "Timing already stopped");
		try {
			long endNanos = System.nanoTime();
			add(endNanos - startNanos);
		} finally {
			startNanos = NOT_STARTED;
		}
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param experiment
	 *            the experiment to repeat
	 */
	public void run(int iterations, Runnable experiment) {
		run(iterations, experiment, (Runnable)null);
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param experiment
	 *            the experiment to repeat
	 * @param cleanup
	 *            an optional clean-up action to perform after every iteration. The input to the clean-up
	 *            action is the iteration counter
	 */
	public void run(int iterations, Runnable experiment, IntConsumer cleanup) {
		for (int i = 0; i < iterations; i++) {
			start();
			try {
				experiment.run();
			} finally {
				stop();

				if (cleanup != null) {
					cleanup.accept(i);
				}
			}
		}
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param experiment
	 *            the experiment to repeat
	 * @param cleanup
	 *            an optional clean-up action to perform after every iteration
	 */
	public void run(int iterations, Runnable experiment, Runnable cleanup) {
		for (int i = 0; i < iterations; i++) {
			start();
			try {
				experiment.run();
			} finally {
				stop();

				if (cleanup != null) {
					cleanup.run();
				}
			}
		}
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param setup
	 *            a supplier of the inputs to the experiment
	 * @param experiment
	 *            the experiment to repeat
	 * @param cleanup
	 *            an optional clean-up action to perform after every iteration
	 */
	public <T, U> void run(int iterations, Supplier<? extends Pair<? extends T, ? extends U>> setup,
			BiConsumer<? super T, ? super U> experiment, BiConsumer<? super T, ? super U> cleanup) {
		for (int i = 0; i < iterations; i++) {
			Pair<? extends T, ? extends U> input = setup.get();

			start();
			try {
				experiment.accept(input.first(), input.second());
			} finally {
				stop();

				if (cleanup != null) {
					cleanup.accept(input.first(), input.second());
				}
			}
		}
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param setup
	 *            a supplier of the input to the experiment
	 * @param experiment
	 *            the experiment to repeat
	 * @param cleanup
	 *            an optional clean-up action to perform after every iteration
	 */
	public <T, U> void run(int iterations, Supplier<? extends Pair<? extends T, ? extends U>> setup,
			BiConsumer<? super T, ? super U> experiment, Runnable cleanup) {
		for (int i = 0; i < iterations; i++) {
			Pair<? extends T, ? extends U> input = setup.get();

			start();
			try {
				experiment.accept(input.first(), input.second());
			} finally {
				stop();

				if (cleanup != null) {
					cleanup.run();
				}
			}
		}
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param setup
	 *            a supplier of the input to the experiment
	 * @param experiment
	 *            the experiment to repeat
	 * @param cleanup
	 *            an optional clean-up action to perform after every iteration
	 */
	public <T> void run(int iterations, Supplier<? extends T> setup, Consumer<? super T> experiment,
			Consumer<? super T> cleanup) {
		for (int i = 0; i < iterations; i++) {
			T input = setup.get();

			start();
			try {
				experiment.accept(input);
			} finally {
				stop();

				if (cleanup != null) {
					cleanup.accept(input);
				}
			}
		}
	}

	/**
	 * Runs an {@code experiment} a specified number of times, accumulating the duration of each iteration in
	 * our statistics.
	 * 
	 * @param iterations
	 *            the number of times
	 * @param setup
	 *            a supplier of the input to the experiment
	 * @param experiment
	 *            the experiment to repeat
	 * @param cleanup
	 *            an optional clean-up action to perform after every iteration
	 */
	public <T> void run(int iterations, Supplier<? extends T> setup, Consumer<? super T> experiment,
			Runnable cleanup) {
		for (int i = 0; i < iterations; i++) {
			T input = setup.get();

			start();
			try {
				experiment.accept(input);
			} finally {
				stop();

				if (cleanup != null) {
					cleanup.run();
				}
			}
		}
	}

	/**
	 * Accumulate a new time-interval measurement.
	 * 
	 * @param nanos
	 *            the new time interval
	 */
	public void add(long nanos) {
		samples.add(Long.valueOf(nanos));
	}

	/**
	 * Obtains my summary statistics, computed now if necessary because they don't exist or a previous
	 * computation was invalidated by new measurements.
	 * 
	 * @return my current summary statistics
	 */
	private Stats getStats() {
		if (stats == null) {
			stats = samples.stream().skip(warmupIterations).collect(Stats.collector());
		}

		return stats;
	}

	/**
	 * Queries the total time measured over all intervals.
	 * 
	 * @param unit
	 *            the time unit in which terms to report the time (rounded as necessary)
	 * @return the total running time in the given {@code unit} measure
	 */
	public long total(TimeUnit unit) {
		Stats s = getStats();

		return unit.convert(s.getSum(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Queries the average time measured across all intervals.
	 * 
	 * @param unit
	 *            the time unit in which terms to report the time (rounded as necessary)
	 * @return the average interval running time in the given {@code unit} measure
	 */
	public long average(TimeUnit unit) {
		Stats s = getStats();

		return unit.convert((long)s.getAverage(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Queries the sample standard deviation of the time measured across all intervals.
	 * 
	 * @param unit
	 *            the time unit in which terms to report the time (rounded as necessary)
	 * @return the sample standard deviation of the running time in the given {@code unit} measure
	 */
	public long stddev(TimeUnit unit) {
		Stats s = getStats();

		return unit.convert((long)s.getStandardDeviation(), TimeUnit.NANOSECONDS);
	}

	/**
	 * Obtains an immutable copy of all of the samples measured so far.
	 * 
	 * @param unit
	 *            the time unit in which terms to report the samples (rounded as necessary)
	 * @return the samples measured so far
	 */
	public List<Long> samples(TimeUnit unit) {
		ImmutableList.Builder<Long> result = ImmutableList.builder();
		samples.stream() //
				.map(s -> Long.valueOf(unit.convert(s.longValue(), TimeUnit.NANOSECONDS))) //
				.forEach(result::add);
		return result.build();
	}

	//
	// Nested types
	//

	/**
	 * An expansion of the long-valued summary statistics that includes the sample standard deviation of the
	 * collected measurements.
	 *
	 * @author Christian W. Damus
	 */
	static final class Stats extends LongSummaryStatistics {
		private List<Long> samples = Lists.newArrayList();

		private double stddev;

		/**
		 * Initializes me.
		 */
		Stats() {
			super();
		}

		/**
		 * Copy constructor.
		 * 
		 * @param stats
		 *            instance to copy
		 */
		Stats(Stats stats) {
			this();

			this.samples.addAll(stats.samples);
			this.stddev = stats.stddev;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void accept(long value) {
			samples.add(Long.valueOf(value));
			super.accept(value);
		}

		void combine(Stats other) {
			combine((LongSummaryStatistics)other);
			samples.addAll(other.samples);
		}

		double getStandardDeviation() {
			return stddev;
		}

		Stats finish() {
			if (getCount() <= 1) {
				stddev = Double.NaN;
			} else {
				final double average = getAverage();

				stddev = Math.sqrt(samples.stream().mapToDouble(new ToDoubleFunction<Long>() {
					/**
					 * {@inheritDoc}
					 */
					@Override
					public double applyAsDouble(Long value) {
						double delta = value.doubleValue() - average;
						return delta * delta;
					}
				}).sum() / (getCount() - 1));
			}

			return this;
		}

		static Collector<Long, Stats, Stats> collector() {
			return Collector.of(Stats::new, Stats::accept, (s, r) -> {
				s.combine(r);
				return s;
			}, Stats::finish);
		}
	}
}
