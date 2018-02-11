package com.kayo.android.aspectj

import org.gradle.internal.time.Clock

@SuppressWarnings("GroovyUnusedDeclaration")
class TrackClock implements Clock {

    long currentTimeMillis
    Object tag

    TrackClock() {
        setCurrentTimeMillis()
    }

    void setCurrentTimeMillis(){
        this.currentTimeMillis = System.currentTimeMillis()
    }

    void setCurrentTimeMillis(long millis) {
        this.currentTimeMillis = millis
    }

    void setTag(Object tag) {
        this.tag = tag
    }

    @Override
    long getCurrentTime() {
        return currentTimeMillis
    }
    /**
     * @return 获取到 setCurrentTimeMillis 到 次方法被调用的时间
     */
    long getRunTimeInMillis() {
        return System.currentTimeMillis() - currentTimeMillis
    }

    Object getTag() {
        return tag
    }
}