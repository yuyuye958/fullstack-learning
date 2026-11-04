package com.aya.learning.drills;

import com.aya.learning.drills.answers.Answers;
import com.aya.learning.moneybook.Category;
import com.aya.learning.moneybook.InvalidAmountException;
import com.aya.learning.moneybook.solution.CategoryAnswer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每天 drill 的验收测试。全部针对参考答案，所以现在就是绿的。
 * <p>你做完当天练习后，把对应的 {@code Answers.xxx} 换成 {@code DrillDayX.xxx}，
 * 再跑一遍；还是绿的就说明你的实现和参考答案行为一致。</p>
 */
class DrillsTest {

    @Test
    @DisplayName("Day1：== 比引用，equals 比内容")
    void day1_equality() {
        assertTrue(Answers.sameWithOperator("a", "a"));
        assertFalse(Answers.sameWithOperator(new String("a"), "a"));
        assertTrue(Answers.sameWithEquals(new String("a"), "a"));
        assertFalse(Answers.sameWithEquals(null, "a"));
    }

    @Test
    @DisplayName("Day1：int 除法会截断，(5+2)/2 要得到 3.5 而不是 3.0")
    void day1_intDivision() {
        assertEquals(3.5, Answers.averageOfTwo(5, 2));
    }

    @Test
    @DisplayName("Day1：Integer 缓存只覆盖 -128~127")
    void day1_integerCache() {
        assertTrue(Answers.boxedSame(127, 127));
        assertFalse(Answers.boxedSame(1000, 1000));
    }

    @Test
    @DisplayName("Day2：改字段会同步，重新赋值不会影响调用方")
    void day2_passByValue() {
        DrillDay2.Person person = new DrillDay2.Person("张三");
        Answers.mutateName(person, "李四");
        assertEquals("李四", person.name);

        DrillDay2.Person other = new DrillDay2.Person("王五");
        assertEquals("赵六", Answers.reassignLocally(other));
        assertEquals("王五", other.name, "重新赋值只改了方法内的局部变量");
    }

    @Test
    @DisplayName("Day3：枚举按中文名反查")
    void day3_categoryFromDisplayName() {
        assertEquals(Category.FOOD, CategoryAnswer.fromDisplayName("餐饮"));
        assertEquals(Category.SALARY, CategoryAnswer.fromDisplayName(" 工资 "));
        assertEquals(Category.OTHER, CategoryAnswer.fromDisplayName("不存在的分类"));
    }

    @Test
    @DisplayName("Day4：非法金额抛 InvalidAmountException，合法金额保留两位小数")
    void day4_parseAmount() {
        assertEquals(0, new BigDecimal("12.50").compareTo(Answers.parseAmount("12.5")));
        assertThrows(InvalidAmountException.class, () -> Answers.parseAmount("abc"));
        assertThrows(InvalidAmountException.class, () -> Answers.parseAmount("-1"));
        assertThrows(InvalidAmountException.class, () -> Answers.parseAmount(null));
        assertThrows(InvalidAmountException.class, () -> Answers.parseAmount("0"));
    }

    @Test
    @DisplayName("Day6：double 算不准，BigDecimal 可以")
    void day6_moneyPrecision() {
        assertNotEquals(0.3, Answers.doubleSumProblem());
        assertEquals(0, new BigDecimal("0.3").compareTo(Answers.bigDecimalSumFixed()));
    }

    @Test
    @DisplayName("Day6：日期格式化")
    void day6_dateFormat() {
        assertEquals("2026年11月04日", Answers.formatDate(LocalDate.of(2026, 11, 4)));
    }
}
