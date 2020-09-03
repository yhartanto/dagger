/*
 * Copyright (C) 2014 The Dagger Authors.
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

import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.graph.ImmutableNetwork;
import dagger.model.BindingGraph.Edge;
import dagger.model.BindingGraph.Node;
import dagger.model.ComponentPath;
import dagger.model.Key;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/** An implementation of {@link BindingGraph}. */
@AutoValue
public abstract class BindingGraph {

  @AutoValue
  abstract static class TopLevelBindingGraph extends dagger.model.BindingGraph {
    static TopLevelBindingGraph create(
        ImmutableNetwork<Node, Edge> network, boolean isFullBindingGraph) {
      return new AutoValue_BindingGraph_TopLevelBindingGraph(network, isFullBindingGraph);
    }

    @Override
    @Memoized
    public ImmutableSetMultimap<Class<? extends Node>, ? extends Node> nodesByClass() {
      return super.nodesByClass();
    }
  }

  static BindingGraph create(
      ComponentPath componentPath,
      LegacyBindingGraph legacyBindingGraph,
      TopLevelBindingGraph topLevelBindingGraph) {
    return new AutoValue_BindingGraph(componentPath, legacyBindingGraph, topLevelBindingGraph);
  }

  BindingGraph() {}

  public abstract ComponentPath componentPath();

  // TODO(bcorso): Delete this after we migrate all usages to the new BindinGraph API.
  abstract LegacyBindingGraph legacyBindingGraph();

  public abstract TopLevelBindingGraph topLevelBindingGraph();

  public boolean isFullBindingGraph() {
    return topLevelBindingGraph().isFullBindingGraph();
  }

  public ComponentDescriptor componentDescriptor() {
    return legacyBindingGraph().componentDescriptor();
  }

  public final ImmutableMap<Key, ResolvedBindings> contributionBindings() {
    return legacyBindingGraph().contributionBindings();
  }

  public final ImmutableMap<Key, ResolvedBindings> membersInjectionBindings() {
    return legacyBindingGraph().membersInjectionBindings();
  }

  public final ResolvedBindings resolvedBindings(BindingRequest request) {
    return legacyBindingGraph().resolvedBindings(request);
  }

  public final Iterable<ResolvedBindings> resolvedBindings() {
    return legacyBindingGraph().resolvedBindings();
  }

  public final TypeElement componentTypeElement() {
    return legacyBindingGraph().componentTypeElement();
  }

  public final ImmutableSet<TypeElement> ownedModuleTypes() {
    return legacyBindingGraph().ownedModuleTypes();
  }

  public final Optional<ExecutableElement> factoryMethod() {
    return legacyBindingGraph().factoryMethod();
  }

  public final ImmutableMap<ComponentRequirement, VariableElement> factoryMethodParameters() {
    return legacyBindingGraph().factoryMethodParameters();
  }

  public final ImmutableSet<ComponentRequirement> componentRequirements() {
    return legacyBindingGraph().componentRequirements();
  }

  public final ImmutableSet<ComponentDescriptor> componentDescriptors() {
    return legacyBindingGraph().componentDescriptors();
  }

  @Memoized
  public ImmutableList<BindingGraph> subgraphs() {
    return legacyBindingGraph().subgraphs().stream()
        .map(
            subgraph -> create(
                componentPath().childPath(subgraph.componentDescriptor().typeElement()),
                subgraph,
                topLevelBindingGraph()))
        .collect(toImmutableList());
  }
}
