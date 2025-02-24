import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.cs.eo.gdal.reader.info.ExpressionEvaluator;

public class ExpressionTest {
    private double[] values;
    private double[] values2;
    private String simpleExpression;
    private String doubleExpression;

    @BeforeEach
    void setUp() {
        values = new double[1024];
        values2 = new double[1024];
        for (int i = 0; i < 1024; i++) {
            values[i] = Math.random();
            values2[i] = Math.random();
        }
        simpleExpression = "sqrt($1) + 2";
        doubleExpression = "($1 + $2) / 2";
    }

    @Test
    void testSimpleExpression() {
        double[] results = new double[values.length];
        ExpressionEvaluator<Double> evaluator = new ExpressionEvaluator<>(double.class);
        try {
            for (int i = 0; i < values.length; i++) {
                results[i] = evaluator.evaluate(values[i], simpleExpression);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotEquals(results[0], values[0]);
    }

    @Test
    void testDoubleExpression() {
        double[] results = new double[values.length];
        ExpressionEvaluator<Double> evaluator = new ExpressionEvaluator<>(double.class);
        try {
            for (int i = 0; i < values.length; i++) {
                results[i] = evaluator.evaluate(values[i], values2[i], doubleExpression);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotEquals(0.0, results[0]);
    }
}
