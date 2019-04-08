package com.example.mythreadpool;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultThreadFactory implements MyThreadFactory {
    private AtomicInteger mNum = new AtomicInteger(0);
    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, "Default-Thread-#" + mNum.getAndAdd(1));
    }
}
