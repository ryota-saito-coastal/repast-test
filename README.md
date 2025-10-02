# Repast Batch Logging Enhancements

This scenario adds a lightweight logging subsystem so that Repast Simphony batch runs
surface their progress without the full GUI.

## Features

* Console logging for major simulation events per tick.
* Mirrored `.log` files stored under `logs/` with timestamps.
* Optional CSV (`tick,agent_id,event,state`) and JSON (newline separated objects)
  streams for downstream analytics.
* Output modes controlled via a simple properties file.

## Configuration

Logging behaviour is configured through [`launch.props`](./launch.props). Set the
`sim.logger.outputs` property to a comma-separated list of the desired targets:

```
sim.logger.outputs=console,file,csv,json
```

Supported values are:

| Value   | Description                                                  |
|---------|--------------------------------------------------------------|
| `console` | Print messages directly to standard output.                  |
| `file`    | Write the same messages to `logs/run_<timestamp>.log`.       |
| `csv`     | Append structured events to `logs/run_<timestamp>.csv`.      |
| `json`    | Append structured events as newline-delimited JSON objects to `logs/run_<timestamp>.json`. |

If the property is omitted or empty the logger defaults to `console,file`.

## Output Files

Each run creates a new timestamped file for every enabled output mode under the
`logs/` directory. The CSV and JSON formats capture the tick number, agent (or
component) identifier, the event keyword, and a short state summary string. These
files are designed to be lightweight inputs for Python / C++ post-processing.

## Running the Scenario

The batch entry point remains unchanged. From the project root:

```bash
java -cp "bin;${CLASSPATH_WIN}" repast.simphony.batch.BatchMain "$(pwd -W)/test250930.rs"
```

While the simulation runs you should now see tick-by-tick messages in the
terminal, confirming that the model is active.
