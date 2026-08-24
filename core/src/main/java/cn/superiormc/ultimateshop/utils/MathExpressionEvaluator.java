package cn.superiormc.ultimateshop.utils;

import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.ExpressionOptions;
import net.momirealms.sparrow.expr.binding.BoundFunction;
import net.momirealms.sparrow.expr.binding.ParameterBinding;
import net.momirealms.sparrow.expr.binding.ParameterBinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Evaluates UltimateShop math expressions with Sparrow while preserving the
 * legacy EvalEx syntax used by existing configurations.
 */
final class MathExpressionEvaluator {

    static final int SIGMA_MAX_ITERATIONS = 100_000;
    static final int SIGMA_MAX_TOTAL_ITERATIONS = 1_000_000;

    private static final int SIGMA_MAX_NESTING = 32;
    private static final ExpressionOptions OPTIONS = new ExpressionOptions(true);

    private final ExpressionCompiler<EvaluationContext> expressionCompiler;

    MathExpressionEvaluator() {
        ParameterBinder<EvaluationContext> parameters = name -> {
            if (!"i".equalsIgnoreCase(name)) {
                return null;
            }
            return ParameterBinding.number(context -> {
                if (!context.indexAvailable()) {
                    throw new IllegalArgumentException("Variable 'i' is only available inside SIGMA body");
                }
                return context.index();
            });
        };
        this.expressionCompiler = new ExpressionCompiler<>(
                parameters,
                OPTIONS,
                MathExpressionEvaluator::bindConstant,
                MathExpressionEvaluator::bindFunction);
    }

    double evaluate(String source) {
        Objects.requireNonNull(source, "source");
        String compatibleSource = normalizeLegacySyntax(rewriteLegacySigma(source, 0));
        CompiledExpression<EvaluationContext> expression = expressionCompiler.compile(compatibleSource);
        EvaluationContext context = EvaluationContext.root();
        return requireFinite(expression.evaluate(context), source);
    }

    private static Double bindConstant(String name) {
        return "NULL".equalsIgnoreCase(name) ? Double.NaN : null;
    }

    private static BoundFunction<EvaluationContext> bindFunction(String name, int argumentCount) {
        return switch (name) {
            case "SIGMA" -> sigma(argumentCount);
            case "EVALEX_SIN" -> unary(argumentCount,
                    value -> Math.sin(Math.toRadians(value)));
            case "EVALEX_COS" -> unary(argumentCount,
                    value -> Math.cos(Math.toRadians(value)));
            case "EVALEX_TAN" -> unary(argumentCount,
                    value -> Math.tan(Math.toRadians(value)));
            case "EVALEX_COT" -> unary(argumentCount,
                    value -> 1.0 / Math.tan(Math.toRadians(value)));
            case "EVALEX_SEC" -> unary(argumentCount,
                    value -> 1.0 / Math.cos(Math.toRadians(value)));
            case "EVALEX_CSC" -> unary(argumentCount,
                    value -> 1.0 / Math.sin(Math.toRadians(value)));
            case "EVALEX_ASIN" -> unary(argumentCount,
                    value -> Math.toDegrees(Math.asin(value)));
            case "EVALEX_ACOS" -> unary(argumentCount,
                    value -> Math.toDegrees(Math.acos(value)));
            case "EVALEX_ATAN" -> unary(argumentCount,
                    value -> Math.toDegrees(Math.atan(value)));
            case "EVALEX_ACOT" -> unary(argumentCount,
                    value -> Math.toDegrees(Math.atan(1.0 / value)));
            case "EVALEX_ATAN2" -> binary(argumentCount,
                    (left, right) -> Math.toDegrees(Math.atan2(left, right)));
            case "COALESCE" -> coalesce(argumentCount);
            default -> null;
        };
    }

    private static BoundFunction<EvaluationContext> sigma(int argumentCount) {
        if (argumentCount != 3) {
            return null;
        }
        return (context, arguments) -> {
            int start = toIntegerIndex(
                    requireFinite(arguments.value(0, context), "SIGMA start"), "start");
            int end = toIntegerIndex(
                    requireFinite(arguments.value(1, context), "SIGMA end"), "end");
            if (start > end) {
                return 0.0;
            }

            long count = (long) end - start + 1L;
            if (count > SIGMA_MAX_ITERATIONS) {
                throw new ArithmeticException("SIGMA: too many iterations (" + count
                        + "), maximum is " + SIGMA_MAX_ITERATIONS);
            }
            context.budget().consume(count);

            double sum = 0.0;
            double compensation = 0.0;
            for (long offset = 0; offset < count; offset++) {
                double index = (double) ((long) start + offset);
                double term = requireFinite(
                        arguments.value(2, context.withIndex(index)), "SIGMA body");
                double adjusted = term - compensation;
                double next = sum + adjusted;
                compensation = (next - sum) - adjusted;
                sum = next;
            }
            return requireFinite(sum, "SIGMA");
        };
    }

    private static BoundFunction<EvaluationContext> unary(
            int argumentCount, DoubleUnaryOperator operator) {
        if (argumentCount != 1) {
            return null;
        }
        return (context, arguments) -> operator.applyAsDouble(arguments.value(0, context));
    }

    private static BoundFunction<EvaluationContext> binary(
            int argumentCount, DoubleBinaryOperator operator) {
        if (argumentCount != 2) {
            return null;
        }
        return (context, arguments) -> operator.applyAsDouble(
                arguments.value(0, context), arguments.value(1, context));
    }

    private static BoundFunction<EvaluationContext> coalesce(int argumentCount) {
        if (argumentCount < 1) {
            return null;
        }
        return (context, arguments) -> {
            for (int i = 0; i < arguments.size(); i++) {
                double value = arguments.value(i, context);
                if (!Double.isNaN(value)) {
                    return value;
                }
            }
            return Double.NaN;
        };
    }

    private static int toIntegerIndex(double value, String argumentName) {
        if (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ArithmeticException("SIGMA " + argumentName
                    + " must fit in a 32-bit integer: " + value);
        }
        return (int) value;
    }

    private static double requireFinite(double value, String source) {
        if (!Double.isFinite(value)) {
            throw new ArithmeticException("Expression produced a non-finite result: " + source);
        }
        return value;
    }

    /**
     * Sparrow custom functions accept numeric expressions, while the legacy
     * SIGMA syntax stores its third argument as a string. Rewrite only that
     * quoted body, then let Sparrow compile and evaluate SIGMA natively.
     */
    private static String rewriteLegacySigma(String source, int nesting) {
        if (nesting > SIGMA_MAX_NESTING) {
            throw new ArithmeticException("SIGMA nesting exceeds " + SIGMA_MAX_NESTING);
        }

        StringBuilder rewritten = new StringBuilder(source);
        int searchFrom = 0;
        while (true) {
            SigmaCall call = findNextSigma(rewritten, searchFrom);
            if (call == null) {
                return rewritten.toString();
            }

            List<String> arguments = call.arguments();
            String start = rewriteLegacySigma(arguments.get(0), nesting + 1);
            String end = rewriteLegacySigma(arguments.get(1), nesting + 1);
            String body = rewriteLegacySigma(decodeBody(arguments.get(2)), nesting + 1);
            String replacement = "SIGMA(" + start + "," + end + "," + body + ")";
            rewritten.replace(call.start(), call.endExclusive(), replacement);
            searchFrom = call.start() + replacement.length();
        }
    }

    private static SigmaCall findNextSigma(CharSequence source, int from) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = Math.max(0, from); i < source.length(); i++) {
            char current = source.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                continue;
            }
            if (!matchesSigma(source, i)) {
                continue;
            }

            int openParenthesis = i + 5;
            while (openParenthesis < source.length()
                    && Character.isWhitespace(source.charAt(openParenthesis))) {
                openParenthesis++;
            }
            if (openParenthesis >= source.length() || source.charAt(openParenthesis) != '(') {
                continue;
            }
            int closeParenthesis = findClosingParenthesis(source, openParenthesis);
            List<String> arguments = splitArguments(
                    source.subSequence(openParenthesis + 1, closeParenthesis).toString());
            return new SigmaCall(i, closeParenthesis + 1, arguments);
        }
        return null;
    }

    private static boolean matchesSigma(CharSequence source, int start) {
        if (start > 0) {
            char previous = source.charAt(start - 1);
            if (Character.isLetter(previous) || previous == '_') {
                return false;
            }
        }
        if (start + 5 > source.length()) {
            return false;
        }
        String candidate = source.subSequence(start, start + 5).toString();
        if (!"SIGMA".equals(candidate.toUpperCase(Locale.ROOT))) {
            return false;
        }
        return start + 5 == source.length() || !isIdentifierPart(source.charAt(start + 5));
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private static int findClosingParenthesis(CharSequence source, int openParenthesis) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = openParenthesis; i < source.length(); i++) {
            char current = source.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '(') {
                depth++;
            } else if (current == ')' && --depth == 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unclosed SIGMA parenthesis");
    }

    private static List<String> splitArguments(String source) {
        List<String> arguments = new ArrayList<>(3);
        int start = 0;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            } else if (current == ',' && depth == 0) {
                arguments.add(source.substring(start, i).trim());
                start = i + 1;
            }
        }
        arguments.add(source.substring(start).trim());
        if (arguments.size() != 3 || arguments.stream().anyMatch(String::isEmpty)) {
            throw new IllegalArgumentException("SIGMA requires exactly 3 arguments");
        }
        return arguments;
    }

    private static String decodeBody(String argument) {
        String body = argument.trim();
        if (!isSingleStringLiteral(body)) {
            return body;
        }

        StringBuilder decoded = new StringBuilder(body.length() - 2);
        for (int i = 1; i < body.length() - 1; i++) {
            char current = body.charAt(i);
            if (current != '\\') {
                decoded.append(current);
                continue;
            }
            if (++i >= body.length() - 1) {
                throw new IllegalArgumentException("Unterminated escape in SIGMA body");
            }
            char escaped = body.charAt(i);
            switch (escaped) {
                case '"' -> decoded.append('"');
                case '\\' -> decoded.append('\\');
                case 'n' -> decoded.append('\n');
                case 'r' -> decoded.append('\r');
                case 't' -> decoded.append('\t');
                case 'b' -> decoded.append('\b');
                case 'f' -> decoded.append('\f');
                case 'u' -> {
                    if (i + 4 >= body.length() - 1) {
                        throw new IllegalArgumentException("Invalid Unicode escape in SIGMA body");
                    }
                    String hex = body.substring(i + 1, i + 5);
                    try {
                        decoded.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException(
                                "Invalid Unicode escape in SIGMA body: " + hex, exception);
                    }
                    i += 4;
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported escape in SIGMA body: \\" + escaped);
            }
        }
        return decoded.toString();
    }

    private static boolean isSingleStringLiteral(String value) {
        if (value.length() < 2 || value.charAt(0) != '"') {
            return false;
        }
        boolean escaped = false;
        for (int i = 1; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return i == value.length() - 1;
            }
        }
        throw new IllegalArgumentException("Unclosed string literal in SIGMA body");
    }

    private static String normalizeLegacySyntax(String expression) {
        StringBuilder normalized = new StringBuilder(expression.length() + 16);
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < expression.length();) {
            char current = expression.charAt(i);
            if (inString) {
                normalized.append(current);
                i++;
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
                normalized.append(current);
                i++;
                continue;
            }
            if (!Character.isLetter(current) && current != '_') {
                normalized.append(current);
                i++;
                continue;
            }

            int end = i + 1;
            while (end < expression.length() && isIdentifierPart(expression.charAt(end))) {
                end++;
            }
            String identifier = expression.substring(i, end);
            int lookahead = end;
            while (lookahead < expression.length()
                    && Character.isWhitespace(expression.charAt(lookahead))) {
                lookahead++;
            }
            if (lookahead < expression.length() && expression.charAt(lookahead) == '(') {
                normalized.append(legacyFunctionName(identifier));
            } else {
                normalized.append(identifier);
            }
            i = end;
        }
        return normalized.toString();
    }

    private static String legacyFunctionName(String identifier) {
        return switch (identifier.toUpperCase(Locale.ROOT)) {
            case "SIN" -> "EVALEX_SIN";
            case "COS" -> "EVALEX_COS";
            case "TAN" -> "EVALEX_TAN";
            case "COT" -> "EVALEX_COT";
            case "SEC" -> "EVALEX_SEC";
            case "CSC" -> "EVALEX_CSC";
            case "ASIN" -> "EVALEX_ASIN";
            case "ACOS" -> "EVALEX_ACOS";
            case "ATAN" -> "EVALEX_ATAN";
            case "ACOT" -> "EVALEX_ACOT";
            case "ATAN2" -> "EVALEX_ATAN2";
            case "SINR" -> "SIN";
            case "COSR" -> "COS";
            case "TANR" -> "TAN";
            case "COTR" -> "COT";
            case "SECR" -> "SEC";
            case "CSCR" -> "CSC";
            case "ASINR" -> "ASIN";
            case "ACOSR" -> "ACOS";
            case "ATANR" -> "ATAN";
            case "ACOTR" -> "ACOT";
            case "ATAN2R" -> "ATAN2";
            default -> identifier;
        };
    }

    private record SigmaCall(int start, int endExclusive, List<String> arguments) {
    }

    private record EvaluationContext(
            double index, boolean indexAvailable, EvaluationBudget budget) {

        static EvaluationContext root() {
            return new EvaluationContext(0.0, false, new EvaluationBudget());
        }

        EvaluationContext withIndex(double newIndex) {
            return new EvaluationContext(newIndex, true, budget);
        }
    }

    private static final class EvaluationBudget {
        private long remaining = SIGMA_MAX_TOTAL_ITERATIONS;

        void consume(long iterations) {
            if (iterations > remaining) {
                throw new ArithmeticException("SIGMA total iteration budget exceeded (maximum "
                        + SIGMA_MAX_TOTAL_ITERATIONS + ")");
            }
            remaining -= iterations;
        }
    }
}
