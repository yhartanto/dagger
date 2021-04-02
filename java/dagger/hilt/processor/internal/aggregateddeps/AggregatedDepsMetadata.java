/*
 * Copyright (C) 2021 The Dagger Authors.
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

package dagger.hilt.processor.internal.aggregateddeps;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;

import com.google.auto.common.MoreElements;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.processor.internal.AnnotationValues;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.hilt.processor.internal.Processors;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * A class that represents the values stored in an {@link
 * dagger.hilt.processor.internal.aggregateddeps.AggregatedDeps} annotation.
 */
@AutoValue
abstract class AggregatedDepsMetadata {
  private static final String AGGREGATED_DEPS_PACKAGE = "hilt_aggregated_deps";

  enum DependencyType {
    MODULE,
    ENTRY_POINT,
    COMPONENT_ENTRY_POINT
  }

  abstract Optional<TypeElement> testElement();

  abstract ImmutableSet<TypeElement> componentElements();

  abstract DependencyType dependencyType();

  abstract TypeElement dependency();

  abstract ImmutableSet<TypeElement> replacedDependencies();

  /** Returns all aggregated deps in the aggregating package. */
  public static ImmutableSet<AggregatedDepsMetadata> from(Elements elements) {
    PackageElement packageElement = elements.getPackageElement(AGGREGATED_DEPS_PACKAGE);
    checkState(
        packageElement != null,
        "Couldn't find package %s. Did you mark your @Module classes with @InstallIn annotations?",
        AGGREGATED_DEPS_PACKAGE);

    List<? extends Element> aggregatedDepsElements = packageElement.getEnclosedElements();
    checkState(
        !aggregatedDepsElements.isEmpty(),
        "No dependencies found. Did you mark your @Module classes with @InstallIn annotations?");

    ImmutableSet.Builder<AggregatedDepsMetadata> builder = ImmutableSet.builder();
    for (Element element : aggregatedDepsElements) {
      ProcessorErrors.checkState(
          element.getKind() == ElementKind.CLASS,
          element,
          "Only classes may be in package %s. Did you add custom code in the package?",
          AGGREGATED_DEPS_PACKAGE);

      builder.add(create(MoreElements.asType(element), elements));
    }
    return builder.build();
  }

  private static AggregatedDepsMetadata create(TypeElement element, Elements elements) {
    AnnotationMirror aggregatedDeps =
        Processors.getAnnotationMirror(element, ClassNames.AGGREGATED_DEPS);

    ProcessorErrors.checkState(
        aggregatedDeps != null,
        element,
        "Classes in package %s must be annotated with @AggregatedDeps: %s. Found: %s.",
        AGGREGATED_DEPS_PACKAGE,
        element.getSimpleName(),
        element.getAnnotationMirrors());

    ImmutableMap<String, AnnotationValue> aggregatedDepsValues =
        Processors.getAnnotationValues(elements, aggregatedDeps);

    return new AutoValue_AggregatedDepsMetadata(
        getTestElement(aggregatedDepsValues.get("test"), elements),
        getComponents(aggregatedDepsValues.get("components"), elements),
        getDependencyType(
            aggregatedDepsValues.get("modules"),
            aggregatedDepsValues.get("entryPoints"),
            aggregatedDepsValues.get("componentEntryPoints")),
        getDependency(
            aggregatedDepsValues.get("modules"),
            aggregatedDepsValues.get("entryPoints"),
            aggregatedDepsValues.get("componentEntryPoints"),
            elements),
        getReplacedDependencies(aggregatedDepsValues.get("replaces"), elements));
  }

  private static Optional<TypeElement> getTestElement(
      AnnotationValue testValue, Elements elements) {
    checkNotNull(testValue);
    String test = AnnotationValues.getString(testValue);
    return test.isEmpty() ? Optional.empty() : Optional.of(elements.getTypeElement(test));
  }

  private static ImmutableSet<TypeElement> getComponents(
      AnnotationValue componentsValue, Elements elements) {
    checkNotNull(componentsValue);
    ImmutableSet<TypeElement> componentNames =
        AnnotationValues.getAnnotationValues(componentsValue).stream()
            .map(AnnotationValues::getString)
            .map(
                // This is a temporary hack to map the old ApplicationComponent to the new
                // SingletonComponent. Technically, this is only needed for backwards compatibility
                // with libraries using the old processor since new processors should convert to the
                // new SingletonComponent when generating the metadata class.
                componentName ->
                    componentName.contentEquals(
                            "dagger.hilt.android.components.ApplicationComponent")
                        ? ClassNames.SINGLETON_COMPONENT.canonicalName()
                        : componentName)
            .map(elements::getTypeElement)
            .collect(toImmutableSet());
    checkState(!componentNames.isEmpty());
    return componentNames;
  }

  private static DependencyType getDependencyType(
      AnnotationValue modulesValue,
      AnnotationValue entryPointsValue,
      AnnotationValue componentEntryPointsValue) {
    checkNotNull(modulesValue);
    checkNotNull(entryPointsValue);
    checkNotNull(componentEntryPointsValue);

    ImmutableSet.Builder<DependencyType> dependencyTypes = ImmutableSet.builder();
    if (!AnnotationValues.getAnnotationValues(modulesValue).isEmpty()) {
      dependencyTypes.add(DependencyType.MODULE);
    }
    if (!AnnotationValues.getAnnotationValues(entryPointsValue).isEmpty()) {
      dependencyTypes.add(DependencyType.ENTRY_POINT);
    }
    if (!AnnotationValues.getAnnotationValues(componentEntryPointsValue).isEmpty()) {
      dependencyTypes.add(DependencyType.COMPONENT_ENTRY_POINT);
    }
    return getOnlyElement(dependencyTypes.build());
  }

  private static TypeElement getDependency(
      AnnotationValue modulesValue,
      AnnotationValue entryPointsValue,
      AnnotationValue componentEntryPointsValue,
      Elements elements) {
    checkNotNull(modulesValue);
    checkNotNull(entryPointsValue);
    checkNotNull(componentEntryPointsValue);

    return elements.getTypeElement(
        AnnotationValues.getString(
            getOnlyElement(
                ImmutableSet.<AnnotationValue>builder()
                    .addAll(AnnotationValues.getAnnotationValues(modulesValue))
                    .addAll(AnnotationValues.getAnnotationValues(entryPointsValue))
                    .addAll(AnnotationValues.getAnnotationValues(componentEntryPointsValue))
                    .build())));
  }

  private static ImmutableSet<TypeElement> getReplacedDependencies(
      AnnotationValue replacedDependenciesValue, Elements elements) {
    // Allow null values to support libraries using a Hilt version before @TestInstallIn was added
    return replacedDependenciesValue == null
        ? ImmutableSet.of()
        : AnnotationValues.getAnnotationValues(replacedDependenciesValue).stream()
            .map(AnnotationValues::getString)
            .map(elements::getTypeElement)
            .map(replacedDep -> getPublicDependency(replacedDep, elements))
            .collect(toImmutableSet());
  }

  /** Returns the public Hilt wrapper module, or the module itself if its already public. */
  private static TypeElement getPublicDependency(TypeElement dependency, Elements elements) {
    return PkgPrivateMetadata.of(elements, dependency, ClassNames.MODULE)
        .map(metadata -> elements.getTypeElement(metadata.generatedClassName().toString()))
        .orElse(dependency);
  }
}
