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

package dagger.hilt.processor.internal.root;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreElements.isType;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.processor.internal.AnnotationValues;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.hilt.processor.internal.Processors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Represents the values stored in an {@link dagger.hilt.internal.aggregatedroot.AggregatedRoot}.
 */
@AutoValue
abstract class AggregatedRootMetadata {

  /** Returns the element that was annotated with the root annotation. */
  abstract TypeElement rootElement();

  /** Returns the root annotation as an element. */
  abstract TypeElement rootAnnotation();

  static ImmutableSet<AggregatedRootMetadata> from(Elements elements) {
    PackageElement packageElement = elements.getPackageElement(ClassNames.AGGREGATED_ROOT_PACKAGE);

    if (packageElement == null) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<AggregatedRootMetadata> builder = ImmutableSet.builder();
    for (Element element : packageElement.getEnclosedElements()) {
      ProcessorErrors.checkState(
          isType(element),
          element,
          "Only types may be in package %s. Did you add custom code in the package?",
          ClassNames.AGGREGATED_ROOT_PACKAGE);

      builder.add(create(asType(element), elements));
    }

    return builder.build();
  }

  private static AggregatedRootMetadata create(TypeElement element, Elements elements) {
    AnnotationMirror annotationMirror =
        Processors.getAnnotationMirror(element, ClassNames.AGGREGATED_ROOT);

    ProcessorErrors.checkState(
        annotationMirror != null,
        element,
        "Classes in package %s must be annotated with @%s: %s. Found: %s.",
        ClassNames.AGGREGATED_ROOT_PACKAGE,
        ClassNames.AGGREGATED_ROOT,
        element.getSimpleName(),
        element.getAnnotationMirrors());

    ImmutableMap<String, AnnotationValue> values =
        Processors.getAnnotationValues(elements, annotationMirror);

    return new AutoValue_AggregatedRootMetadata(
        elements.getTypeElement(AnnotationValues.getString(values.get("root"))),
        AnnotationValues.getTypeElement(values.get("rootAnnotation")));
  }
}
