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
package org.eclipse.emf.compare.uml2.facade.tests.annotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

/**
 * This is the {@code InputDataGenerator} type. Enjoy.
 *
 * @author Christian W. Damus
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("org.eclipse.emf.compare.uml2.facade.tests.data.Input")
public class InputDataGenerator extends AbstractProcessor {

	/**
	 * Initializes me.
	 */
	public InputDataGenerator() {
		super();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.isEmpty()) {
			return false;
		}

		TypeElement inputType = annotations.iterator().next();
		Types types = processingEnv.getTypeUtils();

		for (TypeElement type : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(inputType))) {

			AnnotationMirror facadeInput = type.getAnnotationMirrors().stream()
					.filter(m -> types.asElement(m.getAnnotationType()).equals(inputType)).findFirst().get();

			PackageElement package_ = (PackageElement)type.getEnclosingElement();
			String typeQName = type.getQualifiedName().toString();
			String typeName = type.getSimpleName().toString();
			String genQName = typeQName + "Gen"; //$NON-NLS-1$
			String genName = typeName + "Gen"; //$NON-NLS-1$

			try {
				processingEnv.getMessager().printMessage(Kind.NOTE, "Generating " + genQName, type); //$NON-NLS-1$
				JavaFileObject file = processingEnv.getFiler().createSourceFile(genQName, type);
				try (PrintWriter output = new PrintWriter(file.openWriter())) {
					output.printf("package %s;%n", package_.getQualifiedName()); //$NON-NLS-1$
					output.println();
					output.println("import static org.junit.Assert.fail;"); //$NON-NLS-1$
					output.println();
					output.println("import java.io.IOException;"); //$NON-NLS-1$
					output.println("import javax.annotation.Generated;"); //$NON-NLS-1$
					output.println("import org.eclipse.emf.ecore.resource.Resource;"); //$NON-NLS-1$
					if (!package_.getQualifiedName().contentEquals("org.eclipse.emf.compare.uml2.tests")) { //$NON-NLS-1$
						output.println("import org.eclipse.emf.compare.uml2.tests.AbstractUMLInputData;"); //$NON-NLS-1$
					}
					output.println();
					output.printf("@Generated(date=\"%s\", value=\"%s\")%n", //$NON-NLS-1$
							DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
							getClass().getName());
					output.printf("public abstract class %s extends AbstractUMLInputData {%n%n", genName); //$NON-NLS-1$

					output.printf("\tprotected %s() {%n", genName); //$NON-NLS-1$
					output.println("\t\tsuper();"); //$NON-NLS-1$
					output.println("\t}"); //$NON-NLS-1$
					output.println();

					// This annotation has only one 'value' attribute
					AnnotationValue value = facadeInput.getElementValues().values().iterator().next();
					value.accept(resourceAccessorGenerator(), output);

					output.println("}"); //$NON-NLS-1$
				}
			} catch (IOException e) {
				processingEnv.getMessager().printMessage(Kind.ERROR,
						String.format("Failed to generate %s: %s", genName, e.getLocalizedMessage()), type); //$NON-NLS-1$
			}
		}

		return true;
	}

	static String cap(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}

	protected AnnotationValueVisitor<Void, PrintWriter> resourceAccessorGenerator() {
		return new SimpleAnnotationValueVisitor6<Void, PrintWriter>() {
			@Override
			public Void visitArray(List<? extends AnnotationValue> vals, PrintWriter out) {
				vals.forEach(v -> visit(v, out));
				return null;
			}

			@Override
			public Void visitString(String scenarioName, PrintWriter out) {
				generateAccessor(out, scenarioName, "left"); //$NON-NLS-1$
				generateAccessor(out, scenarioName, "right"); //$NON-NLS-1$
				generateAccessor(out, scenarioName, "base"); //$NON-NLS-1$
				return null;
			}
		};
	}

	protected void generateAccessor(PrintWriter output, String scenarioName, String side) {
		output.printf("\tpublic Resource get%s%s() {%n", cap(scenarioName), cap(side)); //$NON-NLS-1$
		output.println("\t\ttry {"); //$NON-NLS-1$
		output.printf("\t\t\treturn loadFromClassLoader(\"%s/%s.uml\"); //$NON-NLS-1$%n", scenarioName, //$NON-NLS-1$
				side);
		output.println("\t\t} catch (IOException e) {"); //$NON-NLS-1$
		output.println("\t\t\te.printStackTrace();"); //$NON-NLS-1$
		output.printf("\t\t\tfail(\"Failed to load test resource %s/%s.uml\"); //$NON-NLS-1$%n", //$NON-NLS-1$
				scenarioName, side);
		output.println("\t\t\treturn null; // Unreachable"); //$NON-NLS-1$
		output.println("\t\t}"); //$NON-NLS-1$
		output.println("\t}"); //$NON-NLS-1$
		output.println();
	}
}
