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
but for the Whatsit bean (conflict)
