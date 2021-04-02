/*
 * Copyright (C) 2019 The Dagger Authors.
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

import static com.google.common.base.Preconditions.checkState;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableMap;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.ClassName;
import dagger.hilt.processor.internal.BadInputException;
import dagger.hilt.processor.internal.ClassNames;
import dagger.hilt.processor.internal.ComponentDescriptor;
import dagger.hilt.processor.internal.Processors;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/** Represents information needed to create a component (i.e. modules, entry points, etc) */
@AutoValue
public abstract class ComponentDependencies {
  private static Builder builder() {
    return new AutoValue_ComponentDependencies.Builder();
  }

  /** Returns the modules for a component, without any filtering. */
  public abstract Dependencies modules();

  /** Returns the entry points associated with the given a component. */
  public abstract Dependencies entryPoints();

  /** Returns the component entry point associated with the given a component. */
  public abstract Dependencies componentEntryPoints();

  /** Returns {@code true} if any entry points are annotated with {@code EarlyEntryPoints}. */
  @Memoized
  public boolean hasEarlySingletonEntryPoints() {
    return entryPoints().getGlobalSingletonDeps().stream()
        .anyMatch(entryPoint -> Processors.hasAnnotation(entryPoint, ClassNames.EARLY_ENTRY_POINT));
  }

  /**
   * Returns {@code true} if the test binds or uninstalls test-specific bindings that would prevent
   * it from sharing components with other test roots.
   */
  public final boolean includesTestDeps(ClassName root) {
    return modules().testDeps().keySet().stream().anyMatch((key) -> key.test().equals(root))
        || modules().uninstalledTestDeps().containsKey(root);
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Dependencies.Builder modulesBuilder();

    abstract Dependencies.Builder entryPointsBuilder();

    abstract Dependencies.Builder componentEntryPointsBuilder();

    abstract ComponentDependencies autoBuild();

    ComponentDependencies build(Elements elements) {
      validateModules(modulesBuilder().build(), elements);
      return autoBuild();
    }
  }

  /** A key used for grouping a test dependency by both its component and test name. */
  @AutoValue
  abstract static class TestDepKey {
    static TestDepKey of(ClassName component, ClassName test) {
      return new AutoValue_ComponentDependencies_TestDepKey(component, test);
    }

    /** Returns the name of the component this dependency should be installed in. */
    abstract ClassName component();

    /** Returns the name of the test that this dependency should be installed in. */
    abstract ClassName test();
  }

  /**
   * Holds a set of component dependencies, e.g. modules or entry points.
   *
   * <p>This class handles separating dependencies into global and test dependencies. Global
   * dependencies are installed with every test, where test dependencies are only installed with the
   * specified test. The total set of dependencies includes all global + test dependencies.
   */
  @AutoValue
  public abstract static class Dependencies {
    static Builder builder() {
      return new AutoValue_ComponentDependencies_Dependencies.Builder();
    }

    /** Returns the global deps keyed by component. */
    abstract ImmutableSetMultimap<ClassName, TypeElement> globalDeps();

    /** Returns the global test deps keyed by component. */
    abstract ImmutableSetMultimap<ClassName, TypeElement> globalTestDeps();

    /** Returns the test deps keyed by component and test. */
    abstract ImmutableSetMultimap<TestDepKey, TypeElement> testDeps();

    /** Returns the uninstalled test deps keyed by test. */
    abstract ImmutableSetMultimap<ClassName, TypeElement> uninstalledTestDeps();

    /** Returns the global uninstalled test deps. */
    abstract ImmutableSet<TypeElement> globalUninstalledTestDeps();

    /** Returns the dependencies to be installed in the global singleton component. */
    ImmutableSet<TypeElement> getGlobalSingletonDeps() {
      return ImmutableSet.<TypeElement>builder()
          .addAll(
              globalDeps().get(ClassNames.SINGLETON_COMPONENT).stream()
                  .filter(dep -> !globalUninstalledTestDeps().contains(dep))
                  .collect(toImmutableSet()))
          .addAll(globalTestDeps().get(ClassNames.SINGLETON_COMPONENT))
          .build();
    }

    /** Returns the dependencies to be installed in the given component for the given root. */
    public ImmutableSet<TypeElement> get(ClassName component, ClassName root, boolean isTestRoot) {
      if (!isTestRoot) {
        return globalDeps().get(component);
      }

      ImmutableSet<TypeElement> uninstalledTestDepsForRoot = uninstalledTestDeps().get(root);
      return ImmutableSet.<TypeElement>builder()
          .addAll(
              globalDeps().get(component).stream()
                  .filter(dep -> !uninstalledTestDepsForRoot.contains(dep))
                  .filter(dep -> !globalUninstalledTestDeps().contains(dep))
                  .collect(toImmutableSet()))
          .addAll(globalTestDeps().get(component))
          .addAll(testDeps().get(TestDepKey.of(component, root)))
          .build();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract ImmutableSetMultimap.Builder<ClassName, TypeElement> globalDepsBuilder();

      abstract ImmutableSetMultimap.Builder<ClassName, TypeElement> globalTestDepsBuilder();

      abstract ImmutableSetMultimap.Builder<TestDepKey, TypeElement> testDepsBuilder();

      abstract ImmutableSetMultimap.Builder<ClassName, TypeElement> uninstalledTestDepsBuilder();

      abstract ImmutableSet.Builder<TypeElement> globalUninstalledTestDepsBuilder();

      abstract Dependencies build();
    }
  }

  /**
   * Pulls the component dependencies from the {@code packageName}.
   *
   * <p>Dependency files are generated by the {@link AggregatedDepsProcessor}, and have the form:
   *
   * <pre>{@code
   * {@literal @}AggregatedDeps(
   *   components = {
   *       "foo.FooComponent",
   *       "bar.BarComponent"
   *   },
   *   modules = "baz.BazModule"
   * )
   *
   * }</pre>
   */
  public static ComponentDependencies from(
      ImmutableSet<ComponentDescriptor> descriptors, Elements elements) {
    ImmutableMap<ClassName, ComponentDescriptor> descriptorLookup =
        descriptors.stream()
            .collect(toImmutableMap(ComponentDescriptor::component, descriptor -> descriptor));
    ImmutableSet<AggregatedDepsMetadata> metadatas = AggregatedDepsMetadata.from(elements);

    ComponentDependencies.Builder componentDependencies = ComponentDependencies.builder();
    for (AggregatedDepsMetadata metadata : metadatas) {
      Dependencies.Builder builder = null;
      switch (metadata.dependencyType()) {
        case MODULE:
          builder = componentDependencies.modulesBuilder();
          break;
        case ENTRY_POINT:
          builder = componentDependencies.entryPointsBuilder();
          break;
        case COMPONENT_ENTRY_POINT:
          builder = componentDependencies.componentEntryPointsBuilder();
          break;
      }
      for (TypeElement componentElement : metadata.componentElements()) {
        ClassName componentName = ClassName.get(componentElement);
        checkState(
            descriptorLookup.containsKey(componentName),
            "%s is not a valid Component.",
            componentName);
        if (metadata.testElement().isPresent()) {
          // In this case the @InstallIn or @TestInstallIn applies to only the given test root.
          ClassName test = ClassName.get(metadata.testElement().get());
          builder.testDepsBuilder().put(TestDepKey.of(componentName, test), metadata.dependency());
          builder.uninstalledTestDepsBuilder().putAll(test, metadata.replacedDependencies());
        } else {
          // In this case the @InstallIn or @TestInstallIn applies to all roots
          if (!metadata.replacedDependencies().isEmpty()) {
            // If there are replacedDependencies() it means this is a @TestInstallIn
            builder.globalTestDepsBuilder().put(componentName, metadata.dependency());
            builder.globalUninstalledTestDepsBuilder().addAll(metadata.replacedDependencies());
          } else {
            builder.globalDepsBuilder().put(componentName, metadata.dependency());
          }
        }
      }
    }

    // Collect all @UninstallModules.
    // TODO(b/176438516): Filter @UninstallModules at the root.
    metadatas.stream()
        .filter(metadata -> metadata.testElement().isPresent())
        .map(metadata -> metadata.testElement().get())
        .distinct()
        .filter(testElement -> Processors.hasAnnotation(testElement, ClassNames.IGNORE_MODULES))
        .forEach(
            testElement ->
                componentDependencies
                    .modulesBuilder()
                    .uninstalledTestDepsBuilder()
                    .putAll(
                        ClassName.get(testElement), getUninstalledModules(testElement, elements)));

    return componentDependencies.build(elements);
  }

  // Validate that the @UninstallModules doesn't contain any test modules.
  private static Dependencies validateModules(Dependencies moduleDeps, Elements elements) {
    SetMultimap<ClassName, TypeElement> invalidTestModules = HashMultimap.create();
    moduleDeps.testDeps().entries().stream()
        .filter(
            e -> moduleDeps.uninstalledTestDeps().containsEntry(e.getKey().test(), e.getValue()))
        .forEach(e -> invalidTestModules.put(e.getKey().test(), e.getValue()));

    // Currently we don't have a good way to throw an error for all tests, so we sort (to keep the
    // error reporting order stable) and then choose the first test.
    // TODO(bcorso): Consider using ProcessorErrorHandler directly to report all errors at once?
    Optional<ClassName> invalidTest =
        invalidTestModules.keySet().stream()
            .min((test1, test2) -> test1.toString().compareTo(test2.toString()));
    if (invalidTest.isPresent()) {
      throw new BadInputException(
          String.format(
              "@UninstallModules on test, %s, should not containing test modules, "
                  + "but found: %s",
              invalidTest.get(),
              invalidTestModules.get(invalidTest.get()).stream()
                  // Sort modules to keep stable error messages.
                  .sorted((test1, test2) -> test1.toString().compareTo(test2.toString()))
                  .collect(toImmutableList())),
          elements.getTypeElement(invalidTest.get().toString()));
    }
    return moduleDeps;
  }

  private static ImmutableSet<TypeElement> getUninstalledModules(
      TypeElement testElement, Elements elements) {
    ImmutableList<TypeElement> userUninstallModules =
        Processors.getAnnotationClassValues(
            elements,
            Processors.getAnnotationMirror(testElement, ClassNames.IGNORE_MODULES),
            "value");

    // For pkg-private modules, find the generated wrapper class and uninstall that instead.
    return userUninstallModules.stream()
        .map(uninstallModule -> getPublicDependency(uninstallModule, elements))
        .collect(toImmutableSet());
  }

  /** Returns the public Hilt wrapper module, or the module itself if its already public. */
  private static TypeElement getPublicDependency(TypeElement dependency, Elements elements) {
    return PkgPrivateMetadata.of(elements, dependency, ClassNames.MODULE)
        .map(metadata -> elements.getTypeElement(metadata.generatedClassName().toString()))
        .orElse(dependency);
  }
}
