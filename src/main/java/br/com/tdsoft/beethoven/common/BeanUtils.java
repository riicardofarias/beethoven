package br.com.tdsoft.beethoven.common;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class BeanUtils {
    private BeanUtils() {
    }

    public static <T> T getBean(Class<?> config, Class<T> bean) {
        try (var context = new AnnotationConfigApplicationContext(config)) {
            return context.getBean(bean);
        }
    }
}
