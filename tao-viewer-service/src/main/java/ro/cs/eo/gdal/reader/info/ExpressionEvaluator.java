package ro.cs.eo.gdal.reader.info;

import javassist.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator for mathematical expressions written with functions from java.lang.Math.
 * @param <T>   The type of the values
 */
public class ExpressionEvaluator<T> {
    private static final Pattern bandVariablePattern;
    private static final Map<String, MethodHandle> compiledExpressions;
    private static final String[] mathMethods;
    private static final String[] replacements;
    private static final ClassPool classPool;
    private static final Map<Class<?>, Class<?>> primitiveTypeMap;
    private static final String singleDeclaration;
    private static final String doubleDeclaration;
    private Class<T> pixelType;

    static {
        bandVariablePattern = Pattern.compile("\\$(\\d{1})");
        compiledExpressions = new HashMap<>();
        primitiveTypeMap = new HashMap<>();
        primitiveTypeMap.put(Byte.class, byte.class);
        primitiveTypeMap.put(Short.class, short.class);
        primitiveTypeMap.put(Integer.class, int.class);
        primitiveTypeMap.put(Float.class, float.class);
        primitiveTypeMap.put(Double.class, double.class);
        classPool = ClassPool.getDefault();
        classPool.importPackage("java.lang.Math");
        final Method[] methods = Math.class.getDeclaredMethods();
        mathMethods = new String[methods.length];
        replacements = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            mathMethods[i] = methods[i].getName();
            replacements[i] = "Math." + mathMethods[i];
        }
        singleDeclaration = "public static final double execute(%s value) { return %s; }";
        doubleDeclaration = "public static final double execute(%s value1, %s value2) { return %s; }";
    }

    /**
     * Constructs an evaluator for the given value type
     * @param pixelType The value type
     */
    public ExpressionEvaluator(Class<T> pixelType) {
        this.pixelType = pixelType;
    }

    /**
     * Applies the expression to the given single input value.
     * At the first invocation for a given expression, the expression is evaluated and compiled.
     * Subsequent invocations will use the compiled version.
     *
     * @param pixelValue    The input value
     * @param expression    The expression to be computed
     */
    public double evaluate(T pixelValue, String expression) throws Throwable {
        final String key = expression + ":" + pixelType.getSimpleName();
        synchronized (compiledExpressions) {
            if (!compiledExpressions.containsKey(key)) {
                compiledExpressions.put(key, createHandle(expression, 1));
            }
        }
        final MethodHandle invoker = compiledExpressions.get(key);
        return (double) invoker.invoke(pixelValue);
    }
    /**
     * Applies the expression to all of the given input values.
     * At the first invocation for a given expression, the expression is evaluated and compiled.
     * Subsequent invocations will use the compiled version.
     *
     * @param pixelValues    The input values
     * @param expression    The expression to be computed
     */
    public double evaluateAll(T[] pixelValues, String expression) throws Throwable {
        final String key = expression + ":" + pixelType.getSimpleName();
        synchronized (compiledExpressions) {
            if (!compiledExpressions.containsKey(key)) {
                compiledExpressions.put(key, createHandle(expression, 1));
            }
        }
        final MethodHandle invoker = compiledExpressions.get(key);
        return (double) invoker.invoke(pixelValues);
    }
    /**
     * Applies the expression to the given input values.
     * At the first invocation for a given expression, the expression is evaluated and compiled.
     * Subsequent invocations will use the compiled version.
     *
     * @param pixelA    The first input value
     * @param pixelB    The second input value
     * @param expression    The expression to be computed
     */
    public double evaluate(T pixelA, T pixelB, String expression) throws Throwable {
        final String key = expression + ":" + pixelType.getSimpleName();
        synchronized (compiledExpressions) {
            if (!compiledExpressions.containsKey(key)) {
                compiledExpressions.put(key, createHandle(expression, 2));
            }
        }
        final MethodHandle invoker = compiledExpressions.get(key);
        return (double) invoker.invoke(pixelA, pixelB);
    }

    /**
     * Determines the band indices that are used in the given expression.
     * @param expression    The expression to be evaluated
     */
    public int[] getUsedBandIndices(String expression) {
        final Matcher matcher = bandVariablePattern.matcher(expression);
        int[] indices = null;
        if (matcher.find()) {
            indices = new int[matcher.groupCount()];
            for (int i = 1; i <= indices.length; i++) {
                indices[i - 1] = Integer.parseInt(matcher.group(i));
            }
        }
        return indices;
    }

    private MethodHandle createHandle(String code, int nbArgs) throws CannotCompileException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final String className = ExpressionEvaluator.class.getPackageName() + ".ExprInvoker" + compiledExpressions.size();
        CtClass clazz = classPool.makeClass(className);
        clazz.setModifiers(Modifier.PUBLIC);
        String exp = code;
        final Class<?> actualType = primitiveTypeMap.getOrDefault(pixelType, pixelType);
        final boolean includeCast = !double.class.equals(actualType);
        for (int i = 1; i <= nbArgs; i++) {
            exp = exp.replace("$" + i,
                              (includeCast ? "(double)value" : "value") + (nbArgs == 1 ? "" : i));
        }
        exp = StringUtils.replaceEach(exp, mathMethods, replacements);
        final CtMethod method;
        final String decl = nbArgs == 1
                            ? String.format(singleDeclaration, actualType.getSimpleName(), exp)
                            : String.format(doubleDeclaration, actualType.getSimpleName(), actualType.getSimpleName(), exp);
        method = CtNewMethod.make(decl, clazz);
        clazz.addMethod(method);
        final Class<?> loadedClass = clazz.toClass();
        final MethodType methodType = nbArgs == 1 ? MethodType.methodType(double.class, pixelType) : MethodType.methodType(double.class, pixelType, pixelType);
        return MethodHandles.lookup().findStatic(loadedClass, "execute", methodType);
    }

    private class BasicInvoker implements Function<String, MethodHandle> {
        private int arity;

        public BasicInvoker(int arity) {
            this.arity = arity;
        }

        @Override
        public MethodHandle apply(String value) {
            try {
                return createHandle(value, arity);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
