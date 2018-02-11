package com.kayo.android.aspectj

/**
 * 自定义属性类
 */
class AspectjExtension {

    List<String> includeJarFilter = new ArrayList<String>()
    List<String> excludeJarFilter = new ArrayList<String>()
    List<String> ajcArgs=new ArrayList<>()
    boolean trackLog = false

    AspectjExtension includeJarFilter(String...filters) {

        if (filters != null) {
            filters.each {filter ->
                println "AndroidAspectjPlugin  includeJarFilter:"+filter
            }
            includeJarFilter.addAll(filters)
        }

        return this
    }

    AspectjExtension excludeJarFilter(String...filters) {
        if (filters != null) {
            filters.each {filter ->
                println "AndroidAspectjPlugin  excludeJarFilter:"+filter
            }
            excludeJarFilter.addAll(filters)
        }

        return this
    }
    AspectjExtension ajcArgs(String...ajcArgs) {
        if (ajcArgs != null) {
            ajcArgs.each {ajcArg ->
                println "AndroidAspectjPlugin  ajcArg:"+ajcArg
            }
            this.ajcArgs.addAll(ajcArgs)
        }
        return this
    }

    AspectjExtension trackLog(boolean show){
        trackLog = show
        return this
    }
}