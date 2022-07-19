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

package dagger.internal.codegen.xprocessing;

import androidx.room.compiler.processing.XAnnotationValue;
import com.google.common.base.Equivalence;

// TODO(bcorso): Consider moving these methods into XProcessing library.
/** A utility class for {@link XAnnotationValue} helper methods. */
public final class XAnnotationValues {
  private static final Equivalence<XAnnotationValue> XANNOTATION_VALUE_EQUIVALENCE =
      new Equivalence<XAnnotationValue>() {
        @Override
        protected boolean doEquivalent(XAnnotationValue left, XAnnotationValue right) {
          if (left.hasAnnotationValue()) {
            return right.hasAnnotationValue()
                && XAnnotations.equivalence().equivalent(left.asAnnotation(), right.asAnnotation());
          } else if (left.hasListValue()) {
            return right.hasListValue()
                && XAnnotationValues.equivalence()
                    .pairwise()
                    .equivalent(left.asAnnotationValueList(), right.asAnnotationValueList());
          } else if (left.hasTypeValue()) {
            return right.hasTypeValue()
                && XTypes.equivalence().equivalent(left.asType(), right.asType());
          }
          return left.getValue().equals(right.getValue());
        }

        @Override
        protected int doHash(XAnnotationValue value) {
          if (value.hasAnnotationValue()) {
            return XAnnotations.equivalence().hash(value.asAnnotation());
          } else if (value.hasListValue()) {
            return XAnnotationValues.equivalence().pairwise().hash(value.asAnnotationValueList());
          } else if (value.hasTypeValue()) {
            return XTypes.equivalence().hash(value.asType());
          }
          return value.getValue().hashCode();
        }

        @Override
        public String toString() {
          return "XAnnotationValues.equivalence()";
        }
      };

  /** Returns an {@link Equivalence} for {@link XAnnotationValue}. */
  public static Equivalence<XAnnotationValue> equivalence() {
    return XANNOTATION_VALUE_EQUIVALENCE;
  }

  private XAnnotationValues() {}
}
