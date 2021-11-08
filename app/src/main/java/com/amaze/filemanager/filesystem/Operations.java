/*
 * Copyright (C) 2014-2020 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.filesystem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.amaze.filemanager.filesystem.files.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executor;

public class Operations {
  //CYCLO / LOC = 21/604 = 0.03 < 0.16
  //LOC / METHOD = 604 / 9 = 67 > 10
  //FANOUT/METHOD looks too high

  // reserved characters by OS, shall not be allowed in file names
  private static final String FOREWARD_SLASH = "/";
  private static final String BACKWARD_SLASH = "\\";
  private static final String COLON = ":";
  private static final String ASTERISK = "*";
  private static final String QUESTION_MARK = "?";
  private static final String QUOTE = "\"";
  private static final String GREATER_THAN = ">";
  private static final String LESS_THAN = "<";

  private static final String FAT = "FAT";

  public interface ErrorCallBack {

    /** Callback fired when file being created in process already exists */
    void exists(HybridFile file);

    /**
     * Callback fired when creating new file/directory and required storage access framework
     * permission to access SD Card is not available
     */
    void launchSAF(HybridFile file);

    /**
     * Callback fired when renaming file and required storage access framework permission to access
     * SD Card is not available
     */
    void launchSAF(HybridFile file, HybridFile file1);

    /**
     * Callback fired when we're done processing the operation
     *
     * @param b defines whether operation was successful
     */
    void done(HybridFile hFile, boolean b);

    /** Callback fired when an invalid file name is found. */
    void invalidName(HybridFile file);
  }

  /**
   * Well, we wouldn't want to copy when the target is inside the source otherwise it'll end into a
   * loop
   *
   * @return true when copy loop is possible
   */
  public static boolean isCopyLoopPossible(HybridFileParcelable sourceFile, HybridFile targetFile) {
    return targetFile.getPath().contains(sourceFile.getPath());
  }

  /**
   * Validates file name special reserved characters shall not be allowed in the file names on FAT
   * filesystems
   *
   * @param fileName the filename, not the full path!
   * @return boolean if the file name is valid or invalid
   */
  public static boolean isFileNameValid(String fileName) {

    // Trim the trailing slash if there is one.
    if (fileName.endsWith("/")) fileName = fileName.substring(0, fileName.lastIndexOf('/') - 1);
    // Trim the leading slashes if there is any.
    if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf('/') + 1);

    return !TextUtils.isEmpty(fileName)
        && !(fileName.contains(ASTERISK)
            || fileName.contains(BACKWARD_SLASH)
            || fileName.contains(COLON)
            || fileName.contains(FOREWARD_SLASH)
            || fileName.contains(GREATER_THAN)
            || fileName.contains(LESS_THAN)
            || fileName.contains(QUESTION_MARK)
            || fileName.contains(QUOTE));
  }

  private static boolean isFileSystemFAT(String mountPoint) {
    String[] args =
        new String[] {
          "/bin/bash",
          "-c",
          "df -DO_NOT_REPLACE | awk '{print $1,$2,$NF}' | grep \"^" + mountPoint + "\""
        };
    try {
      Process proc = new ProcessBuilder(args).start();
      OutputStream outputStream = proc.getOutputStream();
      String buffer = null;
      outputStream.write(buffer.getBytes());
      return buffer != null && buffer.contains(FAT);
    } catch (IOException e) {
      e.printStackTrace();
      // process interrupted, returning true, as a word of cation
      return true;
    }
  }
}
