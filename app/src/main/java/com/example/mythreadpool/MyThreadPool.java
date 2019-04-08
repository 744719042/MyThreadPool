package com.example.mythreadpool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.mythread.MyThreadPool.PoolState.OPEN;
import static com.example.mythread.MyThreadPool.PoolState.SHUTDOWN;
import static com.example.mythread.MyThreadPool.PoolState.SHUTDOWN_NOW;

public class MyThreadPool {
    public enum PoolState {
        OPEN,
        SHUTDOWN,
        SHUTDOWN_NOW
    }
    private volatile PoolState mState = OPEN;
    private JobQueue mJobQueue;
    final Set<Thread> mThreads = new HashSet<>();
    private int mCoreThreadCount;
    private int mMaxThreadCount;
    private TimeUnit mTimeUnit;
    private int mKeepTime;
    private MyThreadFactory mThreadFactory;
    private int mMaxPendingJobs = Integer.MAX_VALUE;
    private RejectPolicy mRejectPolicy;

    public MyThreadPool(int coreThreadCount, int keepCount, int keepTime, TimeUnit timeUnit,
                        MyThreadFactory threadFactory, int maxPendingJobs, RejectPolicy rejectPolicy) {
        this.mCoreThreadCount = coreThreadCount;
        this.mMaxThreadCount = keepCount;
        this.mTimeUnit = timeUnit;
        this.mKeepTime = keepTime;
        this.mThreadFactory = threadFactory;
        this.mMaxPendingJobs = maxPendingJobs;
        this.mRejectPolicy = rejectPolicy;
        validate();
        this.mJobQueue = new JobQueue(maxPendingJobs);
    }

    private void validate() {
        if (mCoreThreadCount < 0) {
            throw new IllegalArgumentException("core thread count should >= 0");
        }

        if (mMaxThreadCount < 0) {
            throw new IllegalArgumentException("keep thread count should >= 0");
        }

        if (mKeepTime < 0) {
            throw new IllegalArgumentException("keep time should >= 0");
        }

        if (mMaxPendingJobs < 0) {
            mMaxPendingJobs = Integer.MAX_VALUE;
        }

        if (mMaxThreadCount < mCoreThreadCount) {
            mMaxThreadCount = mCoreThreadCount;
        }
    }

    private void addThread(boolean core, Runnable firstTask) {
        Thread thread = mThreadFactory.newThread(core ? new CoreWorkerTask(this, mJobQueue, firstTask) :
                new WorkerTask(this, mJobQueue, mKeepTime, mTimeUnit, firstTask));
        if (thread.isAlive()) {
            throw new IllegalStateException("thread factory new thread should not be alive");
        }

        final Thread.UncaughtExceptionHandler handler = thread.getUncaughtExceptionHandler();
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                // 工作线程执行用户任务抛出异常，该工作线程终止清除引用
                synchronized (mThreads) {
                    mThreads.remove(thread);
                }
                handler.uncaughtException(thread, throwable);
            }
        });

        mThreads.add(thread);
        thread.start();
    }

    public MyThreadPool(int coreThreadCount, int keepCount, TimeUnit timeUnit, int keepTime) {
        this(coreThreadCount, keepCount, keepTime, timeUnit, new DefaultThreadFactory(), -1, new DefaultRejectPolicy());
    }

    public boolean isOpen() {
        return mState == OPEN;
    }

    public boolean isShutdown() {
        return mState == SHUTDOWN;
    }

    public boolean isShutdownNow() {
        return mState == SHUTDOWN_NOW;
    }

    public void submit(Runnable runnable) {
        if (!isOpen()) {
            return;
        }

        synchronized (mThreads) {
            // 如果核心线程未满,添加核心线程
            if (mThreads.size() < mCoreThreadCount) {
                addThread(true, runnable);
                return;
            }
        }

        // 核心线程已满，加入任务队列
        if (mJobQueue.add(runnable)) {
            return;
        }

        // 任务队列已满，无法加入，判断普通工作线程是否已满
        synchronized (mThreads) {
            if (mThreads.size() < mMaxThreadCount) {
                // 启动普通工作线程处理任务
                addThread(false, runnable);
            } else {
                // 任务队列和普通工作线程都已满，执行拒绝策略
                mRejectPolicy.reject(runnable);
            }
        }
    }

    @Override
    public String toString() {
        return "MyThreadPool{" +
                "mState=" + mState +
                ", mJobQueue=" + mJobQueue +
                ", mThreads=" + mThreads +
                ", mCoreThreadCount=" + mCoreThreadCount +
                ", mMaxThreadCount=" + mMaxThreadCount +
                ", mTimeUnit=" + mTimeUnit +
                ", mKeepTime=" + mKeepTime +
                ", mThreadFactory=" + mThreadFactory +
                ", mMaxPendingJobs=" + mMaxPendingJobs +
                ", mRejectPolicy=" + mRejectPolicy +
                '}';
    }

    public void shutdown() {
        this.mState = SHUTDOWN;
        interruptThreads();
    }

    public void shutdownNow() {
        this.mState = SHUTDOWN_NOW;
        interruptThreads();
    }

    private void interruptThreads() {
        synchronized (mThreads) {
            for (Thread thread : mThreads) {
                thread.interrupt();
            }
            mThreads.clear();
        }
    }
}
