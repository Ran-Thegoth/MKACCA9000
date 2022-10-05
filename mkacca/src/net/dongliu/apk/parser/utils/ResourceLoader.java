package net.dongliu.apk.parser.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * methods for load resources.
 *
 * @author dongliu
 */
public class ResourceLoader {


    /**
     * load system attr ids for parse binary xml.
     */
    public static Map<Integer, String> loadSystemAttrIds() {
    	Map<Integer, String> map = new HashMap<>();
    	return map;
    }

    public static Map<Integer, String> loadSystemStyles() {
        Map<Integer, String> map = new HashMap<>();
        return map;
    }


    private static BufferedReader toReader(String path) {
        return new BufferedReader(new InputStreamReader(
                ResourceLoader.class.getResourceAsStream(path)));
    }
}
