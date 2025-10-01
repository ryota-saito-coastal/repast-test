package repast.simphony.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Lightweight replacement for Repast Simphony's runtime. The real runtime is
 * fairly involved and requires a native installation; for testing purposes we
 * only need enough behaviour to construct the context and call scheduled agent
 * methods on a fixed tick loop.
 */
public final class RepastMain {

    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("<string>\\s*([^<]+?)\\s*</string>");
    private static final int DEFAULT_TICKS = 40;

    private RepastMain() {
    }

    public static void main(String[] args) {
        try {
            new RepastMain().run(args);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to execute lightweight Repast runtime", e);
        }
    }

    private void run(String[] args) throws Exception {
        Path scenarioDir = resolveScenarioDirectory(args);
        String builderClassName = findBuilderClassName(scenarioDir)
                .orElseThrow(() -> new IllegalStateException("Unable to locate context builder definition in scenario " + scenarioDir));

        ContextBuilder<Object> builder = instantiateBuilder(builderClassName);
        Context<Object> context = Objects.requireNonNull(builder.build(null), "Context builder returned null context");

        System.out.printf("[RepastMain] Running scenario '%s' using builder %s%n", scenarioDir.getFileName(), builderClassName);

        List<ScheduledEntry> schedule = new ArrayList<>();
        Map<Object, Boolean> knownObjects = new IdentityHashMap<>();
        RunEnvironment.Schedule scheduleState = RunEnvironment.getInstance().getCurrentSchedule();

        for (int tick = 0; tick < DEFAULT_TICKS; tick++) {
            scheduleState.setTickCount(tick);
            registerNewAgents(context, knownObjects, schedule);
            invokeScheduledMethods(schedule, tick);
        }

        System.out.printf("[RepastMain] Simulation finished after %d ticks.%n", DEFAULT_TICKS);
    }

    private void registerNewAgents(Context<Object> context, Map<Object, Boolean> knownObjects, List<ScheduledEntry> schedule) {
        for (Object agent : context) {
            if (knownObjects.putIfAbsent(agent, Boolean.TRUE) == null) {
                discoverScheduledMethods(agent, schedule);
            }
        }
    }

    private void invokeScheduledMethods(List<ScheduledEntry> schedule, int tick) {
        for (ScheduledEntry entry : schedule) {
            if (entry.shouldRun(tick)) {
                entry.invoke();
            }
        }
    }

    private void discoverScheduledMethods(Object agent, List<ScheduledEntry> schedule) {
        Class<?> type = agent.getClass();
        for (Method method : type.getDeclaredMethods()) {
            ScheduledMethod annotation = method.getAnnotation(ScheduledMethod.class);
            if (annotation == null) {
                continue;
            }
            if (method.getParameterCount() != 0) {
                System.err.printf("[RepastMain] Ignoring scheduled method %s.%s because it declares parameters.%n",
                        type.getSimpleName(), method.getName());
                continue;
            }
            method.setAccessible(true);
            schedule.add(new ScheduledEntry(agent, method, annotation));
        }
    }

    private ContextBuilder<Object> instantiateBuilder(String builderClassName)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> builderClass = Class.forName(builderClassName);
        Object instance = builderClass.getDeclaredConstructor().newInstance();
        if (!(instance instanceof ContextBuilder)) {
            throw new IllegalStateException(builderClassName + " does not implement ContextBuilder");
        }
        @SuppressWarnings("unchecked")
        ContextBuilder<Object> builder = (ContextBuilder<Object>) instance;
        return builder;
    }

    private Optional<String> findBuilderClassName(Path scenarioDir) {
        Path loaderFile = scenarioDir.resolve("repast.simphony.dataLoader.engine.ClassNameDataLoaderAction_0.xml");
        if (Files.isRegularFile(loaderFile)) {
            return Optional.of(parseClassName(loaderFile));
        }
        // Fallback: look for context.xml as a last resort.
        Path contextFile = scenarioDir.resolve("context.xml");
        if (Files.isRegularFile(contextFile)) {
            try {
                return Files.lines(contextFile, StandardCharsets.UTF_8)
                        .map(String::trim)
                        .filter(line -> line.startsWith("<model.initializer"))
                        .map(this::extractClassAttribute)
                        .filter(Objects::nonNull)
                        .findFirst();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read context.xml", e);
            }
        }
        return Optional.empty();
    }

    private String parseClassName(Path loaderFile) {
        try {
            String content = Files.readString(loaderFile, StandardCharsets.UTF_8);
            Matcher matcher = CLASS_NAME_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + loaderFile, e);
        }
        throw new IllegalStateException("Could not parse builder class from " + loaderFile);
    }

    private String extractClassAttribute(String line) {
        int index = line.indexOf("class=");
        if (index < 0) {
            return null;
        }
        int startQuote = line.indexOf('"', index);
        int endQuote = line.indexOf('"', startQuote + 1);
        if (startQuote < 0 || endQuote < 0) {
            return null;
        }
        return line.substring(startQuote + 1, endQuote).trim();
    }

    private Path resolveScenarioDirectory(String[] args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No arguments supplied. Use '-scenario <path>' to select a scenario directory.");
        }
        for (int i = 0; i < args.length; i++) {
            if ("-scenario".equals(args[i]) && i + 1 < args.length) {
                return toDirectory(args[i + 1]);
            }
        }
        if (args.length == 1) {
            return toDirectory(args[0]);
        }
        throw new IllegalArgumentException("Unable to determine scenario directory from arguments");
    }

    private Path toDirectory(String rawPath) {
        Path path = Paths.get(rawPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Scenario path does not exist or is not a directory: " + path);
        }
        return path;
    }

    private static final class ScheduledEntry {

        private static final double EPSILON = 1e-9;

        private final Object target;
        private final Method method;
        private final double startTick;
        private final double interval;

        private ScheduledEntry(Object target, Method method, ScheduledMethod annotation) {
            this.target = target;
            this.method = method;
            this.startTick = annotation.start();
            this.interval = annotation.interval();
        }

        private boolean shouldRun(int tick) {
            double currentTick = tick;
            if (currentTick + EPSILON < startTick) {
                return false;
            }
            if (interval <= EPSILON) {
                return currentTick + EPSILON >= startTick;
            }
            double delta = currentTick - startTick;
            if (delta < -EPSILON) {
                return false;
            }
            double quotient = delta / interval;
            double rounded = Math.rint(quotient);
            return Math.abs(quotient - rounded) < EPSILON;
        }

        private void invoke() {
            try {
                method.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke scheduled method " + method, e);
            }
        }
    }
}
