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

package dagger.hilt.processor.internal.aliasof;

import static com.google.auto.common.MoreElements.asType;
import static com.google.auto.common.MoreElements.isType;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.hilt.processor.internal.Processors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * A class that represents the values stored in an {@link
 * dagger.hilt.internal.aliasof.AliasOfPropagatedData} annotation.
 */
@AutoValue
abstract class AliasOfPropagatedDataMetadata {

  abstract TypeElement defineComponentScopeElement();

  abstract TypeElement aliasElement();

  static ImmutableSet<AliasOfPropagatedDataMetadata> from(Elements elements) {
    PackageElement packageElement =
        elements.getPackageElement(ClassNames.ALIAS_OF_PROPAGATED_DATA_PACKAGE);

    if (packageElement == null) {
      return ImmutableSet.of();
    }

    ImmutableSet<Element> aggregatedElements =
        ImmutableSet.copyOf(packageElement.getEnclosedElements());

    ProcessorErrors.checkState(
        !aggregatedElements.isEmpty(),
        packageElement,
        "No dependencies found. Did you remove code in package %s?",
        ClassNames.ALIAS_OF_PROPAGATED_DATA_PACKAGE);

    ImmutableSet.Builder<AliasOfPropagatedDataMetadata> builder = ImmutableSet.builder();
    for (Element element : aggregatedElements) {
      ProcessorErrors.checkState(
          isType(element),
          element,
          "Only types may be in package %s. Did you add custom code in the package?",
          ClassNames.ALIAS_OF_PROPAGATED_DATA_PACKAGE);

      builder.add(create(asType(element), elements));
    }
    return builder.build();
  }

  private static AliasOfPropagatedDataMetadata create(TypeElement element, Elements elements) {
    AnnotationMirror annotationMirror =
        Processors.getAnnotationMirror(element, ClassNames.ALIAS_OF_PROPAGATED_DATA);

    ProcessorErrors.checkState(
        annotationMirror != null,
        element,
        "Classes in package %s must be annotated with @%s: %s."
            + " Found: %s. Files in this package are generated, did you add custom code in the"
            + " package? ",
        ClassNames.ALIAS_OF_PROPAGATED_DATA_PACKAGE,
        ClassNames.ALIAS_OF_PROPAGATED_DATA,
        element.getSimpleName(),
        element.getAnnotationMirrors());

    TypeElement defineComponentScopeElement =
        Processors.getAnnotationClassValue(elements, annotationMirror, "defineComponentScope");

    TypeElement aliasElement =
        Processors.getAnnotationClassValue(elements, annotationMirror, "alias");

    return new AutoValue_AliasOfPropagatedDataMetadata(defineComponentScopeElement, aliasElement);
  }
}
