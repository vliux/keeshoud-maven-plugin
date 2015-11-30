package com.taobao.android.mapping;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.expr.MethodCall;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vliux on 15-11-26.
 */
public class InstrumentMappings {
    private Map<String, Map<SourceMethodDesc, TargetMethodDesc>> mMappings;

    public void load(Log log) throws IOException, MappingFormatError {
        mMappings = new MappingParser().loadMappings(log);
    }

    public TargetMethodDesc getMappingTarget(MethodCall methodCall, Log log) throws NotFoundException {
        String clzName = methodCall.getMethod().getDeclaringClass().getName();
        if(mMappings.containsKey(clzName)){
            CtMethod ctMethod = methodCall.getMethod();
            Map<SourceMethodDesc, TargetMethodDesc> methodMappings = mMappings.get(clzName);
            // param types from CtMethod
            List<String> params = new ArrayList<String>();
            for(CtClass pCtClz : ctMethod.getParameterTypes()){
                params.add(pCtClz.getName());
            }

            SourceMethodDesc key = new SourceMethodDesc(ctMethod.getName(), params.toArray(new String[params.size()]));
            if(methodMappings.containsKey(key)){
                return methodMappings.get(key);
            }
        }

        return null;
    }
}
