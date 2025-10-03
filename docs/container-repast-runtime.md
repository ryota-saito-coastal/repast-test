# Running Repast Simulations in the Container

This repository ships only the model sources and scenario files. The Repast Simphony
runtime itself is not committed because of its size. When working inside the Codex
container you can still compile and exercise the model end-to-end by following the
steps below.

## 1. Download the Repast runtime

Use the helper script to download the official Repast Simphony 2.11.0 update site
archive, extract every plugin bundle, and generate a flattened classpath file.

```bash
./scripts/setup_repast_runtime.sh
```

The script expands the update site under `lib/repast-2.11.0/`. Because each OSGi
bundle stores its bytecode in a `bin/` directory, the script also extracts those
folders and nested dependency JARs before writing `lib/repast-2.11.0/classpath.txt`.
This file lists one classpath entry per line (it is quite long). You can create a
colon-separated value that is safe to consume in a shell variable via:

```bash
CLASSPATH=$(./scripts/repast_classpath.sh)
```

> **Tip:** the joined classpath easily exceeds the default terminal line length.
> Redirect it into a file or pipe it through `tr ':' '\n' | head` if you need to
> inspect the contents.

## 2. Compile the model code

Once the runtime is in place the sources can be compiled against it. The commands
below recompile the `src/test250930` package into the existing `bin`
output directory:

```bash
find src/test250930 -name "*.java" -print0 \
  | xargs -0 javac -cp "$CLASSPATH" -d bin
```

The compiler will emit a warning about annotation processors being discovered on
the classpath; it is safe to ignore for this workflow.

### Alternative workflow for Windows development environments

When running from Windows (for example via Git Bash or the Windows Subsystem for
Linux) you can follow the same overall structure with a few platform-specific
adjustments:

1. Generate the Repast classpath as usual and convert it into the Windows
   separator format (`;` instead of `:`):

   ```bash
   CLASSPATH=$(./scripts/repast_classpath.sh)
   CLASSPATH_WIN=$(echo "$CLASSPATH" | tr ':' ';')
   ```

2. Compile the sources by passing the Windows-formatted classpath and writing the
   results into a separate `bin/test250930` folder if you want to keep the
   Windows build artifacts apart from the container ones:

   ```bash
   find src/test250930 -name "*.java" > sources.txt
   javac -cp "$CLASSPATH_WIN" -d bin/test250930 @sources.txt
   ```

3. Launch the batch runner. `pwd -W` expands to a Windows absolute path in Git
   Bash, which keeps Repast happy when it tries to resolve relative entries from
   `user_path.xml`:

   ```bash
   java \
     --add-opens java.base/java.lang=ALL-UNNAMED \
     --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
     -cp "bin;$CLASSPATH_WIN" \
     repast.simphony.runtime.RepastBatchMain \
     -params "$(pwd -W)\\test250930.rs\\batch_params.xml" \
     "$(pwd -W)\\test250930.rs"
   ```

The additional `--add-opens` flags mirror the container invocation so that the
runtime can perform the reflective access it expects. Repast will then execute
the same scenario as in the container without needing the Swing UI.

## 3. Execute the scenario in headless (batch) mode

`RepastMain` launches the Swing-based UI and therefore throws a `HeadlessException`
in the container. Use the batch runner instead. Passing the absolute path to the
scenario directory is important because the bundled `user_path.xml` uses relative
entries such as `../bin` and `../lib`.

Repast Simphony 2.11 ships with a parameter sweeper that drives batch runs. The
included `test250930.rs/batch_params.xml` file requests a single iteration and is
referenced via the `-params` flag.

```bash
java --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp "bin:$CLASSPATH" \
  repast.simphony.runtime.RepastBatchMain \
  -params "$(pwd)/test250930.rs/batch_params.xml" \
  "$(pwd)/test250930.rs"
```

This will load `test250930.rs/scenario.xml`, initialise the context defined in
`PortBuilder`, and advance the schedule until completion without attempting to
render the GUI.

## 4. Automating repeated runs

If you need to run multiple experiments, wrap steps 2 and 3 inside a shell script
so that you can recompile and execute in one command. The batch runner accepts the
usual Repast sweep options (`-params`, `-opt`, etc.) should you need parameter
experiments.

---

By following this sequence you can run the model start-to-finish, even though the
container does not ship with the Repast Simphony runtime preinstalled.
