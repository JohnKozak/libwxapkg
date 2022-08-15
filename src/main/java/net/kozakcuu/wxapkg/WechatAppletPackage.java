package net.kozakcuu.wxapkg;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

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

    public static void main(String[] args) {
        WechatAppletCommandLineTool.fromArgs(args).start();
    }
}
