package com.vertispan.serial.discovery;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Discovery2 {
    @Option(name = "-excludeAnnotation", usage = "indicate that any type with this annotation should be ignored")
    List<String> excludeAnnotations = Arrays.asList("GwtIncompatible");//TODO don't do defaults like this

    @Option(name = "-url", usage = "add a jar or class dir to scan using the given rules", required = true)
    List<String> urls;

    @Option(name = "-package", usage = "adds a package to scan within. defaults to all")
    List<String> packages;

    @Option(name = "-out", required = true)
    String outfile;

    public List<URL> getUrls() {
        return urls.stream()
                .map(e -> {
                    try {
                        return new URL("file", "", e);
                    } catch (MalformedURLException e1) {
                        throw new UncheckedIOException(e1);
                    }
                })
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws FileNotFoundException {
        Discovery2 discovery = new Discovery2();
        org.kohsuke.args4j.CmdLineParser parser = new CmdLineParser(discovery);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            parser.printUsage(System.err);
            System.exit(1);
        }


        FastClasspathScanner scanner = new FastClasspathScanner("!");
//        scanner.
        scanner.overrideClassLoaders(new URLClassLoader(discovery.getUrls().toArray(new URL[0]), null));

        ScanResult result = scanner.scan();

        try (PrintWriter writer = new PrintWriter(new FileOutputStream(discovery.outfile))) {
            result.getNamesOfAllClasses().forEach(writer::println);
        }
    }
}
