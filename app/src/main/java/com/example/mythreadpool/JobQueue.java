package com.example.mythreadpool;

import java.util.LinkedList;

public class JobQueue {
    private final Object mLock = new Object();
    private LinkedList<Runnable> mJobs = new LinkedList<>();
    private int mMaxJobs;

    public JobQueue(int maxJobs) {
        this.mMaxJobs = maxJobs;
    }

    public boolean add(Runnable runnable) {
        synchronized (mLock) {
            if (mJobs.size() >= mMaxJobs) {
                return false;
            }

            mJobs.addFirst(runnable);
            mLock.notify();
            return true;
        }
    }

    public Runnable remove(long timeout) {
        synchronized (mLock) {
            long start = System.currentTimeMillis();
            while (mJobs.isEmpty()) { // 如果任务队列为空，等待
                try {
                    // 已经等待的时间
                    long waitTime = System.currentTimeMillis() - start;
                    // 伪唤醒导致wait()返回，等待时间没达到timeout，继续等待
                    if (waitTime < timeout) {
                        waitTime = timeout - waitTime;
                    } else { // 如果已经到了超时时间，依然没有任务，不再等待返回空对象
                        return null;
                    }
                    mLock.wait(waitTime);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                    // 如果wait期间收到中断异常，用户关闭了线程池
                    if (mJobs.isEmpty()) {
                        return null;
                    }
                }
            }

            return mJobs.removeLast();
        }
    }

    public Runnable remove() {
        synchronized (mLock) {
            while (mJobs.isEmpty()) {
                try {
                    mLock.wait();
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                    // 如果wait期间收到中断异常，用户关闭了线程池
                    if (mJobs.isEmpty()) {
                        return null;
                    }
                }
            }

            return mJobs.removeLast();
        }
    }

    public boolean isEmpty() {
        synchronized (mLock) {
            return mJobs.isEmpty();
        }
    }

    @Override
    public String toString() {
        return "JobQueue{" +
                "mLock=" + mLock +
                ", mJobs=" + mJobs +
                ", mMaxJobs=" + mMaxJobs +
                '}';
    }
}
