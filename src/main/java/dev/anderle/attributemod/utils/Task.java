package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.Main;

import java.util.concurrent.Callable;

public class Task {
    private final Callable<Void> function;
    private final String name;
    private final int interval;
    private boolean active = false;

    public Task(String name, int interval, Callable<Void> function) {
        this.function = function;
        this.interval = interval;
        this.name = name;
    }
    public void execute() {
        if(!this.active) try {
            this.active = true;
            this.function.call();
        } catch (Exception e) {
            Main.LOGGER.error("Failed to execute task '" + this.name + "' due to the following error: " + e.getMessage());
        } finally {
            this.active = false;
        }
    }
    public int getInterval() { return this.interval; }
}
