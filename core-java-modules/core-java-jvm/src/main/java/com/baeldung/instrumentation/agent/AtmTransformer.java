package com.baeldung.instrumentation.agent;

import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class AtmTransformer implements ClassFileTransformer {

    private static Logger LOGGER = LoggerFactory.getLogger(AtmTransformer.class);

    private static final String WITHDRAW_MONEY_METHOD = "withdrawMoney";

    /** The internal form class name of the class to transform */
    private String targetClassName;
    /** The class loader of the class we want to transform */
    private ClassLoader targetClassLoader;

    private String logName;

    public AtmTransformer(String targetClassName, ClassLoader targetClassLoader) {
        this.targetClassName = targetClassName;
        this.targetClassLoader = targetClassLoader;
        this.logName = "LOGGER";
    }

    public AtmTransformer(String targetClassName, ClassLoader targetClassLoader, String logName) {
        this.targetClassName = targetClassName;
        this.targetClassLoader = targetClassLoader;
        this.logName = logName;
    }



    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] byteCode = classfileBuffer;

        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); //replace . with /
        if (!className.equals(finalTargetClassName)) {
            return byteCode;
        }

        if (className.equals(finalTargetClassName) && loader.equals(targetClassLoader)) {
            LOGGER.info("[Agent] Transforming class MyAtm  ############");
            try {
                ClassPool cp = ClassPool.getDefault();
                cp.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
                cp.appendClassPath(new LoaderClassPath(loader));
                ClassClassPath ccpath = new ClassClassPath(this.getClass());
                cp.insertClassPath(ccpath);
                CtClass cc = cp.get(targetClassName);
//                CtMethod m = cc.getDeclaredMethod("processQuery");
                CtMethod[] methods = cc.getDeclaredMethods();
                for (CtMethod oneMethod : methods) {
                    oneMethod.insertBefore(this.logName + ".info(\"[agent instrument] enter\");");
                    oneMethod.insertAfter(this.logName + ".info(\"[agent instrument]  exit\");");
                }

                byteCode = cc.toBytecode();
                cc.detach();
            } catch (NotFoundException | CannotCompileException | IOException e) {
                LOGGER.error("Exception", e);
            }
        }
        return byteCode;
    }
}
