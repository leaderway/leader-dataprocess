package org.leader.data.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.util.*;

/**
 * Map工具类
 *
 * @author ldh
 * @since 2017-02-18 10:44
 */
public class MapUtils {

    /**
     * /将Map按照词频降序排序
     * @param map
     * @return
     */
    public static List<String> sortIntegerMap(Map map) {
        List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : entryList) {
            String entryStr = entry.getKey() + "  " + entry.getValue();
            list.add(entryStr);
        }
        return list;
    }

    /**
     * /将Map按照某一比率降序排序
     * @param map
     * @return
     */
    public static List<String> sortDoubleMap(Map map) {
        List<Map.Entry<String, Double>> entryList = new ArrayList<Map.Entry<String, Double>>(map.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        List<String> list = new ArrayList<String>();
        for (Map.Entry<String, Double> entry : entryList) {
            String entryStr = entry.getKey() + "  " + entry.getValue();
            list.add(entryStr);
        }
        return list;
    }

    /**
     * 将键值对文件实例化成Map
     * @param path
     * @param regex
     * @return
     * @throws Exception
     */
    public static Map<String, String> getMapFromFile(String path, String regex) throws Exception {
        File file = FileUtils.getFile(path);
        Map<String, String> map = new HashMap<String, String>();
        if (file.exists()) {
            LineIterator lineIterator = FileUtils.lineIterator(file);
            while (lineIterator.hasNext()) {
                String line = lineIterator.next();
                String[] lineArray = line.split(regex);
                map.put(lineArray[0], lineArray[1]);
            }
        }
        return map;
    }
}
