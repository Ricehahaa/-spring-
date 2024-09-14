package com.spring;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZhouyuApplicationContext {
    private Class configClass;

    //单例池
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    //用来存放BeanPostProcessor类型的bean对象
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    public ZhouyuApplicationContext(Class configClass) {
        this.configClass = configClass;
        //解析配置类
        //Component注解--》扫描路径--》扫描
        scan(configClass);
        //通过上面的扫描,我们就得到了所有的bean定义
        //现在就来弄我们的单例池
        //遍历bean定义
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();
            //如果bean是单例的,就创建bean,而且放进单例池中
            if(beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();

            //依赖注入
            //就是遍历每一个字段,看看是否有AutoWired注解
            //之后就依赖注入,这里只是简单的注入了,循环依赖啥的都没管
            for (Field declaredField : clazz.getDeclaredFields()) {
                if(declaredField.isAnnotationPresent(AutoWired.class)) {
                    Object bean = getBean(declaredField.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(instance,bean);
                }
            }
            //BeanNameAware接口处理
            if(instance instanceof BeanNameAware) {
                ((BeanNameAware)instance).setBeanName(beanName);
            }

            //初始化前操作
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            //初始化
            if(instance instanceof InitializingBean) {
                ((InitializingBean)instance).afterPropertiesSet();
            }

            //初始化后操作
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }



            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void scan(Class configClass) {
        ComponentScan componentScanAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = componentScanAnnotation.value(); //扫描路径
        //扫描
        //3种类加载器,忘了可以去网上查
        //Bootstrap--->加载jre/lib
        //Ext--->加载jre/ext/lib
        //App--->加载classpath下的

        //得到类加载器
        ClassLoader classLoader = ZhouyuApplicationContext.class.getClassLoader();
        //把.替换成/ ,就变成了com/zhouyu/service
        path = path.replace(".","/");
        //这个需要用com/zhouyu/service,而不能是com.zhouyu.service
        URL resource = classLoader.getResource(path);
        //拿到对应的文件
        File file = new File(resource.getFile());
        if(file.isDirectory()){

            File[] files = file.listFiles();
            for (File f : files) {
                //得到绝对路径比如D:\IDEA_project\myspring\target\classes\com\zhouyu\service\UserService.class
                String fileName = f.getAbsolutePath();
                //需要加载的是.class结尾的
                if(fileName.endsWith(".class")){
                    //截取成com\zhouyu\service\UserService
                    String className = fileName.substring(fileName.indexOf("com"),fileName.indexOf(".class"));
                    //把\替换成. 因为是\是转义啊,所以要两个\\,不然"\",\就是转义后面的"了
                    //com.zhouyu.service.UserService
                    className = className.replace("\\",".");

                    try {
                        //有了com.zhouyu.service.UserService就可以用应用类加载器去加载了
                        //因为这个加载器是默认从类路径下加载的啊
                        //这里简单说个小点,为什么命名成clazz,因为class是关键字啊,用不了,又想表达这个意思,歪果人就习惯写成clazz
                        Class<?> clazz  = classLoader.loadClass(className);
                        //判断是否有Component注解,有才会被注册到BeanDefinition中
                        if(clazz.isAnnotationPresent(Component.class)){
//                            if(clazz instanceof BeanPostProcessor.class)
                            //如果是BeanPostProcessor类型就放到beanPostProcessorList中
                            if(BeanPostProcessor.class.isAssignableFrom(clazz)){
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }



                            Component declaredAnnotation = clazz.getDeclaredAnnotation(Component.class);
                            //从Component注解上拿到bean的名字
                            String beanName = declaredAnnotation.value();
                            //创建bean的定义,无论你是单例bean,还是原型bean
                            BeanDefinition beanDefinition = new BeanDefinition();
                            //设置bean定义的类型
                            beanDefinition.setClazz(clazz);
                            //根据scope注解,设置bean定义的作用域
                            if(clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            } else {
                                beanDefinition.setScope("singleton");
                            }
                            //无论是单例bean,还是原型bean都会创建bean的定义,并且放到beanDefinitionMap里
                            beanDefinitionMap.put(beanName, beanDefinition);
                        }

                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public Object getBean(String beanName){
        //从bean定义里看看有没有这个bean
        //因为无论是原型bean还是单例bean,它的数据都会放到beanDefinitionMap里
        if(beanDefinitionMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            //如果是单例bean,就直接从单例池里拿
            if(beanDefinition.getScope().equals("singleton")){
                Object o = singletonObjects.get(beanName);
                return o ;
            } else{
                // 创建原型bean
                return createBean(beanName, beanDefinition);
            }
        } else {
            //没有对应的bean,随便拿的异常
            throw new NullPointerException("不存在对应的bean");
        }
    }
}
