package com.aya.learning.moneybook;

/**
 * 金额非法（为空、非数字、小于等于 0）。
 * <p>单独一个子类，是为了让调用方能精确捕获：{@code catch (InvalidAmountException e)}
 * 只处理金额问题，不会把其他业务异常一起吞掉。</p>
 */
public class InvalidAmountException extends MoneyBookException {

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
