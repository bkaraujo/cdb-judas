package br.commons;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NullMarked
public abstract class RT {
    private RT(){}

    public static volatile boolean running = false;
    public static final List<String> packages = new CopyOnWriteArrayList<>(
            List.of(
                    "java.",
                    "javax.",
                    "jdk.",
                    "sun."
            )
    );
}

