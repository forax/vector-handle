package com.github.forax.vectorhandle;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.lang.invoke.MethodHandles.lookup;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class VectorHandleTest {
  @Test
  public void applyII() {
    var vh = VectorHandle.of(lookup());

    var dest = new int[3];
    var a = new int[] { 1, 2, 3 };
    vh.apply(dest, a, x -> - x);
    assertArrayEquals(new int[] { -1, -2, -3 }, dest);
  }
  @Test
  public void applyLL() {
    var vh = VectorHandle.of(lookup());

    var dest = new long[3];
    var a = new long[] { 1, 2, 3 };
    vh.apply(dest, a, x -> - x);
    assertArrayEquals(new long[] { -1, -2, -3 }, dest);
  }
  @Test
  public void applyFF() {
    var vh = VectorHandle.of(lookup());

    var dest = new float[3];
    var a = new float[] { 1, 2, 3 };
    vh.apply(dest, a, x -> - x);
    assertArrayEquals(new float[] { -1, -2, -3 }, dest);
  }
  @Test
  public void applyDD() {
    var vh = VectorHandle.of(lookup());

    var dest = new double[3];
    var a = new double[] { 1, 2, 3 };
    vh.apply(dest, a, x -> - x);
    assertArrayEquals(new double[] { -1, -2, -3 }, dest);
  }
  @Test
  public void applyIIHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new int[10_000];
    var a = new int[10_000];
    Arrays.fill(a, 1);
    vh.apply(dest, a, x -> - x);
    var expected = new int[10_000];
    Arrays.fill(expected, -1);
    assertArrayEquals(expected, dest);
  }
  @Test
  public void applyLLHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new long[10_000];
    var a = new long[10_000];
    Arrays.fill(a, 1);
    vh.apply(dest, a, x -> - x);
    var expected = new long[10_000];
    Arrays.fill(expected, -1);
    assertArrayEquals(expected, dest);
  }
  @Test
  public void applyFFHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new float[10_000];
    var a = new float[10_000];
    Arrays.fill(a, 1);
    vh.apply(dest, a, x -> - x);
    var expected = new float[10_000];
    Arrays.fill(expected, -1);
    assertArrayEquals(expected, dest);
  }
  @Test
  public void applyDDHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new double[10_000];
    var a = new double[10_000];
    Arrays.fill(a, 1);
    vh.apply(dest, a, x -> - x);
    var expected = new double[10_000];
    Arrays.fill(expected, -1);
    assertArrayEquals(expected, dest);
  }

  @Test
  public void applyIII() {
    var vh = VectorHandle.of(lookup());

    var dest = new int[4];
    var a = new int[] { 1, 2, 3, 4 };
    var b = new int[] { 4, 4, 4, 4 };
    vh.apply(dest, a, b, (x, y) -> x + y * 2);
    assertArrayEquals(new int[] { 9, 10, 11, 12 }, dest);
  }
  @Test
  public void applyLLL() {
    var vh = VectorHandle.of(lookup());

    var dest = new long[4];
    var a = new long[] { 1, 2, 3, 4 };
    var b = new long[] { 4, 4, 4, 4 };
    vh.apply(dest, a, b, (x, y) -> x + y * 2);
    assertArrayEquals(new long[] { 9, 10, 11, 12 }, dest);
  }
  @Test
  public void applyFFF() {
    var vh = VectorHandle.of(lookup());

    var dest = new float[4];
    var a = new float[] { 1, 2, 3, 4 };
    var b = new float[] { 4, 4, 4, 4 };
    vh.apply(dest, a, b, (x, y) -> x + y * 2);
    assertArrayEquals(new float[] { 9, 10, 11, 12 }, dest);
  }
  @Test
  public void applyDDD() {
    var vh = VectorHandle.of(lookup());

    var dest = new double[4];
    var a = new double[] { 1, 2, 3, 4 };
    var b = new double[] { 4, 4, 4, 4 };
    vh.apply(dest, a, b, (x, y) -> x + y * 2);
    assertArrayEquals(new double[] { 9, 10, 11, 12 }, dest);
  }
  @Test
  public void applyIIIHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new int[10_000];
    var a = new int[10_000];
    Arrays.fill(a, 2);
    var b = new int[10_000];
    Arrays.fill(b, 3);
    vh.apply(dest, a, b, (x, y) -> Math.min(x, y));
    var expected = new int[10_000];
    Arrays.fill(expected, 2);
    assertArrayEquals(expected, dest);
  }
  @Test
  public void applyLLLHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new long[10_000];
    var a = new long[10_000];
    Arrays.fill(a, 2);
    var b = new long[10_000];
    Arrays.fill(b, 3);
    vh.apply(dest, a, b, (x, y) -> Math.min(x, y));
    var expected = new long[10_000];
    Arrays.fill(expected, 2);
    assertArrayEquals(expected, dest);
  }
  @Test
  public void applyFFFHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new float[10_000];
    var a = new float[10_000];
    Arrays.fill(a, 2);
    var b = new float[10_000];
    Arrays.fill(b, 3);
    vh.apply(dest, a, b, (x, y) -> Math.min(x, y));
    var expected = new float[10_000];
    Arrays.fill(expected, 2);
    assertArrayEquals(expected, dest);
  }
  @Test
  public void applyDDDHuge() {
    var vh = VectorHandle.of(lookup());

    var dest = new double[10_000];
    var a = new double[10_000];
    Arrays.fill(a, 2);
    var b = new double[10_000];
    Arrays.fill(b, 3);
    vh.apply(dest, a, b, (x, y) -> Math.min(x, y));
    var expected = new double[10_000];
    Arrays.fill(expected, 2);
    assertArrayEquals(expected, dest);
  }
}