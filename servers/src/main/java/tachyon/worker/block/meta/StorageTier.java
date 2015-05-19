/*
 * Licensed to the University of California, Berkeley under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package tachyon.worker.block.meta;

import java.util.HashSet;
import java.util.Set;

import tachyon.Constants;
import tachyon.conf.TachyonConf;
import tachyon.util.CommonUtils;

/**
 * Represents a tier of storage, for example memory or SSD. The methods provided by this class are
 * thread safe.
 */
public class StorageTier {
  private Set<StorageDir> mStorageDirs;

  public StorageTier(TachyonConf tachyonConf, int tier) {

    String tierDirPathConf =
        String.format(Constants.WORKER_TIERED_STORAGE_LEVEL_DIRS_PATH_FORMAT, tier);
    String[] dirPaths = tachyonConf.get(tierDirPathConf, "/mnt/ramdisk").split(",");

    String tierDirCapacityConf =
        String.format(Constants.WORKER_TIERED_STORAGE_LEVEL_DIRS_QUOTA_FORMAT, tier);
    String[] dirQuotas = tachyonConf.get(tierDirCapacityConf, "0").split(",");

    mStorageDirs = new HashSet<StorageDir>(dirPaths.length);

    for (int i = 0; i < dirPaths.length; i ++) {
      int index = i >= dirQuotas.length ? dirQuotas.length - 1 : i;
      long capacity = CommonUtils.parseSpaceSize(dirQuotas[index]);
      mStorageDirs.add(new StorageDir(capacity, dirPaths[i]));
    }
  }

  public BlockMeta createBlock(long userId, long blockId, long blockSize) {
    for (StorageDir dir : mStorageDirs) {
      BlockMeta newBlock = dir.createBlock(userId, blockId, blockSize);
      if (newBlock != null) {
        return newBlock;
      }
    }
    return null;
  }
}
