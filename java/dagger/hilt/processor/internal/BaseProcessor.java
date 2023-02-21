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

package dagger.hilt.processor.internal;

import static androidx.room.compiler.processing.XElementKt.isTypeElement;
import static androidx.room.compiler.processing.compat.XConverters.toJavac;
import static androidx.room.compiler.processing.compat.XConverters.toXProcessing;
import static com.google.common.base.Preconditions.checkState;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;
import static dagger.internal.codegen.xprocessing.XElements.asTypeElement;

import androidx.room.compiler.processing.XElement;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.XProcessingEnvConfig;
import androidx.room.compiler.processing.XRoundEnv;
import androidx.room.compiler.processing.XTypeElement;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.ClassName;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * Implements default configurations for Processors, and provides structure for exception handling.
 *
 * <p>By default #process() will do the following:
 *
 * <ol>
 *   <li> #preRoundProcess()
 *   <li> foreach element:
 *     <ul><li> #processEach()</ul>
 *   </li>
 *   <li> #postRoundProcess()
 * </ol>
 *
 * <p>#processEach() allows each element to be processed, even if exceptions are thrown. Due to the
 * non-deterministic ordering of the processed elements, this is needed to ensure a consistent set
 * of exceptions are thrown with each build.
 */
public abstract class BaseProcessor extends AbstractProcessor {
  private static final XProcessingEnvConfig PROCESSING_ENV_CONFIG =
      new XProcessingEnvConfig.Builder().disableAnnotatedElementValidation(true).build();

  /** Stores the state of processing for a given annotation and element. */
  @AutoValue
  abstract static class ProcessingState {
    private static ProcessingState of(XTypeElement annotation, XElement element) {
      // We currently only support TypeElements directly annotated with the annotation.
      // TODO(bcorso): Switch to using BasicAnnotationProcessor if we need more than this.
      // Note: Switching to BasicAnnotationProcessor is currently not possible because of cyclic
      // references to generated types in our API. For example, an @AndroidEntryPoint annotated
      // element will indefinitely defer its own processing because it extends a generated type
      // that it's responsible for generating.
      checkState(isTypeElement(element));
      checkState(element.hasAnnotation(annotation.getClassName()));
      return new AutoValue_BaseProcessor_ProcessingState(
          annotation.getClassName(), asTypeElement(element).getClassName());
    }

    /** Returns the class name of the annotation. */
    abstract ClassName annotationClassName();

    /** Returns the type name of the annotated element. */
    abstract ClassName elementClassName();

    /** Returns the annotation that triggered the processing. */
    XTypeElement annotation(XProcessingEnv processingEnv) {
      return processingEnv.requireTypeElement(annotationClassName());
    }

    /** Returns the annotated element to process. */
    XTypeElement element(XProcessingEnv processingEnv) {
      return processingEnv.requireTypeElement(elementClassName());
    }
  }

  private final Set<ProcessingState> stateToReprocess = new LinkedHashSet<>();
  private XProcessingEnv env;
  private ProcessorErrorHandler errorHandler;

  @Override
  public final ImmutableSet<String> getSupportedOptions() {
    // This is declared here rather than in the actual processors because KAPT will issue a
    // warning if any used option is not unsupported. This can happen when there is a module
    // which uses Hilt but lacks any @AndroidEntryPoint annotations.
    // See: https://github.com/google/dagger/issues/2040
    return ImmutableSet.<String>builder()
        .addAll(HiltCompilerOptions.getProcessorOptions())
        .addAll(additionalProcessingOptions())
        .build();
  }

  /** Returns additional processing options that should only be applied for a single processor. */
  protected Set<String> additionalProcessingOptions() {
    return ImmutableSet.of();
  }

  /** Used to perform initialization before each round of processing. */
  protected void preRoundProcess(XRoundEnv roundEnv) {}

  /**
   * Called for each element in a round that uses a supported annotation.
   *
   * <p>Note that an exception can be thrown for each element in the round. This is usually
   * preferred over throwing only the first exception in a round. Only throwing the first exception
   * in the round can lead to flaky errors that are dependent on the non-deterministic ordering that
   * the elements are processed in.
   */
  protected void processEach(XTypeElement annotation, XElement element) throws Exception {}

  /**
   * Used to perform post processing at the end of a round. This is especially useful for handling
   * additional processing that depends on aggregate data, that cannot be handled in #processEach().
   *
   * <p>Note: this will not be called if an exception is thrown during #processEach() -- if we have
   * already detected errors on an annotated element, performing post processing on an aggregate
   * will just produce more (perhaps non-deterministic) errors.
   */
  protected void postRoundProcess(XRoundEnv roundEnv) throws Exception {}

  /**
   * @return true if you want to delay errors to the last round. Useful if the processor
   * generates code for symbols used a lot in the user code. Delaying allows as much code to
   * compile as possible for correctly configured types and reduces error spam.
   */
  protected boolean delayErrors() {
    return false;
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnvironment) {
    super.init(processingEnvironment);
    this.env = XProcessingEnv.create(processingEnvironment, PROCESSING_ENV_CONFIG);
    this.errorHandler = new ProcessorErrorHandler(env);
    HiltCompilerOptions.checkWrongAndDeprecatedOptions(env);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  /** This should not be overridden, as it defines the order of the processing. */
  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    process(
        annotations.stream()
            .map(annotation -> toXProcessing(annotation, env))
            .collect(toImmutableList()),
        XRoundEnv.create(env, roundEnv, roundEnv.processingOver()));
    return false;  // Don't claim annotations
  }

  private void process(ImmutableList<XTypeElement> annotations, XRoundEnv roundEnv) {
    preRoundProcess(roundEnv);

    boolean roundError = false;

    // Gather the set of new and deferred elements to process, grouped by annotation.
    SetMultimap<XTypeElement, XElement> elementMultiMap = LinkedHashMultimap.create();
    for (ProcessingState processingState : stateToReprocess) {
      elementMultiMap.put(processingState.annotation(env), processingState.element(env));
    }
    for (XTypeElement annotation : annotations) {
      elementMultiMap.putAll(
          annotation, roundEnv.getElementsAnnotatedWith(annotation.getQualifiedName()));
    }

    // Clear the processing state before reprocessing.
    stateToReprocess.clear();

    for (Map.Entry<XTypeElement, Collection<XElement>> entry : elementMultiMap.asMap().entrySet()) {
      XTypeElement annotation = entry.getKey();
      for (XElement element : entry.getValue()) {
        try {
          processEach(annotation, element);
        } catch (Exception e) {
          if (e instanceof ErrorTypeException && !roundEnv.isProcessingOver()) {
            // Allow an extra round to reprocess to try to resolve this type.
            stateToReprocess.add(ProcessingState.of(annotation, element));
          } else {
            errorHandler.recordError(e);
            roundError = true;
          }
        }
      }
    }

    if (!roundError) {
      try {
        postRoundProcess(roundEnv);
      } catch (Exception e) {
        errorHandler.recordError(e);
      }
    }

    if (!delayErrors() || roundEnv.isProcessingOver()) {
      errorHandler.checkErrors();
    }
  }

  public final XProcessingEnv processingEnv() {
    return env;
  }

  // TODO(bcorso): Remove this once all usages are converted to XProcessing.
  public final ProcessingEnvironment getProcessingEnv() {
    return toJavac(env);
  }

  // TODO(bcorso): Remove this once all usages are converted to XProcessing.
  public final Elements getElementUtils() {
    return toJavac(env).getElementUtils();
  }
}
