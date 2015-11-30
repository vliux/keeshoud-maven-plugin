package com.taobao.android;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.taobao.android.mapping.InstrumentMappings;
import com.taobao.android.mapping.MappingFormatError;
import com.taobao.android.mapping.TargetMethodDesc;
import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.Collection;

/**
 * Goal which touches a timestamp file.
 *
 * @deprecated Don't use!
 */
@Mojo( name = "hurdle-jassist", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class JassitMojo extends AbstractMojo {

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    @Parameter( defaultValue = "${project.build.directory}", readonly = true )
    protected File targetDirectory;

    @Parameter( defaultValue = "${project.build.outputDirectory}", readonly = true )
    protected File projectOutputDirectory;

    private InstrumentMappings instrumentMappings = new InstrumentMappings();

    public void execute() throws MojoExecutionException {
        Log log = getLog();
        log.warn("hurdle-jassist start execution ......");

        try {
            doExec(log);
        } catch (Throwable e) {
            throw new MojoExecutionException("failed to execute hurdle-jassist", e);
        }
    }

    private void printInfo(Log log){
        printArtifacts(project.getArtifacts(), log, "");
        log.warn(String.format("hurdle-jassist: === PROJ: %s, FILE=%s, BUILD.DIR=%s, BUUILD.OUTPUT=%s",
                project.getName(),
                project.getFile(),
                project.getBuild().getDirectory(),
                project.getBuild().getOutputDirectory()));

        printArtifacts(project.getCompileArtifacts(), log, "COMPILE");
    }

    private static void printArtifacts(Collection<Artifact> artifacts, Log log, String prefix){
        for(Artifact artifact : artifacts){
            File artFile = artifact.getFile();
            String name= artFile.getName();
            log.warn(String.format("hurdle-jassist: === %s_ARTIFACT: %s, FILE=%s", prefix, name, artFile));
        }
    }

    private void doExec(final Log log) throws NotFoundException, CannotCompileException, IOException, MappingFormatError {
        log.info("hurdle-jassist: config ClassPool ...");
        ClassPool classPool = ClassPool.getDefault();
        log.info(String.format("hurdle-jassist: inserting artifact: %s", projectOutputDirectory.getAbsolutePath()));
        classPool.appendClassPath(projectOutputDirectory.getAbsolutePath());

        for(Artifact artifact : project.getArtifacts()){
            File artFile = artifact.getFile();
            log.info(String.format("hurdle-jassist: inserting artifact: %s", artFile.getAbsolutePath()));
            classPool.insertClassPath(artFile.getAbsolutePath());
        }

        log.info("hurdle-jassist: obtain and defrost MyActivity CtClass");
        CtClass activityCtClz = classPool.getCtClass("com.example.testapp.MyActivity");
        activityCtClz.defrost();

        log.info("hurdle-jassist: obtain Bundle CtClass");
        CtClass bundleCtClz = classPool.getCtClass("android.os.Bundle");
        CtMethod method = activityCtClz.getDeclaredMethod("onCreate", new CtClass[]{bundleCtClz});

        log.info("hurdle-jassist: load mappings ...");
        instrumentMappings.load(log);

        log.info("hurdle-jassist: start instrumentation ...");
        //setUIMethod.insertBefore("{android.util.Log.d(\"vliux\", \"inserted code (insertBefore())\");}");
        long startTime = System.currentTimeMillis();
        doInstrumentExpEditor(classPool, activityCtClz, method);

        log.info(String.format("hurdle-jassist: instrument cost %d ms", System.currentTimeMillis() - startTime));
        activityCtClz.freeze();
        byte[] byteCode = activityCtClz.toBytecode();
        log.info(String.format("hurdle-jassist: generated bytecode length = %d", byteCode.length));
        //InputStream inputStream = new ByteArrayInputStream(byteCode);
        log.info("hurdle-jassist: writting to file class file ...");
        toClassFile(byteCode);
    }

    private void doInstrumentCodeConvertor(ClassPool classPool, CtClass activityClz, CtMethod onCreateMethod)
            throws NotFoundException, CannotCompileException {
        CodeConverter codeConverter = new CodeConverter();
        //CtClass instrumentUtilClz = classPool.get("com.example.testapp.InstrumentUtil");
        CtMethod targetsetUiMethod = activityClz.getDeclaredMethod("setUIUI");
        // setUI method
        CtMethod orgSetUiMethod = activityClz.getDeclaredMethod("setUI");
        codeConverter.redirectMethodCall(orgSetUiMethod, targetsetUiMethod);
        onCreateMethod.instrument(codeConverter);
    }

    private void doInstrumentExpEditor(ClassPool classPool, CtClass activityClz, CtMethod onCreateMethod) throws CannotCompileException, IOException, MappingFormatError {
        onCreateMethod.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                /*if (m.getMethodName().equals("setContentView")) {
                    m.replace("com.example.testapp.InstrumentUtil.setContentView($0);$_ = $proceed($$);");
                }*/

                TargetMethodDesc targetMethodDesc = null;
                try {
                    targetMethodDesc = instrumentMappings.getMappingTarget(m, getLog());
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }

                if(null != targetMethodDesc){
                    m.replace(String.format("%s.%s($0,$$);$_ = $proceed($$);", targetMethodDesc.getClassName(), targetMethodDesc.getMethodName()));
                }
            }
        });
    }

    private void toClassFile(byte[] byteCode) throws IOException {
        File clzFile = new File(projectOutputDirectory, "com/example/testapp/MyActivity.class");
        getLog().info(String.format("hurdle-jassist: save class file to %s", clzFile.getAbsolutePath()));
        OutputStream outputStream = new FileOutputStream(clzFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        bufferedOutputStream.write(byteCode);
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
    }
}
