package test250930;

import java.nio.file.Path;
import java.nio.file.Paths;

import repast.simphony.runtime.RepastMain;

/**
 * Convenience launcher so the scenario can be started from Eclipse using
 * "Run As → Java Application". The main method simply forwards to
 * {@link RepastMain} while pointing at the bundled scenario folder.
 */
public class PortSimulationMain {

    private static final String DEFAULT_SCENARIO_PATH = "test250930.rs";

    public static void main(String[] args) {
        if (args.length > 0) {
            RepastMain.main(args);
            return;
        }

        Path scenarioPath = Paths.get(DEFAULT_SCENARIO_PATH).toAbsolutePath();
        String scenario = scenarioPath.toString();
        RepastMain.main(new String[] { "-scenario", scenario });
    }
}