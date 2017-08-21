## Basic Facade Use Cases

These use cases cover the basic manipulation of the *façade* (in which tests
there is no actual comparing or merging being done) as well as simple
compare/merge scenarios.  All tests are based on the UML representation
of the **J2EE Façade Model**, using the **J2EE Profile** on classes and interfaces.

### Use case a1

Two-way comparison of models where the left model

* changes a bean kind

### Use case a2

Two-way comparison of models where the left model

* adds a home-interface for the bean

### Use case a3

Two-way comparison of models where the right model already has a finder
interface for the bean and the left model

* adds another finder for the bean

### Use case b1

Three-way comparison of models where the base model has no home interfaces
and

* the left model adds a home interface for the Thing bean
* the right model adds a similar home interface for the Thing bean

### Use case b2

Three-way comparison of models where the base model has no home interfaces
and

* the left model adds a home interface for the Thing bean
* the right model adds a home interface of the same name
but for the Whatsit bean (**conflict**)

### Use case f1

Simple models where the left side has a complete set of J2EE elements for
testing the *Façade Dynamic Proxy* API.

### Use case m1

Two-way comparison of mixed-mode models, with a UML design model that references a J2EE model.  The left-side models

* add another finder for a bean in the J2EE content
* change the type of a Collaboration role in the UML design model to the new finder interface

### Use case m2

Three-way comparison of mixed-mode models, with a UML design model that references a J2EE model.  The left-side models

* add another finder for a bean in the J2EE content
* change the type of a Collaboration role in the UML design model to the new finder interface

whereas the right-side models

* add a different new finder for the same bean in the J2EE content
* change the type of the same Collaboration role as the left side to the right side's new finder interface

So, there is no conflict in the J2EE content (two new finder interfaces independently added) but
there is a conflict in the UML content, where both sides change the same collaboration role to
assign a different type.

### Use case u1

A pure UML scenario (no profile, no façade) for verification that UML models
without façades are still sensibly compared.  Provides a three-way comparison
in which

* the base model has a class with an attribute of some primitive type
* the left model changes the attribute's type to some other primitive type
* the right model changes that attribute's type to some third type

## Partial Façade Use Cases

These are a suite of use cases for a partial façade, covering only the opaque expressions
of the UML metamodel.

### Use case o1

Two-way comparison of a very simple model containing an opaque expression:

* the left side adds a matched (language, body) pair to the opaque expression from the right side

### Use case o2

Three-way comparison of a very simple model containing an opaque expression with a
conflict in the value of the expression body of an opaque-expression:

* the left side changes the body of the expression for some language
* the right side changes the body of the expression for the same language as the
  left, but to a different value.  Conflict

### Use case o3

Three-way comparison of a very simple model containing an opaque expression with an
add-add conflict in the bodies of an opaque-expression:

* the left side adds a body for some language, in some position in the list
* the right side adds a different body for the same language, in a different position
  in the list.  Conflict

### Use case o4

Three-way comparison of a very simple model containing an opaque expression with an
add-add conflict in the bodies of an opaque-expression:

* the left side adds a body for some language, in some position in the list
* the right side adds a different body for the same language, in the same position
  in the list.  Conflict

This use case exists despite its apparent redundance with **o3** because, at the time
of its definition, this use case actually resulted only in a pseudo-conflict, despite
that left and right values for the same language are different
