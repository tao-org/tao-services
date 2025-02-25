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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluator for mathematical expressions written with functions from java.lang.Math.
 * @param <T>   The type of the input values
 * @param <R>   The type of the return value
 */
public class ExpressionEvaluator<T, R> {
    private static final Pattern bandVariablePattern;
    private static final Pattern functionPattern;
    private static final Map<String, MethodHandle> compiledExpressions;
    private static final String[] mathMethods;
    private static final String[] replacements;
    private static final Map<Class<? extends Number>, String[]> replacementsWithCast;
    private static final ClassPool classPool;
    private static final Map<Class<?>, Class<?>> primitiveTypeMap;
    private static final String singleDeclaration;
    private static final String doubleDeclaration;
    private static final String classPrefix;
    private Class<T> pixelType;
    private Class<R> returnType;

    static {
        bandVariablePattern = Pattern.compile("\\$(\\d{1})");
        functionPattern = Pattern.compile("([a-zA-Z]+\\()\\w+(,?)\\w+(,?)\\)");
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
        replacementsWithCast = new HashMap<>() {{
            put(byte.class, new String[methods.length]);
            put(short.class, new String[methods.length]);
            put(int.class, new String[methods.length]);
            put(float.class, new String[methods.length]);
            put(double.class, new String[methods.length]);
        }};
        for (int i = 0; i < methods.length; i++) {
            mathMethods[i] = methods[i].getName();
            replacements[i] = "Math." + mathMethods[i];
            replacementsWithCast.get(byte.class)[i] = "(byte)Math." + mathMethods[i];
            replacementsWithCast.get(short.class)[i] = "(short)Math." + mathMethods[i];
            replacementsWithCast.get(int.class)[i] = "(int)Math." + mathMethods[i];
            replacementsWithCast.get(float.class)[i] = "(float)Math." + mathMethods[i];
            replacementsWithCast.get(double.class)[i] = "(double)Math." + mathMethods[i];
        }
        singleDeclaration = "public static final %s execute(%s value) { return %s; }";
        doubleDeclaration = "public static final %s execute(%s value1, %s value2) { return %s; }";
        classPrefix = ExpressionEvaluator.class.getPackageName() + ".ExprInvoker";
    }

    /**
     * Constructs an evaluator for the given value type
     * @param pixelType The value type
     */
    public ExpressionEvaluator(Class<T> pixelType, Class<R> returnType) {
        this.pixelType = pixelType;
        this.returnType = returnType;
    }

    /**
     * Applies the expression to the given single input value.
     * At the first invocation for a given expression, the expression is evaluated and compiled.
     * Subsequent invocations will use the compiled version.
     *
     * @param pixelValue    The input value
     * @param expression    The expression to be computed
     */
    public R evaluate(T pixelValue, String expression) throws Throwable {
        final String key = expression + ":" + pixelType.getSimpleName();
        synchronized (compiledExpressions) {
            if (!compiledExpressions.containsKey(key)) {
                compiledExpressions.put(key, createHandle(expression, returnType, 1));
            }
        }
        final MethodHandle invoker = compiledExpressions.get(key);
        return (R) invoker.invoke(pixelValue);
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
    public R evaluate(T pixelA, T pixelB, String expression) throws Throwable {
        final String key = expression + ":" + pixelType.getSimpleName();
        synchronized (compiledExpressions) {
            if (!compiledExpressions.containsKey(key)) {
                compiledExpressions.put(key, createHandle(expression, returnType, 2));
            }
        }
        final MethodHandle invoker = compiledExpressions.get(key);
        return (R) invoker.invoke(pixelA, pixelB);
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

    private MethodHandle createHandle(String code, Class<?> returnType, int nbArgs)
            throws CannotCompileException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        try {
            final String className = classPrefix + compiledExpressions.size();
            final CtClass clazz = classPool.makeClass(className);
            clazz.setModifiers(Modifier.PUBLIC);
            String exp = code;
            final Class<?> paramType = primitiveTypeMap.getOrDefault(pixelType, pixelType);
            final Class<?> retType = primitiveTypeMap.getOrDefault(returnType, returnType);
            final String retTypeName = retType.getSimpleName();
            final boolean includeCast = !retType.equals(paramType);
            // Replace parameters placeholders with (optionally casted) arguments
            for (int i = 1; i <= nbArgs; i++) {
                exp = exp.replace("$" + i,
                                  (includeCast ? "(" + retTypeName + ")" : "") + "value" + (nbArgs == 1 ? "" : i));
            }
            // For functions from java.lang.Math, cast the arguments to double)
            final Matcher matcher = functionPattern.matcher(exp);
            if (matcher.find()) {
                String group;
                for (int i = 0; i < matcher.groupCount(); i++) {
                    if (!(group = matcher.group(i + 1)).isEmpty()) {
                        exp = exp.replace(group, group + "(double)");
                    }
                }
            }
            // Qualify the function names with Math.
            final String[] strings = replacementsWithCast.get(retType);
            exp = StringUtils.replaceEach(exp, mathMethods, strings);
            final String decl = nbArgs == 1
                                ? String.format(singleDeclaration, retTypeName, paramType.getSimpleName(), exp)
                                : String.format(doubleDeclaration, retTypeName, paramType.getSimpleName(), paramType.getSimpleName(), exp);
            clazz.addMethod(CtNewMethod.make(decl, clazz));
            final Class<?> loadedClass = clazz.toClass();
            final MethodType methodType = nbArgs == 1
                                          ? MethodType.methodType(retType, pixelType)
                                          : MethodType.methodType(retType, pixelType, pixelType);
            return MethodHandles.lookup().findStatic(loadedClass, "execute", methodType);
        } catch (Throwable t) {
            Logger.getLogger(ExpressionEvaluator.class.getName()).severe(t.getMessage());
            throw new RuntimeException(t);
        }
    }
}
