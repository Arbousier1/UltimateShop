package cn.superiormc.ultimateshop.utils;

import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.managers.ErrorManager;
import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.binding.ParameterBinding;
import net.momirealms.sparrow.expr.binding.ParameterBinder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MathUtil {

    public static int scale;

    public static DecimalFormat integerFormat;

    public static DecimalFormat decimalFormat;

    private static ExpressionCompiler<Double> compiler;

    private static final int SIGMA_MAX_ITERATIONS = 100000;

    public static void init() {
        scale = ConfigManager.configManager.getInt("math.scale", 2);
        String integerPattern = ConfigManager.configManager.getString("number-display.format.integer", "#,##0");
        String decimalPattern = ConfigManager.configManager.getString("number-display.format.decimal", "#,##0.00##########");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        integerFormat = new DecimalFormat(integerPattern, symbols);
        decimalFormat = new DecimalFormat(decimalPattern, symbols);

        ParameterBinder<Double> binder = name -> {
            if ("i".equals(name)) {
                return ParameterBinding.number(ctx -> ctx);
            }
            return ParameterBinding.number(ctx -> 0.0);
        };
        compiler = new ExpressionCompiler<>(binder);
    }

    public static double multiply(double left, double right) {
        return BigDecimal.valueOf(left).multiply(BigDecimal.valueOf(right)).doubleValue();
    }

    public static String toDisplayString(double value) {
        return toDisplayString(BigDecimal.valueOf(value));
    }

    public static String toDisplayString(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal working = ConfigManager.configManager.getBoolean("number-display.strip-trailing-zeros.enabled") ? value.stripTrailingZeros() : value;
        if (!ConfigManager.configManager.getBoolean("number-display.format.enabled")) {
            return value.setScale(Math.max(0, value.scale()), RoundingMode.HALF_UP).toPlainString();
        }
        return working.scale() <= 0
                ? integerFormat.format(working)
                : decimalFormat.format(working);
    }

    public static BigDecimal doCalculate(String mathStr) {
        return doCalculate(mathStr, scale);
    }

    public static BigDecimal doCalculate(String mathStr, int reqScale) {
        try {
            if (!ConfigManager.configManager.getBoolean("math.enabled")) {
                return new BigDecimal(mathStr);
            }
            double result;
            if (mathStr.startsWith("SIGMA(")) {
                result = evaluateSigma(mathStr);
            } else {
                CompiledExpression<Double> expr = compiler.compile(convertLogFunctions(mathStr));
                result = expr.evaluate(0.0);
            }
            return BigDecimal.valueOf(result).setScale(reqScale, RoundingMode.HALF_UP);
        } catch (Throwable throwable) {
            if (ConfigManager.configManager.getBoolean("debug")) {
                throwable.printStackTrace();
            }
            ErrorManager.errorManager.sendErrorMessage("§cError: Your number option value " +
                    mathStr + " can not be read as a number, maybe " +
                    "set math.enabled to false in config.yml maybe solve this problem!");
            return BigDecimal.ZERO;
        }
    }

    private static double evaluateSigma(String raw) {
        int open = raw.indexOf('(');
        int lastClose = raw.lastIndexOf(')');
        if (open < 0 || lastClose < 0) {
            throw new IllegalArgumentException("Invalid SIGMA expression: " + raw);
        }
        String inner = raw.substring(open + 1, lastClose);
        int firstComma = findUnquotedComma(inner);
        int secondComma = findUnquotedComma(inner, firstComma + 1);
        if (firstComma < 0 || secondComma < 0) {
            throw new IllegalArgumentException("SIGMA requires 3 arguments: " + raw);
        }
        int start = (int) Double.parseDouble(inner.substring(0, firstComma).trim());
        int end = (int) Double.parseDouble(inner.substring(firstComma + 1, secondComma).trim());
        String body = inner.substring(secondComma + 1).trim();
        // Strip surrounding double quotes if present (legacy EvalEx format)
        if (body.length() >= 2 && body.charAt(0) == '"' && body.charAt(body.length() - 1) == '"') {
            body = body.substring(1, body.length() - 1);
        }
        if (start > end) return 0.0;
        int count = end - start + 1;
        if (count > SIGMA_MAX_ITERATIONS) {
            throw new ArithmeticException("SIGMA: too many iterations (" + count + "), maximum is " + SIGMA_MAX_ITERATIONS);
        }
        String convertedBody = convertLogFunctions(body);
        CompiledExpression<Double> bodyExpr = compiler.compile(convertedBody);
        double sum = 0.0;
        for (int i = start; i <= end; i++) {
            double result = bodyExpr.evaluate((double) i);
            sum += result;
        }
        return sum;
    }

    private static int findUnquotedComma(String s) {
        boolean inQuote = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inQuote = !inQuote;
            else if (c == ',' && !inQuote) return i;
        }
        return -1;
    }

    private static int findUnquotedComma(String s, int from) {
        boolean inQuote = false;
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inQuote = !inQuote;
            else if (c == ',' && !inQuote) return i;
        }
        return -1;
    }

    private static String convertLogFunctions(String expr) {
        return expr.replaceAll("\\bLOG\\((?!10)", "LN(");
    }
}
