/*
 * Copyright (C) 2016 The Dagger Authors.
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

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.common.base.Equivalence;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import dagger.internal.codegen.javapoet.TypeNames;
import dagger.spi.model.Key;
import java.util.Optional;
import javax.lang.model.element.Name;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleTypeVisitor8;

/**
 * Information about an {@code Optional} {@link TypeMirror}.
 *
 * <p>{@link com.google.common.base.Optional} and {@link java.util.Optional} are supported.
 */
@AutoValue
public abstract class OptionalType {

  /** A variant of {@code Optional}. */
  public enum OptionalKind {
    /** {@link com.google.common.base.Optional}. */
    GUAVA_OPTIONAL(TypeNames.GUAVA_OPTIONAL, "absent"),

    /** {@link java.util.Optional}. */
    JDK_OPTIONAL(TypeNames.JDK_OPTIONAL, "empty");

    private final ClassName className;
    private final String absentMethodName;

    OptionalKind(ClassName className, String absentMethodName) {
      this.className = className;
      this.absentMethodName = absentMethodName;
    }

    /** Returns {@code valueType} wrapped in the correct class. */
    public ParameterizedTypeName of(TypeName valueType) {
      return ParameterizedTypeName.get(className, valueType);
    }

    /** Returns an expression for the absent/empty value. */
    public CodeBlock absentValueExpression() {
      return CodeBlock.of("$T.$L()", className, absentMethodName);
    }

    /**
     * Returns an expression for the absent/empty value, parameterized with {@link #valueType()}.
     */
    public CodeBlock parameterizedAbsentValueExpression(OptionalType optionalType) {
      return CodeBlock.of("$T.<$T>$L()", className, optionalType.valueType(), absentMethodName);
    }

    /** Returns an expression for the present {@code value}. */
    public CodeBlock presentExpression(CodeBlock value) {
      return CodeBlock.of("$T.of($L)", className, value);
    }

    /**
     * Returns an expression for the present {@code value}, returning {@code Optional<Object>} no
     * matter what type the value is.
     */
    public CodeBlock presentObjectExpression(CodeBlock value) {
      return CodeBlock.of("$T.<$T>of($L)", className, TypeName.OBJECT, value);
    }
  }

  private static final TypeVisitor<Optional<OptionalKind>, Void> OPTIONAL_KIND =
      new SimpleTypeVisitor8<Optional<OptionalKind>, Void>(Optional.empty()) {
        @Override
        public Optional<OptionalKind> visitDeclared(DeclaredType t, Void p) {
          for (OptionalKind optionalKind : OptionalKind.values()) {
            Name qualifiedName = MoreElements.asType(t.asElement()).getQualifiedName();
            if (qualifiedName.contentEquals(optionalKind.className.canonicalName())) {
              return Optional.of(optionalKind);
            }
          }
          return Optional.empty();
        }
      };

  /**
   * The optional type itself, wrapped using {@link MoreTypes#equivalence()}.
   *
   * @deprecated Use {@link #declaredOptionalType()} instead.
   */
  @Deprecated
  protected abstract Equivalence.Wrapper<DeclaredType> wrappedDeclaredOptionalType();

  /** The optional type itself. */
  @SuppressWarnings("deprecation")
  private DeclaredType declaredOptionalType() {
    return wrappedDeclaredOptionalType().get();
  }

  /** Which {@code Optional} type is used. */
  public OptionalKind kind() {
    return declaredOptionalType().accept(OPTIONAL_KIND, null).get();
  }

  /** The value type. */
  public TypeMirror valueType() {
    return declaredOptionalType().getTypeArguments().get(0);
  }

  /** Returns {@code true} if {@code type} is an {@code Optional} type. */
  private static boolean isOptional(TypeMirror type) {
    return type.accept(OPTIONAL_KIND, null).isPresent();
  }

  /** Returns {@code true} if {@code key.type()} is an {@code Optional} type. */
  public static boolean isOptional(Key key) {
    return isOptional(key.type().java());
  }

  /**
   * Returns a {@link OptionalType} for {@code type}.
   *
   * @throws IllegalArgumentException if {@code type} is not an {@code Optional} type
   */
  public static OptionalType from(TypeMirror type) {
    checkArgument(isOptional(type), "%s must be an Optional", type);
    return new AutoValue_OptionalType(MoreTypes.equivalence().wrap(MoreTypes.asDeclared(type)));
  }

  /**
   * Returns a {@link OptionalType} for {@code key}'s {@link Key#type() type}.
   *
   * @throws IllegalArgumentException if {@code key.type()} is not an {@code Optional} type
   */
  public static OptionalType from(Key key) {
    return from(key.type().java());
  }
}
