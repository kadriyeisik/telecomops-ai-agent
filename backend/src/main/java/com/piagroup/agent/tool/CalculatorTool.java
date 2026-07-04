package com.piagroup.agent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evaluates simple math expressions. Example: "12*(4+3)/2"
 * Note: a simple implementation for learning purposes; instead of Nashorn,
 * a small hand-written expression parser is used (see below).
 */
// This tool has been superseded by the Telecom AI Operations Agent tools.
// @Component intentionally removed — class retained for reference only.
public class CalculatorTool implements Tool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "Evaluates a mathematical expression. Supports addition, subtraction, multiplication, division, and parentheses. " +
                "Always use this tool when the user asks a numerical or mathematical question.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "expression", Map.of(
                                "type", "string",
                                "description", "Mathematical expression to evaluate, e.g. '(3+5)*2'"
                        )
                ),
                "required", new String[]{"expression"}
        );
    }

    @Override
    public String execute(Map<String, Object> args) {
        String expression = String.valueOf(args.get("expression"));
        try {
            double result = new SimpleExpressionEvaluator(expression).parse();
            // Return without ".0" if it's a whole number
            if (result == Math.floor(result) && !Double.isInfinite(result)) {
                return String.valueOf((long) result);
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return "Error: could not evaluate expression -> " + e.getMessage();
        }
    }

    /**
     * A small, dependency-free arithmetic expression evaluator
     * (+, -, *, /, parentheses, negative numbers).
     */
    static class SimpleExpressionEvaluator {
        private final String expr;
        private int pos = -1;
        private int ch;

        SimpleExpressionEvaluator(String expr) {
            this.expr = expr.replaceAll("\\s+", "");
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < expr.length()) throw new RuntimeException("Beklenmeyen karakter: " + (char) ch);
            return x;
        }

        void nextChar() {
            ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                eat(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(expr.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Beklenmeyen karakter: " + (char) ch);
            }
            return x;
        }
    }
}
