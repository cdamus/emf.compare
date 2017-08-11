<!--
Title: Façade-based Compare/Merge  
Author: Christian W. Damus  
Affiliation: Eclipse.org  
Version: 1.1  
Date: 11 August, 2017  
Copyright: Copyright © 2017 Christian W. Damus and others.  
           All rights reserved.  
           This program and the accompanying materials  
           are made available under the terms of the Eclipse Public License v1.0  
           which accompanies this distribution, and is available at  
           http://www.eclipse.org/legal/epl-v10.html  
-->

# Façade-based Compare/Merge

This document describes a small framework for bi-directional synchronization of a
model and a façade encapsulating it, and integration of façade models into the
comparison and merge operations in EMF Compare.

## Use Cases

All of the use cases for façade models are geared to simplifying and clarifying tasks
for different stakeholders in the EMF Compare system.

![Use Cases](Use_Cases.SVG)

### UC1: Define Façade

Up to and including the Oxygen release, developers of UML Profile based DSMLs have had to
employ a variety of extension points in EMF Compare to tell the framework about the
particular constraints on compare and merge operations that cannot be inferred from the
structure of the UML metamodel and the domain profile.  Challenges include:

* the Eclipse UML2 implementation uses XMI IDs for object identity, which can make
  add-add conflicts difficult to detect without hints
* the UML metamodel makes much use of distinct elements for relationships between
  objects in a model, which usually implies open-ended multiplicity.  This can make
  it difficult to detect conflicts in relationships that the domain would constrain
  in cardinality (often at most one)
  
The purpose of a façade is to provide a more tightly constrained representation of the
domain concepts that "bakes in" these hints within the façade model, itself.  The goal
then is to obviate some of the extensions that would be required to customize the EMF
Compare matching, differencing, and merging algorithms.

**Use case steps:**

1. Developer defines a façade model in Ecore that describes the domain concepts in
   precise terms, combining aspects of the UML metaclass and stereotypes from the profile.
1. Developer code the façade adapters that synchronize the façade model with the
   underlying profiled UML model.
2. Developer codes the façade provider extension that hooks the façade into the
   matching phase (see [below][facadematcher]) and the copier
   extension point that takes care of synchronizing XMI IDs of implicitly merged UML
   model elements (see [below][facadecopier]).

### UC2: Compare DSL Model [uc2]

For the modeling user, the primary benefit of the façade-based compare/merge is an
user interface that presents the model difference in domain-specific terms.  This should
be feasible without a façade, using the extension points available in EMF Compare, so
ultimately it should be transparent to the user.

**Use case steps:**

1. User develops a model with some tooling tailored to the particular DSML, shared in git.
2. Meanwhile, another user also is making changes to the same model and pushes them.
3. The first user pulls from git with rebase, which detects conflicts that must be merged.
4. The system presents a domain-specific view of the incoming and local changes and
   conflicts between then.
5. The user completes the rebase by accepting non-conflicting changes and resolving
   conflicts by rejecting some changes and accepting others. 

### UC2.1: Compare Model without Façade

In a variant of [Use Case UC2][uc2], the user wants to see the most detailed view of the
merge possible, in the raw profiled UML representation, for whatever reason (perhaps
because related tasks are only implemented in UML terms).  In this case, it is as easy
as turning off a single Façade Provider extension in the preferences; the user does not
have to chase down several different extensions, some of which might easily be missed,
but all of which must be enabled or disabled consistently.

**Use case steps:**

1. User develops a model with some tooling tailored to the particular DSML, shared in git.
2. Meanwhile, another user also is making changes to the same model and pushes them.
3. The first user pulls from git with rebase, which detects conflicts that must be merged.
4. This user disables the DSML's façade provider in the preferences.
5. The system presents a generic UML view of the incoming and local changes, exposing
   details of complex relationships and stereotype applications.  Some conflicts may be
   invisible to the tooling and must be inferred by the user.
6. The user completes the rebase by carefully accepting and rejecting changes as
   required by her deep knowledge of the domain profile.

* * *

## Design Overview

The façade-based compare/merge framework, implementations, and tests are defined in
a few OSGi bundles as shown in the diagram below:

* `org.eclipse.emf.compare` — defines core extension points for matchers, mergers,
  and copiers
* `org.eclipse.emf.compare.facade` — defines an extension point for façade providers
  and provides a matcher and copier for façade-based compare/merge.  Also a framework
  for façade-model synchronization
* `org.eclipse.emf.compare.uml2.facade` — extends the core façade copier for UML
  semantics.  Likewise the core façade-model synchronization framework
* `org.eclipse.emf.compare.uml2.facade.tests` — provides two façade customizations:
    + a J2EE Profile for UML with a J2EE Ecore model as a façade, with concrete façade
      provider and copier for J2EE, used in the definition of many of the UML façade test
      cases
    + a partial façade for *opaque expressions* in UML, implementing a façade for the
      `OpaqueExpression` metaclass with a single property that is a list of `BodyEntry`
      elements that are integrated (language,&nbsp;body) pairs where the UML metamodel
      has them as parallel sequences with a one-to-one correspondence

![Façade-related Components](Fa_ade_Bundles.SVG)

## Core Façade API

The core API comprises two parts:  an adapter-based framework for construction
of a façade from and ongoing synchronization of the façade with its underlying
model, and a protocol that façades may optionally implement for integration
into EMF Compare (which will be implemented on their behalf if it is not done
natively in the façade model).

### Bundle org.eclipse.emf.compare.facade

#### Package oeec.facade

![Core Façade API](Core_API.SVG)

The `FacadeObject` interface extends `EObject` from EMF with several additional or altered APIs:

* a `getUnderlyingObject()` method that obtains the principal model element in the
 "real" (persisted) model for which the object presents a façade.  A façade  may map to
  more than one underlying element, but one must be designated as the primary
  implementation of the concept that it represents.  Also, there is no restriction on
  multiple façades mapping to the same underlying element
* a `getFacadeAdapter()` method that obtains the adapter that bridges the façade with
  the underlying model (more on this, below)
* a re-specification of the `EObject::eResource()` API (and indirectly of
   `InternalEObject::eDirectResource()` also) to change the semantics to return the
   corresponding resource of the underlying object if the façade is not, itself,
   contained in a resource

The `FacadeObjectImpl` is a basic implementation of the specification of the
`FacadeObject` interface.  It is particularly convenient for façades that are intended
for use only in the customization of the user's compare/merge experience to generate
them with this interface as class as the "root extends" and "root implementation" in the
EMF Generator Model.

The `FacadeAdapter` is an EMF `Adapter` that attaches to a façade and to its
underlying model element(s) to synchronize their structure in both directions.  This
includes provision for an initial phase to populate the façade object.  The mini-framework
that this implements is expanded on below.

For façade models that do not natively implement the `FacadeObject`, either because they
are provided by third parties and cannot be changed or because they otherwise need to be
used in independent contexts for their different use cases, a dynamic proxy facility is
provided by the `FacadeProxy` utility class.  It provides one public method:

* `createProxy(EObject) : FacadeObject` will create a Java dynamic proxy for a façade
   object that adds the `FacadeObject` interface to the façade.  For objects that are not
   proper façades with a `FacadeAdapter` attached, the behaviour of this proxy is
   undefined

##### Class FacadeAdapter

The `FacadeAdapter` class binds a façade object to its primary counterpart in the
underlying model.  Its main job is to react to changes, signalled by EMF `Notification`s,
on one side of the bridge to effect the appropriate corresponding changes on the other
side.  It may additionally attach itself to other related elements on either side of this
bridge in the case of more complex mappings than just one-to-one.

On receipt of a feature-change notification that is not a *touch*, the `FacadeAdapter`
looks for a synchronizer to dispatch the notification to.  This is a public method of the
subclass having a name and signature of one of the following four forms, according to the
direction of synchronization:

* <code>sync<i>Feature</i>ToModel(<i>FacadeType</i>, <i>ModelType</i>, Notification)</code> —
  in the case of synchronizing a change in the façade to the model
* <code>sync<i>Feature</i>ToModel(<i>FacadeType</i>, <i>ModelType</i>)</code> —
  façade-to-model sync that doesn't need the details of the change in the feature
* <code>sync<i>Feature</i>ToFacade(<i>ModelType</i>, <i>FacadeType</i>, Notification)</code> —
  in the case of synchronizing a change in the underlying model to the façade
* <code>sync<i>Feature</i>ToFacade(<i>ModelType</i>, <i>FacadeType</i>)</code> —
  model-to-façade sync that doesn't need the details of the change in the feature

where *Feature* is the capitalized name of the feature that changed, *FacadeType* is the
type (or a supertype) of the façade object, and *ModelType* is the type (or a supertype)
of the underlying model element.

In resolving these synchronization methods, the most specific matching signature is used
for any pair of façade and underlying model element, where specificity is determined by
how near to the actual types of the model elements on either side of the bridge are the
method's declared parameter types and the signature accepting a notification is more
specific than the signature that does not.  So, for example, in a UML/Ecore
synchronization, a façade adapter may have methods for synchronizing the type parameters
of classifiers.  In such case, the adapter may declare several methods:

1. `syncETypeParametersToModel(EClassifier, Classifier)`
2. `syncETypeParametersToModel(EClass, Class, Notification)`
3. `syncETypeParametersToModel(EClass, Class)`

Consider an `EClass` façade for a UML `Class` stereotyped as `«EClass»` using the Eclipse
UML2 Ecore Profile.  When this adapter get a notification of change to the type parameters
of the `EClass`, it will call the second method mecause that is the most specific match:
the first two parameters are exact matches for the type of the façade and its underlying
model and the method accepts the notification.  However, for an `EDataType` facade for an
`«EDataType» Class`, that method signature is not a match and so the first is used to
perform the synchronization.  The third method signature in this example is used only for
initial synchronization of the model from a façade, in which case there is no notification
to trigger it.

It is entirely possible that a feature on one side or the other of the façade bridge will
not trigger synchronization, either because it is unmapped or because for some reason it
will always be handled indirectly via some other feature.  In these cases, no
synchronization method is required and the façade just substitutes a no-op synchronizer
for the absent method.  This facilitates caching of the synchronizers to avoid the cost
of this look-up on every notification with comparison of the specificity of multiple
matching methods.

As alluded to above, in addition to the `Notification`-driven incremental synchronization,
the adapter provides API for an initial synchronization, which is generally performed
after the underlying model element and its façade are first connected:

* `initialSync(SyncDirectionKind)` will, according to the direction, iterate over the
  mutable features of the object on one side or the other and invokes any synchronization
  methods available for those features in the appropriate direction.  Thus it is possible
  to initialize a new façade from the underlying model or a new underlying model from the
  façade; the algorithm is symmetric.  Also of note is that this initialization only looks
  for synchronization methods that do not have the `Notification` parameter because, of
  course, there is no notification to provide.  Thus it is easy to support the two
  different synchronization use cases just by providing a signature that has the
  notification and ignoring it if necessary to have an incremental synchronizer distinct
  from the initial synchronizer

The adapter also has hooks to allow subclasses to discover the addition and removal of
dependencies:  contained or referenced objects that it needs to track for further changes,
including recursive addition and removal of dependencies.  In a similar fashion to the
synchronization methods, a subclass of the `FacadeAdapter` may extend the
`handleNotification(Notification)` method to invoke the protected
`handleDependencies(Notification, BiPredicate<? super EReference, ? super EObject>)` API.
For any notification about a containment or a cross-reference and from an object that
together with the reference satisfies the predicate, will dynamically dispatch to the most
specific public method in the subclass of two forms:

* <code><i>feature</i>Added(<i>OwnerType</i>, <i>ElementType</i>)</code> — in the
  case of an element added to a reference feature
* <code><i>feature</i>Removed(<i>OwnerType</i>, <i>ElementType</i>)</code> — in the
  case of an element removed from a reference feature

where *feature* is the name of the reference feature that changed, *OwnerType* is the
type of object in which the feature changed, and *ElementType* is the type of object that
was added to or removed from the reference in the owner of the reference.  As for
synchronization methods, the most specific matching signature for both the owner type and
the type of object added/removed is selected for dispatch and it is simply a no-op if
there is no matching dependency-handler method.

It is expected that usually these dependency handlers will add/remove the façade adapter
on the related (dependency) objects to react to and synchronize changes in them using the
same dynamic dispatch scheme on the types of those related objects.

## EMF Compare Integration API

The integration API comprises primarily two extension points:  façade providers (for
comparison) and copiers (for merge), with support APIs intended for use by implementations
of these extensions.

### Bundle org.eclipse.emf.compare.facade

#### Package oecc.facade

The `IFacadeProvider` interface defines a protocol for extensions to plug in façade models
to be substituted for the raw inputs to the compare/merge operation.  It follows the
pattern of other extension SPIs in EMF Compare, such as especially the `IMatchEngine`.
Factories for façade providers are registered on the `oeec.facade.facadeProvider`
extension point with ranking to address contention for the same underlying models and
user-facing information for display in the preferences.  Most importantly, the
`IFacadeProvider::Factory` interface defines a query

> `isFactoryProviderFactoryFor(IComparisonScope) : boolean`

which the EMF Compare framework uses to determine which façade providers should be
consulted for façades wrapping the elements in the scope.  In the case that multiple
façade providers are applicable, for any given input object they are consulted in
descending ranking order until one provides a façade.

#### Package oecc.facade.internal.match [facadematcher]

![Facade Match Engine and Supporting Classes](Fa_ade_Match_Phase.SVG)

The `FacadeMatchEngine` class is the primary hook for injection of façades into a
compare/merge operation.  It wraps the default match engine and, for the incoming
`ComparisonScope`:

* looks for façade providers that can provide façades for the contents of the scope
	+ if any façade providers are found, wraps the scope in a `FacadeComparisonScope` and
	  delegates to the default match engine with that
	+ otherwise just forwards the scope as is to the default match engine, as there will
	  be no façades anyways
    
The `FacadeComparisonScope` is where façades are actually injected into the matching
process (and from thence to the rest of the compare/merge operation).  This is a wrapper
for a scope delegate (usually the `DefaultComparisonScope` that provides, with some
filtering, the raw contents of the models to be compared.  Further to this, the
`FacadeComparisonScope` applies transformations to inject façades:

* for resource sets, there is no such thing as a "façade resource", so it just discovers
  resources as per the wrapped scope
* for resources, if no façade provider provides façades for its contents, then it is
  just forwarded to the wrapped scope.  Otherwise, the available façade providers are
  used to get façades for the contents of the resource.  Any contained object for which
  there is no façade is ignored on the assumption that it is just related to some object
  that does have a façade.  For partial façades, then, it is required that the provider
  return the original input objects as though they were façades when necessary.  The
  façades thus computed are then extrapolated to their proper content trees as per the
  next item.
* for `EObject`s, the proper content tree is iterated (and filtered by the scope delegate)
  and transformed where possible to façade objects.  If a façade is obtained for some
  object in the tree, then that façade is further descended to fill out the tree.
  Otherwise, the raw model object is retained as is and the matching process descends
  into that
  
Because the façade providers found for a given comparison scope are composed in rank
order, it is normal and expected that multiple different façades could be found within
a single resource, possibly even mixed with original (non-façade) content.  This extends
also to mixing of façades and original content amongst disparate resources in the scope.
This leads to a problem for objects that should not be presented to the matching process
because they are implicitly covered by façades even though they do not, themselves, map
to any façade object.  Such is the case for UML stereotype applications, for example,
that distinguish the domain-specific concepts that are modeled by the façade and
implemented by the extended "base" element.  So, the façade provider for the domain needs
to be able to return an explicit null façade token that indicates to the match engine
that there is no object to match, without the next provider in line getting a chance to
provide a façade for it.  This token is the `FacadeObject::NULL` instance.

Another problem for the matching process is that it is typically based on object identity.
This works well for UML models created with the Eclipse UML2 implementation because that
uses unique generated XMI IDs.  However, these XMI IDs are tracked by resources and the
façade objects are not properly contained in resources.  Moreover, they pretend to the
EMF Compare framework that they are in the original input resources to provide
traceability to resources for grouping and dependency analysis.  To ensure a sensible
identifier-based matching of façade objects, then, the `FacadeMatchEngine` employs a
specialized `IEObjectMatcher`, the `FacadeIdentifierEObjectMatcher`, which for any façade 
object that doesn't have its own identifier infers one from the underlying model element
according to the usual algorithm.  In the case that this object matcher falls back on
structural comparison for matching when the underlying model does not use IDs, that
structural algorithm works the same on façades as it does on anything else because the
façades fully support the Ecore reflection that it requires.

In addition to this, the `FacadeIdentifierEObjectMatcher` infers containment for façades
that don't have any also from the underlying model, to ensure that they are integrated
at the correct point in the match tree and do not end up as top-level matches.

#### Package oecc.facade.internal.merge [facadecopier]

This package defines the `FacadeCopier` class that plugs in an `ICopier` implementation
(see [below][icopier]) for façade models.  It simply ensures two things:

* when the source object is a `FacadeObject` but the copy is not, because the façade model
  does not natively implement this interface (the `EcoreUtil::Copier` only creates an
  instance of the `EClass`'s implementation Java class)., uses the `FacadeProxy` utility
  to wrap the copy in a dynamic proxy implementing the `FacadeObject` interface
* copies XMI IDs of the underlying objects of façades instead of attempting to copy the
  XMI IDs of the façades, themselves

The copying of an object is delegated to a `FacadeCopierImpl`, which is a subclass of the
`EMFCompareCopier` for its peculiar copy semantics.

### Bundle org.eclipse.emf.compare

The preceding section discusses the injection of façades into the compare/merge operation
in the matching phase, by way of the `ComparisonScope`.  This section deals with an
expansion of the core framework's merging phase, to handle the synchronization of XMI IDs
in models that require it (especially UML).

#### Package oecc.merge [icopier]

Hitherto, the `ReferenceChangeMerger` class has hard-coded the propagation of XMI IDs
for objects copied from the source side to the result of a merge in a containment
reference.  Likewise the copying of these objects in the first place.

The introduction of façades with one-to-many mappings from a façade object to its
underlying model elements requires a refactoring of these copy functions to allow the
façade layer to plug in appropriate behaviour.  This copying protocol is specified by
a new `ICopier` interface.  Copiers are registered on the `oecc.rcp.copier` extension
point with XML enablement expression that match the model elements to which they apply
(such as, for example, instances of a particular façade model).  When a merger needs to
copy an object or propagate the XMI ID of an object to its copy, it looks up the
appropriate copier from the registry on this extension point and delegates to the copier.
There is always at least the default copier implementation that does as was previously
hard-coded in the `ReferenceChangeMerger` class (as in the Oxygen release).

The `ICopier` interface defines two operations:

* `copy(EObject) : EObject` — copies an object from the source side for insertion into
  the merge result (but doesn't actually insert it; the merger does that)
* `copyXMIIDs(EObject, EObject)` — copies the XMI IDs of an object and any related objects
  that depend on it from the original object on the source side to the copy and its
  corresponding related objects in the merge result

It is necessary to provide this hook in the copy step for the reason described above,
wrapping façade objects that do not natively implement the `FacadeObject` protocol.  The
hook for copying of XMI IDs is more obvious:  the IDs of façade objects are not
interesting because they are not persisted (indeed, they cannot even have XMI IDs because
those are maintained by the resources).  The point is that it is the underlying model
elements (for example, UML elements) that need to have their XMI IDs synchronized to
ensure sensible matching in the case that façades are not employed in another context,
and the core framework knows nothing of façades.

## Facades for UML Models

A common use case for façades is anticipated to be in DSMLs implemented in UML with
profiles.   This section discusses extensions of the core façade framework to help
with the integration of façades for UML profiles.

### Bundle org.eclipse.emf.compare.uml2.facade

#### Package oecc.uml2.facade

The `UMLFacadeAdapter` class is an abstract subclass of the `FacadeAdapter` from the
core that facilitates the synchronization of façade objects with stereotype applications.
It tracks a single optional stereotype application that distinguishes the domain concept
apart from the base UML metaclass.  When this stereotype application is present, the
façade/model synchronization is extended to include this stereotype application also,
using the same dynamic dispatch of synchronization methods as described above.  This
works best with profiles that are generated to code, so that the UML-side parameter of
the method signatures can have a more specific type than `EObject`.

An adjunct to the `UMLFacadeAdapter` is the `UMLFacadeResourceAdapter` class.  This is
responsible for discovering the addition of stereotype applications in the contents of
the UML resource to trigger creation of façade objects.  This is dispatched dynamically
to subclasses via methods of the forms

> <code>createFacade(<i>BaseType</i>, <i>Stereotype</i>)</code>

where *BaseType* is the type (metaclass) of the base UML element and *Stereotype* is the
type of the stereotype application.  It is up to these factory methods to connect the
newly created façade (if any) with its underlying UML model element and initialize it
using the `FacadeAdapter::initialSync(…)` API.

#### Package oecc.uml2.facade.merge

This package defines an `UMLFacadeCopier` class that extends the `FacadeCopier` with
flexible support for copying the XMI IDs of elements related to the principal
underlying element of the copied façade in one-to-many façade-to-UML mappings.  A special
case of these are the stereotype applications of the underlying element; these are handled
automatically because there is an uniform pattern and API to discovering them.  However,
for DSMLs the discovery of related elements in the underlying model is necessarily
domain-specific.  Subclasses can implement dynamically dispatched (as usual) methods to
supply the elements related to the principal underlying element (to discover one-to-many
mappings):

> <code>getRelatedElements(<i>FacadeType</i>, <i>ModelType</i>, <i>FacadeType</i>, <i>ModelType</i>) : Stream&lt;Pair&gt;</code>

where, as usual, *FacadeType* is the type of a façade element and *ModelType* its
principal underlying model element (UML in this case).  The first pair of arguments
are the source side (original) of the merge and the second pair are the target side
(merge result).

The `UMLFacadeCopier` provides convenience APIs for creating the pairs of
(original, copy) UML elements to have the XMI IDs copied.  These `pair(…, …)` methods
create and return a pair if both sides are present, otherwise if the related element
on the original side does not have a correspondent in the merge result, then the
mapping is deferred for later copying of XMI IDs when the copy is eventually created
in the merge result, usually indirectly by subsequent merging of properties of the
façade object.  In fact, the deferral algorithm depends on this trigger.


