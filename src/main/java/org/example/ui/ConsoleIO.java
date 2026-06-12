package org.example.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class ConsoleIO {

    private final Scanner scanner;
    private final PrintStream out;

    public ConsoleIO(InputStream in, PrintStream out) {
        this.scanner = new Scanner(in);
        this.out = out;
    }

    public void print(String msg) {
        out.print(msg);
    }

    public void println(String msg) {
        out.println(msg);
    }

    public void println() {
        out.println();
    }

    public String readLine() {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    public int readInt(String prompt) {
        print(prompt);
        String input = readLine();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
