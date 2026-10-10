package generators;

import com.github.curiousoddman.rgxgen.RgxGen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Генератор случайных моделей по классу.
 *
 * <p>Поддерживает:
 * <ul>
 *     <li><b>record</b> — через канонический конструктор и
 *         {@link RecordComponent#getAnnotation(Class)};</li>
 *     <li><b>обычные POJO</b> — через no-arg конструктор и рефлексию
 *         по полям;</li>
 *     <li>{@link GeneratingRule} — генерация по regex через
 *         {@link RgxGen};</li>
 *     <li>примитивы, обёртки, {@link String}, {@link Date},
 *         {@link Enum}, {@code List<String>}, вложенные объекты.</li>
 * </ul>
 *
 * <p>Не поддерживает: {@code List<T>} для произвольного {@code T},
 * дженерик-поля с несколькими параметрами, финальные коллекции
 * с инициализацией в объявлении.
 */
public class RandomModelGenerator {

    private static final Random random = new Random();

    /**
     * Генерирует экземпляр {@code clazz}, заполняя все поля случайными
     * значениями. Если у поля есть {@link GeneratingRule} — значение
     * генерируется по regex.
     *
     * @param clazz класс модели
     * @param <T>   тип модели
     * @return заполненный экземпляр
     */
    public static <T> T generate(Class<T> clazz) {
        try {
            if (clazz.isRecord()) {
                return generateRecord(clazz);
            }
            return generatePojo(clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate " + clazz.getSimpleName(), e);
        }
    }

    // ---------- record ----------

    private static <T> T generateRecord(Class<T> clazz) throws Exception {
        RecordComponent[] components = clazz.getRecordComponents();

        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] paramValues = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent rc = components[i];
            paramTypes[i] = rc.getType();

            GeneratingRule rule = rc.getAnnotation(GeneratingRule.class);
            paramValues[i] = rule != null
                    ? generateFromRegex(rule.regex(), rc.getType())
                    : generateRandomValue(rc.getType(), rc.getGenericType());
        }

        Constructor<T> canonical = clazz.getDeclaredConstructor(paramTypes);
        canonical.setAccessible(true);
        return canonical.newInstance(paramValues);
    }

    // ---------- POJO ----------

    private static <T> T generatePojo(Class<T> clazz) throws Exception {
        T instance = clazz.getDeclaredConstructor().newInstance();

        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);

            GeneratingRule rule = field.getAnnotation(GeneratingRule.class);
            Object value = rule != null
                    ? generateFromRegex(rule.regex(), field.getType())
                    : generateRandomValue(field.getType(), field.getGenericType());

            field.set(instance, value);
        }
        return instance;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    // ---------- значение поля ----------

    private static Object generateRandomValue(Class<?> type, Type genericType) {
        if (type.equals(String.class)) {
            return UUID.randomUUID().toString().substring(0, 8);

        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return random.nextInt(1000);

        } else if (type.equals(Long.class) || type.equals(long.class)) {
            return random.nextLong();

        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return random.nextDouble() * 100;

        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return random.nextBoolean();

        } else if (type.equals(Date.class)) {
            return new Date(System.currentTimeMillis() - random.nextInt(1_000_000_000));

        } else if (type.isEnum()) {
            return randomEnum(type);

        } else if (List.class.isAssignableFrom(type)) {
            return generateRandomList(genericType);

        } else {
            // вложенный объект / record
            return generate(type);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object randomEnum(Class<?> type) {
        Object[] constants = type.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        return constants[random.nextInt(constants.length)];
    }

    // ---------- regex ----------

    private static Object generateFromRegex(String regex, Class<?> type) {
        String result = new RgxGen(regex).generate();

        if (type.equals(String.class)) {
            return result;
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.parseInt(result);
        }
        if (type.equals(Long.class) || type.equals(long.class)) {
            return Long.parseLong(result);
        }
        if (type.equals(Double.class) || type.equals(double.class)) {
            return Double.parseDouble(result);
        }
        if (type.isEnum()) {
            return parseEnum(type, result);
        }
        // по умолчанию — строка
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Class<?> type, String value) {
        try {
            return Enum.valueOf((Class<? extends Enum>) type, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Значение '" + value + "' не найдено в enum " + type.getSimpleName()
                            + ". Проверь @GeneratingRule.regex — он должен генерировать "
                            + "имя одной из констант.", e);
        }
    }

    // ---------- list ----------

    private static List<?> generateRandomList(Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            Type elementType = pt.getActualTypeArguments()[0];

            if (elementType == String.class) {
                return List.of(
                        UUID.randomUUID().toString().substring(0, 5),
                        UUID.randomUUID().toString().substring(0, 5));
            }
            if (elementType instanceof Class<?> elementClass
                    && elementClass.isEnum()) {
                return List.of(
                        randomEnum(elementClass),
                        randomEnum(elementClass));
            }
        }
        return Collections.emptyList();
    }
}