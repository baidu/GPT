/**
 * Copyright (c) 2014 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.android.gporter.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

/**
 * Tools to build a quick partial crc of zip files
 *
 * @author liuhaitao
 * @since 2018-01-09
 */
public final class ZipUtil {
    /**
     * CentralDirectory
     */
    static class CentralDirectory {
        /**
         * offset
         */
        long offset;
        /**
         * size
         */
        long size;
    }

    /**
     * redefine those constant here because of bug 13721174 preventing to compile using the
     * constants defined in ZipFile
     */
    private static final int ENDHDR = 22;
    private static final int ENDSIG = 0x6054b50;

    /**
     * Size of reading buffers.
     */
    private static final int BUFFER_SIZE = 0x4000;

    /**
     * Compute crc32 of the central directory of an apk. The central directory contains
     * the crc32 of each entries in the zip so the computed result is considered valid for the whole
     * zip file. Does not support zip64 nor multidisk but it should be OK for now since ZipFile does
     * not either.
     *
     * @param apk 文件
     * @return computeCrcOfCentralDir
     * @throws IOException
     */
    static long getZipCrc(File apk) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(apk, "r");
        try {
            CentralDirectory dir = findCentralDirectory(raf);

            return computeCrcOfCentralDir(raf, dir);
        } finally {
            raf.close();
        }
    }

    /**
     * findCentralDirectory
     *
     * @param raf RandomAccessFile
     * @return CentralDirectory
     * @throws IOException,ZipException
     */
    static CentralDirectory findCentralDirectory(RandomAccessFile raf) throws IOException,
            ZipException {
        long scanOffset = raf.length() - ENDHDR;
        if (scanOffset < 0) {
            throw new ZipException("File too short to be a zip file: " + raf.length());
        }

        long stopOffset = scanOffset - 0x10000 /* ".ZIP file comment"'s max length */;
        if (stopOffset < 0) {
            stopOffset = 0;
        }

        int endSig = Integer.reverseBytes(ENDSIG);
        while (true) {
            raf.seek(scanOffset);
            if (raf.readInt() == endSig) {
                break;
            }

            scanOffset--;
            if (scanOffset < stopOffset) {
                throw new ZipException("End Of Central Directory signature not found");
            }
        }
        // Read the End Of Central Directory. ENDHDR includes the signature
        // bytes, which we've already read.

        // Pull out the information we need.
        raf.skipBytes(2); // diskNumber
        raf.skipBytes(2); // diskWithCentralDir
        raf.skipBytes(2); // numEntries
        raf.skipBytes(2); // totalNumEntries
        CentralDirectory dir = new CentralDirectory();
        dir.size = Integer.reverseBytes(raf.readInt()) & 0xFFFFFFFFL;
        dir.offset = Integer.reverseBytes(raf.readInt()) & 0xFFFFFFFFL;
        return dir;
    }

    /**
     * computeCrcOfCentralDir
     *
     * @param raf RandomAccessFile
     * @param dir CentralDirectory
     * @return crc
     * @throws IOException
     */
    static long computeCrcOfCentralDir(RandomAccessFile raf, CentralDirectory dir)
            throws IOException {
        CRC32 crc = new CRC32();
        long stillToRead = dir.size;
        raf.seek(dir.offset);
        int length = (int) Math.min(BUFFER_SIZE, stillToRead);
        byte[] buffer = new byte[BUFFER_SIZE];
        length = raf.read(buffer, 0, length);
        while (length != -1) {
            crc.update(buffer, 0, length);
            stillToRead -= length;
            if (stillToRead == 0) {
                break;
            }
            length = (int) Math.min(BUFFER_SIZE, stillToRead);
            length = raf.read(buffer, 0, length);
        }
        return crc.getValue();
    }

    /**
     * 递归获得该文件下所有文件名(不包括目录名)
     *
     * @param file File
     * @return List<File>
     */
    private static List<File> loadFilename(File file) {
        List<File> filenameList = new ArrayList<File>();
        if (file.isFile()) {
            filenameList.add(file);
        }
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                filenameList.addAll(loadFilename(f));
            }
        }
        return filenameList;
    }

    /**
     * zipfile
     *
     * @param source File
     * @param dest   File
     * @throws IOException
     */
    public static void zipfile(File source, File dest) throws IOException {
        List<File> fileList = loadFilename(source);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest));

        byte[] buffere = new byte[8192];
        int length;
        BufferedInputStream bis;

        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            zos.putNextEntry(new ZipEntry(file.getName()));
            bis = new BufferedInputStream(new FileInputStream(file));

            while (true) {
                length = bis.read(buffere);
                if (length == -1)
                    break;
                zos.write(buffere, 0, length);
            }
            bis.close();
            zos.closeEntry();
        }
        zos.close();
    }
}
