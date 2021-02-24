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

package dagger.hilt.android.processor.internal.earlyentrypoint;

import static com.google.auto.common.MoreElements.asType;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;
import static net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import dagger.hilt.processor.internal.BaseProcessor;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ComponentDescriptor;
import dagger.hilt.processor.internal.Components;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.hilt.processor.internal.Processors;
import java.util.Optional;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;

/** Processes usages of {@link dagger.hilt.android.EarlyEntryPoint}. */
@IncrementalAnnotationProcessor(ISOLATING)
@AutoService(Processor.class)
public final class EarlyEntryPointProcessor extends BaseProcessor {

  @Override
  public ImmutableSet<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(ClassNames.EARLY_ENTRY_POINT.toString());
  }

  @Override
  public void processEach(TypeElement annotation, Element element) {
    ProcessorErrors.checkState(
        Processors.hasAnnotation(element, ClassNames.ENTRY_POINT),
        element,
        "@EarlyEntryPoint must be used with @EntryPoint");

    // There should always be an @InstallIn with @EntryPoint, but we let the AggregatedDepsProcessor
    // handle the case when it doesn't exist.
    // TODO(bcorso): Consider moving this logic into AggregatedDepsProcessor
    if (Processors.hasAnnotation(element, ClassNames.INSTALL_IN)) {
      ImmutableSet<ClassName> components =
          Components.getComponentDescriptors(getElementUtils(), element).stream()
              .map(ComponentDescriptor::component)
              .collect(toImmutableSet());

      ProcessorErrors.checkState(
          components.equals(ImmutableSet.of(ClassNames.SINGLETON_COMPONENT)),
          element,
          "@EarlyEntryPoint can only be installed into the SingletonComponent. Found: %s",
          components);

      Optional<TypeElement> optionalTestElement =
          Processors.getOriginatingTestElement(element, getElementUtils());
      ProcessorErrors.checkState(
          !optionalTestElement.isPresent(),
          element,
          "@EarlyEntryPoint-annotated entry point, %s, cannot be nested in (or originate from) "
              + "a @HiltAndroidTest-annotated class, %s. This requirement is to avoid confusion "
              + "with other, test-specific entry points.",
          asType(element).getQualifiedName().toString(),
          optionalTestElement
              .map(testElement -> testElement.getQualifiedName().toString())
              .orElse(""));
    }
  }
}
