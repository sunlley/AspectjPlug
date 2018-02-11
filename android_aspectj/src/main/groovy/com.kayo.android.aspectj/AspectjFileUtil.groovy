package com.kayo.android.aspectj

import com.android.annotations.NonNull

class AspectjFileUtil {
    static void deleteFolder(File folder) throws IOException {
        if (folder == null || !folder.exists()) {
            return
        }
        File[] files = folder.listFiles()
        if (files != null) { // i.e. is a directory.
            for (final File file : files) {
                deleteFolder(file)
            }
        }
        if (!folder.delete()) {
            throw new IOException(String.format("Could not delete folder %s", folder))
        }
    }

    static void mkdirs(File folder) {
        if (folder == null) {
            return
        }

        if (!folder.mkdirs() && !folder.exists()) {
            throw new RuntimeException("Cannot create directory " + folder)
        }
    }

    static void deleteIfExists(@NonNull File file) throws IOException {
        boolean result = file.delete()
        if (!result && file.exists()) {
            throw new IOException("Failed to delete " + file.getAbsolutePath())
        }
    }

}