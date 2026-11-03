package models.comparison;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сравнивает поля двух моделей по маппингу {@code requestField -> responseField}.
 *
 * <p><b>Про BigDecimal.</b> {@code new BigDecimal("500")} и
 * {@code new BigDecimal("500.00")} — семантически равные значения, но
 * {@code equals} между ними возвращает {@code false} (разный scale).
 * Поэтому для {@link BigDecimal} используется {@link BigDecimal#compareTo},
 * а не {@code String.valueOf(...)}.
 */
public class ModelComparator {

    public static <A, B> ComparisonResult compareFields(A request,
                                                        B response,
                                                        Map<String, String> fieldMappings) {
        List<Mismatch> mismatches = new ArrayList<>();

        for (Map.Entry<String, String> entry : fieldMappings.entrySet()) {
            String requestField = entry.getKey();
            String responseField = entry.getValue();

            Object value1 = getFieldValue(request, requestField);
            Object value2 = getFieldValue(response, responseField);

            if (!valuesMatch(value1, value2)) {
                mismatches.add(new Mismatch(
                        requestField + " -> " + responseField, value1, value2));
            }
        }

        return new ComparisonResult(mismatches);
    }

    /**
     * Сравнивает два значения с учётом семантики типов.
     *
     * <ul>
     *     <li>{@link BigDecimal} — через {@link BigDecimal#compareTo}, чтобы
     *         {@code 500} и {@code 500.00} считались равными;</li>
     *     <li>остальное — через {@link Objects#equals} по {@code String.valueOf}.</li>
     * </ul>
     */
    private static boolean valuesMatch(Object v1, Object v2) {
        if (v1 == null && v2 == null) {
            return true;
        }
        if (v1 == null || v2 == null) {
            return false;
        }
        if (v1 instanceof BigDecimal b1 && v2 instanceof BigDecimal b2) {
            return b1.compareTo(b2) == 0;
        }
        return Objects.equals(String.valueOf(v1), String.valueOf(v2));
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot access field: " + fieldName, e);
            }
        }
        throw new RuntimeException(
                "Field not found: " + fieldName + " in class " + obj.getClass().getName());
    }

    public static class ComparisonResult {
        private final List<Mismatch> mismatches;

        public ComparisonResult(List<Mismatch> mismatches) {
            this.mismatches = mismatches;
        }

        public boolean isSuccess() {
            return mismatches.isEmpty();
        }

        public List<Mismatch> getMismatches() {
            return mismatches;
        }

        @Override
        public String toString() {
            if (isSuccess()) {
                return "All fields match.";
            }
            StringBuilder sb = new StringBuilder("Mismatched fields:\n");
            for (Mismatch m : mismatches) {
                sb.append("- ").append(m.fieldName)
                        .append(": expected=").append(m.expected)
                        .append(", actual=").append(m.actual).append("\n");
            }
            return sb.toString();
        }
    }

    public static class Mismatch {
        public final String fieldName;
        public final Object expected;
        public final Object actual;

        public Mismatch(String fieldName, Object expected, Object actual) {
            this.fieldName = fieldName;
            this.expected = expected;
            this.actual = actual;
        }
    }
}