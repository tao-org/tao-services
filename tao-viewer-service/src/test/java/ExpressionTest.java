import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.cs.eo.gdal.reader.info.ExpressionEvaluator;

import java.util.Random;

public class ExpressionTest {
    private static final int LENGTH = 1024*1024;
    private short[] values;
    private short[] values2;
    private String simpleExpression;
    private String doubleExpression;

    @BeforeEach
    void setUp() {
        values = new short[LENGTH];
        values2 = new short[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            values[i] = (short) new Random().nextInt();
            values2[i] = (short) new Random().nextInt();
        }
        simpleExpression = "sqrt($1) + 2";
        doubleExpression = "($1 + $2) / 2";
    }

    @Test
    void testSimpleExpression() {
        double[] results = new double[values.length];
        ExpressionEvaluator evaluator = new ExpressionEvaluator<>(short.class);
        try {
            for (int i = 0; i < values.length; i++) {
                results[i] = evaluator.evaluate(values[i], simpleExpression);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotEquals(results[0], values[0]);
    }

    @Test
    void testDoubleExpression() {
        double[] results = new double[values.length];
        ExpressionEvaluator evaluator = new ExpressionEvaluator<>(short.class);
        try {
            for (int i = 0; i < values.length; i++) {
                results[i] = evaluator.evaluate(values[i], values2[i], doubleExpression);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotEquals(0.0, results[0]);
    }
}
