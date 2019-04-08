package com.example.mythreadpool;

import java.util.concurrent.TimeUnit;

public class WorkerTask implements Runnable {
    private JobQueue mJobQueue;
    private MyThreadPool mThreadPool;
    private int mKeepTime;
    private TimeUnit mTimeUnit;
    private Runnable mFirstTask;

    public WorkerTask(MyThreadPool threadPool, JobQueue jobQueue,
                      int keepTime, TimeUnit timeUnit, Runnable firstTask) {
        this.mJobQueue = jobQueue;
        this.mThreadPool = threadPool;
        this.mKeepTime = keepTime;
        this.mTimeUnit = timeUnit;
        this.mFirstTask = firstTask;
    }

    @Override
    public void run() {
        if (mFirstTask != null) {
            mFirstTask.run();
            mFirstTask = null;
        }

        while (mThreadPool.isOpen() || mThreadPool.isShutdown()) {
            long waitTime = mTimeUnit.toMillis(mKeepTime);
            Runnable job = mJobQueue.remove(waitTime);
            if (job != null) {
                job.run();
                if (!mThreadPool.isOpen() && mJobQueue.isEmpty()) {
                    break;
                }
            } else {
                synchronized (mThreadPool.mThreads) {
                    mThreadPool.mThreads.remove(Thread.currentThread());
                }
                break;
            }
        }
    }
}
