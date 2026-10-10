package jupiter.extension;

import jupiter.annotation.Data;
import models.TestData;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.ArrayList;

public class TestDataExtension implements BeforeEachCallback, ParameterResolver {
    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TestDataExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), Data.class)
                .ifPresent(annotation -> {
                    setContent(new TestData(new ArrayList<>()));
                });
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getRequiredTestMethod().isAnnotationPresent(Data.class) &&
                parameterContext.getParameter().getType().equals(TestData.class);
    }

    @Override
    public TestData resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getUniqueId(), TestData.class);
    }

    public static TestData getContent() {
        final ExtensionContext context = TestsMethodContextExtension.context();
        return context.getStore(NAMESPACE).get(context.getUniqueId(), TestData.class);
    }

    public static void setContent(TestData data) {
        final ExtensionContext context = TestsMethodContextExtension.context();
        context.getStore(NAMESPACE).put(context.getUniqueId(), data);
    }
}
