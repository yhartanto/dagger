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
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableNetwork;
import dagger.model.BindingGraph.Edge;
import dagger.model.BindingGraph.Node;
import dagger.model.ComponentPath;
import dagger.model.Key;
import java.util.HashMap;
import java.util.Map;
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
    ImmutableSet<BindingNode> reachableBindingNodes =
        Graphs.reachableNodes(
                topLevelBindingGraph.network().asGraph(),
                topLevelBindingGraph.componentNode(componentPath).get()).stream()
            .filter(node -> isSubpath(componentPath, node.componentPath()))
            .filter(node -> node instanceof BindingNode)
            .map(node -> (BindingNode) node)
            .collect(toImmutableSet());

    // Construct the maps of the ContributionBindings and MembersInjectionBindings.
    Map<Key, BindingNode> contributionBindings = new HashMap<>();
    Map<Key, BindingNode> membersInjectionBindings = new HashMap<>();
    for (BindingNode bindingNode : reachableBindingNodes) {
      Map<Key, BindingNode> bindingsMap;
      if (bindingNode.delegate() instanceof ContributionBinding) {
        bindingsMap = contributionBindings;
      } else if (bindingNode.delegate() instanceof MembersInjectionBinding) {
        bindingsMap = membersInjectionBindings;
      } else {
        throw new AssertionError("Unexpected binding node type: " + bindingNode.delegate());
      }

      // TODO(bcorso): Mapping binding nodes by key is flawed since bindings that depend on local
      // multibindings can have multiple nodes (one in each component). In this case, we choose the
      // node in the child-most component since this is likely the node that users of this
      // BindingGraph will want (and to remain consisted with LegacyBindingGraph). However, ideally
      // we would avoid this ambiguity by getting dependencies directly from the top-level network.
      // In particular, rather than using a Binding's list of DependencyRequests (which only
      // contains the key) we would use the top-level network to find the DependencyEdges for a
      // particular BindingNode.
      Key key = bindingNode.key();
      if (!bindingsMap.containsKey(key)
          // Always choose the child-most binding node.
          || bindingNode.componentPath().components().size()
              > bindingsMap.get(key).componentPath().components().size()) {
        bindingsMap.put(key, bindingNode);
      }
    }

    BindingGraph bindingGraph =
        new AutoValue_BindingGraph(componentPath, legacyBindingGraph, topLevelBindingGraph);
    bindingGraph.contributionBindings = ImmutableMap.copyOf(contributionBindings);
    bindingGraph.membersInjectionBindings = ImmutableMap.copyOf(membersInjectionBindings);

    return bindingGraph;
  }

  BindingGraph() {}

  private ImmutableMap<Key, BindingNode> contributionBindings;
  private ImmutableMap<Key, BindingNode> membersInjectionBindings;

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

  public final ContributionBinding contributionBinding(Key key) {
    return (ContributionBinding) contributionBindings.get(key).delegate();
  }

  public final Optional<MembersInjectionBinding> membersInjectionBinding(Key key) {
    return membersInjectionBindings.containsKey(key)
        ? Optional.of((MembersInjectionBinding) membersInjectionBindings.get(key).delegate())
        : Optional.empty();
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
        // Filter out any subgraphs that may have been pruned.
        .filter(subgraph -> topLevelBindingGraph().componentNode(subpathOf(subgraph)).isPresent())
        .map(subgraph -> create(subpathOf(subgraph), subgraph, topLevelBindingGraph()))
        .collect(toImmutableList());
  }

  private ComponentPath subpathOf(LegacyBindingGraph subgraph) {
    return componentPath().childPath(subgraph.componentDescriptor().typeElement());
  }

  public ImmutableSet<BindingNode> bindingNodes(Key key) {
    ImmutableSet.Builder<BindingNode> builder = ImmutableSet.builder();
    if (contributionBindings.containsKey(key)) {
      builder.add(contributionBindings.get(key));
    }
    if (membersInjectionBindings.containsKey(key)) {
      builder.add(membersInjectionBindings.get(key));
    }
    return builder.build();
  }

  @Memoized
  public ImmutableSet<BindingNode> bindingNodes() {
    return ImmutableSet.<BindingNode>builder()
        .addAll(contributionBindings.values())
        .addAll(membersInjectionBindings.values())
        .build();
  }

  // TODO(bcorso): Move this to ComponentPath
  private static boolean isSubpath(ComponentPath path, ComponentPath subpath) {
    if (path.components().size() < subpath.components().size()) {
      return false;
    }
    for (int i = 0; i < subpath.components().size(); i++) {
      if (!path.components().get(i).equals(subpath.components().get(i))) {
        return false;
      }
    }
    return true;
  }
}
