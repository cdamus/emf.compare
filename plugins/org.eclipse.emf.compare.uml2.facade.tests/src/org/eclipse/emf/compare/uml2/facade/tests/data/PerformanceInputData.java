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
package org.eclipse.emf.compare.uml2.facade.tests.data;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Iterator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;
import org.eclipse.emf.compare.utils.TreeIterators;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.uml2.uml.Constraint;
import org.eclipse.uml2.uml.OpaqueExpression;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * Dynamically-generated input data for performance tests. The idea being not to bother with storage of large
 * models that can be generated because it doesn't matter the particular content.
 *
 * @author Christian W. Damus
 */
@SuppressWarnings({"nls", "boxing" })
public class PerformanceInputData extends AbstractUMLInputData {

	public org.eclipse.uml2.uml.Package getModel(Resource umlResource) {
		org.eclipse.uml2.uml.Package result = (org.eclipse.uml2.uml.Package)EcoreUtil
				.getObjectByType(umlResource.getContents(), UMLPackage.Literals.PACKAGE);

		assertThat(result, notNullValue());

		return result;
	}

	public Resource getP1Right() {
		Resource result = createUMLResource("p1");

		org.eclipse.uml2.uml.Package model = UMLFactory.eINSTANCE.createPackage();
		model.setName("p1");
		result.getContents().add(model);

		generateOpaqueExpressions(model, 29, 2);
		setSerialEObjectIds(result);

		return result;
	}

	public Resource getP1Left() {
		Resource result = getP1Right();

		org.eclipse.uml2.uml.Package model = getModel(result);

		addOpaqueExpressionBody(model, 1, "Java", "this.isOK()");
		// The IDs of new elements don't matter because they don't have to correspond to
		// anything that existed before
		return result;
	}

	protected Resource createUMLResource(String name) {
		ResourceSet rset = createResourceSet();
		Resource result = rset.createResource(URI.createURI(String.format("bogus://%s.uml", name)),
				UMLPackage.eCONTENT_TYPE);
		return result;
	}

	//
	// Model generation
	//

	void setSerialEObjectIds(Resource resource) {
		long nextID = 0L;

		XMLResource xml = (XMLResource)resource;
		for (Iterator<EObject> iter = EcoreUtil.getAllProperContents(resource, false); iter.hasNext();) {
			xml.setID(iter.next(), Long.toHexString(nextID++));
		}
	}

	/**
	 * Generate a model containing opaque expressions. The number of opaque expressions generated scales as
	 * the {@code depth} power of {@code breadth}.
	 * 
	 * @param model
	 *            the model in which to put opaque expressions
	 * @param breadth
	 *            the number of opaque expressions to add at each level of packages in the model
	 * @param depth
	 *            the number of levels deep of packages to generate containing opaque expressions
	 */
	void generateOpaqueExpressions(org.eclipse.uml2.uml.Package model, int breadth, int depth) {
		for (int i = 0; i < breadth; i++) {
			Constraint constraint = model.createOwnedRule(format("constraint_%d", i));
			OpaqueExpression expr = (OpaqueExpression)constraint.createSpecification(null, null,
					UMLPackage.Literals.OPAQUE_EXPRESSION);

			expr.getLanguages().add("OCL");
			expr.getBodies().add("self.ok");
			expr.getLanguages().add("English");
			expr.getBodies().add("All is well.");

			if (depth > 1) {
				org.eclipse.uml2.uml.Package nested = model
						.createNestedPackage(format("%s_%d", model.getName(), i));
				generateOpaqueExpressions(nested, breadth, depth - 1);
			}
		}
	}

	/**
	 * Adds a body to every opaque expression in a {@code model}.
	 * 
	 * @param model
	 *            the model to add to
	 * @param index
	 *            the index in every opaque expression at which to add the {@code body}. Opaque expressions
	 *            that don't have enough bodies already are just appended
	 * @param language
	 *            the language of the {@code body} to add
	 * @param body
	 *            the body to add
	 */
	void addOpaqueExpressionBody(org.eclipse.uml2.uml.Package model, int index, String language,
			String body) {

		TreeIterators.filter(model.eAllContents(), OpaqueExpression.class).forEachRemaining(expr -> {
			if (expr.getBodies().size() >= index) {
				expr.getLanguages().add(index, language);
				expr.getBodies().add(index, body);
			} else {
				expr.getLanguages().add(language);
				expr.getBodies().add(body);
			}
		});
	}

}
