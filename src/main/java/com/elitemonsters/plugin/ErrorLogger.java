package com.elitemonsters.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ErrorLogger {
    private static final int MAX_RECENT = 50;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final EliteMonstersPlugin plugin;
    private final File logFile;
    private final List<ErrorEntry> recentErrors = new ArrayList<>();
    private int totalErrors = 0;

    public ErrorLogger(EliteMonstersPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "errors.log");
    }

    public void log(String context, String message) {
        log(context, message, null);
    }

    public void log(String context, Throwable ex) {
        log(context, ex.getMessage(), ex);
    }

    public void log(String context, String message, Throwable ex) {
        String time = LocalDateTime.now().format(FMT);
        String stackTrace = ex != null ? getStackTrace(ex) : "";
        ErrorEntry entry = new ErrorEntry(time, context, message, stackTrace);
        recentErrors.add(entry);
        if (recentErrors.size() > MAX_RECENT) recentErrors.remove(0);
        totalErrors++;
        writeToFile(entry);
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().log(Level.WARNING, "["+context+"] "+message, ex);
        }
    }

    private void writeToFile(ErrorEntry entry) {
        try {
            if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(logFile, true))) {
                pw.println("["+entry.time+"] ["+entry.context+"] "+entry.message);
                if (entry.stackTrace != null && !entry.stackTrace.isEmpty()) {
                    pw.println(entry.stackTrace);
                    pw.println();
                }
            }
        } catch (Exception ignored) {
            plugin.getLogger().warning("Failed to write error log: "+ignored.getMessage());
        }
    }

    private String getStackTrace(Throwable ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public List<ErrorEntry> getRecentErrors() { return Collections.unmodifiableList(recentErrors); }
    public int getTotalErrors() { return totalErrors; }
    public int getRecentCount() { return recentErrors.size(); }

    public void clearRecent() { recentErrors.clear(); }

    public record ErrorEntry(String time, String context, String message, String stackTrace) {
        public boolean hasStackTrace() { return stackTrace != null && !stackTrace.isEmpty(); }
    }
}