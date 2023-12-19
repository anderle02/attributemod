package dev.anderle.attributemod.utils;

import dev.anderle.attributemod.Main;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class Scheduler {
    private final Map<String, Task> tasks = new HashMap<String, Task>();

    /**
     * Registers tasks to be executed every x seconds.
     */
    public void registerTasks() {
        this.tasks.put("ah", new Task("ah", 60000, new Callable<Void>() {
            public Void call() throws IOException {
                Main.api.refreshPrices();
                return null;
            }
        }));
    }

    public void startExecutingTasks(String ...names) {
        for(String name : names) {
            if(!tasks.containsKey(name)) {
                Main.LOGGER.error("Can't start task '" + name + "' because it was never registered.");
                continue;
            }
            final Task task = tasks.get(name);
            new Thread(new Runnable() {
                public void run() {
                    while(true) {
                        long time = System.currentTimeMillis();
                        task.execute();
                        while(time > System.currentTimeMillis() - task.getInterval()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Main.LOGGER.log(Level.DEBUG, e.getMessage());
                            }
                        }
                    }
                }
            }).start();
        }
    }
}