package cn.superiormc.ultimateshop.utils;

import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;
import net.momirealms.sparrow.expr.binding.ParameterBinder;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathFunctionTest {

    private static final int SIGMA_MAX_ITERATIONS = 100_000;
    private static ExpressionCompiler<Double> compiler;
    private static int passed;
    private static int failed;

    static {
        ParameterBinder<Double> binder = name -> {
            if ("i".equals(name)) {
                return ParameterBinding.number(ctx -> ctx);
            }
            return ParameterBinding.number(ctx -> 0.0);
        };
        compiler = new ExpressionCompiler<>(binder);
    }

    static BigDecimal calc(String expr) {
        CompiledExpression<Double> compiled = compiler.compile(convertLogFunctions(expr));
        double raw = compiled.evaluate(0.0);
        return BigDecimal.valueOf(raw).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    static void check(String name, String expr, BigDecimal expected) {
        try {
            BigDecimal result = calc(expr);
            if (result.compareTo(expected) == 0) {
                passed++;
                System.out.println("  PASS: " + name + " = " + result);
            } else {
                failed++;
                System.out.println("  FAIL: " + name);
                System.out.println("        expected: " + expected);
                System.out.println("        got     : " + result);
            }
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + name + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    static void checkSigma(String name, String rawExpr, BigDecimal expected) {
        try {
            BigDecimal result = evalSigma(rawExpr);
            if (result.compareTo(expected) == 0) {
                passed++;
                System.out.println("  PASS: " + name + " = " + result);
            } else {
                failed++;
                System.out.println("  FAIL: " + name);
                System.out.println("        expected: " + expected);
                System.out.println("        got     : " + result);
            }
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + name + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    static BigDecimal evalSigma(String raw) {
        int open = raw.indexOf('(');
        int lastClose = raw.lastIndexOf(')');
        String inner = raw.substring(open + 1, lastClose);
        int c1 = findComma(inner);
        int c2 = findComma(inner, c1 + 1);
        int start = (int) Double.parseDouble(inner.substring(0, c1).trim());
        int end = (int) Double.parseDouble(inner.substring(c1 + 1, c2).trim());
        String body = inner.substring(c2 + 1).trim();
        if (body.length() >= 2 && body.charAt(0) == '"' && body.charAt(body.length() - 1) == '"') {
            body = body.substring(1, body.length() - 1);
        }
        if (start > end) return BigDecimal.ZERO;
        int count = end - start + 1;
        if (count > SIGMA_MAX_ITERATIONS) {
            throw new ArithmeticException("SIGMA: too many iterations");
        }
        CompiledExpression<Double> bodyExpr = compiler.compile(convertLogFunctions(body));
        double sum = 0.0;
        for (int i = start; i <= end; i++) {
            sum += bodyExpr.evaluate((double) i);
        }
        return BigDecimal.valueOf(sum).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    static int findComma(String s) {
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inQuote = !inQuote;
            else if (c == ',' && !inQuote) return i;
        }
        return -1;
    }

    static int findComma(String s, int from) {
        boolean inQuote = false;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inQuote = !inQuote;
            else if (c == ',' && !inQuote) return i;
        }
        return -1;
    }

    static String convertLogFunctions(String expr) {
        return expr.replaceAll("\\bLOG\\((?!10)", "LN(");
    }

    static BigDecimal d(String s) { return new BigDecimal(s); }

    public static void main(String[] args) {
        System.out.println("= Sparrow Expression + SIGMA accuracy tests =\n");

        System.out.println("--- Basic arithmetic ---");
        check("1+1", "1+1", d("2"));
        check("3*4", "3*4", d("12"));
        check("10/3", "10/3", d("3.3333333333"));
        check("2^10", "2^10", d("1024"));
        check("(1+2)*3", "(1+2)*3", d("9"));
        check("10%3", "10%3", d("1"));
        check("-5+8", "-5+8", d("3"));
        check("2.5*4", "2.5*4", d("10"));

        System.out.println("\n--- Built-in functions ---");
        check("SQRT(16)", "SQRT(16)", d("4"));
        check("SQRT(2)", "SQRT(2)", d("1.4142135624"));
        check("ABS(-5)", "ABS(-5)", d("5"));
        check("ROUND(3.14159, 2)", "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR(3.9)", "FLOOR(3.9)", d("3"));
        check("CEILING(3.1)", "CEILING(3.1)", d("4"));
        check("LOG(~e)", "LOG(2.718281828)", d("0.9999999998"));
        check("LOG10(100)", "LOG10(100)", d("2"));
        check("SIN(0)", "SIN(0)", d("0"));
        check("COS(0)", "COS(0)", d("1"));
        check("TAN(0)", "TAN(0)", d("0"));
        check("e^1", "2.718281828459045^1", d("2.7182818285"));

        System.out.println("\n--- Boolean ---");
        check("IF 1>0", "IF(1>0, 100, 0)", d("100"));
        check("IF 1==1", "IF(1==1, 200, 0)", d("200"));
        check("IF 1!=2", "IF(1!=2, 300, 0)", d("300"));

        System.out.println("\n--- SIGMA ---");
        checkSigma("SIGMA(1,10,\"i\")      sum i",     "SIGMA(1, 10, \"i\")", d("55"));
        checkSigma("SIGMA(1,10,\"i*i\")    sum i^2",   "SIGMA(1, 10, \"i*i\")", d("385"));
        checkSigma("SIGMA(1,5,\"i^3\")     sum i^3",   "SIGMA(1, 5, \"i^3\")", d("225"));
        checkSigma("SIGMA(1,100,\"1\")     counting",  "SIGMA(1, 100, \"1\")", d("100"));
        checkSigma("SIGMA(1,1,\"42\")      single",    "SIGMA(1, 1, \"42\")", d("42"));
        checkSigma("SIGMA(0,0,\"99\")      zero index","SIGMA(0, 0, \"99\")", d("99"));
        checkSigma("SIGMA(5,1,\"i\")       start>end", "SIGMA(5, 1, \"i\")", d("0"));

        System.out.println("\n--- SIGMA advanced ---");
        checkSigma("SIGMA + exponential",  "SIGMA(1, 5, \"2.718281828459045^(-0.1*i)\")",
                d("3.7412370975"));
        checkSigma("SIGMA + SQRT", "SIGMA(1, 9, \"SQRT(i)\")",
                d("19.3060005260"));
        checkSigma("SIGMA + IF",   "SIGMA(1, 10, \"IF(i%2==0, i, 0)\")",
                d("30"));
        checkSigma("SIGMA large",  "SIGMA(1, 1000, \"1\")",
                d("1000"));

        System.out.println("\n--- Article formulas ---");
        check("e^{-lambda*n}", "2.718281828459045^(-0.1*10)", d("0.3678794412"));
        check("ROUND(pi,2)",   "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR",         "FLOOR(3.9)", d("3"));
        check("1-e^{-ln}",     "1-2.718281828459045^(-0.1*5)", d("0.3934693403"));
        check("(t-u)^6/(2s^2)","(10-5)^6/(2*2^2)", d("1953.125"));

        System.out.println("\n" + "=".repeat(40));
        System.out.println("Total: " + (passed + failed) + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED!");
        }
    }
}
