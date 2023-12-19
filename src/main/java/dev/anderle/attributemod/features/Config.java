package dev.anderle.attributemod.features;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class Config {
    private final Configuration config;

    public Config(File configFile) {
        this.config = new Configuration(configFile);
        this.config.addCustomCategoryComment(
            "Main Settings",
            "If you don't exactly know what you're doing, please don't edit this manually.");
        this.config.load();
    }

    public Configuration get() {
        return this.config;
    }

    public void set(String key, String value, String defaultValue) {
        Property p = config.get("Main Settings", key, defaultValue);
        p.set(value);
        this.config.save();
        this.config.load();
    }
}
