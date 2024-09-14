package com.spring;

//Bean的后置处理器
public interface BeanPostProcessor {
    //初始化前的操作
    Object postProcessBeforeInitialization(Object bean, String beanName);
    //初始化后的操作
    Object postProcessAfterInitialization(Object bean, String beanName);
}
