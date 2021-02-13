package com.github.forax.vectorhandle;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.invoke.MethodHandles.lookup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 5)
@Measurement(iterations = 5, time = 5)
@Fork(value = 1, jvmArgs = {"--add-modules", "jdk.incubator.vector"})
@State(Scope.Benchmark)
public class VectorHandlePerfTest {
  private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
  private static final VectorHandle VH = VectorHandle.of(lookup());

  private static final int SIZE = 100_000;

  private static final int[] DEST;
  private static final int[] A;
  private static final int[] B;
  static {
    var random = new Random(0);
    A = random.ints(SIZE, 0, 100).toArray();
    B = random.ints(SIZE, 0, 100).toArray();
    DEST = new int[SIZE];
  }

  @Benchmark
  public int[] vector_handle() {
    VH.apply(DEST, A, B, (x, y) -> x + y * 2);
    return DEST;
  }

  //@Benchmark
  public int[] handwritten() {
    int i = 0;
    for (; i < INT_SPECIES.loopBound(A.length); i += INT_SPECIES.length()) {
      var va = IntVector.fromArray(INT_SPECIES, A, i);
      var vb = IntVector.fromArray(INT_SPECIES, B, i);
      var vc = va.add(vb.mul(2));
      vc.intoArray(DEST, i);
    }
    for (; i < A.length; i++) {
      var a = A[i];
      var b = B[i];
      DEST[i] = a + b * 2;
    }
    return DEST;
  }

  //@Benchmark
  public int[] no_vector() {
    int length = A.length;
    for (int i = 0; i < length; i++) {
      var a = A[i];
      var b = B[i];
      var c = a + b * 2;
      DEST[i] = c;
    }
    return DEST;
  }
}
