package com.einmalfel.podlisten;

import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Storage {
  private static final String TAG = "STR";
  private static final String UNKNOWN_STATE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
      Environment.MEDIA_UNKNOWN : "unknown";
  private final File appFilesDir; // /*/Android/data/com.einmalfel.podlisten/files

  @NonNull
  public static List<Storage> getAvailableStorages() {
    List<Storage> result = new LinkedList<>();
    Set<File> dirs = new HashSet<>(Arrays.asList(
        ContextCompat.getExternalFilesDirs(PodListenApp.getContext(), null)));
    for (String env : new String[]{"EXTERNAL_STORAGE", "SECONDARY_STORAGE",
                                   "EXTERNAL_SDCARD_STORAGE", "SECOND_VOLUME_STORAGE",
                                   "THIRD_VOLUME_STORAGE"}) {
      String value = System.getenv(env);
      if (!TextUtils.isEmpty(value)) {
        for (String path : value.split(":")) {
          File storageDir = new File(path);
          if (storageDir.isDirectory()) {
            File filesDir = new File(storageDir, "Android/data/com.einmalfel.podlisten/files");
            if (dirs.add(filesDir)) {
              Log.i(TAG, "Found storage via environment variable: " + filesDir);
            }
          }
        }
      }
    }
    for (File dir : dirs) {
      Log.i(TAG, "Available storage: " + dir);
      if (dir != null) { //getExternalFilesDir could return nulls for currently unavailable storages
        try {
          Storage storage = new Storage(dir);
          if (storage.isAvailableRW()) {
            result.add(storage);
          }
        } catch (IOException e) {
          Log.e(TAG, "File path couldn't be converted to canonical form:" + dir.getAbsolutePath(),
                e);
        }
      }
    }
    return result;
  }

  @NonNull
  public static Storage getPrimaryStorage() {
    try {
      return new Storage(Environment.getExternalStorageDirectory());
    } catch (IOException exception) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        throw new AssertionError("Cant convert primary storage to canonical form", exception);
      } else {
        throw new AssertionError("Cant convert primary storage to canonical form");
      }
    }
  }

  /**
   * @param path directory where audio and image data will be stored
   * @throws IOException if given path couldn't be converted to canonical form
   */
  Storage(@NonNull File path) throws IOException {
    this.appFilesDir = path.getCanonicalFile();
  }

  public void createSubdirs() throws IOException {
    for (File dir : new File[] {getImagesDir(), getPodcastDir()}) {
      if (!dir.exists() && !dir.mkdirs()) {
        throw new IOException("Failed to create " + dir);
      }
    }
  }

  @NonNull
  public File getPodcastDir() {
    return new File(appFilesDir, Environment.DIRECTORY_PODCASTS);
  }

  @NonNull
  public File getImagesDir() {
    return new File(appFilesDir, Environment.DIRECTORY_PICTURES);
  }

  /**
   * Checks whether given file belongs to this storage
   * @throws IOException if File path couldn't be converted to canonical form
   */
  public boolean contains(File file) throws IOException{
    File cFile = file.getCanonicalFile();
    return appFilesDir.equals(cFile) || cFile.getPath().startsWith(appFilesDir + File.separator);
  }

  public boolean isPrimaryStorage() {
    Storage prim = getPrimaryStorage();
    return equals(prim) || appFilesDir.getPath().startsWith(prim.appFilesDir + File.separator);
  }

  public boolean isAvailableRead() {
    String state = getState();
    return Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) ||
        Environment.MEDIA_MOUNTED.equals(state) || UNKNOWN_STATE.equals(state);
  }

  public boolean isAvailableRW() {
    String state = getState();
    return Environment.MEDIA_MOUNTED.equals(state) || UNKNOWN_STATE.equals(state);
  }

  @NonNull
  private String getState() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return Environment.getExternalStorageState(appFilesDir);
    } else {
      return isPrimaryStorage() ? Environment.getExternalStorageState() : UNKNOWN_STATE;
    }
  }

  public boolean isRemovable() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return Environment.isExternalStorageRemovable(appFilesDir);
    } else {
      return isPrimaryStorage() ? Environment.isExternalStorageRemovable() : true;
    }
  }

  @NonNull
  @Override
  public String toString() {
    return appFilesDir.getAbsolutePath();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Storage storage = (Storage) o;

    // it safe to compare these file paths, as they were converted to canonical form in constructor
    return appFilesDir.equals(storage.appFilesDir);
  }

  @Override
  public int hashCode() {
    return appFilesDir.hashCode();
  }
}
