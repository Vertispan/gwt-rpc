package org.gwtproject.rpc.discovery;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Deprecated
public class Discovery {
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
        Discovery discovery = new Discovery();
        org.kohsuke.args4j.CmdLineParser parser = new CmdLineParser(discovery);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            parser.printUsage(System.err);
            System.exit(1);
        }


        ConfigurationBuilder builder = new ConfigurationBuilder()
                .addUrls(discovery.getUrls());
        builder.getScanners().clear();
        builder.addScanners(
                new SubTypesScanner(false),
                new TypeAnnotationsScanner()
        );

        Reflections reflections = new Reflections(builder);

        Set<String> allTypes = reflections.getAllTypes();

        Set<String> excludedByAnnotation = discovery.excludeAnnotations.stream()
                .map(annotationClassname -> reflections.getStore().get(TypeAnnotationsScanner.class.getSimpleName(), annotationClassname))
                .flatMap(iter -> StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter.iterator(), 0), false))
                .collect(Collectors.toSet());
        
        allTypes.removeAll(excludedByAnnotation);


        //TODO consider more exclusions, such as non-static classes (...unless they have custom field serialiers?)
        allTypes.forEach(new PrintWriter(new FileOutputStream(discovery.outfile))::println);
    }
}
