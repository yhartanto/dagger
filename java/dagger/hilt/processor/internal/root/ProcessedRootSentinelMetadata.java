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
import com.google.common.collect.ImmutableSet;
import dagger.hilt.processor.internal.AnnotationValues;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.hilt.processor.internal.Processors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Represents the values stored in an
 * {@link dagger.hilt.internal.processedrootsentinel.ProcessedRootSentinel}.
 */
@AutoValue
abstract class ProcessedRootSentinelMetadata {

  /** Returns the processed root elements. */
  abstract ImmutableSet<TypeElement> rootElements();

  static ImmutableSet<ProcessedRootSentinelMetadata> from(Elements elements) {
    PackageElement packageElement =
        elements.getPackageElement(ClassNames.PROCESSED_ROOT_SENTINEL_PACKAGE);

    if (packageElement == null) {
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<ProcessedRootSentinelMetadata> builder = ImmutableSet.builder();
    for (Element element : packageElement.getEnclosedElements()) {
      ProcessorErrors.checkState(
          isType(element),
          element,
          "Only types may be in package %s. Did you add custom code in the package?",
          ClassNames.PROCESSED_ROOT_SENTINEL_PACKAGE);

      builder.add(create(asType(element), elements));
    }

    return builder.build();
  }

  private static ProcessedRootSentinelMetadata create(TypeElement element, Elements elements) {
    AnnotationMirror annotationMirror =
        Processors.getAnnotationMirror(element, ClassNames.PROCESSED_ROOT_SENTINEL);

    ProcessorErrors.checkState(
        annotationMirror != null,
        element,
        "Classes in package %s must be annotated with @%s: %s. Found: %s.",
        ClassNames.PROCESSED_ROOT_SENTINEL_PACKAGE,
        ClassNames.PROCESSED_ROOT_SENTINEL,
        element.getSimpleName(),
        element.getAnnotationMirrors());

    ImmutableSet.Builder<TypeElement> builder = ImmutableSet.builder();
    for (String root : AnnotationValues.getStringArrayValue(annotationMirror, "roots")) {
      TypeElement rootElement = elements.getTypeElement(root);
      builder.add(rootElement);
    }

    return new AutoValue_ProcessedRootSentinelMetadata(builder.build());
  }
}
