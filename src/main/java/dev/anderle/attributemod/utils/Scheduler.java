package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.AttributeMod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is meant for repetitive tasks that must not run on the main thread because they would freeze the game.
 * To interact with the game from within a task, use AttributeMod.mc.addScheduledTask().
 */
public class Scheduler {
    public static final Map<String, Task> TASKS = new HashMap<>();

    public void registerTasks() {
        TASKS.put("ah", new Task("ah", 2 * 60 * 20, () -> AttributeMod.backend.refreshPrices()));
    }

    public void executeTasksIfNeeded() {
        TASKS.values().forEach(Task::incrementTickCounter);

        List<Task> toExecute = TASKS.values().stream().filter(Task::isDue).collect(Collectors.toList());
        if(toExecute.isEmpty()) return;

        new Thread(() -> toExecute.forEach(Task::execute)).start();
    }

    /** Task to execute every x ticks. Handles errors and skips execution if previous iteration is still running. */
    public static class Task {
        private int tickCounter = Integer.MAX_VALUE / 2; // So tasks get executed with first tick.
        private boolean isActive = false;

        private final Runnable function;
        private final String name;
        private final int interval;

        public Task(String name, int interval, Runnable function) {
            this.function = function;
            this.name = name;
            this.interval = interval;
        }

        public void execute() {
            tickCounter = 0;
            isActive = true;
            try {
                function.run();
            } catch(Exception e) {
                AttributeMod.LOGGER.error("Failed to execute task \"" + name + "\".", e);
            } finally {
                isActive = false;
            }
            AttributeMod.LOGGER.info("Successfully executed task \"" + name + "\".");
        }

        public boolean isDue() {
            return tickCounter >= interval && !isActive;
        }

        public void incrementTickCounter() {
            tickCounter++;
        }
    }
}