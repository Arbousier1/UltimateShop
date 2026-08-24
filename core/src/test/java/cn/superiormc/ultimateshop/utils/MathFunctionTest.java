package cn.superiormc.ultimateshop.utils;

import net.momirealms.sparrow.expr.ExpressionParseException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathFunctionTest {

    private static final MathExpressionEvaluator EVALUATOR = new MathExpressionEvaluator();
    private static int passed;
    private static int failed;

    static BigDecimal calc(String expression) {
        double raw = EVALUATOR.evaluate(expression);
        return BigDecimal.valueOf(raw).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    static void check(String name, String expression, BigDecimal expected) {
        try {
            BigDecimal result = calc(expression);
            if (result.compareTo(expected) == 0) {
                passed++;
                System.out.println("  PASS: " + name + " = " + result);
            } else {
                failed++;
                System.out.println("  FAIL: " + name);
                System.out.println("        expected: " + expected);
                System.out.println("        got     : " + result);
            }
        } catch (Exception exception) {
            failed++;
            System.out.println("  FAIL: " + name + " (threw "
                    + exception.getClass().getSimpleName() + ": " + exception.getMessage() + ")");
        }
    }

    static void checkError(String name, String expression,
                           Class<? extends Throwable> expectedType) {
        try {
            BigDecimal result = calc(expression);
            failed++;
            System.out.println("  FAIL: " + name + " (expected "
                    + expectedType.getSimpleName() + ", got " + result + ")");
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                passed++;
                System.out.println("  PASS: " + name + " rejected with "
                        + throwable.getClass().getSimpleName());
            } else {
                failed++;
                System.out.println("  FAIL: " + name + " (expected "
                        + expectedType.getSimpleName() + ", got "
                        + throwable.getClass().getSimpleName() + ": "
                        + throwable.getMessage() + ")");
            }
        }
    }

    static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    static String nestedSigma(int depth) {
        String expression = "i";
        for (int i = 0; i < depth; i++) {
            expression = "SIGMA(1, 1, " + expression + ")";
        }
        return expression;
    }

    public static void main(String[] args) {
        System.out.println("= Native Sparrow Expression tests =\n");

        System.out.println("--- Basic arithmetic ---");
        check("1+1", "1+1", d("2"));
        check("3*4", "3*4", d("12"));
        check("10/3", "10/3", d("3.3333333333"));
        check("2^10", "2^10", d("1024"));
        check("(1+2)*3", "(1+2)*3", d("9"));
        check("10%3", "10%3", d("1"));
        check("-5+8", "-5+8", d("3"));
        check("2.5*4", "2.5*4", d("10"));
        check("native implicit multiplication", "2(3+4)", d("14"));

        System.out.println("\n--- Native Sparrow functions ---");
        check("SQRT", "SQRT(16)", d("4"));
        check("ABS", "ABS(-5)", d("5"));
        check("ROUND", "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR", "FLOOR(3.9)", d("3"));
        check("CEILING", "CEILING(3.1)", d("4"));
        check("LOG", "LOG(2.718281828)", d("0.9999999998"));
        check("lowercase log", "log(2.718281828)", d("0.9999999998"));
        check("LOG10", "LOG10(1000)", d("3"));
        check("EXP", "EXP(1)", d("2.7182818285"));
        check("FACT", "FACT(5)", d("120"));
        check("MIN", "MIN(3, 1, 4, 2)", d("1"));
        check("MAX", "MAX(3, 1, 4, 2)", d("4"));
        check("SUM", "SUM(1, 2, 3, 4)", d("10"));
        check("AVERAGE", "AVERAGE(1, 2, 3, 4)", d("2.5"));
        check("SWITCH", "SWITCH(2, 1, 100, 2, 200, 0)", d("200"));
        check("SINH", "SINH(0)", d("0"));
        check("COSH", "COSH(0)", d("1"));
        check("PI constant", "PI", d("3.1415926536"));
        check("E constant", "E", d("2.7182818285"));

        System.out.println("\n--- Native radian trigonometry ---");
        check("SIN radians", "SIN(PI / 2)", d("1"));
        check("COS radians", "COS(PI)", d("-1"));
        check("TAN radians", "TAN(PI / 4)", d("1"));
        check("COT radians", "COT(PI / 4)", d("1"));
        check("SEC radians", "SEC(PI / 3)", d("2"));
        check("CSC radians", "CSC(PI / 6)", d("2"));
        check("ASIN radians", "ASIN(1)", d("1.5707963268"));
        check("ACOS radians", "ACOS(-1)", d("3.1415926536"));
        check("ATAN radians", "ATAN(1)", d("0.7853981634"));
        check("ACOT radians", "ACOT(1)", d("0.7853981634"));
        check("ATAN2 radians", "ATAN2(1, 1)", d("0.7853981634"));
        check("SIN receives radians", "SIN(90)", d("0.8939966636"));
        check("RAD", "RAD(180)", d("3.1415926536"));
        check("DEG", "DEG(PI)", d("180"));

        System.out.println("\n--- Boolean ---");
        check("IF 1>0", "IF(1>0, 100, 0)", d("100"));
        check("IF equality", "IF(1==1, 200, 0)", d("200"));
        check("IF inequality", "IF(1!=2, 300, 0)", d("300"));
        check("IF AND", "IF(1<2 && 2<3, 400, 0)", d("400"));
        check("IF OR", "IF(1>2 || 2<3, 500, 0)", d("500"));
        check("IF NOT", "IF(NOT(1>2), 600, 0)", d("600"));

        System.out.println("\n--- Native Sparrow SIGMA ---");
        check("sum i", "SIGMA(1, 10, i)", d("55"));
        check("sum i^2", "SIGMA(1, 10, i*i)", d("385"));
        check("sum i^3", "SIGMA(1, 5, i^3)", d("225"));
        check("counting", "SIGMA(1, 100, 1)", d("100"));
        check("sum i!", "SIGMA(1, 4, FACT(i))", d("33"));
        check("single", "SIGMA(1, 1, 42)", d("42"));
        check("zero index", "SIGMA(0, 0, 99)", d("99"));
        check("start>end", "SIGMA(5, 1, i)", d("0"));
        check("negative range", "SIGMA(-2, 2, i)", d("0"));
        check("SIGMA exponential", "SIGMA(1, 5, E^(-0.1*i))", d("3.7412370975"));
        check("SIGMA SQRT", "SIGMA(1, 9, SQRT(i))", d("19.3060005260"));
        check("SIGMA IF", "SIGMA(1, 10, IF(i%2==0, i, 0))", d("30"));
        check("embedded SIGMA", "1 + SIGMA(1, 3, i)", d("7"));
        check("multiple SIGMA calls",
                "SIGMA(1, 3, i) + SIGMA(1, 2, i)", d("9"));
        check("expression bounds", "SIGMA(1+1, MAX(2, 3), i)", d("5"));
        check("lowercase SIGMA", "sigma(1, 3, i)", d("6"));
        check("implicit multiplication with SIGMA", "2SIGMA(1, 3, i)", d("12"));
        check("implicit multiplication in body", "SIGMA(1, 3, 2i)", d("12"));
        check("nested SIGMA", "SIGMA(1, 3, SIGMA(1, i, i))", d("10"));
        check("SIGMA iteration boundary", "SIGMA(1, 100000, 1)", d("100000"));

        System.out.println("\n--- Invalid expression handling ---");
        checkError("unknown variable", "unknown_name + 1", ExpressionParseException.class);
        checkError("i outside SIGMA", "i + 1", IllegalArgumentException.class);
        checkError("i in root SIGMA bound", "SIGMA(i, 5, 0)", IllegalArgumentException.class);
        checkError("quoted string SIGMA body", "SIGMA(1, 3, \"i\")",
                ExpressionParseException.class);
        checkError("unsupported SINR alias", "SINR(PI / 2)", ExpressionParseException.class);
        checkError("unsupported NULL constant", "NULL", ExpressionParseException.class);
        checkError("unsupported COALESCE", "COALESCE(1, 2)", ExpressionParseException.class);
        checkError("wrong SIGMA arity", "SIGMA(1, 2)", ExpressionParseException.class);
        checkError("too many SIGMA iterations", "SIGMA(1, 100001, 1)",
                ArithmeticException.class);
        checkError("non-finite result", "1 / 0", ArithmeticException.class);
        checkError("SIGMA nesting limit", nestedSigma(34), ArithmeticException.class);
        checkError("SIGMA total iteration budget",
                "SIGMA(1, 11, SIGMA(1, 100000, 1))", ArithmeticException.class);

        System.out.println("\n--- Article formulas ---");
        check("e^{-lambda*n}", "E^(-0.1*10)", d("0.3678794412"));
        check("ROUND(pi,2)", "ROUND(PI, 2)", d("3.14"));
        check("FLOOR", "FLOOR(3.9)", d("3"));
        check("1-e^{-ln}", "1-E^(-0.1*5)", d("0.3934693403"));
        check("(t-u)^6/(2s^2)", "(10-5)^6/(2*2^2)", d("1953.125"));

        System.out.println("\n" + "=".repeat(48));
        System.out.println("Total: " + (passed + failed)
                + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        }
        System.out.println("ALL PASSED!");
    }
}
