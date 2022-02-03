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

import dagger.Module;
import library2.MySimpleTransitiveAnnotation;
import library2.MyTransitiveAnnotation;

/** A simple module that needs to be passed in when creating this component. */
@MySimpleTransitiveAnnotation
@MyTransitiveAnnotation(VALUE)
@Module
public final class MySubcomponentModule {
  @MySimpleTransitiveAnnotation
  @MyTransitiveAnnotation(VALUE)
  public MySubcomponentModule(
      @MySimpleTransitiveAnnotation
      @MyTransitiveAnnotation(VALUE)
      int i) {}
}
