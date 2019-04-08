package com.example.mythreadpool;

public class DefaultRejectPolicy implements RejectPolicy {
    @Override
    public void reject(Runnable job) {
        System.out.println(job.toString());
    }
}
