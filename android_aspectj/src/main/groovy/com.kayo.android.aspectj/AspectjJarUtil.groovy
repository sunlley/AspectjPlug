package com.kayo.android.aspectj

import com.android.annotations.NonNull
import com.google.common.io.Closer

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class AspectjJarUtil {


    private final byte[] buffer = new byte[8192]

    @NonNull
    private File jarFile
    private Closer closer
    private JarOutputStream jarOutputStream

    private IZipEntryFilter filter

    AspectjJarUtil(@NonNull File jar){
        jarFile = jar
    }

    private void init() throws IOException {
        if (closer == null) {
            AspectjFileUtil.mkdirs(jarFile.getParentFile())

            closer = Closer.create()

            FileOutputStream fos = closer.register(new FileOutputStream(jarFile))
            jarOutputStream = closer.register(new JarOutputStream(fos))
        }
    }

    /**
     * Sets a list of regex to exclude from the jar.
     */
    void setFilter(@NonNull IZipEntryFilter filter) {
        this.filter = filter
    }

    void addFolder(@NonNull File folder) throws IOException {
        init()
        try {
            addFolderI(folder, "")
        } catch (IZipEntryFilter.ZipAbortException e) {
            throw new IOException(e)
        }
    }

    private void addFolderI(@NonNull File folder, @NonNull String path)
            throws IOException, IZipEntryFilter.ZipAbortException {
        File[] files = folder.listFiles()
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String entryPath = path + file.getName()
                    if (filter == null || filter.checkEntry(entryPath)) {
                        // new entry
                        jarOutputStream.putNextEntry(new JarEntry(entryPath))

                        // put the file content
                        Closer localCloser = Closer.create()
                        try {
                            FileInputStream fis = localCloser.register(new FileInputStream(file))
                            int count
                            while ((count = fis.read(buffer)) != -1) {
                                jarOutputStream.write(buffer, 0, count)
                            }
                        } finally {
                            localCloser.close()
                        }

                        // close the entry
                        jarOutputStream.closeEntry()
                    }
                } else if (file.isDirectory()) {
                    addFolderI(file, path + file.getName() + "/")
                }
            }
        }
    }

    void addJar(@NonNull File file) throws IOException {
        addJar(file, false)
    }

    void addJar(@NonNull File file, boolean removeEntryTimestamp) throws IOException {
        init()

        Closer localCloser = Closer.create()
        try {
            FileInputStream fis = localCloser.register(new FileInputStream(file))
            ZipInputStream zis = localCloser.register(new ZipInputStream(fis))

            // loop on the entries of the jar file package and put them in the final jar
            ZipEntry entry
            while ((entry = zis.getNextEntry()) != null) {
                // do not take directories or anything inside a potential META-INF folder.
                if (entry.isDirectory()) {
                    continue
                }

                String name = entry.getName()
                if (filter != null && !filter.checkEntry(name)) {
                    continue
                }

                JarEntry newEntry

                // Preserve the STORED method of the input entry.
                if (entry.getMethod() == JarEntry.STORED) {
                    newEntry = new JarEntry(entry)
                } else {
                    // Create a new entry so that the compressed len is recomputed.
                    newEntry = new JarEntry(name)
                }
                if (removeEntryTimestamp) {
                    newEntry.setTime(0)
                }

                // add the entry to the jar archive
                jarOutputStream.putNextEntry(newEntry)

                // read the content of the entry from the input stream, and write it into the archive.
                int count
                while ((count = zis.read(buffer)) != -1) {
                    jarOutputStream.write(buffer, 0, count)
                }

                // close the entries for this file
                jarOutputStream.closeEntry()
                zis.closeEntry()
            }
        } catch (IZipEntryFilter.ZipAbortException e) {
            throw new IOException(e)
        } finally {
            localCloser.close()
        }
    }

    void addEntry(@NonNull String path, @NonNull byte[] bytes) throws IOException {
        init()

        jarOutputStream.putNextEntry(new JarEntry(path))
        jarOutputStream.write(bytes)
        jarOutputStream.closeEntry()
    }

    void close() throws IOException {
        if (closer != null) {
            closer.close()
        }
    }

    /**
     * Classes which implement this interface provides a method to check whether a file should
     * be added to a Jar file.
     */
    static interface IZipEntryFilter {
        /**
         * An exception thrown during packaging of a zip file into APK file.
         * This is typically thrown by implementations of
         * {@link AspectjJarUtil.IZipEntryFilter#checkEntry(String)}.
         */
        class ZipAbortException extends Exception {
            private static final long serialVersionUID = 1L

            ZipAbortException() {
                super()
            }

            ZipAbortException(String format, Object... args) {
                super(String.format(format, args))
            }

            ZipAbortException(Throwable cause, String format, Object... args) {
                super(String.format(format, args), cause)
            }

            ZipAbortException(Throwable cause) {
                super(cause)
            }
        }

        /**
         * Checks a file for inclusion in a Jar archive.
         * @param archivePath the archive file path of the entry
         * @return <code>true</code> if the file should be included.
         * @throws com.kayo.android.aspectj.AspectjJarUtil.IZipEntryFilter.ZipAbortException if writing the file should be aborted.
         */
        boolean checkEntry(String archivePath) throws IZipEntryFilter.ZipAbortException
    }
}