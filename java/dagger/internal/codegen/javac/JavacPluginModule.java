/*
 * Copyright (C) 2017 The Dagger Authors.
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

package dagger.internal.codegen.javac;

import androidx.room.compiler.processing.XMessager;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.compat.XConverters;
import com.sun.tools.javac.util.Context;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.internal.codegen.binding.BindingGraphFactory;
import dagger.internal.codegen.binding.ComponentDescriptorFactory;
import dagger.internal.codegen.compileroption.CompilerOptions;
import dagger.internal.codegen.compileroption.JavacPluginCompilerOptions;
import dagger.internal.codegen.langmodel.DaggerElements;
import dagger.internal.codegen.langmodel.DaggerTypes;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Singleton;

/**
 * A module that provides a {@link BindingGraphFactory} and {@link ComponentDescriptorFactory} for
 * use in {@code javac} plugins. Requires a binding for the {@code javac} {@link Context}.
 */
@Module
public abstract class JavacPluginModule {
  @Binds
  abstract CompilerOptions compilerOptions(JavacPluginCompilerOptions compilerOptions);

  @Provides
  static XMessager messager() {
    return XConverters.toXProcessing(JavacPlugins.getNullMessager());
  }

  @Provides
  static DaggerElements daggerElements(XProcessingEnv xProcessingEnv) {
    ProcessingEnvironment env = XConverters.toJavac(xProcessingEnv);
    return new DaggerElements(
        env.getElementUtils(), env.getTypeUtils());  // ALLOW_TYPES_ELEMENTS
  }

  @Provides
  static DaggerTypes daggerTypes(XProcessingEnv xProcessingEnv, DaggerElements elements) {
    return new DaggerTypes(
        XConverters.toJavac(xProcessingEnv).getTypeUtils(), elements);  // ALLOW_TYPES_ELEMENTS
  }

  @Provides
  @Singleton
  static XProcessingEnv xProcessingEnv(Context javaContext) {
    return JavacPlugins.getXProcessingEnv(javaContext);
  }

  private JavacPluginModule() {}
}
