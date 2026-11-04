package com.aya.learning.taskrunner;

import java.util.List;

/**
 * 并发任务调度器骨架——<b>第 3 周实现</b>，本周只读，不要在这里花时间。
 * <p>第 3 周要补：{@code ThreadPoolExecutor} 的 7 个参数怎么配、队列满了走什么拒绝策略、
 * 超时怎么取消、{@code CompletableFuture} 怎么聚合结果、怎么优雅关闭。</p>
 * <p>为什么本周就建骨架：先让你看见“接口长什么样”，第 1 周的类/接口/枚举知识在这里能复用。</p>
 */
public final class TaskRunner {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueCapacity;

    /**
     * @param corePoolSize  核心线程数
     * @param maxPoolSize   最大线程数
     * @param queueCapacity 等待队列容量
     */
    public TaskRunner(int corePoolSize, int maxPoolSize, int queueCapacity) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;
        throw new UnsupportedOperationException("TODO 第 3 周：创建 ThreadPoolExecutor");
    }

    /**
     * 提交一批任务，等全部结束（或超时）后返回成功/失败数量。
     * <p>第 3 周实现后，用 100 个任务（其中 3 个抛异常）验证 failure 是否为 3。</p>
     */
    public Result runAll(List<Runnable> tasks) {
        throw new UnsupportedOperationException("TODO 第 3 周：submit + Future 聚合");
    }

    /**
     * 优雅关闭：不再接收新任务，等待已提交任务完成，超时则强制中断。
     */
    public void shutdown() {
        throw new UnsupportedOperationException("TODO 第 3 周：shutdown + awaitTermination");
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * 汇总结果。
     */
    public record Result(int success, int failure) {
    }
}
