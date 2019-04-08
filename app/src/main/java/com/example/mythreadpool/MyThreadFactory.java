package com.example.mythreadpool;

public interface MyThreadFactory {
    Thread newThread(Runnable runnable);
}
