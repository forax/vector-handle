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
 *   private static final VectorHandle VH = VectorHandle.of(lookup());
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
  /**
   * A function that takes an int and returns an int.
   */
  @FunctionalInterface
  interface IIOp extends Serializable {
    /**
     * A function that takes an int and returns an int.
     * @param a an int.
     * @return an int.
     */
    int apply(int a);
  }

  /**
   * A function that takes a long and returns a long.
   */
  @FunctionalInterface
  interface LLOp extends Serializable {
    /**
     * A function that takes a long and returns a long.
     * @param a a long.
     * @return a long.
     */
    long apply(long a);
  }

  /**
   * A function that takes a float and returns a float.
   */
  @FunctionalInterface
  interface FFOp extends Serializable {
    /**
     * A function that takes a float and returns a float.
     * @param a a float.
     * @return a float.
     */
    float apply(float a);
  }

  /**
   * A function that takes a double and returns a double.
   */
  @FunctionalInterface
  interface DDOp extends Serializable {
    /**
     * A function that takes a double and returns a double.
     * @param a a double.
     * @return a double.
     */
    double apply(double a);
  }

  /**
   * A function that takes two ints and returns an int.
   */
  @FunctionalInterface
  interface IIIOp extends Serializable {
    /**
     * A function that takes two ints and returns an int.
     * @param a an int.
     * @param b an int.
     * @return an int.
     */
    int apply(int a, int b);
  }

  /**
   * A function that takes two longs and returns a long.
   */
  @FunctionalInterface
  interface LLLOp extends Serializable {
    /**
     * A function that takes two longs and returns a long.
     * @param a a long.
     * @param b a long.
     * @return a long.
     */
    long apply(long a, long b);
  }

  /**
   * A function that takes two floats and returns a float.
   */
  @FunctionalInterface
  interface FFFOp extends Serializable {
    /**
     * A function that takes two floats and returns a float.
     * @param a a float.
     * @param b a float.
     * @return a float.
     */
    float apply(float a, float b);
  }

  /**
   * A function that takes two doubles and returns a double.
   */
  @FunctionalInterface
  interface DDDOp extends Serializable {
    /**
     * A function that takes two doubles and returns a double.
     * @param a a double.
     * @param b a double.
     * @return a double.
     */
    double apply(double a, double b);
  }

  /**
   * Apply an operator specified by lambda on several vectors.
   *
   * The lambda is first converted into an operator by
   * transforming any operations used by the lambda into the equivalent operations
   * on vectors. By example, the operation {@code +} is converted to the operation {@code Vector.add(Vector)}.
   *
   * The number of vector and the number of parameters of the lambda must match.
   *
   * The type of the vectors must match the type of the parameters/return type of the lambda.
   * <pre>
   *   lambda parameter | vector type
   *   ------------------------------
   *     int            |  IntVector
   *     long           |  LongVector
   *     float          |  FloatVector
   *     double         |  DoubleVector
   * </pre>
   *
   * @param lambda a lambda that takes at most 4 parameters.
   * @param va a vector or null if the lambda as no first parameter.
   * @param vb a vector or null if the lambda as no second parameter.
   * @param vc a vector or null if the lambda as no third parameter.
   * @param vd a vector or null if the lambda as no forth parameter.
   * @return a vector
   */
  Object invoke(Object lambda, Object va, Object vb, Object vc, Object vd);

  /**
   * Apply the operator on each values of the array {@code a} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and {@code b} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and {@code b} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and {@code b} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Apply the operator on each values of the array {@code a} and {@code b} and store
   * each result in the array {@code dest}.
   * @param dest the destination array.
   * @param a the array of parameters.
   * @param operator a lambda that specify the operator.
   * @throws IllegalArgumentException if the arrays does not have the same length.
   * @throws IllegalStateException if the lambda can not be converted to an operator.
   */
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

  /**
   * Creates a {@link VectorHandle} with a Lookup.
   *
   * When a method {@code apply} is called, the {@code lookup} is used to
   * transform the lambda into a tree of expressions and this tree is then
   * converted to a new lambda using the vector operations.
   *
   * The {@code lookup} must be created on a class that can see the lambda declaration
   * so the class containing a method containing the lambda declaration and the lookup class must be
   * either the same class or class inside the same compilation unit (i.e. the same Java source file).
   *
   * In term of performance, a {@link VectorHandle} should always be stored into a static final field.
   * <pre>
   *  private static final VectorHandle VH = VectorHandle.of(lookup());
   * </pre>
   *
   * @param lookup a lookup used to access the lambda passed to the method {@code apply}.
   * @return a new {@link VectorHandle}
   */
  static VectorHandle of(Lookup lookup) {
    var target = Impl.createMH(lookup);
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
