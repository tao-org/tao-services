package ro.cs.eo.gdal.reader.info;

import bsh.EvalError;
import bsh.Interpreter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionEvaluator {
    private static final Interpreter interpreter;
    private static final Pattern bandVariablePattern;

    static {
        interpreter = new Interpreter();
        interpreter.getNameSpace().importStatic(java.lang.Math.class);
        bandVariablePattern = Pattern.compile("\\$(\\d{1})");
    }
    public static <T> T evaluate(double pixelValue, String expression) throws EvalError {
        return (T) interpreter.eval(expression.replace("$1", String.valueOf(pixelValue)));
    }

    public static <T> T evaluate(double[] pixelValues, String expression) throws EvalError {
        String exp = expression;
        for (int i = 1; i <= pixelValues.length; i++) {
            exp = exp.replace("$" + i, String.valueOf(pixelValues[i - 1]));
        }
        return (T) interpreter.eval(exp);
    }

    public static int[] getUsedBandIndices(String expression) {
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

}
