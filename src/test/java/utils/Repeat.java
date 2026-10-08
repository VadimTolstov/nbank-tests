package utils;

public class Repeat {
    private Repeat() {
    }

    public static void repeat(int times, Runnable runnable) {
        for (int i = 0; i < times; i++) {
            runnable.run();
        }
    }
}
