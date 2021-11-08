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

package com.amaze.filemanager.filesystem.hybridfile;

import android.content.Context;

import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.filesystem.DeleteOperation;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.root.DeleteFileCommand;

import java.io.File;

import jcifs.smb.SmbException;

/** Hybrid file for handeling all types of files */
public class ROOTfile extends FILEfile {

  @Override
  public long lastModified() {
    HybridFileParcelable baseFile = generateBaseFileFromParent();
    if (baseFile != null) return baseFile.getDate();
    return new File("/").lastModified();
  }

  /** Helper method to find length */
  @Override
  public long length(Context context) {
    HybridFileParcelable baseFile = generateBaseFileFromParent();
    if (baseFile != null) return baseFile.getSize();
    return 0L;
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  @Override
  public boolean isDirectory() {
    boolean isDirectory;
    try {
      isDirectory = RootHelper.isDirectory(path, 5);
    } catch (ShellNotRunningException e) {
      e.printStackTrace();
      isDirectory = false;
    }
    return isDirectory;
  }

  @Override
  public boolean isDirectory(Context context) {
    boolean isDirectory;

    try {
      isDirectory = RootHelper.isDirectory(path, 5);
    } catch (ShellNotRunningException e) {
      e.printStackTrace();
      isDirectory = false;
    }
    return isDirectory;
  }

  /** @deprecated use {@link #folderSize(Context)} */
  @Override
  public long folderSize() {
    long size = 0L;

    HybridFileParcelable baseFile = generateBaseFileFromParent();
    if (baseFile != null) size = baseFile.getSize();
    return size;
  }

  /** Helper method to get length of folder in an otg */
  @Override
  public long folderSize(Context context) {

    long size = 0l;

    HybridFileParcelable baseFile = generateBaseFileFromParent();
    if (baseFile != null) size = baseFile.getSize();
    return size;
  }

  @Override
  public boolean exists() {
    return RootHelper.fileExists(path);
  }

  @Override
  public boolean delete(Context context, boolean rootmode)
      throws ShellNotRunningException, SmbException {
    if (rootmode) {
      DeleteFileCommand.INSTANCE.deleteFile(getPath());
    } else {
      DeleteOperation.deleteFile(getFile(), context);
    }
    return !exists();
  }
}
