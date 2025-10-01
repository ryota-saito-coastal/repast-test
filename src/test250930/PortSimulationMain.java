package test250930;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            RepastMain.main(normaliseArgs(args));
            return;
        }

        File scenarioPath = new File(DEFAULT_SCENARIO_PATH).getAbsoluteFile();
        RepastMain.main(new String[] { scenarioPath.getPath() });
    }

    private static String[] normaliseArgs(String[] args) {
        List<String> argList = Arrays.asList(args);
        int scenarioFlagIndex = argList.indexOf("-scenario");

        if (scenarioFlagIndex >= 0 && scenarioFlagIndex + 1 < args.length) {
            String scenarioPath = absolutise(args[scenarioFlagIndex + 1]);
            List<String> reordered = new ArrayList<>();
            reordered.add(scenarioPath);
            for (int i = 0; i < args.length; i++) {
                if (i == scenarioFlagIndex || i == scenarioFlagIndex + 1) {
                    continue;
                }
                reordered.add(args[i]);
            }
            return reordered.toArray(new String[0]);
        }

        if (args.length > 0 && !args[0].startsWith("-")) {
            String[] copy = Arrays.copyOf(args, args.length);
            copy[0] = absolutise(copy[0]);
            return copy;
        }

        return args;
    }

    private static String absolutise(String path) {
        return new File(path).getAbsoluteFile().getPath();
    }
}
