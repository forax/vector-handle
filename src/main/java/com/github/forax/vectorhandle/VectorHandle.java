package com.github.forax.vectorhandle;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;

import java.io.Serializable;
import java.lang.invoke.MethodHandles.Lookup;

import static com.github.forax.vectorhandle.Impl.DOUBLE_SPECIES;
import static com.github.forax.vectorhandle.Impl.FLOAT_SPECIES;
import static com.github.forax.vectorhandle.Impl.INT_SPECIES;
import static com.github.forax.vectorhandle.Impl.LONG_SPECIES;

/**
 * A more high level API able to vectorize operations on arrays. Use of the methods {@code apply}
 * that takes an array as destination and several arrays as source and a lambda that specify for
 * each element the operation that should be performed. The implementation automatically derives the
 * operation that should be used on vectors to improve performance.
 *
 * <p>Example
 * <pre>
 *   private static final VH = VectorHandle.of(lookup(), float.class, float.class, float.class);
 *   ...
 *   var dest = new float[4];
 *   var a = new float[] { 1, 2, 3, 4 };
 *   var b = new float[] { 4, 4, 4, 4 };
 *   VH.apply(dest, a, b, (x, y) -> x + y * 2);
 *   System.out.println(Arrays.toString(dest));
 * }
 * </pre>
 *
 * <p>
 * Note: you can not use a {@code VectorHandle} for more than one lambda, and the lambda
 * can not capture values, do side effects or call an opaque/unknown method.
 */
public interface VectorHandle {
  @FunctionalInterface
  interface IIOp extends Serializable {
    int apply(int a);
  }
  @FunctionalInterface
  interface LLOp extends Serializable {
    long apply(long a);
  }
  @FunctionalInterface
  interface FFOp extends Serializable {
    float apply(float a);
  }
  @FunctionalInterface
  interface DDOp extends Serializable {
    double apply(double a);
  }

  @FunctionalInterface
  interface IIIOp extends Serializable {
    int apply(int a, int b);
  }
  @FunctionalInterface
  interface LLLOp extends Serializable {
    long apply(long a, long b);
  }
  @FunctionalInterface
  interface FFFOp extends Serializable {
    float apply(float a, float b);
  }
  @FunctionalInterface
  interface DDDOp extends Serializable {
    double apply(double a, double b);
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
  default void apply(long[] dest, long[] a, LLOp operator) {
    var length = dest.length;
    if (a.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += LONG_SPECIES.length()) {
      var mask = LONG_SPECIES.indexInRange(i, length);
      var va = LongVector.fromArray(LONG_SPECIES, a, i, mask);
      var vc = (LongVector) invoke(operator, va, null, null, null);
      vc.intoArray(dest, i, mask);
    }
  }
  default void apply(float[] dest, float[] a, FFOp operator) {
    var length = dest.length;
    if (a.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += FLOAT_SPECIES.length()) {
      var mask = FLOAT_SPECIES.indexInRange(i, length);
      var va = FloatVector.fromArray(FLOAT_SPECIES, a, i, mask);
      var vc = (FloatVector) invoke(operator, va, null, null, null);
      vc.intoArray(dest, i, mask);
    }
  }
  default void apply(double[] dest, double[] a, DDOp operator) {
    var length = dest.length;
    if (a.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += DOUBLE_SPECIES.length()) {
      var mask = DOUBLE_SPECIES.indexInRange(i, length);
      var va = DoubleVector.fromArray(DOUBLE_SPECIES, a, i, mask);
      var vc = (DoubleVector) invoke(operator, va, null, null, null);
      vc.intoArray(dest, i, mask);
    }
  }


  default void apply(int[] dest, int[] a, int[] b, IIIOp operator) {
    var length = dest.length;
    if (a.length != length || b.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += INT_SPECIES.length()) {
      var mask = INT_SPECIES.indexInRange(i, length);
      var va = IntVector.fromArray(INT_SPECIES, a, i, mask);
      var vb = IntVector.fromArray(INT_SPECIES, b, i, mask);
      var vc = (IntVector) invoke(operator, va, vb, null, null);
      vc.intoArray(dest, i, mask);
    }
  }
  default void apply(long[] dest, long[] a, long[] b, LLLOp operator) {
    var length = dest.length;
    if (a.length != length || b.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += LONG_SPECIES.length()) {
      var mask = LONG_SPECIES.indexInRange(i, length);
      var va = LongVector.fromArray(LONG_SPECIES, a, i, mask);
      var vb = LongVector.fromArray(LONG_SPECIES, b, i, mask);
      var vc = (LongVector) invoke(operator, va, vb, null, null);
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
  default void apply(double[] dest, double[] a, double[] b, DDDOp operator) {
    var length = dest.length;
    if (a.length != length || b.length != length) {
      throw new IllegalArgumentException("wrong length");
    }
    for (int i = 0; i < length; i += DOUBLE_SPECIES.length()) {
      var mask = DOUBLE_SPECIES.indexInRange(i, length);
      var va = DoubleVector.fromArray(DOUBLE_SPECIES, a, i, mask);
      var vb = DoubleVector.fromArray(DOUBLE_SPECIES, b, i, mask);
      var vc = (DoubleVector) invoke(operator, va, vb, null, null);
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
