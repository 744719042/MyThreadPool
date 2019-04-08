package com.example.mythreadpool;

public interface RejectPolicy {
    void reject(Runnable job);
}
