package com.loganalyser;

/**
 * Entry point. Delegates entirely to {@link LogAnalyserCli}.
 */
public class Main {

    public static void main(String[] args) {
        LogAnalyserCli logAnalyserCli = new LogAnalyserCli(System.out, System.err);
        int run = logAnalyserCli.run(args);
        System.exit(run);
    }
}
