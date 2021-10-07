/*
 * Copyright (C) 2019 The Dagger Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dagger.internal.codegen.base;

import static androidx.room.compiler.processing.compat.XConverters.toJavac;
import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.auto.common.MoreTypes.asTypeElements;
import static com.google.common.base.Preconditions.checkState;
import static dagger.internal.codegen.base.MoreAnnotationValues.asAnnotationValues;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;
import static dagger.internal.codegen.langmodel.DaggerElements.getAnyAnnotation;

import androidx.room.compiler.processing.XAnnotation;
import androidx.room.compiler.processing.XElement;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import dagger.internal.codegen.javapoet.TypeNames;
import java.util.Collection;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * A {@code @Component}, {@code @Subcomponent}, {@code @ProductionComponent}, or
 * {@code @ProductionSubcomponent} annotation, or a {@code @Module} or {@code @ProducerModule}
 * annotation that is being treated as a component annotation when validating full binding graphs
 * for modules.
 */
@AutoValue
public abstract class ComponentAnnotation {
  /** The root component annotation types. */
  private static final ImmutableSet<ClassName> ROOT_COMPONENT_ANNOTATIONS =
      ImmutableSet.of(TypeNames.COMPONENT, TypeNames.PRODUCTION_COMPONENT);

  /** The subcomponent annotation types. */
  private static final ImmutableSet<ClassName> SUBCOMPONENT_ANNOTATIONS =
      ImmutableSet.of(TypeNames.SUBCOMPONENT, TypeNames.PRODUCTION_SUBCOMPONENT);

  // TODO(erichang): Move ComponentCreatorAnnotation into /base and use that here?
  /** The component/subcomponent creator annotation types. */
  private static final ImmutableSet<ClassName> CREATOR_ANNOTATIONS =
      ImmutableSet.of(
          TypeNames.COMPONENT_BUILDER,
          TypeNames.COMPONENT_FACTORY,
          TypeNames.PRODUCTION_COMPONENT_BUILDER,
          TypeNames.PRODUCTION_COMPONENT_FACTORY,
          TypeNames.SUBCOMPONENT_BUILDER,
          TypeNames.SUBCOMPONENT_FACTORY,
          TypeNames.PRODUCTION_SUBCOMPONENT_BUILDER,
          TypeNames.PRODUCTION_SUBCOMPONENT_FACTORY);

  /** All component annotation types. */
  private static final ImmutableSet<ClassName> ALL_COMPONENT_ANNOTATIONS =
      ImmutableSet.<ClassName>builder()
          .addAll(ROOT_COMPONENT_ANNOTATIONS)
          .addAll(SUBCOMPONENT_ANNOTATIONS)
          .build();

  /** All component and creator annotation types. */
  private static final ImmutableSet<ClassName> ALL_COMPONENT_AND_CREATOR_ANNOTATIONS =
      ImmutableSet.<ClassName>builder()
          .addAll(ALL_COMPONENT_ANNOTATIONS)
          .addAll(CREATOR_ANNOTATIONS)
          .build();

  /** All production annotation types. */
  private static final ImmutableSet<ClassName> PRODUCTION_ANNOTATIONS =
      ImmutableSet.of(
          TypeNames.PRODUCTION_COMPONENT,
          TypeNames.PRODUCTION_SUBCOMPONENT,
          TypeNames.PRODUCER_MODULE);

  /** The annotation itself. */
  public abstract AnnotationMirror annotation();

  /** The simple name of the annotation type. */
  public final String simpleName() {
    return annotationClassName().simpleName();
  }

  /**
   * Returns {@code true} if the annotation is a {@code @Subcomponent} or
   * {@code @ProductionSubcomponent}.
   */
  public final boolean isSubcomponent() {
    return SUBCOMPONENT_ANNOTATIONS.contains(annotationClassName());
  }

  /**
   * Returns {@code true} if the annotation is a {@code @ProductionComponent},
   * {@code @ProductionSubcomponent}, or {@code @ProducerModule}.
   */
  public final boolean isProduction() {
    return PRODUCTION_ANNOTATIONS.contains(annotationClassName());
  }

  /**
   * Returns {@code true} if the annotation is a real component annotation and not a module
   * annotation.
   */
  public final boolean isRealComponent() {
    return ALL_COMPONENT_ANNOTATIONS.contains(annotationClassName());
  }

  /** The values listed as {@code dependencies}. */
  @Memoized
  public ImmutableList<AnnotationValue> dependencyValues() {
    return isRootComponent() ? getAnnotationValues("dependencies") : ImmutableList.of();
  }

  /** The types listed as {@code dependencies}. */
  @Memoized
  public ImmutableList<TypeMirror> dependencyTypes() {
    return dependencyValues().stream().map(MoreAnnotationValues::asType).collect(toImmutableList());
  }

  /**
   * The types listed as {@code dependencies}.
   *
   * @throws IllegalArgumentException if any of {@link #dependencyTypes()} are error types
   */
  @Memoized
  public ImmutableList<TypeElement> dependencies() {
    return asTypeElements(dependencyTypes()).asList();
  }

  /** The values listed as {@code modules}. */
  @Memoized
  public ImmutableList<AnnotationValue> moduleValues() {
    return getAnnotationValues(isRealComponent() ? "modules" : "includes");
  }

  /** The types listed as {@code modules}. */
  @Memoized
  public ImmutableList<TypeMirror> moduleTypes() {
    return moduleValues().stream().map(MoreAnnotationValues::asType).collect(toImmutableList());
  }

  /**
   * The types listed as {@code modules}.
   *
   * @throws IllegalArgumentException if any of {@link #moduleTypes()} are error types
   */
  @Memoized
  public ImmutableSet<TypeElement> modules() {
    return asTypeElements(moduleTypes());
  }

  private ImmutableList<AnnotationValue> getAnnotationValues(String parameterName) {
    return asAnnotationValues(getAnnotationValue(annotation(), parameterName));
  }

  private final boolean isRootComponent() {
    return ROOT_COMPONENT_ANNOTATIONS.contains(annotationClassName());
  }

  private ClassName annotationClassName() {
    return ClassName.get(asTypeElement(annotation().getAnnotationType()));
  }

  /**
   * Returns an object representing a root component annotation, not a subcomponent annotation, if
   * one is present on {@code typeElement}.
   */
  public static Optional<ComponentAnnotation> rootComponentAnnotation(TypeElement typeElement) {
    return anyComponentAnnotation(typeElement, ROOT_COMPONENT_ANNOTATIONS);
  }

  /**
   * Returns an object representing a subcomponent annotation, if one is present on {@code
   * typeElement}.
   */
  public static Optional<ComponentAnnotation> subcomponentAnnotation(TypeElement typeElement) {
    return anyComponentAnnotation(typeElement, SUBCOMPONENT_ANNOTATIONS);
  }

  /**
   * Returns an object representing a root component or subcomponent annotation, if one is present
   * on {@code typeElement}.
   */
  public static Optional<ComponentAnnotation> anyComponentAnnotation(XElement element) {
    return anyComponentAnnotation(toJavac(element), ALL_COMPONENT_ANNOTATIONS);
  }

  private static Optional<ComponentAnnotation> anyComponentAnnotation(
      Element element, Collection<ClassName> annotations) {
    return getAnyAnnotation(element, annotations).map(ComponentAnnotation::componentAnnotation);
  }

  /** Returns {@code true} if the argument is a component annotation. */
  public static boolean isComponentAnnotation(AnnotationMirror annotation) {
    ClassName className = ClassName.get(asTypeElement(annotation.getAnnotationType()));
    return ALL_COMPONENT_ANNOTATIONS.contains(className);
  }

  /** Creates an object representing a component or subcomponent annotation. */
  public static ComponentAnnotation componentAnnotation(XAnnotation annotation) {
    return componentAnnotation(toJavac(annotation));
  }

  /** Creates an object representing a component or subcomponent annotation. */
  public static ComponentAnnotation componentAnnotation(AnnotationMirror annotation) {
    checkState(
        isComponentAnnotation(annotation),
        annotation
            + " must be a Component, Subcomponent, ProductionComponent, "
            + "or ProductionSubcomponent annotation");
    return new AutoValue_ComponentAnnotation(annotation);
  }

  /** Creates a fictional component annotation representing a module. */
  public static ComponentAnnotation fromModuleAnnotation(ModuleAnnotation moduleAnnotation) {
    return new AutoValue_ComponentAnnotation(moduleAnnotation.annotation());
  }

  /** The root component annotation types. */
  public static ImmutableSet<ClassName> rootComponentAnnotations() {
    return ROOT_COMPONENT_ANNOTATIONS;
  }

  /** The subcomponent annotation types. */
  public static ImmutableSet<ClassName> subcomponentAnnotations() {
    return SUBCOMPONENT_ANNOTATIONS;
  }

  /** All component annotation types. */
  public static ImmutableSet<ClassName> allComponentAnnotations() {
    return ALL_COMPONENT_ANNOTATIONS;
  }

  /** All component and creator annotation types. */
  public static ImmutableSet<ClassName> allComponentAndCreatorAnnotations() {
    return ALL_COMPONENT_AND_CREATOR_ANNOTATIONS;
  }
}
