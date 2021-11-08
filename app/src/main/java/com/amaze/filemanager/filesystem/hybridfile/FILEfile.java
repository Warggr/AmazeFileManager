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
import android.util.Log;

import androidx.annotation.NonNull;

import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.filesystem.files.FileUtils;

import java.io.File;

/** Hybrid file for handeling all types of files */
public class FILEfile extends HybridFile.InnerFile {
  @Override
  public long lastModified() {
    return getFile().lastModified();
  }

  /** Helper method to find length */
  @Override
  public long length(Context context) {
        return getFile().length();
  }

  @Override
  public String getName(Context context) {
    return getFile().getName();
  }

  /** Helper method to get parent path */
  @Override
  public String getParent(Context context) {
    return getFile().getParent();
  }

  /** @deprecated use {@link #folderSize(Context)} */
  @Override
  public long folderSize() {
    return FileUtils.folderSize(getFile(), null);
  }

  /** Helper method to get length of folder in an otg */
  @Override
  public long folderSize(Context context) {
    return FileUtils.folderSize(getFile(), null);
  }

  /** Gets usable i.e. free space of a device */
  @Override
  public long getUsableSpace() {
    return getFile().getUsableSpace();
  }

  /** Gets total size of the disk */
  @Override
  public long getTotal(Context context) {
    return getFile().getTotalSpace();
  }

  @Override
  public boolean exists() {
    return getFile().exists();
  }

  /** Helper method to check file existence in otg */
  @Override
  public boolean exists(Context context) {
    try {
      return (exists());
    } catch (Exception e) {
      Log.i(getClass().getSimpleName(), "Failed to find file", e);
    }
    return false;
  }

  /**
   * Generates a {@link LayoutElementParcelable} adapted compatible element. Currently supports only
   * local filesystem
   */
  @Override
  public LayoutElementParcelable generateLayoutElement(@NonNull Context c, boolean showThumbs) {
    File file = getFile();
    LayoutElementParcelable layoutElement;
    if (isDirectory()) {
      layoutElement =
          new LayoutElementParcelable(
              c,
              path,
              RootHelper.parseFilePermission(file),
              "",
              folderSize() + "",
              0,
              true,
              file.lastModified() + "",
              false,
              showThumbs,
              mode); //mode is not necessarily FILE, since ROOT inherits from us
    } else {
      layoutElement =
          new LayoutElementParcelable(
              c,
              file.getPath(),
              RootHelper.parseFilePermission(file),
              file.getPath(),
              file.length() + "",
              file.length(),
              false,
              file.lastModified() + "",
              false,
              showThumbs,
              mode);
    }
    return layoutElement;
  }
}
