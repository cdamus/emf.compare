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
package org.eclipse.emf.compare.facade;

import static java.util.Objects.requireNonNull;
import static org.eclipse.emf.compare.facade.ReflectiveDispatch.getMethods;
import static org.eclipse.emf.compare.facade.ReflectiveDispatch.resolveMethod;
import static org.eclipse.emf.compare.facade.ReflectiveDispatch.safeInvoke;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * An adapter that links a façade object with its underlying model element, coördination synchronziation of
 * changes between the two.
 *
 * @author Christian W. Damus
 */
public class FacadeAdapter implements Adapter.Internal {
	/** Cache of reflective incremental synchronizers. */
	private static LoadingCache<CacheKey, Synchronizer> synchronizerCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4).build(CacheLoader.from(FacadeAdapter::resolveSynchronizer));

	/** Cache of reflective initial synchronizers. */
	private static LoadingCache<CacheKey, Synchronizer> initializerCache = CacheBuilder.newBuilder()
			.concurrencyLevel(4).build(CacheLoader.from(FacadeAdapter::resolveInitializer));

	/** The façade object that this adapter links to an underlying model element. */
	private final EObject facade;

	/** The façade's underlying model element in the (real) user model. */
	private final EObject model;

	/** A latch to prevent re-entrant synchronization. */
	private boolean synchronizing;

	/**
	 * Initializes me with the façade and underlying user model element that I associate with one another.
	 * 
	 * @param facade
	 *            the façade element
	 * @param model
	 *            the underlying model element
	 * @throws NullPointerException
	 *             if either argument is {@code null}
	 */
	public FacadeAdapter(EObject facade, EObject model) {
		super();

		this.facade = requireNonNull(facade, "facade"); //$NON-NLS-1$
		this.model = requireNonNull(model, "model"); //$NON-NLS-1$

		addAdapter(facade);
		addAdapter(model);
	}

	/**
	 * Detaches me from all model elements that I adapt.
	 */
	public void dispose() {
		removeAdapter(facade);
		removeAdapter(model);
	}

	/**
	 * Ensures that I am attached to a {@code notifier}, but at most once.
	 * 
	 * @param notifier
	 *            a notifier to adapt
	 */
	public final void addAdapter(Notifier notifier) {
		EList<Adapter> adapters = notifier.eAdapters();
		if (!adapters.contains(this)) {
			adapters.add(this);
		}
	}

	/**
	 * Ensures that I am not attached to a {@code notifier}.
	 * 
	 * @param notifier
	 *            a notifier to unadapt
	 */
	public final void removeAdapter(Notifier notifier) {
		EList<Adapter> adapters = notifier.eAdapters();
		adapters.remove(this);
	}

	/**
	 * The canonical target of this adapter is the {@linkplain #getUnderlyingElement() underlying model
	 * element}.
	 * 
	 * @return the underlying model element
	 * @see #getUnderlyingElement()
	 */
	public Notifier getTarget() {
		return model;
	}

	/**
	 * Queries the façade model element.
	 * 
	 * @return my façade
	 */
	public EObject getFacade() {
		return facade;
	}

	/**
	 * Queries the model element underlying the façade.
	 * 
	 * @return the underlying model element
	 * @see FacadeObject#getUnderlyingElement()
	 */
	public EObject getUnderlyingElement() {
		return model;
	}

	/**
	 * By default, no additional targets are tracked.
	 * 
	 * @param newTarget
	 *            a notifier to which I have been attached
	 */
	public void setTarget(Notifier newTarget) {
		// Don't track any other targets
	}

	/**
	 * {@inheritDoc}
	 */
	public void unsetTarget(Notifier oldTarget) {
		if ((oldTarget == model) || (oldTarget == facade)) {
			dispose();
		}
	}

	/**
	 * Reacts to changes in the façade or underlying element to synchronize with the other.
	 * 
	 * @param notification
	 *            description of a change in either the façade or the underlying element
	 * @see #handleNotification(Notification)
	 */
	public void notifyChanged(Notification notification) {
		if (notification.isTouch()) {
			return;
		}

		handleNotification(notification);
	}

	/**
	 * Implements the handling of a notification to perform synchronization.
	 * 
	 * @param notification
	 *            description of a change in either the façade or the underlying element. It will not be a
	 *            {@link Notification#isTouch() touch} event
	 * @return whether the notification was completely processed an no further synchronization is required
	 */
	protected boolean handleNotification(Notification notification) {
		boolean result = false;

		if (notification.getNotifier() == model) {
			syncModelToFacade(notification);
			result = true;
		} else if (notification.getNotifier() == facade) {
			syncFacadeToModel(notification);
			result = true;
		}

		return result;
	}

	/**
	 * Synchronizes the underlying model to the façade, triggered by the given {@code notification}.
	 * 
	 * @param notification
	 *            description of a change in the model
	 */
	protected void syncModelToFacade(Notification notification) {
		synchronize(model, facade, false, notification);
	}

	/**
	 * Synchronizes a {@code source} object to the {@code target}.
	 * 
	 * @param source
	 *            the synchronization source ('from') object
	 * @param target
	 *            the synchronization target ('to') object
	 * @param toModel
	 *            whether synchronization is to the model (as opposed to the façade)
	 * @param notification
	 *            the notification that triggered synchronization
	 */
	protected final void synchronize(EObject source, EObject target, boolean toModel,
			Notification notification) {

		if (this.synchronizing) {
			return;
		}

		Synchronizer synchronizer;
		if (toModel) {
			synchronizer = resolveFacadeToModelSynchronizer(source, target, notification);
		} else {
			synchronizer = resolveModelToFacadeSynchronizer(source, target, notification);
		}

		final boolean wasSynchronizing = this.synchronizing;
		this.synchronizing = true;

		try {
			synchronizer.synchronize(this, source, target, notification);
		} finally {
			this.synchronizing = wasSynchronizing;
		}
	}

	/**
	 * Synchronizes the façade to its underlying model, triggered by the given {@code notification}.
	 * 
	 * @param notification
	 *            description of a change in the façade
	 */
	protected void syncFacadeToModel(Notification notification) {
		synchronize(facade, model, true, notification);
	}

	/**
	 * I am an adapter for either the {@code FacadeObject} type or the {@code FacadeAdapter} type.
	 * 
	 * @param type
	 *            the adapter type to test for
	 * @return whether I am an adapter of the given {@code type}
	 */
	public boolean isAdapterForType(Object type) {
		return (type == FacadeObject.class) || (type == FacadeAdapter.class);
	}

	/**
	 * <p>
	 * Resolves the synchronizer call-back for a given {@code notification} from the underlying model element.
	 * The default implementation delegates by reflection to a synchronization method defined by this adapter
	 * with the following name pattern and signature:
	 * </p>
	 * <blockquote> <tt>sync<i>FeatureName</i>ToFacade(<i>ModelType</i>, <i>FacadeType</i>, Notification)</tt>
	 * </blockquote>
	 * <p>
	 * where
	 * </p>
	 * <ul>
	 * <li><i>FeatureName</i> is the name of the feature to synchronize (as indicated by the
	 * {@code notification}) with the initial letter upper-cased</li>
	 * <li><i>ModelType</i> is the Java type of the {@code source} object's {@link EClass}</li>
	 * <li><i>FacadeType</i> is the Java type of the {@code target} object's {@link EClass}</li>
	 * </ul>
	 * <p>
	 * The most specific method signature as determined by the <i>ModelType</i> and <i>FacadeType</i> is
	 * selected that is compatible with the actual types of the {@code source} and {@code target} objects,
	 * respectively. However, the {@link Notification} is optional: the synchronization method is not required
	 * to have this parameter, but any compatible method that accepts a notification trumps any method that
	 * does not, even if the latter is more specific in the other parameters.
	 * </p>
	 * 
	 * @param source
	 *            the synchronization source ('from') object
	 * @param target
	 *            the synchronization target ('to') object
	 * @param notification
	 *            a notification
	 * @return the synchronizer. Must never be {@code null}, but at least the no-op {@link Synchronizer#PASS
	 *         PASS} instance
	 */
	protected Synchronizer resolveModelToFacadeSynchronizer(EObject source, EObject target,
			Notification notification) {
		return resolveReflectiveSynchronizer(source, target, notification, false);
	}

	/**
	 * Resolves the reflective method synchronizer for a given {@code notification} in the specified
	 * direction.
	 * 
	 * @param source
	 *            the synchronization source ('from') object
	 * @param target
	 *            the synchronization target ('to') object
	 * @param notification
	 *            a notification
	 * @param toModel
	 *            whether the synchronization direction is façade-to-model
	 * @return the synchronizer
	 */
	private Synchronizer resolveReflectiveSynchronizer(EObject source, EObject target,
			Notification notification, boolean toModel) {

		Synchronizer result = Synchronizer.PASS;

		Object feature = notification.getFeature();
		if (feature instanceof EStructuralFeature) {
			String featureName = ((EStructuralFeature)feature).getName();

			// Use the EClass instance-classes to get the interface type if these models were
			// generated with interfaces. Try first with a notification for specificity, then without
			Class<?> sourceType = source.eClass().getInstanceClass();
			Class<?> targetType = target.eClass().getInstanceClass();

			CacheKey key = new CacheKey(getClass(), featureName, toModel, sourceType, targetType);
			result = synchronizerCache.getUnchecked(key);
		}

		return result;
	}

	/**
	 * Resolves a synchronizer as specified by its caching {@code key}.
	 * 
	 * @param key
	 *            a caching key
	 * @return the synchronizer to cache. Must not be {@code null}, but instead in that case should be the
	 *         {@link Synchronizer#PASS PASS} instance
	 */
	private static Synchronizer resolveSynchronizer(CacheKey key) {
		String directionName;

		if (key.toModel) {
			directionName = "Model"; //$NON-NLS-1$
		} else {
			directionName = "Facade"; //$NON-NLS-1$
		}

		String syncMethodName = String.format("sync%sTo%s", //$NON-NLS-1$
				toInitialUpperCase(key.featureName), directionName);

		// Try first with a notification for specificity, then without
		Optional<Method> syncMethod = resolveMethod(key.owner, syncMethodName, key.sourceType, key.targetType,
				Notification.class);

		return syncMethod
				.<Synchronizer> map(
						method -> (adapter, from, to, msg) -> safeInvoke(adapter, method, from, to, msg))
				.orElseGet(() -> {
					// Optional should have a flatting or-else
					Optional<Method> syncMethodWithoutNotification = resolveMethod(key.owner, syncMethodName,
							key.sourceType, key.targetType);

					return syncMethodWithoutNotification.<Synchronizer> map(
							method -> (adapter, from, to, msg) -> safeInvoke(adapter, method, from, to))
							.orElse(Synchronizer.PASS);
				});
	}

	/**
	 * <p>
	 * Resolves the synchronizer call-back for a given {@code notification} from the façade element. The
	 * default implementation delegates by reflection to a synchronization method defined by this adapter with
	 * the following name pattern and signature:
	 * </p>
	 * <blockquote> <tt>sync<i>FeatureName</i>ToModel(<i>FacadeType</i>, <i>ModelType</i>, Notification)</tt>
	 * </blockquote>
	 * <p>
	 * where
	 * </p>
	 * <ul>
	 * <li><i>FeatureName</i> is the name of the feature to synchronize (as indicated by the
	 * {@code notification}) with the initial letter upper-cased</li>
	 * <li><i>FacadeType</i> is the Java type of the {@code source} object's {@link EClass}</li>
	 * <li><i>ModelType</i> is the Java type of the {@code target} object's {@link EClass}</li>
	 * </ul>
	 * <p>
	 * The most specific method signature as determined by the <i>FcadeType</i> and <i>ModelType</i> is
	 * selected that is compatible with the actual types of the {@code source} and {@code target} objects,
	 * respectively. However, the {@link Notification} is optional: the synchronization method is not required
	 * to have this parameter, but any compatible method that accepts a notification trumps any method that
	 * does not, even if the latter is more specific in the other parameters.
	 * </p>
	 * 
	 * @param source
	 *            the synchronization source ('from') object
	 * @param target
	 *            the synchronization target ('to') object
	 * @param notification
	 *            a notification
	 * @return the synchronizer. Must never be {@code null}, but at least the no-op {@link Synchronizer#PASS
	 *         PASS} instance
	 */
	protected Synchronizer resolveFacadeToModelSynchronizer(EObject source, EObject target,
			Notification notification) {

		return resolveReflectiveSynchronizer(source, target, notification, true);
	}

	/**
	 * Convert a string that may start with a lower case to one that starts with an upper case, if indeed the
	 * first character exists and has case.
	 * 
	 * @param s
	 *            a string or {@code null}
	 * @return {@code s} with the initial character upper case, or else just {@code s}
	 */
	static String toInitialUpperCase(String s) {
		String result = s;

		if (!Strings.isNullOrEmpty(s)) {
			String initial = s.substring(0, 1);
			String upper = initial.toUpperCase();
			if (!initial.equals(upper)) {
				result = upper + s.substring(1);
			}
		}

		return result;
	}

	/**
	 * Initializes synchronization between the model and its façade.
	 * 
	 * @param direction
	 *            the synchronization direction
	 */
	public void initialSync(SyncDirectionKind direction) {
		initialSync(direction, null);
	}

	/**
	 * Performs initial synchronization of a {@code facade} object and its underlying {@code model} element.
	 * 
	 * @param aFacade
	 *            a facade object
	 * @param aModel
	 *            its underlying model element
	 * @param direction
	 *            the synchronization direction
	 */
	protected final void initialSync(EObject aFacade, EObject aModel, SyncDirectionKind direction) {
		initialSync(aFacade, aModel, direction, null);
	}

	/**
	 * Initializes synchronization between the model and its façade.
	 * 
	 * @param direction
	 *            the synchronization direction
	 * @param feature
	 *            the specific feature to synchronize, or {@code null} to synchronize all
	 */
	public void initialSync(SyncDirectionKind direction, EStructuralFeature feature) {
		initialSync(facade, model, direction, feature);
	}

	/**
	 * Performs initial synchronization of a {@code facade} object and its underlying {@code model} element.
	 * 
	 * @param aFacade
	 *            a facade object
	 * @param aModel
	 *            its underlying model element
	 * @param direction
	 *            the synchronization direction
	 * @param feature
	 *            the specific feature to synchronize, or {@code null} to synchronize all
	 */
	protected final void initialSync(EObject aFacade, EObject aModel, SyncDirectionKind direction,
			EStructuralFeature feature) {

		if (this.synchronizing) {
			return;
		}

		final boolean wasSynchronizing = this.synchronizing;
		this.synchronizing = true;

		try {
			String featureName;

			if (feature == null) {
				featureName = null;
			} else {
				featureName = feature.getName();
			}

			Class<?> modelType = aModel.eClass().getInstanceClass();
			Class<?> facadeType = aFacade.eClass().getInstanceClass();

			direction.sync(this, aFacade, aModel,
					() -> initializerCache.getUnchecked(
							new CacheKey(getClass(), featureName, false, modelType, facadeType)),
					() -> initializerCache.getUnchecked(
							new CacheKey(getClass(), featureName, true, facadeType, modelType)));
		} finally {
			this.synchronizing = wasSynchronizing;
		}
	}

	/**
	 * Resolves an initial synchronizer as specified by its caching {@code key}.
	 * 
	 * @param key
	 *            a caching key
	 * @return the initializer to cache. Must not be {@code null}, but instead in that case should be the
	 *         {@link Synchronizer#PASS PASS} instance
	 */
	private static Synchronizer resolveInitializer(CacheKey key) {
		Predicate<String> methodNameFilter;

		if (key.toModel) {
			methodNameFilter = syncToModelName(key.featureName);
		} else {
			methodNameFilter = syncToFacadeName(key.featureName);
		}

		return getMethods(key.owner, methodNameFilter, key.sourceType, key.targetType)
				.<Synchronizer> map(m -> (adapter, from, to, msg) -> safeInvoke(adapter, m, from, to))
				.reduce(Synchronizer::andThen) //
				.orElse(Synchronizer.PASS);
	}

	/**
	 * Matches a sync-to-model method name.
	 * 
	 * @param featureName
	 *            the feature name, or {@code null} to match any feature
	 * @return the to-model synchronization method name filter
	 */
	private static Predicate<String> syncToModelName(String featureName) {
		if (featureName == null) {
			return s -> s.startsWith("sync") && s.endsWith("ToModel"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return String.format("sync%sToModel", toInitialUpperCase(featureName))::equals; //$NON-NLS-1$
		}
	}

	/**
	 * Matches a sync-to-façade method name.
	 * 
	 * @param featureName
	 *            the feature name, or {@code null} to match any feature
	 * @return the to-façade synchronization method name filter
	 */
	private static Predicate<String> syncToFacadeName(String featureName) {
		if (featureName == null) {
			return s -> s.startsWith("sync") && s.endsWith("ToFacade"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			return String.format("sync%sToFacade", toInitialUpperCase(featureName))::equals; //$NON-NLS-1$
		}
	}

	/**
	 * Ensures that a {@code facade} is connected to its underlying {@code model}.
	 * 
	 * @param facade
	 *            a facade object
	 * @param model
	 *            the underlying model element
	 * @param type
	 *            the type of adapter
	 * @param adapterCreator
	 *            a function that creates the adapter, if needed
	 * @return the adapter that connects the {@code facade} with its {@code model}
	 * @param <A>
	 *            the adapter type
	 * @param <F>
	 *            the façade type
	 * @param <M>
	 *            the model element type
	 */
	protected static <A extends FacadeAdapter, F extends EObject, M extends EObject> A connect(F facade,
			M model, Class<A> type, BiFunction<? super F, ? super M, ? extends A> adapterCreator) {

		A result = get(model, type);
		if ((result != null) && (result.getFacade() != facade)) {
			result.dispose();
			result = null;
		}

		if (result == null) {
			result = adapterCreator.apply(facade, model);
		}

		return result;
	}

	/**
	 * Obtains the existing facade adapter of the given {@code type} attached to a {@code notifier}.
	 * 
	 * @param notifier
	 *            an object, either a facade or an underlying model element
	 * @param type
	 *            the adapter type
	 * @return the existing adapter instance, or {@code null} if none
	 * @param <A>
	 *            the adapter type
	 */
	protected static <A extends FacadeAdapter> A get(Notifier notifier, Class<A> type) {
		return type.cast(EcoreUtil.getExistingAdapter(notifier, type));
	}

	/**
	 * A no-op.
	 * 
	 * @param adapter
	 *            ignored
	 * @param from
	 *            ignored
	 * @param to
	 *            ignored
	 * @param msg
	 *            ignored
	 */
	private static void syncNoop(FacadeAdapter adapter, EObject from, EObject to, Notification msg) {
		// Pass
	}

	//
	// Nested types
	//

	/**
	 * Protocol for call-backs that synchronize the façade with its underlying model element or <i>vice
	 * versa</i>.
	 *
	 * @author Christian W. Damus
	 */
	@FunctionalInterface
	public interface Synchronizer {
		/** A no-op synchronizer, doing nothing. */
		Synchronizer PASS = FacadeAdapter::syncNoop;

		/**
		 * Synchronizes a change {@code from} the changed object {@code to} its counterpart in the façade
		 * association.
		 * 
		 * @param adapter
		 *            the façade adapter in which context the synchronization is performed
		 * @param from
		 *            an object, in the underlying model or the façade, that has changed
		 * @param to
		 *            the counterpart of the changed object, in the façade or the underlying model,
		 *            respectively
		 * @param msg
		 *            the description of the change
		 */
		void synchronize(FacadeAdapter adapter, EObject from, EObject to, Notification msg);

		/**
		 * Chains another synchronizer to be invoked after me with the same arguments.
		 * 
		 * @param next
		 *            a synchronizer to chain
		 * @return the chained synchronizer. myself and then the {@code next}
		 */
		default Synchronizer andThen(Synchronizer next) {
			return (adapter, from, to, msg) -> {
				synchronize(adapter, from, to, msg);
				next.synchronize(adapter, from, to, msg);
			};
		}
	}

	/**
	 * Caching key that uniquely identifies a reflective synchronizer.
	 *
	 * @author Christian W. Damus
	 */
	private static final class CacheKey {
		/** The facade-adapter synchronization method owner. */
		final Class<?> owner;

		/** The name of the feature to synchronize. */
		final String featureName;

		/** Whether this synchronizer performs the to-model direction (as opposed to to-façade). */
		final boolean toModel;

		/** The source type in the synchronization. */
		final Class<?> sourceType;

		/** The target type in the synchronization. */
		final Class<?> targetType;

		/**
		 * Initializes me with my caching parameters.
		 * 
		 * @param owner
		 *            the owner type
		 * @param featureName
		 *            the name of the feature to synchronizer
		 * @param toModel
		 *            whether synchronization is in the to-model direction (as opposed to to-façade)
		 * @param sourceType
		 *            the source type
		 * @param targetType
		 *            the target type
		 */
		CacheKey(Class<? extends FacadeAdapter> owner, String featureName, boolean toModel,
				Class<?> sourceType, Class<?> targetType) {

			super();

			this.owner = owner;
			this.featureName = featureName;
			this.toModel = toModel;
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		/**
		 * {@inheritDoc}
		 */
		// CHECKSTYLE:OFF (generated)
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((sourceType == null) ? 0 : sourceType.hashCode());
			result = prime * result + ((featureName == null) ? 0 : featureName.hashCode());
			result = prime * result + ((targetType == null) ? 0 : targetType.hashCode());
			result = prime * result + ((owner == null) ? 0 : owner.hashCode());
			result = prime * result + (toModel ? 1231 : 1237);
			return result;
		}
		// CHECKSTYLE:ON

		/**
		 * {@inheritDoc}
		 */
		// CHECKSTYLE:OFF (generated)
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CacheKey other = (CacheKey)obj;
			if (sourceType == null) {
				if (other.sourceType != null) {
					return false;
				}
			} else if (!sourceType.equals(other.sourceType)) {
				return false;
			}
			if (featureName == null) {
				if (other.featureName != null) {
					return false;
				}
			} else if (!featureName.equals(other.featureName)) {
				return false;
			}
			if (targetType == null) {
				if (other.targetType != null) {
					return false;
				}
			} else if (!targetType.equals(other.targetType)) {
				return false;
			}
			if (owner == null) {
				if (other.owner != null) {
					return false;
				}
			} else if (!owner.equals(other.owner)) {
				return false;
			}
			if (toModel != other.toModel) {
				return false;
			}
			return true;
		}
		// CHECKSTYLE:ON

	}
}
