package sharose.mods.idresolver;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import net.minecraft.src.*;

public class IDResolver {

    private static final String MOD_ID = "IDResolver";
    private static final Pattern BLOCK_PATTERN = Pattern.compile("block\\s*\\{\\s*([\\w.]+)\\s*=\\s*(-?\\d+)");
    private static final Pattern ITEM_PATTERN = Pattern.compile("item\\s*\\{\\s*([\\w.]+)\\s*=\\s*(-?\\d+)");
    private static final int MAX_ID = 4095;

    private static Map<String, Integer> usedBlockIds = new HashMap<String, Integer>();
    private static Map<String, Integer> usedItemIds = new HashMap<String, Integer>();
    private static Map<String, Integer> conflicts = new HashMap<String, Integer>();
    private static File prioritiesFile;

    public static void scan() {
        File configDir = new File("config");
        if (!configDir.exists()) {
            System.out.println("IDResolver: config/ not found, skipping.");
            return;
        }

        prioritiesFile = new File(configDir, "IDResolvermodPriorities.properties");
        usedBlockIds.clear();
        usedItemIds.clear();
        conflicts.clear();

        File[] cfgs = configDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".cfg");
            }
        });

        if (cfgs == null) return;

        for (File cfg : cfgs) {
            scanCfg(cfg, true);
            scanCfg(cfg, false);
        }

        if (conflicts.isEmpty()) {
            System.out.println("IDResolver: No conflicts found! Hooray.");
            return;
        }

        System.out.println("IDResolver: " + conflicts.size() + " conflict(s) found.");
        resolveConflicts();
        writeReport();
    }

    private static void scanCfg(File cfg, boolean isBlock) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(cfg));
            String line;
            Pattern p = isBlock ? BLOCK_PATTERN : ITEM_PATTERN;
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line.trim());
                if (m.find()) {
                    String name = m.group(1);
                    int id = Integer.parseInt(m.group(2));
                    Map<String, Integer> map = isBlock ? usedBlockIds : usedItemIds;
                    Integer existing = map.get(name);
                    if (existing != null && existing != id) {
                        conflicts.put(name, id);
                        System.out.println("IDResolver: Conflict in " + cfg.getName()
                                + " -> " + name + " already " + existing + ", now " + id);
                    } else if (existing == null) {
                        map.put(name, id);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (br != null) br.close(); } catch (Exception e) {}
        }
    }

    private static void resolveConflicts() {
        File bakDir = new File("config_bak");
        if (bakDir.exists()) bakDir.delete();
        bakDir.mkdirs();
        File[] cfgs = new File("config").listFiles();
        if (cfgs != null) {
            for (File f : cfgs) {
                try {
                    copy(f, new File(bakDir, f.getName()));
                } catch (Exception e) {}
            }
        }

        List<Integer> freeBlocks = new ArrayList<Integer>();
        for (int i = MAX_ID; i >= 0; i--) {
            if (!usedBlockIds.containsValue(i)) freeBlocks.add(i);
        }

        int idx = 0;
        for (Map.Entry<String, Integer> e : conflicts.entrySet()) {
            String name = e.getKey();
            int newId = freeBlocks.get(idx++);
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(prioritiesFile, true));
                bw.write("# " + name + " conflict resolved by IDResolver");
                bw.newLine();
                bw.write(name + "=old:" + e.getValue() + ",new:" + newId);
                bw.newLine();
                bw.close();
                System.out.println("IDResolver: " + name + " -> " + newId);
            } catch (Exception ex) {}
        }
    }

    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
    }

    private static void writeReport() {
        try {
            File report = new File("config/IDResolverReport.html");
            BufferedWriter bw = new BufferedWriter(new FileWriter(report));
            bw.write("<html><body><h1>IDResolver Report</h1><table border=1>");
            bw.write("<tr><th>Mod</th><th>Old ID</th><th>New ID</th></tr>");
            BufferedReader br = new BufferedReader(new FileReader(prioritiesFile));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String[] vals = parts[1].split(",");
                    bw.write("<tr><td>" + parts[0] + "</td><td>" + vals[0].replace("old:", "") + "</td><td>" + vals[1].replace("new:", "") + "</td></tr>");
                }
            }
            br.close();
            bw.write("</table></body></html>");
            bw.close();
            System.out.println("IDResolver: Report written to config/IDResolverReport.html");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
