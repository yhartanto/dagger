/*
 * Copyright (C) 2018 The Dagger Authors.
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

package dagger.internal.codegen.binding;

import static androidx.room.compiler.processing.compat.XConverters.toXProcessing;
import static com.google.common.collect.Iterables.getOnlyElement;
import static dagger.internal.codegen.extension.DaggerStreams.presentValues;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;
import static java.util.stream.Collectors.joining;

import androidx.room.compiler.processing.XProcessingEnv;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import dagger.spi.model.BindingGraph.SubcomponentCreatorBindingEdge;
import dagger.spi.model.DaggerTypeElement;

/** An implementation of {@link SubcomponentCreatorBindingEdge}. */
public final class SubcomponentCreatorBindingEdgeImpl implements SubcomponentCreatorBindingEdge {

  private final XProcessingEnv processingEnv;
  private final ImmutableSet<SubcomponentDeclaration> subcomponentDeclarations;

  SubcomponentCreatorBindingEdgeImpl(
      XProcessingEnv processingEnv,
      ImmutableSet<SubcomponentDeclaration> subcomponentDeclarations) {
    this.processingEnv = processingEnv;
    this.subcomponentDeclarations = subcomponentDeclarations;
  }

  @Override
  public ImmutableSet<DaggerTypeElement> declaringModules() {
    return subcomponentDeclarations.stream()
        .map(SubcomponentDeclaration::contributingModule)
        .flatMap(presentValues())
        .map(module -> toXProcessing(module, processingEnv))
        .map(DaggerTypeElement::from)
        .collect(toImmutableSet());
  }

  @Override
  public String toString() {
    return "subcomponent declared by "
        + (subcomponentDeclarations.size() == 1
            ? getOnlyElement(declaringModules()).className().canonicalName()
            : declaringModules().stream()
                .map(DaggerTypeElement::className)
                .map(ClassName::canonicalName)
                .collect(joining(", ", "{", "}")));
  }
}
