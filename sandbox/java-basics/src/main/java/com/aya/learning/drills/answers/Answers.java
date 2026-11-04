package com.aya.learning.drills.answers;

import com.aya.learning.drills.DrillDay2;
import com.aya.learning.moneybook.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 四个 drill 的参考答案，集中放一个类方便对照。
 * <p>先自己写 {@code DrillDay1/2/4/6}，再回来看这里。做对了就把测试里的
 * {@code Answers.xxx} 换成对应的 {@code DrillDayX.xxx}，跑绿即完成当天练习。</p>
 */
public final class Answers {

    private Answers() {
    }

    // ---------- Day 1 ----------

    public static boolean sameWithOperator(String left, String right) {
        return left == right;
    }

    public static boolean sameWithEquals(String left, String right) {
        return Objects.equals(left, right);
    }

    /**
     * 关键点：{@code (a + b) / 2.0}。写成 {@code (a + b) / 2} 会先做整数除法（7/2=3），
     * 再升级成 double 也还是 3.0。
     */
    public static double averageOfTwo(int a, int b) {
        return (a + b) / 2.0;
    }

    public static boolean boxedSame(int a, int b) {
        return Integer.valueOf(a) == Integer.valueOf(b);
    }

    // ---------- Day 2 ----------

    public static DrillDay2.Person mutateName(DrillDay2.Person person, String newName) {
        person.name = newName;
        return person;
    }

    public static String reassignLocally(DrillDay2.Person person) {
        person = new DrillDay2.Person("赵六");
        return person.name;
    }

    // ---------- Day 4 ----------

    public static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidAmountException("金额不能为空");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            // 异常链：把原始异常挂上去，排错时能看到真实的失败原因
            throw new InvalidAmountException("金额格式不正确：" + raw, e);
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("金额必须大于 0：" + raw);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    // ---------- Day 6 ----------

    public static double doubleSumProblem() {
        return 0.1 + 0.1 + 0.1;
    }

    public static BigDecimal bigDecimalSumFixed() {
        BigDecimal cent = new BigDecimal("0.1");
        return cent.add(cent).add(cent);
    }

    public static String formatDate(LocalDate date) {
        return DateTimeFormatter.ofPattern("yyyy年MM月dd日").format(date);
    }
}
