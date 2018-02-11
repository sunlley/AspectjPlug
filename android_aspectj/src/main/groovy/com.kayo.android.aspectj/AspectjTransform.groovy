package com.kayo.android.aspectj

import com.android.SdkConstants
import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformTask
import com.google.common.collect.ImmutableSet
import org.aspectj.util.FileUtil
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

@SuppressWarnings("GrDeprecatedAPIUsage")
class AspectjTransform extends Transform {
    Project project
    String encoding
    String bootClassPath
    String sourceCompatibility
    String targetCompatibility

    AspectjTransform(Project proj) {
        project = proj
        def configuration = new AndroidConfiguration(project)

        project.afterEvaluate {
            configuration.variants.all { variant ->
                JavaCompile javaCompile = variant.hasProperty('javaCompiler') ?
                        variant.javaCompiler : variant.javaCompile
                encoding = javaCompile.options.encoding
                bootClassPath = configuration.bootClasspath.join(File.pathSeparator)
                sourceCompatibility = javaCompile.sourceCompatibility
                targetCompatibility = javaCompile.targetCompatibility
            }
        }
    }

    boolean showLog() {
        if (project == null) {
            return false
        }
        return project.aspectj.trackLog
    }

    @Override
    String getName() {
        return "AspectjTransform"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return ImmutableSet.<QualifiedContent.ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        def name = QualifiedContent.Scope.PROJECT_LOCAL_DEPS.name()
        def deprecated = QualifiedContent.Scope.PROJECT_LOCAL_DEPS.getClass()
                .getField(name).getAnnotation(Deprecated.class)

        if (deprecated == null) {
            if (showLog()) {
//                println "Cannot Find QualifiedContent.Scope.PROJECT_LOCAL_DEPS Deprecated.class "
            }
            return ImmutableSet.<QualifiedContent.Scope> of(QualifiedContent.Scope.PROJECT
                    , QualifiedContent.Scope.PROJECT_LOCAL_DEPS
                    , QualifiedContent.Scope.EXTERNAL_LIBRARIES
                    , QualifiedContent.Scope.SUB_PROJECTS
                    , QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
        } else {
            if (showLog()) {
//                println "Find QualifiedContent.Scope.PROJECT_LOCAL_DEPS Deprecated.class "
            }
            return ImmutableSet.<QualifiedContent.Scope> of(QualifiedContent.Scope.PROJECT
                    , QualifiedContent.Scope.EXTERNAL_LIBRARIES
                    , QualifiedContent.Scope.SUB_PROJECTS)
        }
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {

        TransformTask transformTask = (TransformTask) context

        if (showLog()) {
            println('+===================================================================')
            println '| Transform Info'
            println '|---------------'
            println "|    Task Name:" + transformTask.variantName
            println("|    IsIncremental:" + isIncremental)
            if (!inputs.isEmpty()) {
                println('|    Inputs:')
                println('|    +---------------------------------------------------------')
                inputs.each {input ->
                    for (JarInput jInput : input.jarInputs) {
                        println('|    | jar:' + jInput.file.absolutePath)
                    }
                    for (DirectoryInput dInput : input.directoryInputs) {
                        println('|    | dir:' + dInput.file.absolutePath)
                    }
                }
                println('|    +---------------------------------------------------------')
            }
            if (!referencedInputs.isEmpty()) {
                println('|    Referenced:')
                println('|    +---------------------------------------------------------')
                referencedInputs.each {input ->
                    for (JarInput jInput : input.jarInputs) {
                        println('|    | jar:' + jInput.file.absolutePath)
                    }
                    for (DirectoryInput dInput : input.directoryInputs) {
                        println('|    | dir:' + dInput.file.absolutePath)

                    }
                }
                println('|    +---------------------------------------------------------')
            }
            println('+===================================================================')
        }

        //clean
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        if (transformTask.variantName.contains("AndroidTest")) {
            println "there is no aspectjrt dependencies in classpath, do nothing "
            inputs.each { TransformInput input ->
                input.directoryInputs.each { DirectoryInput directoryInput ->
                    def dest = outputProvider.getContentLocation(directoryInput.name,
                            directoryInput.contentTypes, directoryInput.scopes,
                            Format.DIRECTORY)
                    FileUtil.copyDir(directoryInput.file, dest)
                    println "directoryInput = ${directoryInput.name}"
                }

                input.jarInputs.each { JarInput jarInput ->
                    def jarName = jarInput.name
                    def dest = outputProvider.getContentLocation(jarName,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)

                    FileUtil.copyFile(jarInput.file, dest)
                    println "jarInput = ${jarInput.name}"
                }
            }
        } else {
            doAspectjTransform(outputProvider, inputs)
        }
    }

    private void doAspectjTransform(TransformOutputProvider outputProvider, Collection<TransformInput> inputs) {
        println "Aspectj Start............"
        AspectjWork aspectWork = new AspectjWork(project)
        aspectWork.encoding = encoding
        aspectWork.bootClassPath = bootClassPath
        aspectWork.sourceCompatibility = sourceCompatibility
        aspectWork.targetCompatibility = targetCompatibility

        //create aspect destination dir
        File resultDir = outputProvider.getContentLocation("aspect", getOutputTypes(), getScopes(), Format.DIRECTORY)
        if (resultDir.exists()) {
            println "delete resultDir ${resultDir.absolutePath}"
            AspectjFileUtil.deleteFolder(resultDir)
        }
        AspectjFileUtil.mkdirs(resultDir)

        aspectWork.destinationDir = resultDir.absolutePath

        List<String> includeJarFilter = project.aspectj.includeJarFilter
        List<String> excludeJarFilter = project.aspectj.excludeJarFilter

        aspectWork.setAjcArgs(project.aspectj.ajcArgs)
        inputs.each {TransformInput transformInput ->
            transformInput.directoryInputs.each {directoryInput ->
                aspectWork.aspectPath << directoryInput.file
                aspectWork.inPath << directoryInput.file
                aspectWork.classPath << directoryInput.file
            }
            transformInput.jarInputs.each {jarInput ->
                aspectWork.aspectPath << jarInput.file
                aspectWork.classPath << jarInput.file

                String jarPath = jarInput.file.absolutePath
                if (isIncludeFilterMatched(jarPath, includeJarFilter)
                        && !isExcludeFilterMatched(jarPath, excludeJarFilter)) {
                    if (showLog()) {
                        println "includeJar:::${jarPath}"
                    }
                    aspectWork.inPath << jarInput.file
                } else {
                    if (showLog()) {
                        println "excludeJar:::${jarPath}"
                    }
                    copyJar(outputProvider, jarInput)
                }
            }
        }
        aspectWork.doWork()

        //add class file to aspect result jar
        println "Aspectj *.Jar Merging...."
        if (resultDir.listFiles().length > 0) {
            File jarFile = outputProvider.getContentLocation("aspected", getOutputTypes(), getScopes(), Format.JAR)
            AspectjFileUtil.mkdirs(jarFile.getParentFile())
            AspectjFileUtil.deleteIfExists(jarFile)

            AspectjJarUtil jarMerger = new AspectjJarUtil(jarFile)
            try {
                jarMerger.setFilter(new AspectjJarUtil.IZipEntryFilter() {
                    @Override
                    boolean checkEntry(String archivePath)
                            throws AspectjJarUtil.IZipEntryFilter.ZipAbortException {
                        return archivePath.endsWith(SdkConstants.DOT_CLASS)
                    }
                })
                jarMerger.addFolder(resultDir)
            } catch (Exception e) {
                throw new TransformException(e)
            } finally {
                jarMerger.close()
            }

        }

        AspectjFileUtil.deleteFolder(resultDir)

        println "Aspectj Done............."
    }

    boolean isExcludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
    }

    boolean isIncludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
    }

    boolean isFilterMatched(String str, List<String> filters, FilterPolicy filterPolicy) {
        if (str == null) {
            return false
        }

        if (filters == null || filters.isEmpty()) {
            return filterPolicy == FilterPolicy.INCLUDE
        }
        filters.each {s ->
            if (isContained(str,s)){
                return true
            }
        }

        return false
    }

    static boolean copyJar(TransformOutputProvider outputProvider, JarInput jarInput) {
        if (outputProvider == null || jarInput == null) {
            return false
        }

        String jarName = jarInput.name
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }

        File dest = outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

        FileUtil.copyFile(jarInput.file, dest)

        return true
    }

    static boolean isContained(String str, String filter) {
        if (str == null) {
            return false
        }

        String filterTmp = filter
        if (str.contains(filterTmp)) {
            return true
        } else {
            if (filterTmp.contains("/")) {
                return str.contains(filterTmp.replace("/", File.separator))
            } else if (filterTmp.contains("\\")) {
                return str.contains(filterTmp.replace("\\", File.separator))
            }
        }

        return false
    }

    enum FilterPolicy {
        INCLUDE,
        EXCLUDE
    }
}