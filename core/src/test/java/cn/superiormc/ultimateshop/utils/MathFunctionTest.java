package cn.superiormc.ultimateshop.utils;

import net.momirealms.sparrow.expr.ExpressionParseException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathFunctionTest {

    private static final MathExpressionEvaluator evaluator = new MathExpressionEvaluator();
    private static int passed;
    private static int failed;

    static BigDecimal calc(String expression) {
        double raw = evaluator.evaluate(expression);
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
        System.out.println("= Sparrow Expression legacy compatibility tests =\n");

        System.out.println("--- Basic arithmetic ---");
        check("1+1", "1+1", d("2"));
        check("3*4", "3*4", d("12"));
        check("10/3", "10/3", d("3.3333333333"));
        check("2^10", "2^10", d("1024"));
        check("(1+2)*3", "(1+2)*3", d("9"));
        check("10%3", "10%3", d("1"));
        check("-5+8", "-5+8", d("3"));
        check("2.5*4", "2.5*4", d("10"));
        check("implicit 2(3+4)", "2(3+4)", d("14"));

        System.out.println("\n--- Built-in functions ---");
        check("SQRT(16)", "SQRT(16)", d("4"));
        check("SQRT(2)", "SQRT(2)", d("1.4142135624"));
        check("ABS(-5)", "ABS(-5)", d("5"));
        check("ROUND(3.14159, 2)", "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR(3.9)", "FLOOR(3.9)", d("3"));
        check("CEILING(3.1)", "CEILING(3.1)", d("4"));
        check("LOG(~e)", "LOG(2.718281828)", d("0.9999999998"));
        check("lowercase log", "log(2.718281828)", d("0.9999999998"));
        check("LOG10(100)", "LOG10(100)", d("2"));
        check("lowercase log10", "log10(1000)", d("3"));
        check("e^1", "2.718281828459045^1", d("2.7182818285"));
        check("FACT(5)", "FACT(5)", d("120"));
        check("MIN", "MIN(3, 1, 4, 2)", d("1"));
        check("MAX", "MAX(3, 1, 4, 2)", d("4"));
        check("COALESCE", "COALESCE(NULL, 5)", d("5"));
        check("COALESCE multiple nulls", "COALESCE(NULL, NULL, 7)", d("7"));
        check("SWITCH", "SWITCH(2, 1, 100, 2, 200, 0)", d("200"));
        check("EXP", "EXP(1)", d("2.7182818285"));
        check("SINH", "SINH(0)", d("0"));
        check("COSH", "COSH(0)", d("1"));
        check("RAD", "RAD(180)", d("3.1415926536"));
        check("DEG", "DEG(3.141592653589793)", d("180"));

        System.out.println("\n--- Legacy trigonometric semantics ---");
        check("SIN degrees", "SIN(90)", d("1"));
        check("COS degrees", "COS(180)", d("-1"));
        check("TAN degrees", "TAN(45)", d("1"));
        check("COT degrees", "COT(45)", d("1"));
        check("SEC degrees", "SEC(60)", d("2"));
        check("CSC degrees", "CSC(30)", d("2"));
        check("ASIN degrees", "ASIN(1)", d("90"));
        check("ACOS degrees", "ACOS(-1)", d("180"));
        check("ATAN degrees", "ATAN(1)", d("45"));
        check("ACOT degrees", "ACOT(1)", d("45"));
        check("ATAN2 degrees", "ATAN2(1, 1)", d("45"));
        check("SINR radians", "SINR(1.5707963267948966)", d("1"));
        check("COSR radians", "COSR(3.141592653589793)", d("-1"));
        check("TANR radians", "TANR(0.7853981633974483)", d("1"));
        check("COTR radians", "COTR(0.7853981633974483)", d("1"));
        check("SECR radians", "SECR(1.0471975511965976)", d("2"));
        check("CSCR radians", "CSCR(0.5235987755982988)", d("2"));
        check("ASINR radians", "ASINR(1)", d("1.5707963268"));
        check("ATAN2R radians", "ATAN2R(1, 1)", d("0.7853981634"));

        System.out.println("\n--- Boolean ---");
        check("IF 1>0", "IF(1>0, 100, 0)", d("100"));
        check("IF 1==1", "IF(1==1, 200, 0)", d("200"));
        check("IF 1!=2", "IF(1!=2, 300, 0)", d("300"));
        check("IF AND", "IF(1<2 && 2<3, 400, 0)", d("400"));
        check("IF OR", "IF(1>2 || 2<3, 500, 0)", d("500"));
        check("IF NOT", "IF(NOT(1>2), 600, 0)", d("600"));

        System.out.println("\n--- Legacy SIGMA ---");
        check("sum i", "SIGMA(1, 10, \"i\")", d("55"));
        check("sum i^2", "SIGMA(1, 10, \"i*i\")", d("385"));
        check("sum i^3", "SIGMA(1, 5, \"i^3\")", d("225"));
        check("counting", "SIGMA(1, 100, \"1\")", d("100"));
        check("sum i!", "SIGMA(1, 4, \"FACT(i)\")", d("33"));
        check("single", "SIGMA(1, 1, \"42\")", d("42"));
        check("zero index", "SIGMA(0, 0, \"99\")", d("99"));
        check("start>end", "SIGMA(5, 1, \"i\")", d("0"));
        check("SIGMA exponential", "SIGMA(1, 5, \"2.718281828459045^(-0.1*i)\")",
                d("3.7412370975"));
        check("SIGMA SQRT", "SIGMA(1, 9, \"SQRT(i)\")", d("19.3060005260"));
        check("SIGMA IF", "SIGMA(1, 10, \"IF(i%2==0, i, 0)\")", d("30"));
        check("SIGMA large", "SIGMA(1, 1000, \"1\")", d("1000"));

        System.out.println("\n--- SIGMA composition compatibility ---");
        check("SIGMA suffix arithmetic", "SIGMA(1, 3, \"i\") + 10", d("16"));
        check("SIGMA embedded in expression", "1 + SIGMA(1, 3, \"i\")", d("7"));
        check("multiple SIGMA calls",
                "SIGMA(1, 3, \"i\") + SIGMA(1, 2, \"i\")", d("9"));
        check("SIGMA expression bounds", "SIGMA(1+1, MAX(2, 3), \"i\")", d("5"));
        check("case and whitespace", " sigma (1, 3, \"i\") ", d("6"));
        check("implicit multiplication with SIGMA", "2SIGMA(1, 3, \"i\")", d("12"));
        check("nested SIGMA", "SIGMA(1, 3, \"SIGMA(1, i, \\\"i\\\")\")", d("10"));
        check("native numeric SIGMA body", "SIGMA(1, 3, i)", d("6"));
        check("negative SIGMA range", "SIGMA(-2, 2, \"i\")", d("0"));
        check("implicit multiplication in body", "SIGMA(1, 3, \"2i\")", d("12"));
        check("SIGMA iteration boundary", "SIGMA(1, 100000, \"1\")", d("100000"));

        System.out.println("\n--- Invalid expression handling ---");
        checkError("unknown variable", "unknown_name + 1", ExpressionParseException.class);
        checkError("i outside SIGMA", "i + 1", IllegalArgumentException.class);
        checkError("too many SIGMA iterations", "SIGMA(1, 100001, \"1\")",
                ArithmeticException.class);
        checkError("non-finite result", "1 / 0", ArithmeticException.class);
        checkError("unclosed SIGMA", "SIGMA(1, 2, \"i\"",
                IllegalArgumentException.class);
        checkError("wrong SIGMA arity", "SIGMA(1, 2)", IllegalArgumentException.class);
        checkError("invalid SIGMA escape", "SIGMA(1, 1, \"\\q\")",
                IllegalArgumentException.class);
        checkError("SIGMA nesting limit", nestedSigma(34), ArithmeticException.class);
        checkError("SIGMA total iteration budget",
                "SIGMA(1, 11, \"SIGMA(1, 100000, \\\"1\\\")\")",
                ArithmeticException.class);

        System.out.println("\n--- Article formulas ---");
        check("e^{-lambda*n}", "2.718281828459045^(-0.1*10)", d("0.3678794412"));
        check("ROUND(pi,2)", "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR", "FLOOR(3.9)", d("3"));
        check("1-e^{-ln}", "1-2.718281828459045^(-0.1*5)", d("0.3934693403"));
        check("(t-u)^6/(2s^2)", "(10-5)^6/(2*2^2)", d("1953.125"));

        System.out.println("\n" + "=".repeat(48));
        System.out.println("Total: " + (passed + failed) + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        }
        System.out.println("ALL PASSED!");
    }
}
