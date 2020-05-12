// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.installations;

import android.content.Context;
import android.util.Log;
import com.google.firebase.installations.local.PersistedInstallationEntry;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/** Use file locking to acquire a lock that will also block other processes. */
class CrossProcessLock {
  private static final String TAG = "CrossProcessLock";

  interface CrossProcessPersistedInstallation {
    PersistedInstallationEntry getPersistedInstallations();
  }

  /**
   * Create a lock that is exclusive across processes. If another process has the lock then this
   * call will block until it is released.
   *
   * @param lockName the lockname is global across all processes of an app
   * @return a CrossProcessLock if success. If the lock failed to acquire (maybe due to disk full or
   *     other unexpected and unsupported permissions) then null will be returned.
   */
  static PersistedInstallationEntry whileLocked(
      Context applicationContext,
      String lockName,
      CrossProcessPersistedInstallation persistedInstallation) {

    File file = new File(applicationContext.getFilesDir(), lockName);
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        FileChannel channel = randomAccessFile.getChannel();
        // Use the file channel to create a lock on the file.
        // This method blocks until it can retrieve the lock.
        FileLock lock = channel.lock()) {

      return persistedInstallation.getPersistedInstallations();
    } catch (IOException | Error e) {
      // Certain conditions can cause file locking to fail, such as out of disk or bad permissions.
      // In any case, the acquire will fail and return null instead of a held lock.
      // NOTE: In Java 7 & 8, FileKey creation failure might wrap IOException into Error. See
      // https://bugs.openjdk.java.net/browse/JDK-8025619 for details.
      Log.e(TAG, "encountered error while creating and acquiring the lock, ignoring", e);

      return null;
    }
  }
}
