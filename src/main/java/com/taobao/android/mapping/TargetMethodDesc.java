package com.taobao.android.mapping;

import javassist.CtMethod;

/**
 * Created by vliux on 15-11-26.
 */
public class TargetMethodDesc {
    private String className;
    private String methodName;

    public TargetMethodDesc(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }
}
