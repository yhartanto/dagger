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

package dagger.hilt.processor.internal.uninstallmodules;

import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;

import com.google.auto.common.MoreElements;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.processor.internal.AnnotationValues;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.hilt.processor.internal.Processors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * A class that represents the values stored in an
 * {@link dagger.hilt.android.internal.uninstallmodules.AggregatedUninstallModules} annotation.
 */
@AutoValue
public abstract class AggregatedUninstallModulesMetadata {

  /** Returns the test annotated with {@link dagger.hilt.android.testing.UninstallModules}. */
  public abstract TypeElement testElement();

  /**
   * Returns the list of uninstall modules in {@link dagger.hilt.android.testing.UninstallModules}.
   */
  public abstract ImmutableList<TypeElement> uninstallModuleElements();

  /** Returns all aggregated deps in the aggregating package mapped by the top-level element. */
  public static ImmutableSet<AggregatedUninstallModulesMetadata> from(Elements elements) {
    PackageElement packageElement =
        elements.getPackageElement(ClassNames.AGGREGATED_UNINSTALL_MODULES_PACKAGE);

    if (packageElement == null) {
      return ImmutableSet.of();
    }

    ImmutableSet<Element> aggregatedElements =
        ImmutableSet.copyOf(packageElement.getEnclosedElements());

    ProcessorErrors.checkState(
        !aggregatedElements.isEmpty(),
        packageElement,
        "No dependencies found. Did you remove code in package %s?",
        ClassNames.AGGREGATED_UNINSTALL_MODULES_PACKAGE);

    ImmutableSet.Builder<AggregatedUninstallModulesMetadata> builder = ImmutableSet.builder();
    for (Element element : aggregatedElements) {
      ProcessorErrors.checkState(
          element.getKind() == ElementKind.CLASS,
          element,
          "Only classes may be in package %s. Did you add custom code in the package?",
          ClassNames.AGGREGATED_UNINSTALL_MODULES_PACKAGE);

      builder.add(create(MoreElements.asType(element), elements));
    }

    return builder.build();
  }

  private static AggregatedUninstallModulesMetadata create(TypeElement element, Elements elements) {
    AnnotationMirror annotationMirror =
        Processors.getAnnotationMirror(element, ClassNames.AGGREGATED_UNINSTALL_MODULES);

    ProcessorErrors.checkState(
        annotationMirror != null,
        element,
        "Classes in package %s must be annotated with @%s: %s. Found: %s.",
        ClassNames.AGGREGATED_UNINSTALL_MODULES_PACKAGE,
        ClassNames.AGGREGATED_UNINSTALL_MODULES,
        element.getSimpleName(),
        element.getAnnotationMirrors());

    ImmutableMap<String, AnnotationValue> values =
          Processors.getAnnotationValues(elements, annotationMirror);

    TypeElement testElement =
        elements.getTypeElement(AnnotationValues.getString(values.get("test")));

    ImmutableList<TypeElement> uninstallModuleElements =
        AnnotationValues.getAnnotationValues(values.get("uninstallModules")).stream()
            .map(AnnotationValues::getString)
            .map(elements::getTypeElement)
            .collect(toImmutableList());

    return new AutoValue_AggregatedUninstallModulesMetadata(testElement, uninstallModuleElements);
  }
}
