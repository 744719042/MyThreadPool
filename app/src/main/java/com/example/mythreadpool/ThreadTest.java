package com.example.mythreadpool;

import java.util.concurrent.TimeUnit;

public class ThreadTest {
    public static void main(String[] args) {
        MyThreadPool myThreadPool = new MyThreadPool(10, 15, 30, TimeUnit.SECONDS, new DefaultThreadFactory(), 6, new DefaultRejectPolicy());
        System.out.println(myThreadPool);
        for (int i = 0; i < 300; i++) {
            final int index = i;
            myThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    System.out.println("I am " + index + ", run on " + Thread.currentThread().getName());
                }

                @Override
                public String toString() {
                    return "task " + index + " rejected!!!";
                }
            });
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(myThreadPool.mThreads.size());
        System.out.println(myThreadPool);

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(myThreadPool.mThreads.size());
        System.out.println(myThreadPool);
        myThreadPool.shutdownNow();
    }
}
