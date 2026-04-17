# Log Analyser

Parses Apache/Nginx **Combined Log Format** HTTP request logs and tells you:

- How many unique IP addresses hit the server
- The top 3 most visited URLs
- The top 3 most active IP addresses

Ties are called out in the output, including which position they start from.

---

## Requirements

- Java 17+
- Maven 3.8+

---

## Build

```bash
mvn package
```

This produces `target/log-analyser-1.0.0.jar`.

---

## Run

**Already built the JAR:**

```bash
java -jar target/log-analyser-1.0.0.jar logs/sample.log
```

**Build and run in one step:**

```bash
mvn exec:java -Dexec.mainClass=com.loganalyser.Main -Dexec.args="logs/sample.log"
```

---

## Run in IntelliJ

1. Open **Run → Edit Configurations → + → Application**
2. Set **Main class** to `com.loganalyser.Main`
3. Set **Program arguments** to `logs/sample.log`
4. Set **Working directory** to `$PROJECT_DIR$`
5. Make sure the **JDK** is set to Java 17 (**File → Project Structure → Project** if not)
6. Hit **OK**, then **Shift+F10** to run or **Shift+F9** to debug

---

## Run tests

```bash
mvn test
```

---

## Example output

```
=== Log Analysis Result ===

Unique IP addresses: 11

Top 3 most visited URLs (NOTE: tie from position 2 - multiple entries share this count):
  1. /docs/manage-websites/ (2 visits)
  2. / (1 visit)
  3. /asset.css (1 visit)

Top 3 most active IP addresses (NOTE: tie from position 2 - multiple entries share this count):
  1. 168.41.191.40 (4 requests)
  2. 177.71.128.21 (3 requests)
  3. 50.112.00.11 (3 requests)
```

Warnings (malformed lines, invalid IPs) go to stderr so stdout stays clean for piping.

---

## Assumptions

### Log format

Only **Combined Log Format** is supported:

```
IP IDENT AUTH [TIMESTAMP] "METHOD URL PROTOCOL" STATUS SIZE ["REFERER" "USER_AGENT"]
```

Lines that don't match are skipped with a warning on stderr.

### URL handling

- **Query strings are stripped** before comparison — `/search?q=foo` and `/search?q=bar` both count as `/search`.
- **Matching is case-insensitive** — `/Docs/Page/` and `/docs/page/` are the same URL. The lowercase form is what
  appears in the output.
- **No path-prefix grouping** — `/docs/manage-websites/` and `/docs/manage-users/` are counted separately. Grouping by
  prefix (both counting towards `/docs/`) would give section-level stats, but the right depth depends on the site
  structure and isn't something we can guess. Exact URL matching is the standard approach anyway.
- Absolute URLs (`http://example.net/path/`) and relative URLs (`/path/`) are treated as different.

### Status codes

All status codes (200, 301, 404, 500, …) count equally — a request is a request.

### Ties

The output always shows exactly 3 entries. If the entry just outside the top 3 has the same count as the last one inside
it, there's a tie — the cut-off is arbitrary and the output says so, along with which position the tie starts from.

### IP addresses

IPs are counted as-is. A warning is emitted once per unique address if it doesn't look like a valid IPv4 address (four
dot-separated octets, 0–255, no leading zeros). For example `50.112.00.11` triggers a warning because of the leading
zero in the third octet, but it's still counted.

### Input

Pass the log file path as a command-line argument. A clear error is printed to stderr if it's missing or the file doesn'
t exist.

---

## What would break with very large files

The tool handles large files fine in practice, but two things would become a problem at hundreds of GB:

- **`StreamingLogFileReader` collects all entries into a list before analysis starts.** `Files.lines()` reads lazily, so
  the I/O is fine, but `.toList()` means every parsed entry sits in memory at once. One approach would be to change the
  interface to return a `Stream<LogEntry>` and pipe it straight into the analyser, so entries are counted on the fly and
  thrown away — though that would require rethinking how the pipeline is wired together.

- **`DefaultLogAnalyser` keeps a counter for every unique IP and URL it sees.** For a normal server that's no problem —
  a million unique IPs is maybe 100 MB. But if you had bots hammering random URLs, that map could grow without bound. A
  priority queue capped at N entries could help here: track only the current top candidates and evict the lowest when
  something better comes in. For truly extreme cases the real answer is an external sort — write the keys to disk, sort
  them, then count runs of equal keys in a single pass.

---

## Use of AI

This solution was built with Claude (Anthropic) as an AI pair programmer, as encouraged in the task brief.

AI helped with scaffolding the project structure, generating boilerplate, and suggesting implementation patterns. We
also worked through several design decisions together — things like whether to use `RankedList` vs separate booleans,
how to report tie positions accurately, and whether case-insensitive URL matching was the right call.

All the actual decisions are mine. I can explain every line and the reasoning behind it — the AI made it faster, not
different.
