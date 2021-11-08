package com.amaze.filemanager.filesystem.hybridfile;

import android.content.Context;
import android.util.Log;

import com.amaze.filemanager.filesystem.HybridFile;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class SMBfile extends HybridFile.InnerFile {

  @Override
  public String getName(Context context) {
    SmbFile smbFile = getSmbFile();
    if (smbFile != null) {
      return smbFile.getName();
    }
    return null;
  }

  @Override
  public String getSimpleName(){
    SmbFile smbFile = getSmbFile();
    if (smbFile != null) return smbFile.getName();
    return null;
  }

  @Override
  public String getParent(Context context){
    SmbFile smbFile = getSmbFile();
    if (smbFile != null) {
      return smbFile.getParent();
    }
    return "";
  }

  @Override
  public boolean isDirectory(){
    boolean isDirectory = false;
    SmbFile smbFile = getSmbFile();
    try {
      isDirectory = smbFile != null && smbFile.isDirectory();
    } catch (SmbException e) {
      e.printStackTrace();
      isDirectory = false;
    }
    return isDirectory;
  }

  @Override
  public long lastModified() {
    SmbFile smbFile = getSmbFile();
    if (smbFile != null) {
      try {
      return smbFile.lastModified();
      } catch (SmbException e) {
        Log.e(TAG, "Error getting last modified time for SMB [" + path + "]", e);
        return 0;
      }
    }
    return new File("/").lastModified();
  }

  @Override
  public long length(Context context) {
    long s = 0L;
    SmbFile smbFile = getSmbFile();
    if (smbFile != null)
      try {
      s = smbFile.length();
      } catch (SmbException e) {
      e.printStackTrace();
      }
    return s;
  }

  @Override
  public boolean isDirectory(Context context) {
    boolean isDirectory;
    try {
      isDirectory =
        Single.fromCallable(() -> getSmbFile().isDirectory())
          .subscribeOn(Schedulers.io())
          .blockingGet();
    } catch (Exception e) {
      isDirectory = false;
      if (e.getCause() != null) e.getCause().printStackTrace();
      else e.printStackTrace();
    }
    return isDirectory;
  }

  @Override
  public String getReadablePath(String path) {
    return HybridFile.parseAndFormatUriForDisplay(path);
  }
}
