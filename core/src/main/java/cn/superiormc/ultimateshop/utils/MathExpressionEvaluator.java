package cn.superiormc.ultimateshop.utils;

import net.momirealms.sparrow.expr.CompiledExpression;
import net.momirealms.sparrow.expr.ExpressionCompiler;
import net.momirealms.sparrow.expr.ExpressionOptions;
import net.momirealms.sparrow.expr.binding.BoundFunction;
import net.momirealms.sparrow.expr.binding.ConstantBinder;
import net.momirealms.sparrow.expr.binding.ParameterBinding;
import net.momirealms.sparrow.expr.binding.ParameterBinder;

import java.util.Objects;

/**
 * Evaluates UltimateShop math expressions directly with Sparrow. SIGMA is
 * registered through Sparrow's FunctionBinder and receives a numeric body.
 */
final class MathExpressionEvaluator {

    static final int SIGMA_MAX_ITERATIONS = 100_000;
    static final int SIGMA_MAX_TOTAL_ITERATIONS = 1_000_000;
    static final int SIGMA_MAX_NESTING = 32;

    private static final ExpressionOptions OPTIONS = new ExpressionOptions(true);

    private final ExpressionCompiler<EvaluationContext> expressionCompiler;

    MathExpressionEvaluator() {
        ParameterBinder<EvaluationContext> parameters = name -> {
            if (!"i".equalsIgnoreCase(name)) {
                return null;
            }
            return ParameterBinding.number(context -> {
                if (!context.indexAvailable()) {
                    throw new IllegalArgumentException(
                            "Variable 'i' is only available inside SIGMA body");
                }
                return context.index();
            });
        };
        this.expressionCompiler = new ExpressionCompiler<>(
                parameters,
                OPTIONS,
                ConstantBinder.none(),
                MathExpressionEvaluator::bindFunction);
    }

    double evaluate(String source) {
        Objects.requireNonNull(source, "source");
        CompiledExpression<EvaluationContext> expression = expressionCompiler.compile(source);
        return requireFinite(expression.evaluate(EvaluationContext.root()), source);
    }

    private static BoundFunction<EvaluationContext> bindFunction(
            String name, int argumentCount) {
        if (!"SIGMA".equals(name) || argumentCount != 3) {
            return null;
        }
        return (context, arguments) -> {
            if (context.sigmaDepth() >= SIGMA_MAX_NESTING) {
                throw new ArithmeticException(
                        "SIGMA nesting exceeds " + SIGMA_MAX_NESTING);
            }
            EvaluationContext sigmaContext = context.enterSigma();
            int start = toIntegerIndex(
                    requireFinite(arguments.value(0, sigmaContext), "SIGMA start"), "start");
            int end = toIntegerIndex(
                    requireFinite(arguments.value(1, sigmaContext), "SIGMA end"), "end");
            if (start > end) {
                return 0.0;
            }

            long count = (long) end - start + 1L;
            if (count > SIGMA_MAX_ITERATIONS) {
                throw new ArithmeticException("SIGMA: too many iterations (" + count
                        + "), maximum is " + SIGMA_MAX_ITERATIONS);
            }
            sigmaContext.budget().consume(count);

            double sum = 0.0;
            double compensation = 0.0;
            for (long offset = 0; offset < count; offset++) {
                double index = (double) ((long) start + offset);
                double term = requireFinite(
                        arguments.value(2, sigmaContext.withIndex(index)), "SIGMA body");
                double adjusted = term - compensation;
                double next = sum + adjusted;
                compensation = (next - sum) - adjusted;
                sum = next;
            }
            return requireFinite(sum, "SIGMA");
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
            throw new ArithmeticException(
                    "Expression produced a non-finite result: " + source);
        }
        return value;
    }

    private record EvaluationContext(
            double index,
            boolean indexAvailable,
            int sigmaDepth,
            EvaluationBudget budget) {

        static EvaluationContext root() {
            return new EvaluationContext(0.0, false, 0, new EvaluationBudget());
        }

        EvaluationContext enterSigma() {
            return new EvaluationContext(index, indexAvailable, sigmaDepth + 1, budget);
        }

        EvaluationContext withIndex(double newIndex) {
            return new EvaluationContext(newIndex, true, sigmaDepth, budget);
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
