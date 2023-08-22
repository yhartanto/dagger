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

package dagger.spi.model;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment;
import com.google.errorprone.annotations.DoNotMock;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;

/** Wrapper type for an element. */
@DoNotMock("Only use real implementations created by Dagger")
public abstract class DaggerProcessingEnv {
  /** Represents a type of backend used for compilation. */
  public enum Backend {
    JAVAC,
    KSP
  }

  /**
   * Java representation for the processing environment, returns {@code null} not using java
   * annotation processor.
   */
  @Nullable
  public abstract ProcessingEnvironment java();

  /** Ksp symbol processing environment hosting symbol processors. */
  @Nullable
  public abstract SymbolProcessorEnvironment ksp();

  /** Ksp resolver provides [SymbolProcessor] with access to compiler details such as Symbols. */
  @Nullable
  public abstract Resolver resolver();

  /** Returns the backend used in this compilation. */
  public abstract DaggerProcessingEnv.Backend backend();
}
