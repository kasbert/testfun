package org.testfun.jee.runner.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.testfun.jee.runner.inject.MockInitialContextFactory.MockContext;

public abstract class BaseInjector implements Injector {

    private MockContext instanceByClass = MockInitialContextFactory.getMockContext();

    private MockRegistrar mockRegistrar;

    public BaseInjector withMocking(MockRegistrar mockRegistrar) {
        this.mockRegistrar = mockRegistrar;
        return this;
    }

    abstract Class<? extends Annotation> getAnnotation();

    @Override
    public final <T> void inject(T target, Field field) {
        if (field.isAnnotationPresent(getAnnotation())) {
            doInject(target, field);
        }
    }

    public Object findInstanceByClass(Class<?> clazz) {
        Object instance = instanceByClass.get(clazz);
        return instance != null || mockRegistrar == null ? instance : mockRegistrar.findInstanceByClass(clazz);
    }
    abstract  <T> void doInject(T target, Field field);

    protected void registerByImplementedInterfaces(Object instance) {
        Class<?>[] interfaces = instance.getClass().getInterfaces();
        for (Class<?> implementedInterface : interfaces) {
            instanceByClass.put(implementedInterface, instance);
        }
    }

    protected void registerByClass(Class clazz, Object instance) {
        instanceByClass.put(clazz, instance);
    }

    @Override
    public void reset() {
        instanceByClass.clear();
    }

}
