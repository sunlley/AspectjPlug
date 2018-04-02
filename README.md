
# AspectjPlug [![](https://github.com/kayoSun/resource/blob/master/svgs/version3.0.2.svg)]() [![](https://github.com/kayoSun/resource/blob/master/svgs/apachelicense.svg)](LICENSE.txt)

## 简化Aspectj的开发使用

## 如何使用
  在主工程下的build.gradle 添加如下：
  
  ```groovy
 buildscript {
    repositories {
        jcenter()
    }
    dependencies {
         classpath 'com.kayo.android.aspectj:aspectj-plug:3.0.2'
    }
  }
```
  
  在要发布代码库的build.gradle 添加如下：
  
  ```groovy
apply plugin: 'aspectj-plug'
  aspectj { // 新增
      trackLog true//是否开启插件log
  }
```

##  gradle版本注意事项
  
| jcener-plug version | Java Projects | Android Projects |
|---------------------|---------------|------------------|
| 3.0.2-alpha         | Gradle 4.0+   | Gradle 4.1+      |
| 3.0.2               | Gradle 4.0+   | Gradle 4.1+      |


##  感谢
  最后欢迎提出您的宝贵意见、建议，祝大家工作愉快！
