# Log Analyser

A Java 17 command-line tool that parses Apache/Nginx **Combined Log Format** HTTP request logs and reports:

- Number of unique IP addresses
- Top 3 most visited URLs
- Top 3 most active IP addresses

Ties at position 3 are flagged explicitly in the output.

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

**If you have already built the JAR:**

```bash
java -jar target/log-analyser-1.0.0.jar logs/sample.log
```

**Build and run in one step (no JAR needed):**

```bash
mvn exec:java -Dexec.mainClass=com.loganalyser.Main -Dexec.args="logs/sample.log"
```

---

## Run in IntelliJ

1. Open **Run → Edit Configurations → + → Application**
2. Set **Main class** to `com.loganalyser.Main`
3. Set **Program arguments** to `logs/sample.log`
4. Set **Working directory** to `$PROJECT_DIR$`
5. Ensure the **JDK** is set to Java 17 (**File → Project Structure → Project** if not)
6. Click **OK**, then press **Shift+F10** to run or **Shift+F9** to debug

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

Warnings (malformed lines, invalid IP addresses) are written to **stderr**, keeping stdout clean for piping or
scripting.

---

## Assumptions

### Log format

Only the **Combined Log Format** is supported:

```
IP IDENT AUTH [TIMESTAMP] "METHOD URL PROTOCOL" STATUS SIZE ["REFERER" "USER_AGENT"]
```

Lines that do not match this structure are skipped with a warning on stderr.

### URL handling

- **Query strings are stripped** before comparison: `/search?q=foo` and `/search?q=bar` both count as `/search`.
- No other normalisation is applied — URLs are compared exactly as they appear.
- Absolute URLs (`http://example.net/path/`) and relative URLs (`/path/`) are treated as **distinct**.

### Status codes

All HTTP status codes (200, 301, 404, 500, …) count equally — a request is a request regardless of outcome.

### Tie handling

When more than 3 entries share a count boundary, the output shows exactly 3 and notes the tie. The tie flag is set when
the 4th entry has the same count as the 3rd.

### IP addresses

- IPs are counted **as-is**, with no validation.
- A warning is emitted to stderr (once per unique address) if an IP does not look like a valid IPv4 address: four
  dot-separated octets, each in the range 0–255, with **no leading zeros**. For example, `50.112.00.11` has leading
  zeros in the third octet and triggers a warning. The address is still counted.

### Input

- The log file path is provided as a command-line argument.
- A clear error message is printed to stderr if the argument is missing or the file does not exist.
