package com.example.mythreadpool;

public class CoreWorkerTask implements Runnable {
    private JobQueue mJobQueue;
    private MyThreadPool mThreadPool;
    private Runnable mFirstTask;

    public CoreWorkerTask(MyThreadPool threadPool, JobQueue jobQueue, Runnable firstTask) {
        this.mJobQueue = jobQueue;
        this.mThreadPool = threadPool;
        this.mFirstTask = firstTask;
    }

    @Override
    public void run() {
        if (mFirstTask != null) {
            mFirstTask.run();
            mFirstTask = null;
        }

        while (mThreadPool.isOpen() || mThreadPool.isShutdown()) {
            Runnable job = mJobQueue.remove();
            if (job != null) {
                job.run();
                if (!mThreadPool.isOpen() && mJobQueue.isEmpty()) {
                    break;
                }
            } else {
                break;
            }
        }
    }
}
