package ro.cs.eo.gdal.reader.info;

import javassist.*;
import org.apache.commons.lang3.StringUtils;

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
    private static final Map<String, Method> compiledExpressions;
    private static final String[] mathMethods;
    private static final String[] replacements;
    private static final ClassPool classPool;
    private static final String singleDeclaration;
    private static final String doubleDeclaration;
    private Class<T> pixelType;

    static {
        bandVariablePattern = Pattern.compile("\\$(\\d{1})");
        compiledExpressions = new HashMap<>();
        classPool = ClassPool.getDefault();
        classPool.importPackage("java.lang.Math");
        final Method[] methods = Math.class.getDeclaredMethods();
        mathMethods = new String[methods.length];
        replacements = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            mathMethods[i] = methods[i].getName();
            replacements[i] = "Math." + mathMethods[i];
        }
        singleDeclaration = "public static double execute(%s values) { return %s; }";
        doubleDeclaration = "public static double execute(%s values1, %s values2) { return %s; }";
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
    public double evaluate(T pixelValue, String expression) throws Exception {
        final Method invoker = compiledExpressions.computeIfAbsent(expression, new Function<String, Method>() {
            @Override
            public Method apply(String value) {
                try {
                    return createInvokable(value, 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return invoke(invoker, pixelValue);
    }
    /**
     * Applies the expression to all of the given input values.
     * At the first invocation for a given expression, the expression is evaluated and compiled.
     * Subsequent invocations will use the compiled version.
     *
     * @param pixelValues    The input values
     * @param expression    The expression to be computed
     */
    public double evaluateAll(T[] pixelValues, String expression) throws Exception {
        final Method invoker = compiledExpressions.computeIfAbsent(expression, new Function<String, Method>() {
            @Override
            public Method apply(String value) {
                try {
                    return createInvokable(value, 1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return invoke(invoker, pixelValues);
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
    public double evaluate(T pixelA, T pixelB, String expression) throws Exception {
        final Method invoker = compiledExpressions.computeIfAbsent(expression, new Function<String, Method>() {
            @Override
            public Method apply(String value) {
                try {
                    return createInvokable(value, 2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return invoke(invoker, pixelA, pixelB);
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

    private Method createInvokable(String code, int nbArgs) throws CannotCompileException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        CtClass clazz = classPool.makeClass(ExpressionEvaluator.class.getPackageName() + ".ExprInvoker" + compiledExpressions.size());
        clazz.setModifiers(Modifier.PUBLIC);
        String exp = code;
        for (int i = 1; i <= nbArgs; i++) {
            exp = exp.replace("$" + i, nbArgs == 1 ? "values" : "values" + i);
        }
        exp = StringUtils.replaceEach(exp, mathMethods, replacements);
        final CtMethod method;
        final String decl = nbArgs == 1
                            ? String.format(singleDeclaration, pixelType.getSimpleName().toLowerCase(), exp)
                            : String.format(doubleDeclaration, pixelType.getSimpleName().toLowerCase(), pixelType.getSimpleName().toLowerCase(), exp);
        method = CtNewMethod.make(decl, clazz);
        clazz.addMethod(method);
        final Class<?> loadedClass = clazz.toClass();
        return nbArgs == 1 ? loadedClass.getMethod("execute", pixelType) : loadedClass.getMethod("execute", pixelType, pixelType);
    }

    private double invoke(Method method, T arg) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (double) method.invoke(null, arg);
    }

    private double invoke(Method method, T[] args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (double) method.invoke(null, args);
    }

    private double invoke(Method method, T arg1, T arg2) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return (double) method.invoke(null, arg1, arg2);
    }
}
