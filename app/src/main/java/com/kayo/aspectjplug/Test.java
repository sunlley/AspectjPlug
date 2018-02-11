package com.kayo.aspectjplug;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class Test {

    @After("execution(void com.kayo.aspectjplug.MainActivity.onCreate(..))")
    public void a(){

        System.out.println("onCreate 被调用");
    }
}
