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

import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.errorprone.annotations.DoNotMock;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;

/** Wrapper type for a type element. */
@DoNotMock("Only use real implementations created by Dagger")
public abstract class DaggerTypeElement {
  /** Java representation for the type, returns {@code null} not using java annotation processor. */
  @Nullable
  public abstract TypeElement java();

  /** KSP declaration for the element, returns {@code null} not using KSP. */
  @Nullable
  public abstract KSClassDeclaration ksp();

  public abstract DaggerProcessingEnv.Backend backend();
}
