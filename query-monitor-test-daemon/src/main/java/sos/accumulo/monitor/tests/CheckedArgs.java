package sos.accumulo.monitor.tests;

import java.util.Map;

public class CheckedArgs {

    private final Map<String, String> args;
    
    public CheckedArgs(Map<String, String> args) {
        this.args = args;
    }

    public String getRequired(String arg) {
        String value = args.get(arg);
        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Required arg is not provided: " + arg);
        }
        return value;
    }

    public boolean getBoolean(String arg) {
        String value = args.get(arg);
        if (value == null || value.isEmpty()) {
            return false;
        }

        return value.equalsIgnoreCase("true");
    }

    public int getInt(String arg) {
        String value = args.get(arg);
        if (value == null || value.isEmpty()) {
            return 0;
        }

        return Integer.parseInt(value);
    }
}