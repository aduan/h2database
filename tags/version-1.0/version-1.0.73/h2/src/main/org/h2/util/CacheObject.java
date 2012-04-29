/*
 * Copyright 2004-2008 H2 Group. Multiple-Licensed under the H2 License, 
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.util;

import java.util.Comparator;

import org.h2.constant.SysProperties;
import org.h2.message.Message;
import org.h2.store.DiskFile;

/**
 * The base object for all cached objects.
 */
public abstract class CacheObject {

    public CacheObject previous, next, chained;
    public int cacheQueue;
    
    /**
     * The number of blocks occupied by this object.
     */
    protected int blockCount;
    private int pos;
    private boolean changed;

    /**
     * Check if the object can be removed from the cache.
     * For example pinned objects can not be removed.
     *
     * @return true if it can be removed
     */
    public abstract boolean canRemove();

    public static void sort(ObjectArray recordList) {
        recordList.sort(new Comparator() {
            public int compare(Object a, Object b) {
                int pa = ((CacheObject) a).getPos();
                int pb = ((CacheObject) b).getPos();
                return pa == pb ? 0 : (pa < pb ? -1 : 1);
            }
        });
    }

    public void setBlockCount(int size) {
        this.blockCount = size;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setPos(int pos) {
        if (SysProperties.CHECK && (previous != null || next != null || chained != null)) {
            throw Message.getInternalError("setPos too late");
        }
        this.pos = pos;
    }

    public int getPos() {
        return pos;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean b) {
        changed = b;
    }

    public boolean isPinned() {
        return false;
    }

    /**
     * Get the estimated memory size.
     *
     * @return number of double words (4 bytes)
     */
    public int getMemorySize() {
        return blockCount * (DiskFile.BLOCK_SIZE / 4);
    }

}