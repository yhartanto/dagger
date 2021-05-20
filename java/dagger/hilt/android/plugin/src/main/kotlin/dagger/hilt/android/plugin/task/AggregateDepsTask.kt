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

package dagger.hilt.android.plugin.task

import dagger.hilt.android.plugin.root.AggregatedElementProxyGenerator
import dagger.hilt.android.plugin.root.ComponentTreeDepsGenerator
import dagger.hilt.android.plugin.root.ProcessedRootSentinelGenerator
import dagger.hilt.processor.internal.root.ir.ComponentTreeDepsIrCreator
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.InputChanges
import org.objectweb.asm.Opcodes

/**
 * Aggregates Hilt component dependencies from the compile classpath and outputs Java sources
 * with shareable component trees.
 *
 * The [compileClasspath] input is expected to contain jars or classes transformed by
 * [dagger.hilt.android.plugin.util.AggregatedPackagesTransform].
 */
@CacheableTask
abstract class AggregateDepsTask : DefaultTask() {

  // TODO(danysantiago): Make @Incremental and try to use @CompileClasspath
  @get:Classpath
  abstract val compileClasspath: ConfigurableFileCollection

  @get:Input
  @get:Optional
  abstract val asmApiVersion: Property<Int>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  abstract val testEnvironment: Property<Boolean>

  @TaskAction
  internal fun taskAction(@Suppress("UNUSED_PARAMETER") inputs: InputChanges) {
    // TODO(danysantiago): Use Worker API, https://docs.gradle.org/current/userguide/worker_api.html
    val aggregator = Aggregator.from(
      logger = logger,
      asmApiVersion = asmApiVersion.getOrNull() ?: Opcodes.ASM7,
      input = compileClasspath
    )
    // TODO(danysantiago): Add 3 checks:
    //   * No new roots when test roots are already processed
    //   * No app and test roots to process in the same task
    //   * No multiple app roots
    val processedRootNames = aggregator.processedRoots.flatMap { it.roots }.toSet()
    val rootsToProcess = aggregator.roots.filterNot { processedRootNames.contains(it.root) }.toSet()
    val componentTrees = ComponentTreeDepsIrCreator.components(
      isTest = testEnvironment.get(),
      isSharedTestComponentsEnabled = true,
      aggregatedRoots = rootsToProcess,
      defineComponentDeps = aggregator.defineComponentDeps,
      aliasOfDeps = aggregator.aliasOfDeps,
      aggregatedDeps = aggregator.aggregatedDeps,
      aggregatedUninstallModulesDeps = aggregator.uninstallModulesDeps,
      aggregatedEarlyEntryPointDeps = aggregator.earlyEntryPointDeps,
    )
    ComponentTreeDepsGenerator(
      proxies = aggregator.allAggregatedDepProxies.associate { it.value to it.fqName },
      outputDir = outputDir.get().asFile
    ).let { generator ->
      componentTrees.forEach { generator.generate(it) }
    }
    AggregatedElementProxyGenerator(outputDir.get().asFile).let { generator ->
      (aggregator.allAggregatedDepProxies - aggregator.aggregatedDepProxies).forEach {
        generator.generate(it)
      }
    }
    ProcessedRootSentinelGenerator(outputDir.get().asFile).let { generator ->
      rootsToProcess.map { it.root }.forEach { generator.generate(it) }
    }
  }
}
