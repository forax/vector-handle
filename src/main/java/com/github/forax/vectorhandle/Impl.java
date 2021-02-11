package com.github.forax.vectorhandle;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.Vector;
import jdk.incubator.vector.VectorSpecies;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SerializedLambda;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;

import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DNEG;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FNEG;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INEG;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LCONST_1;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LNEG;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.V11;

class Impl {
  static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
  static final VectorSpecies<Long> LONG_SPECIES = LongVector.SPECIES_PREFERRED;
  static final VectorSpecies<Float> FLOAT_SPECIES = FloatVector.SPECIES_PREFERRED;
  static final VectorSpecies<Double> DOUBLE_SPECIES = DoubleVector.SPECIES_PREFERRED;

  static MethodHandle createMH(Lookup lookup, Class<?> returnType, Class<?>[] parameterTypes) {
    requireNonNull(lookup);
    requireNonNull(returnType);
    for(var parameterType: parameterTypes) {
      requireNonNull(parameterType);
    }
    if (parameterTypes.length > 4) {
      throw new IllegalArgumentException("too many vectors, not yet supported");
    }
    return new CallSiteCache(lookup, returnType, parameterTypes).dynamicInvoker();
  }

  private static class CallSiteCache extends MutableCallSite {
    private static final MethodHandle FALLBACK, POINTER_CHECK, ERROR;
    static {
      var lookup = lookup();
      try {
        FALLBACK = lookup.findVirtual(CallSiteCache.class, "fallback",
            methodType(Object.class, Object.class, Object.class, Object.class, Object.class, Object.class));
        POINTER_CHECK = lookup.findStatic(CallSiteCache.class, "pointerCheck",
            methodType(boolean.class, Object.class, Object.class));
        ERROR = lookup.findStatic(CallSiteCache.class, "error", methodType(void.class));
      } catch (NoSuchMethodException | IllegalAccessException e) {
        throw new AssertionError(e);
      }
    }

    private final Lookup lookup;
    private final Class<?> returnType;
    private final Class<?>[] parameterTypes;

    private CallSiteCache(Lookup lookup, Class<?> returnType, Class<?>[] parameterTypes) {
      super(MethodType.genericMethodType(5));
      this.lookup = lookup;
      this.returnType = returnType;
      this.parameterTypes = parameterTypes;
      setTarget(FALLBACK.bindTo(this));
    }

    private static boolean pointerCheck(Object o1, Object o2) {
      return o1 == o2;
    }

    private static void error() {
      throw new IllegalStateException("the operator lambda is not constant");
    }

    private Object fallback(Object operator, Object va, Object vb, Object vc, Object vd) throws Throwable {
      var serializedLambda = invokeWriteReplace(operator, lookup);
      //System.err.println("serializedLambda " + serializedLambda);

      if (serializedLambda.getCapturedArgCount() != 0) {
        throw new IllegalStateException("The operator lambda should not capture any variable value");
      }
      if (serializedLambda.getImplMethodKind() != Opcodes.H_INVOKESTATIC) {
        throw new IllegalStateException("The operator lambda should be desugared as a static method");
      }

      var signature = Arrays.stream(parameterTypes).map(Class::descriptorString).collect(joining("", "(", ')' + returnType.descriptorString()));
      if (!serializedLambda.getFunctionalInterfaceMethodSignature().equals(signature)) {
        throw new IllegalStateException("The operator lambda descriptor " + serializedLambda.getFunctionalInterfaceMethodSignature()
            + " do not match the VectorHandle descriptor " + signature);
      }

      var bytecode = loadBytecode(lookup.lookupClass(), serializedLambda.getImplClass());
      var expr = walk(bytecode, serializedLambda.getImplMethodName(), serializedLambda.getImplMethodSignature());
      //System.err.println("expr " + expr);

      var returnExprType = Expr.Type.from(returnType);
      var parameterExprTypes = Arrays.stream(parameterTypes).map(Expr.Type::from).toArray(Expr.Type[]::new);
      var classData = gen(lookup.lookupClass(), expr, returnExprType, parameterExprTypes);

      var hiddenLookup = lookup.defineHiddenClass(classData, true, Lookup.ClassOption.NESTMATE, Lookup.ClassOption.STRONG);

      var mh = hiddenLookup.findStatic(hiddenLookup.lookupClass(), "lambda",
            methodType(returnExprType.vectorClass, Arrays.stream(parameterExprTypes).map(type -> type.vectorClass).toArray(Class[]::new)));

      // adjust if too many vectors
      if (1 + mh.type().parameterCount() != type().parameterCount()) {
        mh = dropArguments(mh, mh.type().parameterCount(), Collections.nCopies(type().parameterCount() - mh.type().parameterCount() - 1, Object.class));
      }

      var target =  dropArguments(mh, 0, Object.class);
      var guard = guardWithTest(POINTER_CHECK.bindTo(operator),
          target,
          dropArguments(ERROR, 0, target.type().parameterList()).asType(target.type() /* patch return type */));

      setTarget(guard.asType(type()));  // erase types

      return mh.invokeWithArguments(va, vb, vc, vd);
    }
  }

  private static SerializedLambda invokeWriteReplace(Object lambda, Lookup lookup) {
    MethodHandle writeReplace;
    try {
      writeReplace = lookup.findVirtual(lambda.getClass(), "writeReplace", methodType(Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }

    try {
      return (SerializedLambda) writeReplace.invoke(lambda);
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  private static byte[] loadBytecode(Class<?> lookupClass, String implClassName) {
    try(var input = lookupClass.getResourceAsStream("/" + implClassName + ".class")) {
      if (input == null) {
        throw new IllegalStateException("can not access to the bytecode of " + implClassName + " using lookup " + lookupClass.getName());
      }
      return input.readAllBytes();
    } catch(IOException e) {
      throw new AssertionError(e);
    }
  }

  private static Expr walk(byte[] bytecode, String methodName, String methodDescriptor) {
    var reader = new ClassReader(bytecode);
    var stack = new ArrayDeque<Expr>();
    reader.accept(new ClassVisitor(ASM9) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (!name.equals(methodName) || !descriptor.equals(methodDescriptor)) {
          return null;
        }

        if ((access & Opcodes.ACC_STATIC) ==0) {
          throw new AssertionError("lambda method " + methodName + methodDescriptor + " is not static");
        }

        // local variables need to be re-numbered because long/double takes two slots
        var parameterTypes = Type.getArgumentTypes(descriptor);
        var varIndexArray = new int[Type.getArgumentsAndReturnSizes(descriptor) >> 2];
        var slot = 0;
        for(var i = 0; i < parameterTypes.length; i++) {
          varIndexArray[slot] = i;
          slot += parameterTypes[i].getSize();
        }

        return new MethodVisitor(ASM9) {
          @Override
          public void visitVarInsn(int opcode, int var) {
            stack.push(switch(opcode) {
              case ILOAD, LLOAD, FLOAD, DLOAD -> new Expr.Load(Expr.Type.of(opcode - ILOAD), varIndexArray[var]);
              default -> throw new AssertionError("invalid var opcode: " + opcode + " " + var);
            });
          }

          @Override
          public void visitLdcInsn(Object value) {
            Expr expr;
            if (value instanceof Integer) {
              expr = new Expr.Literal(Expr.Type.INT, value);
            } else if (value instanceof Long) {
              expr = new Expr.Literal(Expr.Type.LONG, value);
            } else if (value instanceof Float) {
              expr = new Expr.Literal(Expr.Type.FLOAT, value);
            } else if (value instanceof Double) {
              expr = new Expr.Literal(Expr.Type.DOUBLE, value);
            } else {
              throw new AssertionError("invalid ldc opcode: " + value);
            }
            stack.push(expr);
          }

          @Override
          public void visitInsn(int opcode) {
            switch (opcode) {
              case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> {
                stack.push(new Expr.Literal(Expr.Type.INT, opcode - ICONST_0));
              }
              case LCONST_0, LCONST_1 -> {
                stack.push(new Expr.Literal(Expr.Type.LONG, (long) (opcode - LCONST_0)));
              }
              case FCONST_0, FCONST_1, FCONST_2 -> {
                stack.push(new Expr.Literal(Expr.Type.FLOAT, (float) (opcode - FCONST_0)));
              }
              case DCONST_0, DCONST_1 -> {
                stack.push(new Expr.Literal(Expr.Type.DOUBLE, (double) (opcode - DCONST_0)));
              }
              case INEG, LNEG, FNEG, DNEG -> {
                var expr = stack.pop();
                stack.push(new Expr.UnOp(Expr.Type.of(opcode - INEG), Expr.UnOp.Kind.neg, expr));
              }
              case IADD, LADD, FADD, DADD -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.of(opcode - IADD), Expr.BinOp.Kind.add, expr1, expr2));
              }
              case ISUB, LSUB, FSUB, DSUB -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.of(opcode - ISUB), Expr.BinOp.Kind.sub, expr1, expr2));
              }
              case IMUL, LMUL, FMUL, DMUL -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.of(opcode - IMUL), Expr.BinOp.Kind.mul, expr1, expr2));
              }
              case IDIV, LDIV, FDIV, DDIV -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.of(opcode - IDIV), Expr.BinOp.Kind.div, expr1, expr2));
              }
              case IRETURN, LRETURN, FRETURN, DRETURN -> {}
              default -> {
                throw new AssertionError("invalid insn: " + opcode);
              }
            }
          }

          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            switch(owner + "." + name + descriptor) {
              case "java/lang/Math.min(II)I" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.INT, Expr.BinOp.Kind.min, expr1, expr2));
              }
              case "java/lang/Math.min(JJ)J" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.LONG, Expr.BinOp.Kind.min, expr1, expr2));
              }
              case "java/lang/Math.min(FF)F" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.FLOAT, Expr.BinOp.Kind.min, expr1, expr2));
              }
              case "java/lang/Math.min(DD)D" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.DOUBLE, Expr.BinOp.Kind.min, expr1, expr2));
              }
              case "java/lang/Math.max(II)I" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.INT, Expr.BinOp.Kind.max, expr1, expr2));
              }
              case "java/lang/Math.max(JJ)J" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.LONG, Expr.BinOp.Kind.max, expr1, expr2));
              }
              case "java/lang/Math.max(FF)F" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.FLOAT, Expr.BinOp.Kind.max, expr1, expr2));
              }
              case "java/lang/Math.max(DD)D" -> {
                var expr2 = stack.pop();
                var expr1 = stack.pop();
                stack.push(new Expr.BinOp(Expr.Type.DOUBLE, Expr.BinOp.Kind.max, expr1, expr2));
              }
              default -> {
                throw new AssertionError("invalid method insn: " + opcode + " " + owner + "." + name + descriptor);
              }
            }
          }

          @Override
          public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (opcode != GETSTATIC) {
              throw new AssertionError("invalid field insn: " + opcode +  " " + owner + "." + name + descriptor);
            }
            switch(descriptor) {
              case "I", "L", "F", "D" -> {
                stack.push(new Expr.Constant(Expr.Type.from(descriptor), owner, name));
              }
              default -> throw new AssertionError("invalid field insn: " + opcode +  " " + owner + "." + name + descriptor);
            }
          }

          @Override
          public void visitIntInsn(int opcode, int operand) {
            throw new AssertionError("invalid int insn: " + opcode + " " + operand);
          }
          @Override
          public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            throw new AssertionError("invalid indy insn: " + bootstrapMethodHandle + Arrays.toString(bootstrapMethodArguments));
          }
          @Override
          public void visitIincInsn(int var, int increment) {
            throw new AssertionError("invalid inc insn: " + var + " " + increment);
          }
          @Override
          public void visitJumpInsn(int opcode, Label label) {
            throw new AssertionError("invalid jump insn: " + opcode);
          }
          @Override
          public void visitTypeInsn(int opcode, String type) {
            throw new AssertionError("invalid type insn: " + opcode + " " + type);
          }
          @Override
          public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            throw new AssertionError("invalid lookup switch insn");
          }
          @Override
          public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            throw new AssertionError("invalid table switch insn");
          }
          @Override
          public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            throw new AssertionError("invalid table new array insn " + descriptor + " " + numDimensions);
          }
          @Override
          public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            throw new AssertionError("invalid try/catch block");
          }
        };
      }
    }, SKIP_DEBUG | SKIP_FRAMES);
    return stack.pop();
  }

  private /*sealed*/ interface Expr {
    enum Type {
      INT(IntVector.class, int.class),
      LONG(LongVector.class, long.class),
      FLOAT(FloatVector.class, float.class),
      DOUBLE(DoubleVector.class, double.class);

      private final Class<?> vectorClass;
      private final String vectorName;
      private final String descriptor;

      Type(Class<?> vectorClass, Class<?> elementClass) {
        this.vectorClass = vectorClass;
        vectorName = nameFrom(vectorClass);
        descriptor = elementClass.descriptorString();
      }

      private static final Type[] VALUES = values();

      public static Type of(int offset) {
        return VALUES[offset];
      }

      public static Type from(String descriptor) {
        return switch (descriptor) {
          case "I" -> Type.INT;
          case "J" -> Type.LONG;
          case "F" -> Type.FLOAT;
          case "D" -> Type.DOUBLE;
          default -> throw new IllegalStateException("invalid descriptor " + descriptor);
        };
      }

      public static Type from(Class<?> type) {
        return from(type.descriptorString());
      }
    }

    record Literal(Type type, Object constant) implements Expr {}
    record Constant(Type type, String owner, String name) implements Expr {}
    record Load(Type type, int variable) implements Expr {}
    record UnOp(Type type, Kind kind, Expr expr) implements Expr {
      enum Kind { neg; }
    }
    record BinOp(Type type, Kind kind, Expr left, Expr right) implements Expr {
      enum Kind { add, sub, mul, div, min, max; }
    }
  }

  private static String nameFrom(Class<?> clazz) {
    return clazz.getName().replace('.', '/');
  }

  private static final String VECTOR_SPECIES_DESC = VectorSpecies.class.descriptorString();
  private static final String VECTOR_DESC = Vector.class.descriptorString();

  private static byte[] gen(Class<?> lookupClass, Expr expr, Expr.Type returnType, Expr.Type[] parameterTypes) {
    var className = nameFrom(lookupClass) + "$Template";
    var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    writer.visit(V11,ACC_FINAL| ACC_SUPER, className, null, "java/lang/Object", null);
    var desc = Arrays.stream(parameterTypes)
        .map(type -> type.vectorClass.descriptorString())
        .collect(joining("", "(", ")" + returnType.vectorClass.descriptorString()));
    var mv = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "lambda", desc, null, null);
    mv.visitCode();
    gen(expr, mv);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(-1, -1);
    mv.visitEnd();
    writer.visitEnd();
    return writer.toByteArray();
  }

  private static void gen(Expr expr, MethodVisitor mv) {
    if (expr instanceof Expr.Literal literal) {
      mv.visitFieldInsn(GETSTATIC, literal.type.vectorName, "SPECIES_PREFERRED", VECTOR_SPECIES_DESC);
      mv.visitLdcInsn(literal.constant);
      mv.visitMethodInsn(INVOKESTATIC, literal.type.vectorName, "broadcast", '(' + VECTOR_SPECIES_DESC + literal.type.descriptor + ')' + literal.type.vectorClass.descriptorString(), false);
    } else if (expr instanceof Expr.Constant constant) {
      mv.visitFieldInsn(GETSTATIC, constant.type.vectorName, "SPECIES_PREFERRED", VECTOR_SPECIES_DESC);
      mv.visitFieldInsn(GETSTATIC, constant.owner, constant.name, constant.type.descriptor);
      mv.visitMethodInsn(INVOKESTATIC, constant.type.vectorName, "broadcast", '(' + VECTOR_SPECIES_DESC + constant.type.descriptor + ')' + constant.type.vectorClass.descriptorString(), false);
    } else if (expr instanceof Expr.Load load) {
      mv.visitVarInsn(ALOAD, load.variable);
    } else if (expr instanceof Expr.UnOp unOp) {
      gen(unOp.expr, mv);
      var vectorDesc = unOp.type.vectorClass.descriptorString();
      mv.visitMethodInsn(INVOKEVIRTUAL, unOp.type.vectorName, unOp.kind.name(),   "()" + vectorDesc, false);
    } else if (expr instanceof Expr.BinOp binOp) {
      gen(binOp.left, mv);
      gen(binOp.right, mv);
      var vectorDesc = binOp.type.vectorClass.descriptorString();
      mv.visitMethodInsn(INVOKEVIRTUAL, binOp.type.vectorName, binOp.kind.name(),   '(' + VECTOR_DESC + ')' + vectorDesc, false);
    } else {
      throw new AssertionError("invalid expression " + expr.getClass().getName());
    }
  }
}
