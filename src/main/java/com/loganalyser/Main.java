package com.loganalyser;

/**
 * Entry point. Delegates entirely to {@link LogAnalyserCli}.
 */
public class Main {

    public static void main(String[] args) {
        System.exit(new LogAnalyserCli(System.out, System.err).run(args));
    }
}
