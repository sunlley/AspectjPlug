package com.kayo.android.aspectj

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidAspectjPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        def moduleName = 'Module:'+project.name
        def line = "============================================"
        println(line)
        println("         Hello AndroidAspectjPlugin !")
        if (moduleName.length() >= line.length()){
            println(" "+moduleName)
        }else {
            def dy = (line.length() -moduleName.length())/2
            println( " "*dy+moduleName)
        }
        println(line)

        //添加仓库
        project.repositories {
            mavenLocal()
        }

        //添加依赖
        project.dependencies {
            implementation 'org.aspectj:aspectjrt:1.8.13'
        }

        //自定义 gradle属性
        project.extensions.create("aspectj", AspectjExtension)

        if (project.plugins.hasPlugin(AppPlugin)) {
            //build time trace
            if (project.aspectj.trackLog){
                project.gradle.addListener(new AspectjTaskTrace())
            }

            //register AspectjTransform
            AppExtension android = project.extensions.getByType(AppExtension)
            android.registerTransform(new AspectjTransform(project))
        }
    }
}
