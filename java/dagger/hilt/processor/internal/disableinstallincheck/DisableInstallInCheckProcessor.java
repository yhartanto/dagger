/*
 * Copyright (C) 2020 The Dagger Authors.
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

package dagger.hilt.processor.internal.disableinstallincheck;

import static net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING;

import androidx.room.compiler.processing.XElement;
import androidx.room.compiler.processing.XTypeElement;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.processor.internal.BaseProcessor;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ProcessorErrors;
import dagger.internal.codegen.xprocessing.XElements;
import javax.annotation.processing.Processor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;

/** Processes the annotations annotated with {@link dagger.hilt.migration.DisableInstallInCheck} */
@IncrementalAnnotationProcessor(ISOLATING)
@AutoService(Processor.class)
public final class DisableInstallInCheckProcessor extends BaseProcessor {
  @Override
  public ImmutableSet<String> getSupportedAnnotationTypes() {
    return ImmutableSet.of(ClassNames.DISABLE_INSTALL_IN_CHECK.toString());
  }

  @Override
  public void processEach(XTypeElement annotation, XElement element) {
    ProcessorErrors.checkState(
        element.hasAnnotation(ClassNames.MODULE),
        element,
        "@DisableInstallInCheck should only be used on modules. However, it was found annotating"
            + " %s",
        XElements.toStableString(element));
  }
}
