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
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.amaze.filemanager.R;
import com.amaze.filemanager.application.AppConfig;
import com.amaze.filemanager.file_operations.exceptions.ShellNotRunningException;
import com.amaze.filemanager.file_operations.filesystem.OpenMode;
import com.amaze.filemanager.filesystem.HybridFile;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.filesystem.SafRootHolder;
import com.amaze.filemanager.filesystem.ssh.SFtpClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientTemplate;
import com.amaze.filemanager.filesystem.ssh.SshClientUtils;
import com.amaze.filemanager.filesystem.ssh.Statvfs;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OnFileFound;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.Buffer;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.RemoteFile;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;

import jcifs.smb.SmbException;

public class SFTPfile extends HybridFile {

  @Nullable
  public DocumentFile getDocumentFile(boolean createRecursive) {
    return OTGUtil.getDocumentFile(
        path,
        SafRootHolder.getUriRoot(),
        AppConfig.getInstance(),
        OpenMode.DOCUMENT_FILE,
        createRecursive);
  }

  @Override
  public long lastModified() {
    final Long returnValue =
        SshClientUtils.execute(
            new SFtpClientTemplate<Long>(path) {
              @Override
              public Long execute(@NonNull SFTPClient client) throws IOException {
                return client.mtime(SshClientUtils.extractRemotePathFrom(path));
              }
            });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining last modification time over SFTP");
    }

    return returnValue == null ? 0L : returnValue;
  }

  /** Helper method to find length */
  @Override
  public long length(Context context) {
    return ((HybridFileParcelable) this).getSize();
  }

  /**
   * Whether this object refers to a directory or file, handles all types of files
   *
   * @deprecated use {@link #isDirectory(Context)} to handle content resolvers
   */
  @Override
  public boolean isDirectory() {
    return isDirectory(AppConfig.getInstance());
  }

  @Override
  public boolean isDirectory(Context context) {
    final Boolean returnValue =
      SshClientUtils.<Boolean>execute(
          new SFtpClientTemplate<Boolean>(path) {
            @Override
            public Boolean execute(SFTPClient client) {
              try {
                return client
                    .stat(SshClientUtils.extractRemotePathFrom(path))
                    .getType()
                    .equals(FileMode.Type.DIRECTORY);
              } catch (IOException notFound) {
                Log.e(
                    getClass().getSimpleName(),
                    "Fail to execute isDirectory for SFTP path :" + path,
                    notFound);
                return false;
              }
            }
          });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining if path is directory over SFTP");
    }

    //noinspection SimplifiableConditionalExpression
    return returnValue == null ? false : returnValue;
  }

  /** @deprecated use {@link #folderSize(Context)} */
  @Override
  public long folderSize() {
    return folderSize(AppConfig.getInstance());
  }

  /** Helper method to get length of folder in an otg */
  public long folderSize(Context context) {
    final Long returnValue =
        SshClientUtils.<Long>execute(
            new SFtpClientTemplate<Long>(path) {
              @Override
              public Long execute(SFTPClient client) throws IOException {
                return client.size(SshClientUtils.extractRemotePathFrom(path));
              }
            });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining size of folder over SFTP");
    }

    return returnValue == null ? 0L : returnValue;
  }

  /** Gets usable i.e. free space of a device */
  public long getUsableSpace() {
    long size = 0L;
    final Long returnValue =
        SshClientUtils.<Long>execute(
            new SFtpClientTemplate<Long>(path) {
              @Override
              public Long execute(@NonNull SFTPClient client) throws IOException {
                try {
                  Statvfs.Response response =
                      new Statvfs.Response(
                          path,
                          client
                              .getSFTPEngine()
                              .request(
                                  Statvfs.request(
                                      client, SshClientUtils.extractRemotePathFrom(path)))
                              .retrieve());
                  return response.diskFreeSpace();
                } catch (SFTPException e) {
                  Log.e(TAG, "Error querying server", e);
                  return 0L;
                } catch (Buffer.BufferException e) {
                  Log.e(TAG, "Error parsing reply", e);
                  return 0L;
                }
              }
            });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining usable space over SFTP");
    }

    size = returnValue == null ? 0L : returnValue;
    return size;
  }

  /** Gets total size of the disk */
  public long getTotal(Context context) {
    long size = 0l;

    final Long returnValue =
        SshClientUtils.<Long>execute(
            new SFtpClientTemplate<Long>(path) {
              @Override
              public Long execute(@NonNull SFTPClient client) throws IOException {
                try {
                  Statvfs.Response response =
                      new Statvfs.Response(
                          path,
                          client
                              .getSFTPEngine()
                              .request(
                                  Statvfs.request(
                                      client, SshClientUtils.extractRemotePathFrom(path)))
                              .retrieve());
                  return response.diskSize();
                } catch (SFTPException e) {
                  Log.e(TAG, "Error querying server", e);
                  return 0L;
                } catch (Buffer.BufferException e) {
                  Log.e(TAG, "Error parsing reply", e);
                  return 0L;
                }
              }
            });

    if (returnValue == null) {
      Log.e(TAG, "Error obtaining total space over SFTP");
    }

    size = returnValue == null ? 0L : returnValue;
    return size;
  }

  /** Helper method to list children of this file */
  public void forEachChildrenFile(Context context, boolean isRoot, OnFileFound onFileFound) {
    SshClientUtils.<Boolean>execute(
        new SFtpClientTemplate<Boolean>(path) {
          @Override
          public Boolean execute(SFTPClient client) {
            try {
              for (RemoteResourceInfo info :
                  client.ls(SshClientUtils.extractRemotePathFrom(path))) {
                boolean isDirectory = false;
                try {
                  isDirectory = SshClientUtils.isDirectory(client, info);
                } catch (IOException ifBrokenSymlink) {
                  Log.w(TAG, "IOException checking isDirectory(): " + info.getPath());
                  continue;
                }
                HybridFileParcelable f = new HybridFileParcelable(path, isDirectory, info);
                onFileFound.onFileFound(f);
              }
            } catch (IOException e) {
              Log.w("DEBUG.listFiles", "IOException", e);
              AppConfig.toast(
                  context,
                  context.getString(
                      R.string.cannot_read_directory,
                      parseAndFormatUriForDisplay(path),
                      e.getMessage()));
            }
            return true;
          }
        });
  }

  /**
   * Helper method to list children of this file
   *
   * @deprecated use forEachChildrenFile()
   */
  public ArrayList<HybridFileParcelable> listFiles(Context context, boolean isRoot) {
    ArrayList<HybridFileParcelable> arrayList = new ArrayList<>();
    arrayList =
        SshClientUtils.execute(
            new SFtpClientTemplate<ArrayList<HybridFileParcelable>>(path) {
              @Override
              public ArrayList<HybridFileParcelable> execute(SFTPClient client) {
                ArrayList<HybridFileParcelable> retval = new ArrayList<>();
                try {
                  for (RemoteResourceInfo info :
                      client.ls(SshClientUtils.extractRemotePathFrom(path))) {
                    boolean isDirectory = false;
                    try {
                      isDirectory = SshClientUtils.isDirectory(client, info);
                    } catch (IOException ifBrokenSymlink) {
                      Log.w(TAG, "IOException checking isDirectory(): " + info.getPath());
                      continue;
                    }
                    HybridFileParcelable f = new HybridFileParcelable(path, isDirectory, info);
                    retval.add(f);
                  }
                } catch (IOException e) {
                  Log.w("DEBUG.listFiles", "IOException", e);
                }
                return retval;
              }
            });
    return arrayList;
  }

  @Override
  public String getReadablePath(String path) {
    return parseAndFormatUriForDisplay(path);
  }

  /**
   * Handles getting input stream for various {@link OpenMode}
   *
   * @deprecated use {@link #getInputStream(Context)} which allows handling content resolver
   */
  @Nullable
  public InputStream getInputStream() {
    return SshClientUtils.execute(
        new SFtpClientTemplate<InputStream>(path) {
          @Override
          public InputStream execute(SFTPClient client) throws IOException {
            final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path));
            return rf.new RemoteFileInputStream() {
              @Override
              public void close() throws IOException {
                try {
                  super.close();
                } finally {
                  rf.close();
                }
              }
            };
          }
        });
  }

  @Nullable
  public InputStream getInputStream(Context context) {
    InputStream inputStream;
    inputStream =
        SshClientUtils.execute(
            new SFtpClientTemplate<InputStream>(path, false) {
              @Override
              public InputStream execute(final SFTPClient client) throws IOException {
                final RemoteFile rf = client.open(SshClientUtils.extractRemotePathFrom(path));
                return rf.new RemoteFileInputStream() {
                  @Override
                  public void close() throws IOException {
                    try {
                      super.close();
                    } finally {
                      rf.close();
                      client.close();
                    }
                  }
                };
              }
            });
    return inputStream;
  }

  @Nullable
  public OutputStream getOutputStream(Context context) {
    OutputStream outputStream;

        return SshClientUtils.execute(
            new SshClientTemplate<OutputStream>(path, false) {
              @Override
              public OutputStream execute(final SSHClient ssh) throws IOException {
                final SFTPClient client = ssh.newSFTPClient();
                final RemoteFile rf =
                    client.open(
                        SshClientUtils.extractRemotePathFrom(path),
                        EnumSet.of(
                            net.schmizz.sshj.sftp.OpenMode.WRITE,
                            net.schmizz.sshj.sftp.OpenMode.CREAT));
                return rf.new RemoteFileOutputStream() {
                  @Override
                  public void close() throws IOException {
                    try {
                      super.close();
                    } finally {
                      try {
                        rf.close();
                        client.close();
                      } catch (Exception e) {
                        Log.w(TAG, "Error closing stream", e);
                      }
                    }
                  }
                };
              }
            });
  }

  public boolean exists() {
    boolean exists = false;

    final Boolean executionReturn =
        SshClientUtils.<Boolean>execute(
            new SFtpClientTemplate<Boolean>(path) {
              @Override
              public Boolean execute(SFTPClient client) throws IOException {
                try {
                  return client.stat(SshClientUtils.extractRemotePathFrom(path)) != null;
                } catch (SFTPException notFound) {
                  return false;
                }
              }
            });

    if (executionReturn == null) {
      Log.e(TAG, "Error obtaining existance of file over SFTP");
    }

    //noinspection SimplifiableConditionalExpression
    exists = executionReturn == null ? false : executionReturn;

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

  public boolean setLastModified(final long date) {
    File f = getFile();
    return f.setLastModified(date);
  }

  public void mkdir(Context context) {
    SshClientUtils.execute(
        new SFtpClientTemplate<Void>(path) {
          @Override
          public Void execute(@NonNull SFTPClient client) {
            try {
              client.mkdir(SshClientUtils.extractRemotePathFrom(path));
            } catch (IOException e) {
              Log.e(TAG, "Error making directory over SFTP", e);
            }
            // FIXME: anything better than throwing a null to make Rx happy?
            return null;
          }
        });
  }

  public boolean delete(Context context, boolean rootmode)
      throws ShellNotRunningException, SmbException {
    Boolean retval =
        SshClientUtils.<Boolean>execute(
            new SFtpClientTemplate(path) {
              @Override
              public Boolean execute(@NonNull SFTPClient client) throws IOException {
                String _path = SshClientUtils.extractRemotePathFrom(path);
                if (isDirectory(AppConfig.getInstance())) client.rmdir(_path);
                else client.rm(_path);
                return client.statExistence(_path) == null;
              }
            });
    return retval != null && retval;
  }

  @Override
  public Void mkkdir(Context context) {
    mkdir(context);
    return null;
  }
}
