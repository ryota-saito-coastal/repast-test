package test250930;

import java.io.File;

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

        File scenarioPath = new File(DEFAULT_SCENARIO_PATH).getAbsoluteFile();
        String scenario = scenarioPath.getPath();
        RepastMain.main(new String[] { "-scenario", scenario });
    }
}
