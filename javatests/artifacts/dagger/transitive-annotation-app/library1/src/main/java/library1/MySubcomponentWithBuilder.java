/*
 * Copyright (C) 2022 The Dagger Authors.
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

package library1;

import static library2.MyTransitiveAnnotation.VALUE;

import dagger.BindsInstance;
import dagger.Subcomponent;
import library2.MySimpleTransitiveAnnotation;
import library2.MyTransitiveAnnotation;

/**
 * A class used to test that Dagger won't fail when non-dagger related annotations cannot be
 * resolved.
 *
 * <p>During the compilation of {@code :app}, {@link MyTransitiveAnnotation} will no longer be on
 * the classpath. In most cases, Dagger shouldn't care that the annotation isn't on the classpath
 */
// @MySimpleTransitiveAnnotation: Not yet supported
// @MyTransitiveAnnotation(VALUE): Not yet supported
@MySubcomponentScope
@Subcomponent(modules = MySubcomponentModule.class)
public abstract class MySubcomponentWithBuilder {
  @MyQualifier
  // @MySimpleTransitiveAnnotation: Not yet supported
  // @MyTransitiveAnnotation(VALUE): Not yet supported
  public abstract MySubcomponentBinding qualifiedMySubcomponentBinding();

  // @MySimpleTransitiveAnnotation: Not yet supported
  // @MyTransitiveAnnotation(VALUE): Not yet supported
  public abstract MySubcomponentBinding unqualifiedMySubcomponentBinding();

  // @MySimpleTransitiveAnnotation: Not yet supported
  // @MyTransitiveAnnotation(VALUE): Not yet supported
  public abstract void injectFoo(
      // @MySimpleTransitiveAnnotation: Not yet supported
      // @MyTransitiveAnnotation(VALUE): Not yet supported
      Foo foo);

  // @MySimpleTransitiveAnnotation: Not yet supported
  // @MyTransitiveAnnotation(VALUE): Not yet supported
  @Subcomponent.Builder
  public abstract static class Builder {
    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public abstract Builder mySubcomponentModule(
        @MySimpleTransitiveAnnotation
        @MyTransitiveAnnotation(VALUE)
        MySubcomponentModule mySubcomponentModule);

    @BindsInstance
    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public abstract Builder qualifiedMySubcomponentBinding(
        @MyQualifier
        // @MySimpleTransitiveAnnotation: Not yet supported
        // @MyTransitiveAnnotation(VALUE): Not yet supported
        MySubcomponentBinding subcomponentBinding);

    @BindsInstance
    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public abstract Builder unqualifiedMySubcomponentBinding(
        // @MySimpleTransitiveAnnotation: Not yet supported
        // @MyTransitiveAnnotation(VALUE): Not yet supported
        MySubcomponentBinding subcomponentBinding);

    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public abstract MySubcomponentWithBuilder build();

    // Non-dagger code

    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public String nonDaggerField = "";

    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public static String nonDaggerStaticField = "";

    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public void nonDaggerMethod(
        @MySimpleTransitiveAnnotation
        @MyTransitiveAnnotation(VALUE)
        String str) {}

    @MySimpleTransitiveAnnotation
    @MyTransitiveAnnotation(VALUE)
    public static void nonDaggerStaticMethod(
        @MySimpleTransitiveAnnotation
        @MyTransitiveAnnotation(VALUE)
        String str) {}
  }

  // Non-dagger code

  @MySimpleTransitiveAnnotation
  @MyTransitiveAnnotation(VALUE)
  public String nonDaggerField = "";

  @MySimpleTransitiveAnnotation
  @MyTransitiveAnnotation(VALUE)
  public static String nonDaggerStaticField = "";

  @MySimpleTransitiveAnnotation
  @MyTransitiveAnnotation(VALUE)
  public void nonDaggerMethod(
      @MySimpleTransitiveAnnotation
      @MyTransitiveAnnotation(VALUE)
      String str) {}

  @MySimpleTransitiveAnnotation
  @MyTransitiveAnnotation(VALUE)
  public static void nonDaggerStaticMethod(
      @MySimpleTransitiveAnnotation
      @MyTransitiveAnnotation(VALUE)
      String str) {}
}
