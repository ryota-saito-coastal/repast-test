package test250930;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Lightweight logger that mirrors messages to the console, structured log files and
 * optional CSV / JSON streams depending on the configured output modes.
 */
public final class SimLogger {

    private static final String CONFIG_FILE = "launch.props";
    private static final String OUTPUT_PROPERTY = "sim.logger.outputs";
    private static final DateTimeFormatter RUN_ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final SimLogger INSTANCE = new SimLogger();

    private final EnumSet<OutputMode> modes;
    private final Object lock = new Object();
    private final PrintWriter logWriter;
    private final PrintWriter csvWriter;
    private final PrintWriter jsonWriter;

    private SimLogger() {
        Properties properties = loadProperties();
        this.modes = parseModes(properties.getProperty(OUTPUT_PROPERTY));
        String runId = LocalDateTime.now().format(RUN_ID_FORMAT);
        Path logsDir = Paths.get("logs");
        ensureDirectory(logsDir);

        this.logWriter = modes.contains(OutputMode.FILE) ? createWriter(logsDir.resolve("run_" + runId + ".log")) : null;
        this.csvWriter = modes.contains(OutputMode.CSV) ? createCsvWriter(logsDir.resolve("run_" + runId + ".csv")) : null;
        this.jsonWriter = modes.contains(OutputMode.JSON) ? createWriter(logsDir.resolve("run_" + runId + ".json")) : null;

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeWriters, "sim-logger-shutdown"));

        info("Simulation logging initialised with modes: " + modes);
    }

    public static void info(String message) {
        INSTANCE.writeInfo(message);
    }

    public static void event(int tick, String agentId, String event, String state) {
        INSTANCE.writeEvent(tick, agentId, event, state);
    }

    private void writeInfo(String message) {
        synchronized (lock) {
            if (modes.contains(OutputMode.CONSOLE)) {
                System.out.println(message);
            }
            if (logWriter != null) {
                logWriter.println(message);
                logWriter.flush();
            }
        }
    }

    private void writeEvent(int tick, String agentId, String event, String state) {
        String infoLine = String.format(Locale.ROOT, "[Tick %d] [%s] %s (%s)", tick, agentId, event, state);
        synchronized (lock) {
            writeInfo(infoLine);
            if (csvWriter != null) {
                csvWriter.printf(Locale.ROOT, "%d,%s,%s,%s%n", tick, escapeCsv(agentId), escapeCsv(event), escapeCsv(state));
                csvWriter.flush();
            }
            if (jsonWriter != null) {
                jsonWriter.println('{'
                        + "\"tick\":" + tick + ','
                        + "\"agentId\":" + quoteJson(agentId) + ','
                        + "\"event\":" + quoteJson(event) + ','
                        + "\"state\":" + quoteJson(state)
                        + "}");
                jsonWriter.flush();
            }
        }
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        Path config = Paths.get(CONFIG_FILE);
        if (!Files.exists(config)) {
            return properties;
        }
        try (var in = Files.newInputStream(config)) {
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load logger configuration", e);
        }
        return properties;
    }

    private EnumSet<OutputMode> parseModes(String configured) {
        EnumSet<OutputMode> selected = EnumSet.noneOf(OutputMode.class);
        if (configured == null || configured.isBlank()) {
            selected.add(OutputMode.CONSOLE);
            selected.add(OutputMode.FILE);
            return selected;
        }
        String[] tokens = configured.split(",");
        for (String token : tokens) {
            OutputMode mode = OutputMode.fromName(token.trim());
            if (mode != null) {
                selected.add(mode);
            }
        }
        if (selected.isEmpty()) {
            selected.add(OutputMode.CONSOLE);
            selected.add(OutputMode.FILE);
        }
        return selected;
    }

    private void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create log directory", e);
        }
    }

    private PrintWriter createWriter(Path path) {
        try {
            return new PrintWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8)), true);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to open log file: " + path, e);
        }
    }

    private PrintWriter createCsvWriter(Path path) {
        PrintWriter writer = createWriter(path);
        writer.println("tick,agent_id,event,state");
        return writer;
    }

    private void closeWriters() {
        synchronized (lock) {
            if (jsonWriter != null) {
                jsonWriter.close();
            }
            if (csvWriter != null) {
                csvWriter.close();
            }
            if (logWriter != null) {
                logWriter.close();
            }
        }
    }

    private static String escapeCsv(String value) {
        String safe = Objects.toString(value, "");
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            safe = '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

    private static String quoteJson(String value) {
        String safe = Objects.toString(value, "");
        StringBuilder builder = new StringBuilder();
        builder.append('"');
        for (char ch : safe.toCharArray()) {
            switch (ch) {
                case '\\':
                case '"':
                    builder.append('\\').append(ch);
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(ch);
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private enum OutputMode {
        CONSOLE,
        FILE,
        CSV,
        JSON;

        static OutputMode fromName(String name) {
            return Arrays.stream(values())
                    .filter(mode -> mode.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }
    }
}
