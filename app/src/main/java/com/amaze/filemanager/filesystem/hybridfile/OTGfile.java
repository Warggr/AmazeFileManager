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

import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/** Hybrid file for handeling all types of files */
public class OTGfile extends HybridFile {

  @Nullable
  public DocumentFile getDocumentFile(boolean createRecursive) {
    return OTGUtil.getDocumentFile(
        path,
        SafRootHolder.getUriRoot(),
        AppConfig.getInstance(),
        OpenMode.DOCUMENT_FILE,
        createRecursive);
  }

  /** Helper method to find length */
  @Override
  public long length(Context context) {
    return OTGUtil.getDocumentFile(path, context, false).length();
  }

  public String getSimpleName() {
    StringBuilder builder = new StringBuilder(path);
    return builder.substring(builder.lastIndexOf("/") + 1, builder.length());
  }

  public String getName(Context context) {
    if (!Utils.isNullOrEmpty(name)) {
      return name;
    }
    return OTGUtil.getDocumentFile(path, context, false).getName();
  }

  public String getParentName() {
    StringBuilder builder = new StringBuilder(path);
    StringBuilder parentPath =
        new StringBuilder(builder.substring(0, builder.length() - (getSimpleName().length() + 1)));
    String parentName = parentPath.substring(parentPath.lastIndexOf("/") + 1, parentPath.length());
    return parentName;
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  @Override
  public boolean isDirectory() {
    // TODO: support for this method in OTG on-the-fly
    // you need to manually call {@link RootHelper#getDocumentFile() method
    return false;
  }

  @Override
  public boolean isDirectory(Context context) {
    boolean isDirectory;

    isDirectory = OTGUtil.getDocumentFile(path, context, false).isDirectory();
    return isDirectory;
  }

  /** Helper method to get length of folder in an otg */
  @Override
  public long folderSize(Context context) {
    return FileUtils.otgFolderSize(path, context);
  }

  /** Gets usable i.e. free space of a device */
  @Override
  public long getUsableSpace() {
    // TODO: Get free space from OTG when {@link DocumentFile} API adds support
    return 0L;
  }

  /** Gets total size of the disk */
  @Override
  public long getTotal(Context context) {
    // TODO: Find total storage space of OTG when {@link DocumentFile} API adds support
    DocumentFile documentFile = OTGUtil.getDocumentFile(path, context, false);
    return documentFile.length();
  }

  /** Helper method to list children of this file */
  @Override
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    OTGUtil.getDocumentFiles(path, context, onFileFound);
  }

  /**
   * Helper method to list children of this file
   *
   * @deprecated use forEachChildrenFile()
   */
  @Override
  public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
    return OTGUtil.getDocumentFilesList(path, context);
  }

  @Nullable
  @Override
  public InputStream getInputStream(Context context) {
    InputStream inputStream;

    ContentResolver contentResolver = context.getContentResolver();
    DocumentFile documentSourceFile = OTGUtil.getDocumentFile(path, context, false);
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
    DocumentFile documentSourceFile = OTGUtil.getDocumentFile(path, context, true);
    try {
      outputStream = contentResolver.openOutputStream(documentSourceFile.getUri());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      outputStream = null;
    }

    return outputStream;
  }

  @Override
  /** Helper method to check file existence in otg */
  public boolean exists(Context context) {
    boolean exists = false;
    try {
      exists = OTGUtil.getDocumentFile(path, context, false) != null;
    } catch (Exception e) {
      Log.i(getClass().getSimpleName(), "Failed to find file", e);
    }
    return exists;
  }

  @Override
  public boolean setLastModified(final long date) {
    File f = getFile();
    return f.setLastModified(date);
  }

  @Override
  public void mkdir(Context context) {
    if (!exists(context)) {
      DocumentFile parentDirectory = OTGUtil.getDocumentFile(getParent(context), context, true);
      if (parentDirectory.isDirectory()) {
        parentDirectory.createDirectory(getName(context));
      }
    }
  }

  @Override
  public Void mkkdir(Context context){
    if (checkOtgNewFileExists(this, context)) {
      errorCallBack.exists(file);
      return null;
    }
    safCreateDirectory.apply(OTGUtil.getDocumentFile(parentFile.getPath(), context, false));
    return null;
  }
}
