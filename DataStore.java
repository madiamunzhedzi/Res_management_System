package com.res;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple, dependency-free flat-file persistence for the residence system.
 *
 * The whole application state is written to a single UTF-8 text file split
 * into [STUDENTS], [CAMPUS], [ISSUES] and [REPORTS] sections. Each record is
 * one physical line of tab-separated fields. Backslash, tab, newline and
 * carriage-return inside a field are escaped so that free-text values (such as
 * a multi-line report description) never break the format.
 *
 * Writes go to a temporary file first and are then atomically moved into place,
 * so a crash mid-save cannot corrupt existing data.
 */
public class DataStore {

    /** Container for the full application state. */
    public static class Data {
        public Map<String, String[]> students = new LinkedHashMap<>();   // id -> [name, pin, block, room]
        public Map<String, List<String>> campusMap = new LinkedHashMap<>(); // block -> rooms
        public List<String> issues = new ArrayList<>();
        public List<String[]> reports = new ArrayList<>();               // [sNum, block, room, issue, desc, status]
    }

    private final Path file;

    public DataStore(Path file) {
        this.file = file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public void save(Data d) throws IOException {
        StringBuilder sb = new StringBuilder();

        sb.append("[STUDENTS]\n");
        for (Map.Entry<String, String[]> e : d.students.entrySet()) {
            String[] v = e.getValue();
            sb.append(esc(e.getKey())).append('\t')
              .append(esc(get(v, 0))).append('\t')
              .append(esc(get(v, 1))).append('\t')
              .append(esc(v.length > 2 ? v[2] : "N/A")).append('\t')
              .append(esc(v.length > 3 ? v[3] : "N/A")).append('\n');
        }

        sb.append("[CAMPUS]\n");
        for (Map.Entry<String, List<String>> e : d.campusMap.entrySet()) {
            sb.append(esc(e.getKey()));
            for (String room : e.getValue()) {
                sb.append('\t').append(esc(room));
            }
            sb.append('\n');
        }

        sb.append("[ISSUES]\n");
        for (String issue : d.issues) {
            sb.append(esc(issue)).append('\n');
        }

        sb.append("[REPORTS]\n");
        for (String[] r : d.reports) {
            for (int i = 0; i < r.length; i++) {
                if (i > 0) sb.append('\t');
                sb.append(esc(r[i]));
            }
            sb.append('\n');
        }

        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.write(tmp, sb.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public Data load() throws IOException {
        Data d = new Data();
        if (!Files.exists(file)) {
            return d;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String section = "";
        for (String line : lines) {
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line;
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = splitTab(line);
            switch (section) {
                case "[STUDENTS]":
                    if (parts.length >= 3) {
                        String block = parts.length > 3 ? parts[3] : "N/A";
                        String room = parts.length > 4 ? parts[4] : "N/A";
                        d.students.put(parts[0], new String[]{parts[1], parts[2], block, room});
                    }
                    break;
                case "[CAMPUS]":
                    List<String> rooms = new ArrayList<>();
                    for (int i = 1; i < parts.length; i++) {
                        rooms.add(parts[i]);
                    }
                    d.campusMap.put(parts[0], rooms);
                    break;
                case "[ISSUES]":
                    d.issues.add(parts[0]);
                    break;
                case "[REPORTS]":
                    String[] r = new String[6];
                    Arrays.fill(r, "");
                    for (int i = 0; i < 6 && i < parts.length; i++) {
                        r[i] = parts[i];
                    }
                    if (r[5].isEmpty()) {
                        r[5] = "Pending";
                    }
                    d.reports.add(r);
                    break;
                default:
                    // ignore unknown sections
                    break;
            }
        }
        return d;
    }

    private static String get(String[] a, int i) {
        return (a != null && i < a.length && a[i] != null) ? a[i] : "";
    }

    private static String esc(String s) {
        if (s == null) s = "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '\t': b.append("\\t"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                default:   b.append(c);
            }
        }
        return b.toString();
    }

    private static String unesc(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case '\\': b.append('\\'); break;
                    case 't':  b.append('\t'); break;
                    case 'n':  b.append('\n'); break;
                    case 'r':  b.append('\r'); break;
                    default:   b.append(n);
                }
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String[] splitTab(String s) {
        String[] raw = s.split("\t", -1);
        String[] out = new String[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = unesc(raw[i]);
        }
        return out;
    }
}
