/*
 * Copyright (C) 2015 The Dagger Authors.
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

package dagger.internal.codegen.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static dagger.internal.codegen.langmodel.DaggerTypes.isTypeOf;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.base.Equivalence;
import com.squareup.javapoet.ClassName;
import dagger.internal.codegen.javapoet.TypeNames;
import dagger.spi.model.Key;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/** Information about a {@link java.util.Map} {@link TypeMirror}. */
@AutoValue
public abstract class MapType {
  /**
   * The map type itself, wrapped using {@link MoreTypes#equivalence()}. Use
   * {@link #declaredMapType()} instead.
   */
  protected abstract Equivalence.Wrapper<DeclaredType> wrappedDeclaredMapType();

  /**
   * The map type itself.
   */
  private DeclaredType declaredMapType() {
    return wrappedDeclaredMapType().get();
  }

  /** {@code true} if the map type is the raw {@link java.util.Map} type. */
  public boolean isRawType() {
    return declaredMapType().getTypeArguments().isEmpty();
  }

  /**
   * The map key type.
   *
   * @throws IllegalStateException if {@link #isRawType()} is true.
   */
  public TypeMirror keyType() {
    checkState(!isRawType());
    return declaredMapType().getTypeArguments().get(0);
  }

  /**
   * The map value type.
   *
   * @throws IllegalStateException if {@link #isRawType()} is true.
   */
  public TypeMirror valueType() {
    checkState(!isRawType());
    return declaredMapType().getTypeArguments().get(1);
  }

  /**
   * Returns {@code true} if the raw type of {@link #valueType()} is {@code className}.
   *
   * @throws IllegalStateException if {@link #isRawType()} is true.
   */
  public boolean valuesAreTypeOf(ClassName className) {
    return MoreTypes.isType(valueType()) && isTypeOf(className, valueType());
  }

  /**
   * Returns {@code true} if the {@linkplain #valueType() value type} of the {@link java.util.Map}
   * is a {@linkplain FrameworkTypes#isFrameworkType(TypeMirror) framework type}.
   */
  public boolean valuesAreFrameworkType() {
    return FrameworkTypes.isFrameworkType(valueType());
  }

  /**
   * {@code V} if {@link #valueType()} is a framework type like {@code Provider<V>} or {@code
   * Producer<V>}.
   *
   * @throws IllegalStateException if {@link #isRawType()} is true or {@link #valueType()} is not a
   *     framework type
   */
  public TypeMirror unwrappedFrameworkValueType() {
    checkState(
        valuesAreFrameworkType(), "called unwrappedFrameworkValueType() on %s", declaredMapType());
    return uncheckedUnwrappedValueType();
  }

  /**
   * {@code V} if {@link #valueType()} is a {@code WrappingClass<V>}.
   *
   * @throws IllegalStateException if {@link #isRawType()} is true or {@link #valueType()} is not a
   *     {@code WrappingClass<V>}
   */
  // TODO(b/202033221): Consider using stricter input type, e.g. FrameworkType.
  public TypeMirror unwrappedValueType(ClassName wrappingClass) {
    checkState(valuesAreTypeOf(wrappingClass), "expected values to be %s: %s", wrappingClass, this);
    return uncheckedUnwrappedValueType();
  }

  private TypeMirror uncheckedUnwrappedValueType() {
    return MoreTypes.asDeclared(valueType()).getTypeArguments().get(0);
  }

  /** {@code true} if {@code type} is a {@link java.util.Map} type. */
  public static boolean isMap(TypeMirror type) {
    return MoreTypes.isType(type) && isTypeOf(TypeNames.MAP, type);
  }

  /** {@code true} if {@code key.type()} is a {@link java.util.Map} type. */
  public static boolean isMap(Key key) {
    return isMap(key.type().java());
  }

  /**
   * Returns a {@link MapType} for {@code type}.
   *
   * @throws IllegalArgumentException if {@code type} is not a {@link java.util.Map} type
   */
  public static MapType from(TypeMirror type) {
    checkArgument(isMap(type), "%s is not a Map", type);
    return new AutoValue_MapType(MoreTypes.equivalence().wrap(MoreTypes.asDeclared(type)));
  }

  /**
   * Returns a {@link MapType} for {@code key}'s {@link Key#type() type}.
   *
   * @throws IllegalArgumentException if {@code key.type()} is not a {@link java.util.Map} type
   */
  public static MapType from(Key key) {
    return from(key.type().java());
  }
}
