package net.kozakcuu.wxapkg;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

public class WechatAppletPackage implements Iterator<WechatAppletPackage.Entity> {
    private long headerCode;
    private long indexInfoSize;
    private long bodyInfoSize;
    private long entityCount;
    private File file;
    private FileInputStream stream;

    private int iterateIndex = 0;

    @Override
    protected void finalize() throws Throwable {
        stream.close();
    }

    public long getHeaderCode() {
        return headerCode;
    }

    public long getIndexInfoSize() {
        return indexInfoSize;
    }

    public long getBodyInfoSize() {
        return bodyInfoSize;
    }

    public long getEntityCount() {
        return entityCount;
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean hasNext() {
        return iterateIndex < entityCount;
    }

    @Override
    public Entity next() {
        if (!hasNext()) throw new IllegalStateException("Iterator reach end.");
        try {
            int bufferSize = Integer.BYTES;
            byte[] buffer = new byte[bufferSize];
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

            int readCount = stream.read(buffer);
            if (readCount != 4) throw new IllegalStateException("Unexpected reach EOF when reading entity header.");

            long nameLength = byteBuffer.getInt() & 0xFFFFFF;
            bufferSize = ((int) nameLength) + (Integer.BYTES * 2);
            buffer = new byte[bufferSize];
            if (bufferSize != stream.read(buffer))
                throw new IllegalStateException("Unexpected reach EOF when reading entity header.");
            byteBuffer = ByteBuffer.wrap(buffer);

            byte[] nameBuffer = new byte[(int) nameLength];
            byteBuffer.get(nameBuffer);
            String entityName = new String(nameBuffer, StandardCharsets.UTF_8);
            long entityOffset = byteBuffer.getInt() & 0xFFFFFF;
            long entitySize = byteBuffer.getInt() & 0xFFFFFF;
            Entity entity = new Entity();
            entity.name = entityName;
            entity.offset = entityOffset;
            entity.size = entitySize;
            entity.packageFile = file;
            iterateIndex++;
            return entity;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "WechatAppletPackage{" +
                "headerCode=" + headerCode +
                ", indexInfoSize=" + indexInfoSize +
                ", bodyInfoSize=" + bodyInfoSize +
                ", entityCount=" + entityCount +
                ", file=" + file.getAbsolutePath() +
                '}';
    }

    static class Entity {
        private String name;
        private long offset;
        private long size;
        private File packageFile;

        public String getName() {
            return name;
        }

        public long getOffset() {
            return offset;
        }

        public long getSize() {
            return size;
        }

        public File getPackageFile() {
            return packageFile;
        }

        public ByteArrayInputStream getEntityStream() throws IOException {
            if (!(packageFile.exists() && packageFile.isFile())) {
                throw new IOException(String.format("Origin package file '%s' does not exists.", packageFile.getAbsolutePath()));
            }

            byte[] buffer = new byte[(int) size];
            try (InputStream inputStream = new FileInputStream(packageFile)) {
                if (!(offset == inputStream.skip(offset) && size == inputStream.read(buffer))) {
                    throw new IllegalStateException("Entity corrupted.");
                }
            }
            return new ByteArrayInputStream(buffer);
        }

        @Override
        public String toString() {
            return "WechatAppletPackage.Entity{" +
                    "name='" + name + '\'' +
                    ", offset=" + offset +
                    ", size=" + size +
                    ", packageFile=" + packageFile.getAbsolutePath() +
                    '}';
        }
    }

    /*
     * Decoder part
     */
    private static final int HEADER_LENGTH = 18;
    private static final int HEADER_MARKER_1 = 190;
    private static final int HEADER_MARKER_2 = 237;

    /**
     * Create WechatAppletPackage from file
     *
     * @param file Wechat applet package file.
     *             Usually ends with '.wxapkg'.
     * @throws IOException           If file cannot be read.
     * @throws IllegalStateException Illegal file format. Such as file header not matching.
     */
    public static WechatAppletPackage from(File file) throws IOException {
        if (!(file.exists() && file.isFile())) {
            throw new IOException(String.format("'%s' not exists or not a file.", file.getAbsolutePath()));
        }
        FileInputStream inputStream = new FileInputStream(file);
        try {
            // Read header
            byte[] buffer = new byte[HEADER_LENGTH];
            int metaReadCount = inputStream.read(buffer);
            if (metaReadCount != HEADER_LENGTH) {
                throw new IllegalStateException("Unexpected reach EOF while reading meta info.");
            }

            // Decode
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            int mark1 = byteBuffer.get() & 0xFF;
            long headerMeta = byteBuffer.getInt() & 0xFFFFFF;
            long indexInfoLength = byteBuffer.getInt() & 0xFFFFFF;
            long bodyInfoLength = byteBuffer.getInt() & 0xFFFFFF;
            int mark2 = byteBuffer.get() & 0xFF;

            if (!(mark1 == HEADER_MARKER_1 && mark2 == HEADER_MARKER_2)) {
                throw new IllegalArgumentException("Header marker not match. Input file is not a wechat applet file.");
            }

            long fileCount = byteBuffer.getInt() & 0xFFFFFF;

            WechatAppletPackage pkg = new WechatAppletPackage();
            pkg.headerCode = headerMeta;
            pkg.indexInfoSize = indexInfoLength;
            pkg.bodyInfoSize = bodyInfoLength;
            pkg.entityCount = fileCount;
            pkg.file = file;
            pkg.stream = inputStream;

            return pkg;
        } catch (Exception e) {
            inputStream.close();
            throw e;
        }
    }

    private static void printHelpAndExit() {
        System.err.println();
        System.err.println("Wechat Applet Package Extract Tool.");
        System.err.println("By: Kozak Lam, @2022-8-12.");
        System.err.println();
        System.err.println("Usage:");
        System.err.println("    unwxapkg <wxapkg file> [destination folder] [-v|--verbose] [-h|--help]");
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

    public static void main(String[] args) {
        String file = null;
        String destination = null;
        boolean verbose = false;
        int distIndex = 1;
        int fileIndex = 0;
        try {
            for (int i = 0; i < args.length; i++) {
                if (Objects.equals(args[i], "-v") || Objects.equals(args[i], "--verbose")) {
                    verbose = true;
                    if (file == null) fileIndex++;
                    if (destination == null) distIndex++;
                } else if (Objects.equals(args[i], "-h") || Objects.equals(args[i], "--help")) printHelpAndExit();
                else if (i == fileIndex) file = args[i];
                else if (i == distIndex) destination = args[i];
            }
            if (file == null) throw new IllegalArgumentException("Input file is required.");
            if (destination == null) destination = "./" + file + ".unpack";
        } catch (Exception e) {
            printHelpAndExit();
            // Will not reach here
            throw new IllegalStateException();
        }

        try {
            //
            // Check destination directory
            //
            File distDir = new File(destination);
            if (!((distDir.exists() && distDir.isDirectory()) || distDir.mkdirs())) {
                throw new IllegalStateException(String.format("Could not extract files to destination: '%s'", distDir.getAbsolutePath()));
            }
            if (verbose) System.err.printf("[Info ] Output to '%s'\n", distDir.getAbsolutePath());

            //
            // Read package info
            //
            WechatAppletPackage pkg = WechatAppletPackage.from(new File(file));
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
                Entity entity = pkg.next();
                File outputFile = new File(distDir, entity.getName());
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
                    int readCount = 0;
                    do {
                        readCount = inputStream.read(buffer);
                        if (readCount <= 0) break;
                        outputStream.write(buffer, 0, readCount);
                    } while (true);
                }

                if (verbose) System.err.println(" Finished.");
            }

            System.err.printf("[Info ] Extract to '%s' finished.\n", distDir.getAbsolutePath());
        } catch (Exception e) {
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
    }
}
