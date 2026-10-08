package com.ben.csp.aecs;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.YamlPrinter;

import java.io.File;
import java.nio.file.Paths;

/**
 * A standalone utility to parse a Java source file and serialize its
 * Abstract Syntax Tree (AST) to a YAML string. This is the Java-side
 * component of the AECS-II "Hybrid Parser Model".
 */
public class AstParser {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Error: No file path provided.");
            System.exit(1);
        }

        File sourceFile = Paths.get(args[0]).toFile();
        if (!sourceFile.exists()) {
            System.err.println("Error: File not found at " + args[0]);
            System.exit(1);
        }

        CompilationUnit cu = StaticJavaParser.parse(sourceFile);

        YamlPrinter printer = new YamlPrinter(true);
        System.out.println(printer.output(cu));
    }
}