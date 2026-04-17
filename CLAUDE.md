# CLAUDE.md

This file provides guidance to Claude Code when working in this repository.

## Project purpose

A Java 17 command-line tool that parses Apache/Nginx **Combined Log Format** HTTP request logs and reports:

- Number of unique IP addresses
- Top 3 most visited URLs (query strings stripped before comparison)
- Top 3 most active IP addresses

Ties at position 3 are flagged explicitly. Invalid-looking IP addresses and unparseable log lines produce warnings on
stderr. The tool is designed to handle large files (streaming, not loading into memory) and partially corrupt logs (bad
lines are skipped, not fatal).

This is a take-home assignment submission. The priority is readable, well-reasoned code with full test coverage — not a
production-hardened system.

## Commands

```bash
# Compile
mvn compile

# Run all tests
mvn test

# Package executable JAR
mvn package

# Run the analyser (from project root)
java -jar target/log-analyser-1.0.0.jar logs/sample.log
```

## Run in IntelliJ

1. **Run → Edit Configurations → + → Application**
2. **Main class:** `com.loganalyser.Main`
3. **Program arguments:** `logs/sample.log`
4. **Working directory:** `$PROJECT_DIR$`
5. **JDK:** Java 17 — verify via **File → Project Structure → Project** if the run fails with a class version error
6. **Shift+F10** to run, **Shift+F9** to debug

Tests must pass before committing. Always run `mvn test` after any change to source or test files.

## Architecture

The pipeline has four stages, each in its own package:

```
LogFileReader ──► LogLineParser ──► LogAnalyser ──► ResultPrinter
(reads file)     (parses lines)    (aggregates)     (formats output)
```

Package layout:

```
com.loganalyser
├── Main                                    # CLI entry point
│                                           # run(args, out, err) returns int — fully testable
├── model
│   ├── LogEntry                            # record: ipAddress, authUser, url
│   └── AnalysisResult                      # record: uniqueIpCount, topUrls (RankedList), mostActiveIpAddresses (RankedList)
├── parser
│   ├── LogLineParser                       # interface
│   ├── CombinedFormatLogLineParser         # impl: regex parser for Combined Log Format
│   ├── LogFileReader                       # interface
│   └── StreamingLogFileReader              # impl: streams file line-by-line via Files.lines()
├── analyser
│   ├── LogAnalyser                         # interface
│   └── DefaultLogAnalyser                  # impl: counts, ranks, detects ties, validates IPs
└── output
    └── ResultPrinter                       # formats AnalysisResult to a PrintStream
```

## Coding rules

**Prefer streams over for loops**
Use Java streams instead of imperative for loops wherever the intent is clearer. When a stream operation would be less
readable than a loop (e.g. accumulating into multiple collections simultaneously), prefer extracting the loop body into
a well-named method instead.

**Extract loop bodies into named methods**
If a loop body does more than one thing, or if naming what it does would add clarity, extract it. The method name
becomes the documentation.

**Extract stream operations into named methods**
Every non-trivial stream pipeline must be extracted into a private method whose name describes what it produces, not
how it produces it. A method named `countBy`, `sortDescending`, or `topRanked` is self-documenting; an inline chain
of `groupingBy`, `sorted`, and `limit` calls is not. The rule applies to lambdas too: if a lambda body is more than
a single expression, extract it into a named method and pass a method reference instead.

**No inline comments in tests**
Test method names must be descriptive enough to need no explanation. Do not add `//` comments inside or between test
methods. Javadoc on shared helper methods is the only exception.

**Every public method must have test coverage**
Every non-record, non-entry-point class must have a dedicated test class named `<ClassName>Test`. Every public method
must have at least one test. The only explicit exception is `Main.main()` — it is a one-line `System.exit` wrapper with
no logic and is not tested. Records (`LogEntry`, `AnalysisResult`, `RankedEntry`) have no behaviour and are not tested
directly.

**All tests use Mockito**
Every test class must be annotated with `@ExtendWith(MockitoExtension.class)`. Collaborators are declared with `@Mock`
and injected via the constructor in `@BeforeEach`. Use `verify()` to assert on interactions and `verifyNoInteractions()`
to assert that a collaborator was never called.

**Mock, Arrange, Assert**
Each test method follows three phases in order:

1. **Mock** — configure mock behaviour with `when().thenReturn()`
2. **Arrange** — set up inputs and the object under test
3. **Assert** — call the method under test, then assert on return values and mock interactions

When a collaborator is only used for output (e.g. `PrintStream err`) and the test needs to inspect the content as a
string, a real `ByteArrayOutputStream`-backed `PrintStream` may be used in place of a mock.

**try-catch must be inside its own method**
Never put a try-catch block inline inside a method that also contains other logic. Extract it so the containing method
reads as a clean sequence of steps, and the error handling is isolated and named.

---

## Code style and conventions

**Naming**

- Interfaces are named after the concept: `LogLineParser`, `LogFileReader`, `LogAnalyser` — no `I` prefix (that is a
  C#/.NET convention, not idiomatic Java).
- Implementations are named to describe *how* they work: `CombinedFormatLogLineParser`, `StreamingLogFileReader`,
  `DefaultLogAnalyser`. This follows the JDK convention (`List`/`ArrayList`, `ExecutorService`/`ThreadPoolExecutor`).

**Dependency injection — Pure DI (no framework)**
This project does not use Spring or any DI framework. Dependencies are wired manually using the **Pure DI** pattern:

- Implementation constructors are `public` and declare all dependencies as parameters. This is intentional —
  constructors are the injection mechanism.
- Only the **composition root** (`LogAnalyserCli`) imports and instantiates concrete implementation classes. Every other
  class depends solely on the interface types.
- If a DI framework were introduced later, `LogAnalyserCli` is the only file that would need to change.

```java
// LogAnalyserCli is the only place that knows which implementations are used:
LogFileReader reader = new StreamingLogFileReader(new CombinedFormatLogLineParser(), err);
LogAnalyser analyser = new DefaultLogAnalyser(err);
```

**Testability**

- Classes that write to stdout or stderr accept a `PrintStream` constructor argument instead of using `System.out`/
  `System.err` directly. This allows tests to capture output without touching global state.
- `LogAnalyserCli.run(String[] args)` returns an `int` exit code; `Main` calls `System.exit` on the result. Tests invoke
  `LogAnalyserCli` directly without forking a process or calling `System.exit`.

**Data model**

- `LogEntry` and `AnalysisResult` are Java records — immutable value types with no behaviour. Keep them that way; don't
  add methods.

**Error handling**

- Malformed log lines are skipped with a warning, never thrown. A single bad line must not abort analysis.
- IP addresses are counted as-is regardless of validity. Warnings are informational only.

## Key design decisions

- **Query string stripping**: `indexOf('?')` in `DefaultLogAnalyser` — no URL parsing library. Stripping happens at
  analysis time, not parse time, so the raw URL is preserved in `LogEntry`.
- **Tie detection**: compares the count at index `TOP_N - 1` with index `TOP_N` in the sorted list. Uses the `TOP_N`
  constant throughout — no magic numbers.
- **Deterministic sort**: `DefaultLogAnalyser.sortDescending` applies a secondary sort by key (ascending) so entries
  with equal counts always appear in the same order. This makes test assertions on list equality reliable.
- **Warning deduplication**: `warnedIps.add(ip)` in `DefaultLogAnalyser` returns `true` only on first insertion — one
  warning per unique invalid IP regardless of how many requests it made.
- **Streaming I/O**: `StreamingLogFileReader` uses `Files.lines()` and keeps the stream open only for the duration of
  `readEntries`. Suitable for very large log files.

## Testing

Tests live in `src/test/java` mirroring the main source structure. Each implementation class has a dedicated test class
named `<ClassName>Test`.

- `CombinedFormatLogLineParserTest` — unit tests on plain strings; no filesystem dependency
- `StreamingLogFileReaderTest` — uses `@TempDir` for real files; mocks `LogLineParser` via Mockito to isolate I/O from
  parsing
- `DefaultLogAnalyserTest` — drives the analyser with hand-crafted `LogEntry` lists; captures stderr via
  `ByteArrayOutputStream`
- `MainTest` — exercises `Main.run()` end-to-end including the integration test against `logs/sample.log`

## Test data — expected results for `logs/sample.log`

| Metric                                | Value                                                                    |
|---------------------------------------|--------------------------------------------------------------------------|
| Unique IPs                            | 11                                                                       |
| Top IP                                | `168.41.191.40` (4 requests)                                             |
| IP tie                                | Yes — `177.71.128.21`, `50.112.00.11`, `72.44.32.10` all have 3 requests |
| Top URL                               | `/docs/manage-websites/` (2 requests)                                    |
| URL tie                               | Yes — all other URLs have 1 request                                      |
| Invalid IPs (leading zeros in octets) | `50.112.00.28`, `50.112.00.11`, `79.125.00.21`                           |
