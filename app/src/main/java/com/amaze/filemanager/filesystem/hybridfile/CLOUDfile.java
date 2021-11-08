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

import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.amaze.filemanager.file_operations.exceptions.CloudPluginException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.utils.OnFileFound;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.types.SpaceAllocation;

import java.io.InputStream;
import java.util.ArrayList;

public class CLOUDfile extends HybridFile.InnerFile {
  protected OpenMode mode;

  /** Helper method to find length */
  public long length(Context context) {
    return dataUtils
                .getAccount(mode)
                .getMetadata(CloudUtil.stripPath(mode, path))
                .getSize();
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  @Override
  public boolean isDirectory() {
    return getFile().isDirectory();
  }

  @Override
  public boolean isDirectory(Context context) {
    return dataUtils
                .getAccount(mode)
                .getMetadata(CloudUtil.stripPath(mode, path))
                .getFolder();
  }

  /** Helper method to get length of folder in an otg */
  @Override
  public long folderSize(Context context) {
    return FileUtils.folderSizeCloud(
                mode, dataUtils.getAccount(mode).getMetadata(CloudUtil.stripPath(mode, path)));
  }

  /** Gets usable i.e. free space of a device */
  @Override
  public long getUsableSpace() {
    SpaceAllocation spaceAllocation = dataUtils.getAccount(mode).getAllocation();
    return spaceAllocation.getTotal() - spaceAllocation.getUsed();
  }

  /** Gets total size of the disk */
  @Override
  public long getTotal(Context context) {
    SpaceAllocation spaceAllocation = dataUtils.getAccount(mode).getAllocation();
    return spaceAllocation.getTotal();
  }

  /** Helper method to list children of this file */
  @Override
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    try {
      CloudUtil.getCloudFiles(path, dataUtils.getAccount(mode), mode, onFileFound);
    } catch (CloudPluginException e) {
      e.printStackTrace();
    }
  }

  /**
   * Helper method to list children of this file
   *
   * @deprecated use forEachChildrenFile()
   */
  @Override
  public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
    ArrayList<HybridFileParcelable> arrayList = new ArrayList<>();
    try {
      arrayList = CloudUtil.listFiles(path, dataUtils.getAccount(mode), mode);
    } catch (CloudPluginException e) {
      e.printStackTrace();
      arrayList = new ArrayList<>();
    }

    return arrayList;
  }

  @Nullable
  @Override
  public InputStream getInputStream(Context context) {
    CloudStorage cloudStorageDropbox = dataUtils.getAccount(mode);
    Log.d(getClass().getSimpleName(), CloudUtil.stripPath(mode, path));
    return cloudStorageDropbox.download(CloudUtil.stripPath(mode, path));
  }

  @Override
  public boolean exists() {
    CloudStorage cloudStorage = dataUtils.getAccount(mode);
    return cloudStorage.exists(CloudUtil.stripPath(mode, path));
  }

  /** Helper method to check file existence in otg */
  @Override
  public boolean exists(Context context) {
    boolean exists = false;
    try {
      return (exists());
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
  public boolean isSimpleFile() { return false; }

  @Override
  public void mkdir(Context context) {
    CloudStorage cloudStorage = dataUtils.getAccount(mode);
    try {
      cloudStorage.createFolder(CloudUtil.stripPath(mode, path));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
