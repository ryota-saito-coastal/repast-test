package test250930.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import repast.simphony.engine.environment.RunEnvironment;

/**
 * Centralised logging utility for the Repast batch simulations.
 */
public final class SimLogger {

    public enum OutputMode {
        CONSOLE,
        FILE,
        CSV,
        JSON
    }

    private static final DateTimeFormatter RUN_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final Locale DEFAULT_LOCALE = Locale.ROOT;
    private static final String LOG_DIRECTORY_OVERRIDE_PROPERTY = "test250930.logging.directory";
    private static final EnumSet<OutputMode> OUTPUTS;
    private static final Path LOG_DIRECTORY;
    private static final String RUN_ID;
    private static final Object LOCK = new Object();

    private static Writer fileWriter;
    private static Writer csvWriter;
    private static Writer jsonWriter;
    private static boolean jsonFirstRecord = true;
    private static Path configDirectory = Paths.get("").toAbsolutePath();

    static {
        LocalDateTime now = LocalDateTime.now();
        RUN_ID = "run_" + RUN_STAMP.format(now);
        Properties properties = loadProperties();
        OUTPUTS = parseOutputs(properties.getProperty("logging.outputs", "console,file"));
        LOG_DIRECTORY = resolveLogDirectory(properties.getProperty("logging.directory", "logs"));
        initialiseWriters();
        Runtime.getRuntime().addShutdownHook(new Thread(SimLogger::shutdown, "sim-logger-shutdown"));
    }

    private SimLogger() {
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        for (Path path : candidateConfigPaths()) {
            if (!Files.exists(path)) {
                continue;
            }
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
                Path parent = path.getParent();
                if (parent != null) {
                    configDirectory = parent.toAbsolutePath();
                }
                return properties;
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to load launch.props from " + path, ex);
            }
        }
        return properties;
    }

    private static List<Path> candidateConfigPaths() {
        Path cwd = Paths.get("").toAbsolutePath();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve("launch.props"));
        candidates.add(cwd.resolve("test250930.rs").resolve("launch.props"));
        Path parent = cwd.getParent();
        if (parent != null) {
            candidates.add(parent.resolve("test250930.rs").resolve("launch.props"));
        }
        String scenarioDir = System.getProperty("repast.simphony.scenario.directory");
        if (scenarioDir != null && !scenarioDir.isBlank()) {
            candidates.add(Paths.get(scenarioDir, "launch.props"));
        }
        return candidates;
    }

    private static EnumSet<OutputMode> parseOutputs(String configured) {
        EnumSet<OutputMode> outputs = EnumSet.noneOf(OutputMode.class);
        if (configured == null || configured.isBlank()) {
            outputs.add(OutputMode.CONSOLE);
            outputs.add(OutputMode.FILE);
            return outputs;
        }
        for (String token : configured.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                outputs.add(OutputMode.valueOf(trimmed.toUpperCase(DEFAULT_LOCALE)));
            } catch (IllegalArgumentException ignored) {
                // Unknown token; ignore to keep simulation running.
            }
        }
        if (outputs.isEmpty()) {
            outputs.add(OutputMode.CONSOLE);
        }
        return outputs;
    }

    private static Path resolveLogDirectory(String configuredDir) {
        Path directory = selectBaseDirectory(configuredDir);
        try {
            Files.createDirectories(directory);
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to create log directory " + directory, ex);
        }
        return directory;
    }

    private static Path selectBaseDirectory(String configuredDir) {
        String override = System.getProperty(LOG_DIRECTORY_OVERRIDE_PROPERTY);
        if (override != null && !override.isBlank()) {
            Path overridePath = Paths.get(override.trim());
            if (!overridePath.isAbsolute()) {
                overridePath = configDirectory.resolve(overridePath).normalize();
            }
            return overridePath;
        }
        Path directory = Paths.get(Objects.requireNonNullElse(configuredDir, "logs"));
        if (!directory.isAbsolute()) {
            directory = configDirectory.resolve(directory).normalize();
        }
        return directory;
    }

    private static void initialiseWriters() {
        if (OUTPUTS.contains(OutputMode.FILE)) {
            Path logFile = LOG_DIRECTORY.resolve(RUN_ID + ".log");
            fileWriter = openWriter(logFile);
        }
        if (OUTPUTS.contains(OutputMode.CSV)) {
            Path csvFile = LOG_DIRECTORY.resolve(RUN_ID + ".csv");
            csvWriter = openWriter(csvFile);
            writeCsvHeader();
        }
        if (OUTPUTS.contains(OutputMode.JSON)) {
            Path jsonFile = LOG_DIRECTORY.resolve(RUN_ID + ".json");
            jsonWriter = openWriter(jsonFile);
            writeJsonPreamble();
        }
    }

    private static Writer openWriter(Path path) {
        try {
            return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to open writer for " + path, ex);
        }
    }

    private static void writeCsvHeader() {
        if (csvWriter == null) {
            return;
        }
        try {
            csvWriter.write("tick,agent,event,state\n");
            csvWriter.flush();
        } catch (IOException ex) {
            handleWriterFailure("CSV", ex);
            closeQuietly(csvWriter);
            csvWriter = null;
        }
    }

    private static void writeJsonPreamble() {
        if (jsonWriter == null) {
            return;
        }
        try {
            jsonWriter.write("[\n");
            jsonWriter.flush();
        } catch (IOException ex) {
            handleWriterFailure("JSON", ex);
            closeQuietly(jsonWriter);
            jsonWriter = null;
        }
    }

    private static void handleWriterFailure(String target, IOException ex) {
        System.err.printf(DEFAULT_LOCALE, "[SimLogger] Failed writing to %s output: %s%n", target, ex.getMessage());
    }

    public static void info(String message) {
        logLine(message);
    }

    public static void info(String format, Object... args) {
        logLine(String.format(DEFAULT_LOCALE, format, args));
    }

    public static void event(String agentId, String event, String stateDescription) {
        long tick = currentTick();
        event(tick, agentId, event, stateDescription);
    }

    public static void event(long tick, String agentId, String event, String stateDescription) {
        String tickLabel = tick < 0 ? "?" : Long.toString(tick);
        StringBuilder builder = new StringBuilder();
        builder.append("[Tick ").append(tickLabel).append("] ");
        if (agentId != null && !agentId.isBlank()) {
            builder.append(agentId).append(" - ");
        }
        builder.append(event == null ? "event" : event);
        if (stateDescription != null && !stateDescription.isBlank()) {
            builder.append(" (").append(stateDescription).append(")");
        }
        logLine(builder.toString());
        writeStructuredRecord(tick, agentId, event, stateDescription);
    }

    private static void logLine(String message) {
        synchronized (LOCK) {
            if (OUTPUTS.contains(OutputMode.CONSOLE)) {
                System.out.println(message);
            }
            if (fileWriter != null) {
                try {
                    fileWriter.write(message);
                    fileWriter.write(System.lineSeparator());
                    fileWriter.flush();
                } catch (IOException ex) {
                    handleWriterFailure("log file", ex);
                    closeQuietly(fileWriter);
                    fileWriter = null;
                }
            }
        }
    }

    private static void writeStructuredRecord(long tick, String agentId, String event, String stateDescription) {
        synchronized (LOCK) {
            if (csvWriter != null) {
                try {
                    csvWriter.write(formatCsv(tick, agentId, event, stateDescription));
                    csvWriter.flush();
                } catch (IOException ex) {
                    handleWriterFailure("CSV", ex);
                    closeQuietly(csvWriter);
                    csvWriter = null;
                }
            }
            if (jsonWriter != null) {
                try {
                    if (!jsonFirstRecord) {
                        jsonWriter.write(",\n");
                    }
                    jsonWriter.write(formatJson(tick, agentId, event, stateDescription));
                    jsonWriter.flush();
                    jsonFirstRecord = false;
                } catch (IOException ex) {
                    handleWriterFailure("JSON", ex);
                    closeQuietly(jsonWriter);
                    jsonWriter = null;
                }
            }
        }
    }

    private static String formatCsv(long tick, String agentId, String event, String stateDescription) {
        StringBuilder builder = new StringBuilder();
        if (tick >= 0) {
            builder.append(tick);
        }
        builder.append(',');
        builder.append(escapeCsv(agentId));
        builder.append(',');
        builder.append(escapeCsv(event));
        builder.append(',');
        builder.append(escapeCsv(stateDescription));
        builder.append('\n');
        return builder.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n");
        String escaped = value.replace("\"", "\"\"");
        if (needsQuoting) {
            return '"' + escaped + '"';
        }
        return escaped;
    }

    private static String formatJson(long tick, String agentId, String event, String stateDescription) {
        StringBuilder builder = new StringBuilder();
        builder.append("  {");
        builder.append("\"tick\": ");
        if (tick < 0) {
            builder.append("null");
        } else {
            builder.append(tick);
        }
        builder.append(", \"agent\": ").append(toJsonString(agentId));
        builder.append(", \"event\": ").append(toJsonString(event));
        builder.append(", \"state\": ").append(toJsonString(stateDescription));
        builder.append("}");
        return builder.toString();
    }

    private static String toJsonString(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return '"' + escaped + '"';
    }

    public static long currentTick() {
        try {
            double tick = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
            return (long) Math.floor(tick);
        } catch (Exception ex) {
            return -1L;
        }
    }

    private static void shutdown() {
        synchronized (LOCK) {
            if (jsonWriter != null) {
                try {
                    if (!jsonFirstRecord) {
                        jsonWriter.write("\n");
                    }
                    jsonWriter.write("]");
                    jsonWriter.flush();
                } catch (IOException ex) {
                    handleWriterFailure("JSON", ex);
                }
            }
            closeQuietly(jsonWriter);
            closeQuietly(csvWriter);
            closeQuietly(fileWriter);
            jsonWriter = null;
            csvWriter = null;
            fileWriter = null;
        }
    }

    private static void closeQuietly(Writer writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
            // Ignore close failures during shutdown.
        }
    }
}
