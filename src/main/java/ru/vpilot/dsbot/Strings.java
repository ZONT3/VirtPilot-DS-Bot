package ru.vpilot.dsbot;

import ru.zont.dsbot2.UTF8Control;
import ru.zont.dsbot2.tools.ZDSBStrings;

import java.util.ResourceBundle;

public class Strings {
    public static final ResourceBundle STR_LOCAL = ResourceBundle.getBundle("strings", new UTF8Control());

    public static class STR {
        public static String getString(String key) {
            if (STR_LOCAL.containsKey(key))
                return STR_LOCAL.getString(key);
            else {
                String string = ZDSBStrings.STR.getString(key);
                if (!string.equals(key)) return string;
            }
            return "ERROR: String not found: " + key;
        }

        public static String getString(String key, Object... args) {
            return String.format(getString(key), args);
        }
    }
}
