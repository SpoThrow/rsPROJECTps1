package com.rsps.interfacemaker.util;

import java.io.*;
import java.util.*;

public class CacheReader {
    
    private static final int CACHE_INDEX = 0; // Main cache index
    private static final int INTERFACE_CONFIG_INDEX = 0; // Interface config in cache
    
    public static class InterfaceData {
        public int id;
        public int parentId;
        public int type;
        public int width;
        public int height;
        public int x;
        public int y;
        public List<ChildData> children = new ArrayList<>();
        
        @Override
        public String toString() {
            return "Interface " + id + " (Parent: " + parentId + ", Type: " + type + 
                   ", Size: " + width + "x" + height + ", Children: " + children.size() + ")";
        }
    }
    
    public static class ChildData {
        public int childId;
        public int x;
        public int y;
        
        @Override
        public String toString() {
            return "Child " + childId + " at (" + x + ", " + y + ")";
        }
    }
    
    /**
     * Attempt to read interface data from a 317 cache directory
     * @param cachePath Path to the cache directory
     * @param interfaceId Interface ID to load
     * @return InterfaceData or null if not found
     */
    public static InterfaceData loadInterfaceFromCache(String cachePath, int interfaceId) {
        try {
            // Try to read from cache structure
            File cacheDir = new File(cachePath);
            if (!cacheDir.exists()) {
                return null;
            }
            
            // For now, return a simulated interface data structure
            // In a full implementation, this would parse the actual cache files
            InterfaceData data = new InterfaceData();
            data.id = interfaceId;
            data.parentId = interfaceId;
            data.type = 0; // Container type
            data.width = 512;
            data.height = 334;
            data.x = 0;
            data.y = 0;
            
            // Add some sample children for demonstration
            if (interfaceId > 0) {
                data.children.add(new ChildData());
                data.children.get(0).childId = interfaceId + 1;
                data.children.get(0).x = 0;
                data.children.get(0).y = 0;
            }
            
            return data;
            
        } catch (Exception e) {
            System.err.println("Error loading interface from cache: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Scan cache to find used interface IDs
     * @param cachePath Path to the cache directory
     * @return Set of used interface IDs
     */
    public static Set<Integer> scanUsedInterfaceIds(String cachePath) {
        Set<Integer> usedIds = new HashSet<>();
        
        try {
            File cacheDir = new File(cachePath);
            if (!cacheDir.exists()) {
                return usedIds;
            }
            
            // In a full implementation, this would scan the actual cache files
            // For now, return some common interface IDs that are typically used
            usedIds.addAll(Arrays.asList(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                2111, 2112, 2113, 2114, 2115, 2116, 2117, 2118, 2119, 2120,
                3822, 3823, 3824, 3825, 3826, 3827, 3828, 3829, 3830,
                4900, 4901, 4902, 4903, 4904, 4905, 4906, 4907, 4908, 4909, 4910,
                5408, 5409, 5410, 5411, 5412, 5413, 5414, 5415, 5416, 5417, 5418,
                5600, 5601, 5602, 5603, 5604, 5605, 5606, 5607, 5608, 5609, 5610,
                6000, 6001, 6002, 6003, 6004, 6005, 6006, 6007, 6008, 6009, 6010,
                6400, 6401, 6402, 6403, 6404, 6405, 6406, 6407, 6408, 6409, 6410,
                7000, 7001, 7002, 7003, 7004, 7005, 7006, 7007, 7008, 7009, 7010,
                8000, 8001, 8002, 8003, 8004, 8005, 8006, 8007, 8008, 8009, 8010,
                9000, 9001, 9002, 9003, 9004, 9005, 9006, 9007, 9008, 9009, 9010,
                12000, 12001, 12002, 12003, 12004, 12005, 12006, 12007, 12008, 12009, 12010,
                15000, 15001, 15002, 15003, 15004, 15005, 15006, 15007, 15008, 15009, 15010,
                20000, 20001, 20002, 20003, 20004, 20005, 20006, 20007, 20008, 20009, 20010,
                25000, 25001, 25002, 25003, 25004, 25005, 25006, 25007, 25008, 25009, 25010,
                30000, 30001, 30002, 30003, 30004, 30005, 30006, 30007, 30008, 30009, 30010,
                35000, 35001, 35002, 35003, 35004, 35005, 35006, 35007, 35008, 35009, 35010,
                40000, 40001, 40002, 40003, 40004, 40005, 40006, 40007, 40008, 40009, 40010,
                45000, 45001, 45002, 45003, 45004, 45005, 45006, 45007, 45008, 45009, 45010
            ));
            
        } catch (Exception e) {
            System.err.println("Error scanning cache for IDs: " + e.getMessage());
        }
        
        return usedIds;
    }
    
    /**
     * Suggest a free interface ID range
     * @param cachePath Path to the cache directory
     * @param rangeSize Size of the range needed
     * @return Suggested starting ID, or -1 if no range found
     */
    public static int suggestFreeIdRange(String cachePath, int rangeSize) {
        Set<Integer> usedIds = scanUsedInterfaceIds(cachePath);
        
        // Try common starting points
        int[] commonStarts = {50000, 55000, 60000, 65000, 70000, 75000, 80000, 85000, 90000, 95000};
        
        for (int start : commonStarts) {
            boolean rangeFree = true;
            for (int i = 0; i < rangeSize; i++) {
                if (usedIds.contains(start + i)) {
                    rangeFree = false;
                    break;
                }
            }
            if (rangeFree) {
                return start;
            }
        }
        
        // If no common range found, try from 50000 upwards
        for (int start = 50000; start < 100000; start += 100) {
            boolean rangeFree = true;
            for (int i = 0; i < rangeSize; i++) {
                if (usedIds.contains(start + i)) {
                    rangeFree = false;
                    break;
                }
            }
            if (rangeFree) {
                return start;
            }
        }
        
        return -1; // No free range found
    }
}