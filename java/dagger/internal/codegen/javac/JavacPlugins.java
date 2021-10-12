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

package dagger.internal.codegen.javac;

import androidx.room.compiler.processing.XProcessingEnv;
import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Context;
import java.util.Locale;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;  // ALLOW_TYPES_ELEMENTS since in interface API
import javax.lang.model.util.Types;  // ALLOW_TYPES_ELEMENTS since in interface API
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;

/** Helper methods for working use in javac plugins. */
public final class JavacPlugins {

  /**
   * Gets an {@link XProcessingEnv} from a Javac {@link Context}. This processing environment is
   * limited in that it uses a no-op Filer and Messager.
   */
  public static XProcessingEnv getXProcessingEnv(Context context) {
    return XProcessingEnv.create(new JavacPluginProcessingEnvironment(context));
  }

  private static final class JavacPluginProcessingEnvironment implements ProcessingEnvironment {
    private final Context context;

    private JavacPluginProcessingEnvironment(Context context) {
      this.context = context;
    }

    @Override public Elements getElementUtils() {
      return JavacElements.instance(context);
    }

    @Override public Filer getFiler() {
      return new Filer() {
        @Override public JavaFileObject createClassFile(
            CharSequence name, Element... originatingElements) {
          throw new UnsupportedOperationException("Cannot use a Filer in this context");
        }

        @Override public FileObject createResource(
            Location location,
            CharSequence pkg,
            CharSequence relativeName,
            Element... originatingElements) {
          throw new UnsupportedOperationException("Cannot use a Filer in this context");
        }

        @Override public JavaFileObject createSourceFile(
            CharSequence name, Element... originatingElements) {
          throw new UnsupportedOperationException("Cannot use a Filer in this context");
        }

        @Override public FileObject getResource(
            Location location, CharSequence pkg, CharSequence relativeName) {
          throw new UnsupportedOperationException("Cannot use a Filer in this context");
        }
      };
    }

    @Override public Locale getLocale() {
      // Null means there's no locale in effect
      return null;
    }

    @Override public Messager getMessager() {
      return getNullMessager();
    }

    @Override public ImmutableMap<String, String> getOptions() {
      // TODO(erichang): You can technically parse options out of the context, but it is internal
      // implementation and unclear that any of the tools will ever be passing an option.
      return ImmutableMap.of();
    }

    @Override public SourceVersion getSourceVersion() {
      // This source version doesn't really matter because it is saying what version generated code
      // should adhere to, which there shouldn't be any because the Filer doesn't work.
      return SourceVersion.latestSupported();
    }

    @Override public Types getTypeUtils() {
      return JavacTypes.instance(context);
    }
  }

  public static Messager getNullMessager() {
    return new Messager() {
      @Override
      public void printMessage(Diagnostic.Kind kind, CharSequence charSequence) {}

      @Override
      public void printMessage(Diagnostic.Kind kind, CharSequence charSequence, Element element) {}

      @Override
      public void printMessage(
          Diagnostic.Kind kind,
          CharSequence charSequence,
          Element element,
          AnnotationMirror annotationMirror) {}

      @Override
      public void printMessage(
          Diagnostic.Kind kind,
          CharSequence charSequence,
          Element element,
          AnnotationMirror annotationMirror,
          AnnotationValue annotationValue) {}
    };
  }

  private JavacPlugins() {}
}
