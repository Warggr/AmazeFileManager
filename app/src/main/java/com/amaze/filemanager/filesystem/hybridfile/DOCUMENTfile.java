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

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.FileProperties;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.Utils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/** Hybrid file for handeling all types of files */
public class DOCUMENTfile extends HybridFile {

  @Override
  public long lastModified() {
    return getDocumentFile(false).lastModified();
  }

  /** Helper method to find length */
  @Override
  public long length(Context context) {
    return getDocumentFile(false).length();
  }

  @Override
  public String getName(Context context) {
    if (!Utils.isNullOrEmpty(name)) {
      return name;
    }
    return OTGUtil.getDocumentFile(
            path, SafRootHolder.getUriRoot(), context, OpenMode.DOCUMENT_FILE, false)
        .getName();
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  @Override
  public boolean isDirectory() {
    return getDocumentFile(false).isDirectory();
  }

  @Override
  public boolean isDirectory(Context context) {
    return getDocumentFile(false).isDirectory();
  }

  /** Helper method to get length of folder in an otg */
  public long folderSize(Context context) {

    long size = 0L;

    final AtomicLong totalBytes = new AtomicLong(0);
    OTGUtil.getDocumentFiles(
        SafRootHolder.getUriRoot(),
        path,
        context,
        OpenMode.DOCUMENT_FILE,
        file -> totalBytes.addAndGet(FileUtils.getBaseFileSize(file, context)));

    return size;
  }

  /** Gets usable i.e. free space of a device */
  @Override
  public long getUsableSpace() {
    return FileProperties.getDeviceStorageRemainingSpace(SafRootHolder.INSTANCE.getVolumeLabel());
  }

  /** Gets total size of the disk */
  @Override
  public long getTotal(Context context) {
    return getDocumentFile(false).length();
  }

  /** Helper method to list children of this file */
  @Override
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    OTGUtil.getDocumentFiles(
        SafRootHolder.getUriRoot(), path, context, OpenMode.DOCUMENT_FILE, onFileFound);
  }

  /**
   * Helper method to list children of this file
   *
   * @deprecated use forEachChildrenFile()
   */
  @Override
  public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
    final ArrayList<HybridFileParcelable> hybridFileParcelables = new ArrayList<>();
    OTGUtil.getDocumentFiles(
        SafRootHolder.getUriRoot(),
        path,
        context,
        OpenMode.DOCUMENT_FILE,
        file -> hybridFileParcelables.add(file));

    return hybridFileParcelables;
  }

  @Nullable
  @Override
  public InputStream getInputStream(Context context) {
    InputStream inputStream;

    ContentResolver contentResolver = context.getContentResolver();
    DocumentFile documentSourceFile = getDocumentFile(false);
    try {
      inputStream = contentResolver.openInputStream(documentSourceFile.getUri());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      inputStream = null;
    }

    return inputStream;
  }

  @Nullable
  @Override
  public OutputStream getOutputStream(Context context) {
    OutputStream outputStream;

    ContentResolver contentResolver = context.getContentResolver();
    DocumentFile documentSourceFile = getDocumentFile(true);
    try {
      outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      outputStream = null;
    }

    return outputStream;
  }

  @Override
  public boolean exists() { return false; }

  /** Helper method to check file existence in otg */
  @Override
  public boolean exists(Context context) {
    boolean exists = false;
    try {
      exists =
          OTGUtil.getDocumentFile(
                  path, SafRootHolder.getUriRoot(), context, OpenMode.DOCUMENT_FILE, false)
              != null;
    } catch (Exception e) {
      Log.i(getClass().getSimpleName(), "Failed to find file", e);
    }
    return exists;
  }

  /**
   * Whether file is a simple file (i.e. not a directory/smb/otg/other)
   *
   * @return true if file; other wise false
   */
  @Override
  public boolean isSimpleFile() {
    return false;
  }

  @Override
  public void mkdir(Context context) {
    if (!exists(context)) {
      DocumentFile parentDirectory =
          OTGUtil.getDocumentFile(
              getParent(context),
              SafRootHolder.getUriRoot(),
              context,
              OpenMode.DOCUMENT_FILE,
              true);
      if (parentDirectory.isDirectory()) {
        parentDirectory.createDirectory(getName(context));
      }
    }
  }
}
