package models.comparison;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Загружает правила сравнения моделей из {@code .properties}-файла.
 *
 * <p><b>Формат строки:</b>
 * <pre>{@code
 *   RequestClass=ResponseClass:field1=field1Response,field2=field2Response
 * }</pre>
 * Классы указываются по {@code getSimpleName()}.
 * Двоеточие обязательно: даже если маппинг пустой, строка должна
 * содержать {@code ":"} — иначе она будет молча пропущена.
 *
 * <p>Пример:
 * <pre>{@code
 *   UserJson=CreateUserResponse:username=username,role=role
 *   CustomerAccountJson=CustomerAccountJson:id=id,accountNumber=accountNumber,balance=balance
 * }</pre>
 */
public class ModelComparisonConfigLoader {

    private final Map<String, ComparisonRule> rules = new HashMap<>();

    public ModelComparisonConfigLoader(String configFile) {
        try (InputStream input = getClass()
                .getClassLoader()
                .getResourceAsStream(configFile)) {

            if (input == null) {
                throw new IllegalArgumentException("Config file not found: " + configFile);
            }

            Properties props = new Properties();
            props.load(input);

            for (String key : props.stringPropertyNames()) {
                String raw = props.getProperty(key);
                String[] target = raw.split(":", 2);

                if (target.length != 2) {
                    throw new IllegalStateException(
                            "Неверный формат правила '" + key + "=" + raw
                                    + "'. Ожидается RequestClass=ResponseClass:field1=field1Response,...");
                }

                String responseClassName = target[0].trim();
                List<String> fields = Arrays.asList(target[1].split(","));

                rules.put(key.trim(), new ComparisonRule(responseClassName, fields));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load DTO comparison config", e);
        }
    }

    public ComparisonRule getRuleFor(Class<?> requestClass) {
        return rules.get(requestClass.getSimpleName());
    }

    public static class ComparisonRule {

        private final String responseClassSimpleName;
        private final Map<String, String> fieldMappings;

        public ComparisonRule(String responseClassSimpleName, List<String> fieldPairs) {
            this.responseClassSimpleName = responseClassSimpleName;
            this.fieldMappings = new HashMap<>();

            for (String pair : fieldPairs) {
                String trimmed = pair.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split("=");
                if (parts.length == 2) {
                    fieldMappings.put(parts[0].trim(), parts[1].trim());
                } else {
                    // fallback: одинаковое имя поля с обеих сторон
                    fieldMappings.put(trimmed, trimmed);
                }
            }
        }

        public String getResponseClassSimpleName() {
            return responseClassSimpleName;
        }

        public Map<String, String> getFieldMappings() {
            return fieldMappings;
        }
    }
}