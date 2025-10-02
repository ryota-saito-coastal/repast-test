# Repast Batch Logging Extensions

This repository extends the `test250930` Repast Simphony scenario with a lightweight logging
layer that works when the model is executed through `BatchMain`. The goal is to make the
progress of the simulation observable from the console while also emitting structured data
for downstream tooling.

## Running the simulation

```bash
java -cp "bin;${CLASSPATH_WIN}" repast.simphony.batch.BatchMain "$(pwd -W)/test250930.rs"
```

During execution the logger writes directly to `System.out`, so messages become visible even
under batch mode.

## Configuring log outputs

Logging is configured through properties added to `test250930.rs/launch.props`:

```properties
logging.outputs=console,file,csv,json
logging.directory=logs
```

The `logging.outputs` property accepts a comma-separated list of destinations:

| Mode    | Behaviour                                                                 |
|---------|----------------------------------------------------------------------------|
| console | Prints messages to standard output.                                       |
| file    | Writes the same content to `logs/run_YYYYMMDD_HHMM.log`.                  |
| csv     | Appends structured rows to `logs/run_YYYYMMDD_HHMM.csv`.                  |
| json    | Produces an array of JSON objects in `logs/run_YYYYMMDD_HHMM.json`.        |

When the property is omitted the logger defaults to `console,file` so that at least one
human-readable stream is produced. The `logging.directory` property can be used to relocate
the output folder (relative paths are resolved against the scenario directory).

## Sample console output

```
[PortBuilder] Initialising offshore port context
[Tick 1] ArrivalScheduler - processing arrivals (pending=2)
[Tick 2] Vessel 101 - arrived (cargo=4)
[Tick 2] Crane 1 - started unloading (NACELLE-001 from vessel 101)
[Tick 3] Yard 1 - stored (type=NACELLE, id=NACELLE-001, source=crane 1, count=1)
[Tick 6] Yard 1 - inventory report (BLADE=2; NACELLE=1; TOWER=2)
```

The same entries are mirrored into the `.log` file. CSV output follows the schema
`tick,agent,event,state`, while the JSON file contains objects of the form:

```json
{
  "tick": 6,
  "agent": "Yard 1",
  "event": "inventory report",
  "state": "BLADE=2; NACELLE=1; TOWER=2"
}
```

## Structured event data

Each agent reports its key state transitions through `SimLogger.event(...)`, which records
both the human-readable message and a structured payload. The following agents have been
instrumented:

- `PortArrivalScheduler` – queue processing and vessel registration.
- `VesselAgent` – arrivals, per-material unload completion, and departure readiness.
- `QuayCraneAgent` – task queueing, start, and completion events.
- `DockAgent` – towing schedule acknowledgements and quay deliveries.
- `YardAgent` – material intake and periodic inventory reports.

These events can be consumed from the CSV or JSON files by external analytics pipelines or
visualisation tools.
