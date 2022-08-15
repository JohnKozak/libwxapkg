package net.kozakcuu.wxapkg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class WechatAppletCommandLineTool {
    private File file;
    private File destination;
    private boolean verbose = false;

    private WechatAppletCommandLineTool() {

    }

    private void prepareDestination() {
        if (!((destination.exists() && destination.isDirectory()) || destination.mkdirs())) {
            throw new IllegalStateException(String.format("Could not extract files to destination: '%s'", destination.getAbsolutePath()));
        }
        if (verbose) System.err.printf("[Info ] Output to '%s'\n", destination.getAbsolutePath());
    }

    private void onError(Exception e) {
        if (verbose) System.err.println(" Error.");
        System.err.printf("[Error] %s, cause: %s", e.getClass().getSimpleName(), e.getMessage());
        if (verbose) System.err.printf(
                "Stack trace: \n%s\n",
                Arrays.stream(e.getStackTrace())
                        .map(stackTraceElement -> String.format(
                                "%s:%s() [@%s line %s]",
                                stackTraceElement.getClassName(),
                                stackTraceElement.getMethodName(),
                                stackTraceElement.getFileName(),
                                stackTraceElement.getLineNumber()
                        ))
                        .collect(Collectors.joining("\n"))
        );
        System.exit(1);
    }

    public void start() {
        try {
            //
            // Check destination directory
            //
            prepareDestination();

            //
            // Read package info
            //
            WechatAppletPackage pkg = WechatAppletPackage.from(file);
            if (verbose) System.err.printf(
                    "[Info ] File: '%s'. Header Code: 0x%08x, index size: %d bytes, body meta size: %d bytes.\n" +
                            "[Info ] Total %d entities.\n",
                    pkg.getFile().getAbsolutePath(),
                    pkg.getHeaderCode(),
                    pkg.getIndexInfoSize(),
                    pkg.getBodyInfoSize(),
                    pkg.getEntityCount()
            );

            byte[] buffer = new byte[1024 * 1024];
            while (pkg.hasNext()) {
                WechatAppletPackage.Entity entity = pkg.next();
                File outputFile = new File(destination, entity.getName());
                File parent = outputFile.getParentFile();
                if (!(parent.exists() || parent.mkdirs())) {
                    throw new IllegalStateException(String.format("Could not create directory '%s'", parent.getAbsolutePath()));
                }

                if (verbose) System.err.printf(
                        "[Info ] Entity '%s'@'%d'('%d' bytes) -> '%s'",
                        entity.getName(),
                        entity.getOffset(),
                        entity.getSize(),
                        outputFile.getAbsolutePath()
                );

                try (InputStream inputStream = entity.getEntityStream();
                     OutputStream outputStream = new FileOutputStream(outputFile)) {
                    int readCount;
                    do {
                        readCount = inputStream.read(buffer);
                        if (readCount <= 0) break;
                        outputStream.write(buffer, 0, readCount);
                    } while (true);
                }

                if (verbose) System.err.println(" Finished.");
            }

            System.err.printf("[Info ] Extract to '%s' finished.\n", destination.getAbsolutePath());
        } catch (Exception notIgnored) {
            onError(notIgnored);
        }
    }

    //
    // Factory
    //

    public static WechatAppletCommandLineTool fromArgs(String[] args) {
        final WechatAppletCommandLineTool tool = new WechatAppletCommandLineTool();
        try {
            if(switchFrom(args, "-h", "--help")) {
                printHelpAndExit();
                throw new IllegalStateException("Should not reach here.");
            }
            String filename = null;
            String destination = null;

            if(switchFrom(args, "-v", "--verbose")) tool.verbose = true;
            CliParam param = valueFrom(args, 0);
            if (param != null) filename = param.value;
            param = valueFrom(args, 1);
            if (param != null) destination = param.value;
            param = optionFrom(args, "-f", "--file");
            if(param != null) filename = param.value;
            param = optionFrom(args, "-d", "--destination");
            if(param != null) destination = param.value;
            if(filename == null) throw new NullPointerException();
            tool.file = new File(filename);
            if(destination == null) throw new NullPointerException();
            tool.destination = new File(destination);

            return tool;
        } catch (Exception ignored) {
            printHelpAndExit();
            throw new IllegalStateException("Should not reach here.");
        }
    }

    private static class CliParam {
        final int index;
        final String value;
        CliParam(int index, String value) {
            this.index = index;
            this.value = value;
        }
    }

    private static void printHelpAndExit() {
        System.err.println();
        System.err.println("Wechat Applet Package Extract Tool.");
        System.err.println("By: Kozak Lam, @2022-08-12.");
        System.err.println();
        System.err.println("Usage:");
        System.err.println("    unwxapkg [-f|--file] <wxapkg file> [-d|--destination] [destination folder] [-v|--verbose] [-h|--help]");
        System.err.println("    java -jar libwxapkg.jar <same as above>");
        System.err.println();
        System.err.println("Options:");
        System.err.println("    -v|--verbose Show more information");
        System.err.println("    -h|--help    Show this information");
        System.err.println();
        System.err.println("The destination directory will be created automatically if it does not exists.");
        System.err.println();
        System.err.println("To use this tool as a library, import the WechatAppletPackage class from the jar file.");
        System.err.println();
        System.exit(0);
    }

    private static CliParam optionFrom(String[] args, final String... name) {
        final List<String> names = Arrays.asList(name);
        for(int i = 0; i < args.length; i++) {
            if(!names.contains(args[i])) continue;
            i++;
            if(i < args.length) return new CliParam(i, args[i]);
            break;
        }
        return null;
    }

    private static Boolean switchFrom(String[] args, final String... name) {
        final List<String> names = Arrays.asList(name);
        for(String value : args) if(names.contains(value)) return true;
        return false;
    }

    private static CliParam valueFrom(String[] args, int index) {
        for(int i = 0; i < args.length; i++) {
            String value = args[i];
            if (!value.startsWith("-")) {
                if(index > 0) {
                    index--;
                    continue;
                }
                return new CliParam(i, value);
            }
        }
        return null;
    }
}
