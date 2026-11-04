package com.aya.learning.taskrunner.demo;

/**
 * TaskRunner 骨架的运行入口：只打印计划，不真正执行任务。
 * <p>第 3 周把 {@code TaskRunner} 实现完，再来把这个 main 改成提交 100 个任务。</p>
 */
public final class TaskRunnerDemo {

    private TaskRunnerDemo() {
    }

    public static void main(String[] args) {
        System.out.println("TaskRunner 骨架（第 3 周实现）");
        System.out.println("构造参数：corePoolSize / maxPoolSize / queueCapacity");
        System.out.println("待实现：ThreadPoolExecutor、拒绝策略、超时、CompletableFuture 聚合、优雅关闭");
        System.out.println("当前构造器会抛 UnsupportedOperationException，这是故意的。");
        System.out.println("第 3 周验收：提交 100 个任务（其中 3 个抛异常），结果 success=97 failure=3。");
    }
}
