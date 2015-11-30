package com.taobao.android.mapping;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vliux on 15-11-26.
 */
class MappingParser {
    private static final String SEP = "```";
    private static final String PATTERN_LEFT = "(\\S+)\\((.*)\\)";

    private Pattern mPatternLeft;

    public MappingParser() {
        mPatternLeft = Pattern.compile(PATTERN_LEFT);
    }

    /**
     * Key: source class name
     * Value: mapping method in source class to target method.
     * @param log
     * @return
     * @throws IOException
     * @throws MappingFormatError
     */
    public Map<String, Map<SourceMethodDesc, TargetMethodDesc>> loadMappings(Log log) throws IOException, MappingFormatError {
        log.info("start parsing mapping file ...");
        InputStream inputStream = getClass().getResourceAsStream("/mappings.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        Map<String, Map<SourceMethodDesc, TargetMethodDesc>> mappings = new HashMap<String, Map<SourceMethodDesc, TargetMethodDesc>>();

        String line = null;
        String currentClz = null;
        Map<SourceMethodDesc, TargetMethodDesc> currentMethodMapping = null;
        int clzCfgIndex = 0; // n-th method inside class block
        while ((line = reader.readLine()) != null){
            line = line.trim();

            if(SEP.equals(line)){
                // new class mappings
                currentClz = reader.readLine();
                log.info(String.format(" parsing source class: %s", currentClz));
                clzCfgIndex = 0;
                if(null == currentClz || currentClz.length() <= 0){
                    throw new MappingFormatError("empty class descriptor line after seperator line");
                }

                if(mappings.containsKey(currentClz)){
                    currentMethodMapping = mappings.get(currentClz);
                }else{
                    currentMethodMapping = new HashMap<SourceMethodDesc, TargetMethodDesc>();
                    mappings.put(currentClz, currentMethodMapping);
                }
            }else{
                parseMethodConfig(line, log, currentClz, ++clzCfgIndex, currentMethodMapping);

            }
        }

        return mappings;
    }

    private void parseMethodConfig(String line, Log log, String currentClz, int clzCfgIndex, Map<SourceMethodDesc, TargetMethodDesc> methodMappings) throws MappingFormatError {
        String[] sects = line.split("=");
        if(null == sects || sects.length != 2){
            throw new MappingFormatError(String.format("invalid format (%dth in %s)", clzCfgIndex, currentClz));
        }

        SourceMethodDesc srcMethod = parseMethodConfigLeftSide(sects[0], log, currentClz, clzCfgIndex);
        TargetMethodDesc tgtMethod = paraseMethodConfigRightSide(sects[1], log, currentClz, clzCfgIndex);
        methodMappings.put(srcMethod, tgtMethod);
        log.info(String.format("%s --> %s.%s", srcMethod.getName(), tgtMethod.getClassName(), tgtMethod.getMethodName()));
    }

    private SourceMethodDesc parseMethodConfigLeftSide(String leftSide, Log log, String currentClz, int clzCfgIndex) throws MappingFormatError {
        Matcher matcher = mPatternLeft.matcher(leftSide);
        if(!matcher.find()){
            throw new MappingFormatError(String.format("invalid format (%dth in %s): left-side is not a method", clzCfgIndex, currentClz));
        }

        String methodName = matcher.group(1);
        String params = matcher.group(2);
        // parse params
        String[] paramSects = params.split(",");
        return new SourceMethodDesc(methodName, paramSects);
    }

    private TargetMethodDesc paraseMethodConfigRightSide(String rightSide, Log log, String currentClz, int clzCfgIndex) throws MappingFormatError {
        int index = rightSide.lastIndexOf('.');
        if(index <= 0){
            throw new MappingFormatError(String.format("invalid format (%dth in %s): right-side wrong", clzCfgIndex, currentClz));
        }

        String clzName = rightSide.substring(0, index);
        String methodName = rightSide.substring(index + 1, rightSide.length());
        return new TargetMethodDesc(clzName, methodName);
    }
}
