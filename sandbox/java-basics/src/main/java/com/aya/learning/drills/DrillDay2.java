package com.aya.learning.drills;

/**
 * Day 2 练习：对象、值传递、重新赋值。
 * <p>JS 里对象参数“传引用”，在函数里改属性外部能看到；Java 也一样——但 Java 传的是
 * <b>引用的副本</b>，在方法里把参数指向新对象，外部完全不受影响。这两件事必须分清。</p>
 */
public final class DrillDay2 {

    private DrillDay2() {
    }

    /**
     * 练习用的可变 POJO，模拟前端里随手可改的普通对象。
     */
    public static class Person {
        public String name;

        public Person(String name) {
            this.name = name;
        }
    }

    /**
     * 在方法内部把 {@code person.name} 改成 {@code newName}，并把 person 返回。
     * <p>验收：调用后外部持有的同一个对象，name 也变了。</p>
     */
    public static Person mutateName(Person person, String newName) {
        throw new UnsupportedOperationException("TODO Day2-1");
    }

    /**
     * 在方法内部把 person 重新赋值成一个新对象（名字用 "赵六"），返回方法内部看到的名字。
     * <p>验收：返回 "赵六"，但调用方原来的 person.name 不变。</p>
     */
    public static String reassignLocally(Person person) {
        throw new UnsupportedOperationException("TODO Day2-2");
    }
}
