package com.ben.csp.aecs;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.serialization.JavaParserJsonSerializer;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Paths;

/**
 * A standalone utility to parse a Java source file and serialize its
 * Abstract Syntax Tree (AST) to a JSON string. This is the Java-side
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

        JavaParserJsonSerializer serializer = new JavaParserJsonSerializer();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator generator = Json.createGenerator(stringWriter);
        serializer.serialize(cu, generator);
        System.out.println(stringWriter.toString());
    }
}