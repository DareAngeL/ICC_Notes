package iccnote;

import java.util.ArrayList;
import java.util.HashMap;
                            // modules,  modules content
public class Module extends HashMap<String, Object> {
    public static final String FIRST_MEETING = "1st Meeting";
    public static final String SECOND_MEETING = "2nd Meeting";
    public static final String THIRD_MEETING = "3rd Meeting";
    public static final String FOURTH_MEETING = "4th Meeting";
    public static final String FIFTH_MEETING = "5th Meeting";
    public static final String ALL = "ALL MODULES";

    public Module(final String moduleName, final Object content) {
        put(moduleName, content);
    }

    public Module(){}
}
