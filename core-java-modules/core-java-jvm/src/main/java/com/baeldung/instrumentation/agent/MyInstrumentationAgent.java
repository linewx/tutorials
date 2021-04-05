package com.baeldung.instrumentation.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public class MyInstrumentationAgent {
    private static Logger LOGGER = LoggerFactory.getLogger(MyInstrumentationAgent.class);

    public static void premain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In premain methods1111");

        String className = "com.hp.maas.platform.ems.impl.EntityManagementServiceImpl";
        transformClass(className,inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        LOGGER.info("[Agent] In agentmain method22222");

        String className = "com.hp.maas.platform.ems.impl.EntityManagementServiceImpl";
        transformClass(className,inst);
    }

    private static void transformClass(String className, Instrumentation instrumentation) {
        Class<?> targetCls = null;
        ClassLoader targetClassLoader = null;
//        // see if we can get the class using forName
//        try {
//            for (Class<?> type : instrumentation.getAllLoadedClasses()) {
//                if (className.equals(type.getName())) {
//                    targetCls = type;
//                    break;
//                }
//            }
////            targetCls = Class.forName(className);
//            targetClassLoader = targetCls.getClassLoader();
//            transform(targetCls, targetClassLoader, instrumentation);
//            return;
//        } catch (Exception ex) {
//            LOGGER.error("Class [{}] not found with Class.forName");
//        }
        // otherwise iterate all loaded classes and find what we want
        for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {

            if(clazz.getName().startsWith("com.hp.maas.platform")) {
                LOGGER.info("injecting class {}", clazz.getName());
                targetCls = clazz;
                targetClassLoader = targetCls.getClassLoader();
                String logName = getLogName(clazz);
                if (logName != null) {
                    transform(targetCls, targetClassLoader, instrumentation, logName);
                }
            }
        }
        //throw new RuntimeException("Failed to find class [" + className + "]");
    }

    static Boolean hasLogger(Class<?> clazz) {
        LOGGER.info("finding logger in class {}", clazz.getName());
        try{
            Field logger = clazz.getField("LOGGER");
            LOGGER.info("found logger in class {}", clazz.getName());
            return true;
        }catch (Exception e){
            LOGGER.error("no logger found in class {}", clazz.getName(), e);
            return false;
        }
    }

    static String getLogName(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
//            if (Logger.class.isAssignableFrom(field.getType())) {
            if ("org.slf4j.Logger".equals(field.getType().getName())) {
                LOGGER.info("found logger {} in class {}", field.getName(), clazz.getName());
                return field.getName();
            }
        }
        return null;
    }

    private static void transform(Class<?> clazz, ClassLoader classLoader, Instrumentation instrumentation, String logName) {
        AtmTransformer dt = new AtmTransformer(clazz.getName(), classLoader, logName);
        instrumentation.addTransformer(dt, true);
        try {
            instrumentation.retransformClasses(clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Transform failed for class: [" + clazz.getName() + "]", ex);
        }
    }

}
