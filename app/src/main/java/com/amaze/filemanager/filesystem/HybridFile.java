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

import static com.amaze.filemanager.filesystem.smb.CifsContexts.SMB_URI_PREFIX;
import static com.amaze.filemanager.filesystem.ssh.SshConnectionPool.SSH_URI_PREFIX;
import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_FAILED_OPS;
import static com.amaze.filemanager.ui.activities.MainActivity.TAG_INTENT_FILTER_GENERAL;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.amaze.filemanager.R;
import com.amaze.filemanager.adapters.data.LayoutElementParcelable;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.database.CloudHandler;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.cloud.CloudUtil;
import com.amaze.filemanager.filesystem.files.FileUtils;
import com.amaze.filemanager.filesystem.hybridfile.CLOUDfile;
import com.amaze.filemanager.filesystem.hybridfile.DOCUMENTfile;
import com.amaze.filemanager.filesystem.hybridfile.FILEfile;
import com.amaze.filemanager.filesystem.hybridfile.OTGfile;
import com.amaze.filemanager.filesystem.hybridfile.ROOTfile;
import com.amaze.filemanager.filesystem.root.ListFilesCommand;
import com.amaze.filemanager.filesystem.root.MakeDirectoryCommand;
import com.amaze.filemanager.filesystem.root.MakeFileCommand;
import com.amaze.filemanager.filesystem.root.RenameFileCommand;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.ui.fragments.preference_fragments.PreferencesConstants;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;
import com.amaze.filemanager.utils.SmbUtil;
import com.amaze.filemanager.filesystem.hybridfile.SMBfile;
import com.amaze.filemanager.filesystem.hybridfile.SFTPfile;
import com.cloudrail.si.interfaces.CloudStorage;

import net.schmizz.sshj.sftp.SFTPClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/** Hybrid file for handeling all types of files */
public class HybridFile {

  private static Executor executor = AsyncTask.THREAD_POOL_EXECUTOR;

  private static final String TAG = Operations.class.getSimpleName();

  public static class InnerFile {

    protected String path;

    protected static final String TAG = HybridFile.class.getSimpleName();

    public static final String DOCUMENT_FILE_PREFIX =
            "content://com.android.externalstorage.documents";

    protected OpenMode mode;
    protected String name;

    protected final DataUtils dataUtils = DataUtils.getInstance();

    public static InnerFile factory(OpenMode mode, String path) {
      InnerFile ret;
      switch (mode) {
        case SFTP: ret = new SFTPfile(); break;
        case SMB: ret = new SMBfile(); break;
        case OTG: ret = new OTGfile(); break;
        case FILE: ret = new FILEfile(); break;
        case ROOT: ret = new ROOTfile(); break;
        case DOCUMENT_FILE: ret = new DOCUMENTfile(); break;
        case DROPBOX:
        case GDRIVE:
        case BOX:
        case ONEDRIVE:
          ret = new CLOUDfile();
          break;
        default:
          ret = new InnerFile();
          break;
      }
      ret.path = path;
      return ret;
    }

    public static InnerFile factory(OpenMode mode, String path, String name, boolean isDirectory) {
      InnerFile ret = factory(mode, path);
      ret.name = name;
      if (path.startsWith(SMB_URI_PREFIX) ||
              mode == OpenMode.SMB || mode == OpenMode.DOCUMENT_FILE || mode == OpenMode.OTG) {
        Uri.Builder pathBuilder = Uri.parse(ret.path).buildUpon().appendEncodedPath(name);
        if ((path.startsWith(SMB_URI_PREFIX) || mode == OpenMode.SMB) && isDirectory) {
          pathBuilder.appendEncodedPath("/");
        }
        ret.path = pathBuilder.build().toString();
      } else if (path.startsWith(SSH_URI_PREFIX) || mode == OpenMode.SFTP) {
        ret.path += "/" + name;
      } else if (mode == OpenMode.ROOT && path.equals("/")) {
        // root of filesystem, don't concat another '/'
        ret.path += name;
      } else {
        ret.path += "/" + name;
      }
      return ret;
    }

    public boolean isSftp() { return mode == OpenMode.SFTP; }
    public boolean isSmb() { return mode == OpenMode.SMB; }
    public boolean isOtgFile() { return mode == OpenMode.OTG; }
    public boolean isDocumentFile() { return mode == OpenMode.DOCUMENT_FILE; }
    public boolean isDropBoxFile() { return mode == OpenMode.DROPBOX; }
    public boolean isGoogleDriveFile() { return mode == OpenMode.GDRIVE; }
    public boolean isOneDriveFile() { return mode == OpenMode.ONEDRIVE; }
    public boolean isLocal() { return mode == OpenMode.FILE; }
    public boolean isRoot() { return mode == OpenMode.ROOT; }
    public boolean isBoxFile() { return mode == OpenMode.BOX; }

    public void generateMode(Context context) {
      if (path.startsWith(SMB_URI_PREFIX)) {
        mode = OpenMode.SMB;
      } else if (path.startsWith(SSH_URI_PREFIX)) {
        mode = OpenMode.SFTP;
      } else if (path.startsWith(OTGUtil.PREFIX_OTG)) {
        mode = OpenMode.OTG;
      } else if (path.startsWith(DOCUMENT_FILE_PREFIX)) {
        mode = OpenMode.DOCUMENT_FILE;
      } else if (isCustomPath()) {
        mode = OpenMode.CUSTOM;
      } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_BOX)) {
        mode = OpenMode.BOX;
      } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_ONE_DRIVE)) {
        mode = OpenMode.ONEDRIVE;
      } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_GOOGLE_DRIVE)) {
        mode = OpenMode.GDRIVE;
      } else if (path.startsWith(CloudHandler.CLOUD_PREFIX_DROPBOX)) {
        mode = OpenMode.DROPBOX;
      } else if (context == null) {
        mode = OpenMode.FILE;
      } else {
        boolean rootmode =
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(PreferencesConstants.PREFERENCE_ROOTMODE, false);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
          mode = OpenMode.FILE;
          if (rootmode && !getFile().canRead()) {
            mode = OpenMode.ROOT;
          }
        } else {
          if (ExternalSdCardOperation.isOnExtSdCard(getFile(), context)) {
            mode = OpenMode.FILE;
          } else if (rootmode && !getFile().canRead()) {
            mode = OpenMode.ROOT;
          }

          // In some cases, non-numeric path is passed into HybridFile while mode is still
          // CUSTOM here. We are forcing OpenMode.FILE in such case too. See #2225
          if (OpenMode.UNKNOWN.equals(mode) || OpenMode.CUSTOM.equals(mode)) {
            mode = OpenMode.FILE;
          }
        }
      }
    }

    public OpenMode getMode() {
      return mode;
    }

    public String getReadablePath(String path) {
      return path;
    }

    public String getSimpleName() {
      StringBuilder builder = new StringBuilder(path);
      String name = builder.substring(builder.lastIndexOf("/") + 1, builder.length());
      return name;
    }

    public String getName(Context context) {
      if (path.isEmpty()) {
        return "";
      }

      String _path = path;
      if (path.endsWith("/")) {
        _path = path.substring(0, path.length() - 1);
      }

      int lastSeparator = _path.lastIndexOf('/');

      return _path.substring(lastSeparator + 1);
    }

    public void setPath(String path) {
      this.path = path;
    }

    @Nullable
    public File getFile() {
      return new File(path);
    }

    @Nullable
    public DocumentFile getDocumentFile(boolean createRecursive) {
      return OTGUtil.getDocumentFile(
              path,
              SafRootHolder.getUriRoot(),
              AppConfig.getInstance(),
              OpenMode.DOCUMENT_FILE,
              createRecursive);
    }

    protected HybridFileParcelable generateBaseFileFromParent() {
      ArrayList<HybridFileParcelable> arrayList =
              RootHelper.getFilesList(getFile().getParent(), true, true);
      for (HybridFileParcelable baseFile : arrayList) {
        if (baseFile.getPath().equals(path)) return baseFile;
      }
      return null;
    }

    public long lastModified() {
      return new File("/").lastModified();
    }

    /**
     * Helper method to find length
     */
    public long length(Context context) {
      return 0L;
    }

    public String getPath() {
      return path;
    }

    public SmbFile getSmbFile(int timeout) {
      try {
        SmbFile smbFile = SmbUtil.create(path);
        smbFile.setConnectTimeout(timeout);
        return smbFile;
      } catch (MalformedURLException e) {
        e.printStackTrace();
        return null;
      }
    }

    public SmbFile getSmbFile() {
      try {
        return SmbUtil.create(path);
      } catch (MalformedURLException e) {
        e.printStackTrace();
        return null;
      }
    }

    public boolean isCustomPath() {
      return path.equals("0")
              || path.equals("1")
              || path.equals("2")
              || path.equals("3")
              || path.equals("4")
              || path.equals("5")
              || path.equals("6");
    }

    /**
     * Helper method to get parent path
     */
    public String getParent(Context context) {
      if (path.length() == getName(context).length()) {
        return null;
      }

      int start = 0;
      int end = path.length() - getName(context).length() - 1;

      return path.substring(start, end);
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
    public boolean isDirectory() {
      return getFile().isDirectory();
    }

    public boolean isDirectory(Context context) {
      return getFile().isDirectory();
    }

    /**
     * @deprecated use {@link #folderSize(Context)}
     */
    public long folderSize() {
      return 0L;
    }

    /**
     * Helper method to get length of folder in an otg
     */
    public long folderSize(Context context) {
      return 0L;
    }

    /**
     * Gets usable i.e. free space of a device
     */
    public long getUsableSpace() {
      return 0L;
    }

    /**
     * Gets total size of the disk
     */
    public long getTotal(Context context) {
      return 0L;
    }

    /**
     * Helper method to list children of this file
     */
    public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
      ListFilesCommand.INSTANCE.listFiles(
              path,
              isRoot,
              true,
              openMode -> null,
              hybridFileParcelable -> {
                onFileFound.onFileFound(hybridFileParcelable);
                return null;
              });
    }

    /**
     * Helper method to list children of this file
     *
     * @deprecated use forEachChildrenFile()
     */
    public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
      return RootHelper.getFilesList(path, isRoot, true);
    }

    /**
     * Handles getting input stream for various {@link OpenMode}
     *
     * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
     */
    @Nullable
    public InputStream getInputStream() {
      InputStream inputStream;
      try {
        inputStream = new FileInputStream(path);
      } catch (FileNotFoundException e) {
        inputStream = null;
        e.printStackTrace();
      }
      return inputStream;
    }

    @Nullable
    public InputStream getInputStream(Context context) {
      InputStream inputStream;
      try {
        inputStream = new FileInputStream(path);
      } catch (FileNotFoundException e) {
        inputStream = null;
        e.printStackTrace();
      }
      return inputStream;
    }

    @Nullable
    public OutputStream getOutputStream(Context context) {
      OutputStream outputStream;
      try {
        outputStream = FileUtil.getOutputStream(getFile(), context);
      } catch (Exception e) {
        outputStream = null;
        e.printStackTrace();
      }
      return outputStream;
    }

    public boolean exists() {
      return false;
    }

    /**
     * Helper method to check file existence in otg
     */
    public boolean exists(Context context) {
      try {
        return (exists());
      } catch (Exception e) {
        Log.i(getClass().getSimpleName(), "Failed to find file", e);
      }
      return false;
    }

    /**
     * Whether file is a simple file (i.e. not a directory/smb/otg/other)
     *
     * @return true if file; other wise false
     */
    public boolean isSimpleFile() {
      return !isCustomPath()
              && !android.util.Patterns.EMAIL_ADDRESS.matcher(path).matches()
              && (getFile() != null && !getFile().isDirectory());
    }

    public boolean setLastModified(final long date) {
      File f = getFile();
      return f.setLastModified(date);
    }

    public void mkdir(Context context) {
      MakeDirectoryOperation.mkdir(getFile(), context);
    }

    public boolean delete(Context context, boolean rootmode)
            throws ShellNotRunningException, SmbException {
      DeleteOperation.deleteFile(getFile(), context);
      return !exists();
    }

    /**
     * Returns the name of file excluding it's extension If no extension is found then whole file name
     * is returned
     */
    public String getNameString(Context context) {
      String fileName = getName(context);

      int extensionStartIndex = fileName.lastIndexOf(".");
      return fileName.substring(
              0, extensionStartIndex == -1 ? fileName.length() : extensionStartIndex);
    }

    /**
     * Generates a {@link LayoutElementParcelable} adapted compatible element. Currently supports only
     * local filesystem
     */
    public LayoutElementParcelable generateLayoutElement(@NonNull Context c, boolean showThumbs) {
      return null;
    }
  }

  private InnerFile innerFile;

  public HybridFile(OpenMode mode, String path) {
    this.innerFile = InnerFile.factory(mode, path);
  }

  public HybridFile(OpenMode mode, String path, String name, boolean isDirectory) {
    this.innerFile = InnerFile.factory(mode, path, name, isDirectory);
  }

  public boolean isSftp() { return innerFile.mode == OpenMode.SFTP; }
  public boolean isSmb() { return innerFile.mode == OpenMode.SMB; }
  public boolean isOtgFile() { return innerFile.mode == OpenMode.OTG; }
  public boolean isDocumentFile() { return innerFile.mode == OpenMode.DOCUMENT_FILE; }
  public boolean isDropBoxFile() { return innerFile.mode == OpenMode.DROPBOX; }
  public boolean isGoogleDriveFile() { return innerFile.mode == OpenMode.GDRIVE; }
  public boolean isOneDriveFile() { return innerFile.mode == OpenMode.ONEDRIVE; }
  public boolean isBoxFile() { return innerFile.isBoxFile(); }
  public boolean isLocal() { return innerFile.mode == OpenMode.FILE; }
  public boolean isRoot() { return innerFile.mode == OpenMode.ROOT; }

  public void generateMode(Context context) { innerFile.generateMode(context); }

  public OpenMode getMode() {
    return innerFile.getMode();
  }

  public void setMode(OpenMode mode) {
    innerFile = InnerFile.factory(mode, innerFile.path, innerFile.name, innerFile.isDirectory());
  }

  public String getReadablePath(String path) {
    return innerFile.getReadablePath(path);
  }

  public static String parseAndFormatUriForDisplay(@NonNull String uriString) {
    Uri uri = Uri.parse(uriString);
    return String.format("%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPath());
  }

  public String getSimpleName() { return innerFile.getSimpleName(); }

  public String getName(Context context) { return innerFile.getName(context); }

  public void setPath(String path) { innerFile.setPath(path); }

  @Nullable
  public File getFile() {
    return innerFile.getFile();
  }

  @Nullable
  public DocumentFile getDocumentFile(boolean createRecursive) {
    return innerFile.getDocumentFile(createRecursive);
  }

  protected HybridFileParcelable generateBaseFileFromParent() {
    return innerFile.generateBaseFileFromParent();
  }

  public long lastModified() {
    return innerFile.lastModified();
  }

  /**
   * Helper method to find length
   */
  public long length(Context context) {
    return 0L;
  }

  public String getPath() {
    return innerFile.getPath();
  }

  public SmbFile getSmbFile(int timeout) { return innerFile.getSmbFile(timeout); }

  public SmbFile getSmbFile() { return innerFile.getSmbFile(); }

  public boolean isCustomPath() {
    return innerFile.isCustomPath();
  }

  /**
   * Helper method to get parent path
   */
  public String getParent(Context context) {
    return innerFile.getParent(context);
  }

  public String getParentName() {
    return innerFile.getParentName();
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  public boolean isDirectory() {
    return innerFile.isDirectory();
  }

  public boolean isDirectory(Context context) {
    return innerFile.isDirectory(context);
  }

  /**
   * @deprecated use {@link #folderSize(Context)}
   */
  public long folderSize() {
    return innerFile.folderSize();
  }

  /**
   * Helper method to get length of folder in an otg
   */
  public long folderSize(Context context) {
    return innerFile.folderSize(context);
  }

  /**
   * Gets usable i.e. free space of a device
   */
  public long getUsableSpace() {
    return innerFile.getUsableSpace();
  }

  /**
   * Gets total size of the disk
   */
  public long getTotal(Context context) {
    return innerFile.getTotal(context);
  }

  /**
   * Helper method to list children of this file
   */
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    innerFile.forEachChildrenFile(context, isRoot, onFileFound);
  }

  /**
   * Helper method to list children of this file
   *
   * @deprecated use forEachChildrenFile()
   */
  public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
    return innerFile.listFiles(context, isRoot);
  }

  /**
   * Handles getting input stream for various {@link OpenMode}
   *
   * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
   */
  @Nullable
  public InputStream getInputStream() {
    return innerFile.getInputStream();
  }

  @Nullable
  public InputStream getInputStream(Context context) {
    return innerFile.getInputStream(context);
  }

  @Nullable
  public OutputStream getOutputStream(Context context) {
    return innerFile.getOutputStream(context);
  }

  public boolean exists() {
    return false;
  }

  /**
   * Helper method to check file existence in otg
   */
  public boolean exists(Context context) {
    try {
      return (exists());
    } catch (Exception e) {
      Log.i(getClass().getSimpleName(), "Failed to find file", e);
    }
    return false;
  }

  /**
   * Whether file is a simple file (i.e. not a directory/smb/otg/other)
   *
   * @return true if file; other wise false
   */
  public boolean isSimpleFile() {
    return innerFile.isSimpleFile();
  }

  public boolean setLastModified(final long date) {
    return innerFile.setLastModified(date);
  }

  public void mkdir(Context context) {
    innerFile.mkdir(context);
  }

  public boolean delete(Context context, boolean rootmode)
          throws ShellNotRunningException, SmbException {
    return innerFile.delete(context, rootmode);
  }

  /**
   * Returns the name of file excluding it's extension If no extension is found then whole file name
   * is returned
   */
  public String getNameString(Context context) {
    return innerFile.getNameString(context);
  }

  /**
   * Generates a {@link LayoutElementParcelable} adapted compatible element. Currently supports only
   * local filesystem
   */
  public LayoutElementParcelable generateLayoutElement(@NonNull Context c, boolean showThumbs) {
    return innerFile.generateLayoutElement(c, showThumbs);
  }
}
