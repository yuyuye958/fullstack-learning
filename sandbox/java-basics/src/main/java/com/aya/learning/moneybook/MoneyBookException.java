package com.aya.learning.moneybook;

/**
 * 记账本业务异常的基类。
 * <p>继承 {@link RuntimeException}（非受检异常）：业务规则违规通常调用方也没法当场恢复，
 * 强制 try-catch 只会让代码更脏。需要调用方处理的 IO 问题才用受检异常（例如导出 CSV 的 {@code IOException}）。</p>
 */
public class MoneyBookException extends RuntimeException {

    public MoneyBookException(String message) {
        super(message);
    }

    public MoneyBookException(String message, Throwable cause) {
        super(message, cause);
    }
}
