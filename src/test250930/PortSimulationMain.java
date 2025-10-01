package test250930;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Convenience launcher so the scenario can be started from Eclipse using
 * "Run As → Java Application". The main method simply forwards to the
 * Repast runtime while pointing at the bundled scenario folder.
 *
 * <p>
 * The actual Repast dependency is optional. When the runtime is not on the
 * classpath the launcher prints a clear message instead of failing with a
 * compilation error caused by the missing library.
 * </p>
 */
public class PortSimulationMain {

    private static final String DEFAULT_SCENARIO_PATH = "test250930.rs";
    private static final String REPAST_MAIN_CLASS = "repast.simphony.runtime.RepastMain";

    public static void main(String[] args) {
        try {
            Method repastMain = locateRepastMain();
            String[] launchArgs = args.length > 0 ? args : buildDefaultArgs();
            repastMain.invoke(null, (Object) launchArgs);
        } catch (ClassNotFoundException e) {
            System.err.println("Repast runtime not found on the classpath. "
                    + "Add the Repast Simphony libraries or provide explicit launch arguments.");
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to access RepastMain.main(String[]).", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Repast runtime reported an error during start-up.", cause);
        }
    }

    private static Method locateRepastMain() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> repastMainClass = Class.forName(REPAST_MAIN_CLASS);
        return repastMainClass.getMethod("main", String[].class);
    }

    private static String[] buildDefaultArgs() {
        File scenarioPath = new File(DEFAULT_SCENARIO_PATH).getAbsoluteFile();
        return new String[] { "-scenario", scenarioPath.getPath() };
    }
}
