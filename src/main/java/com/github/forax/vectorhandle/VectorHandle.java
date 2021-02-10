package com.github.forax.vectorhandle;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;

import java.io.Serializable;
import java.lang.invoke.MethodHandles.Lookup;

import static com.github.forax.vectorhandle.Impl.FLOAT_SPECIES;
import static com.github.forax.vectorhandle.Impl.INT_SPECIES;

/**
 * A more high level API able to vectorize operations on arrays. Use of the methods {@code apply}
 * that takes an array as destination and several arrays as source and a lambda that specify for
 * each element the operation that should be performed. The implementation automatically derives the
 * operation that should be used on vectors to improve performance.
 *
 * <p>Example
 * {@code
 *   private static final VH = VectorHandle.of(lookup(), float.class, float.class, float.class);
 *   ...
 *   var dest = new float[4];
 *   var a = new float[] { 1, 2, 3, 4 };
 *   var b = new float[] { 4, 4, 4, 4 };
 *   VH.apply(dest, a, b, (x, y) -> x + y * 2);
 *   System.out.println(Arrays.toString(dest));
 * }
 *
 * <p>
 * Note: you can not use a {@code VectorHandle} for more than one lambda, and the lambda
 * can not capture values, do side effects or call an opaque method.
 */
public interface VectorHandle {
  @FunctionalInterface
  interface IIOp extends Serializable {
    int apply(int a);
  }
  @FunctionalInterface
  interface FFFOp extends Serializable {
    float apply(float a, float b);
  }

  Object invoke(Object operator, Object va, Object vb, Object vc, Object vd);

  default void apply(int[] dest, int[] a, IIOp operator) {
    var length = dest.length;
    if (a.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += INT_SPECIES.length()) {
      var mask = INT_SPECIES.indexInRange(i, length);
      var va = IntVector.fromArray(INT_SPECIES, a, i, mask);
      var vc = (IntVector) invoke(operator, va, null, null, null);
      vc.intoArray(dest, i, mask);
    }
  }

  default void apply(float[] dest, float[] a, float[] b, FFFOp operator) {
    var length = dest.length;
    if (a.length != length || b.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += FLOAT_SPECIES.length()) {
      var mask = FLOAT_SPECIES.indexInRange(i, length);
      var va = FloatVector.fromArray(FLOAT_SPECIES, a, i, mask);
      var vb = FloatVector.fromArray(FLOAT_SPECIES, b, i, mask);
      var vc = (FloatVector) invoke(operator, va, vb, null, null);
      vc.intoArray(dest, i, mask);
    }
  }

  static VectorHandle of(Lookup lookup, Class<?> returnType, Class<?>... parameterTypes) {
    var target = Impl.createMH(lookup, returnType, parameterTypes);
    return (operator, va, vb, vc, vd) -> {
      try {
        return target.invoke(operator, va, vb, vc, vd);
      } catch(RuntimeException | Error e) {
        throw e;
      } catch (Throwable t) {
        throw new AssertionError(t);
      }
    };
  }
}
