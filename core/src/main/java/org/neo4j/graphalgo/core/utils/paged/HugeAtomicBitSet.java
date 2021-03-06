/*
 * Copyright (c) 2017-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.graphalgo.core.utils.paged;

import org.neo4j.graphalgo.core.utils.BitUtil;
import org.neo4j.graphalgo.core.utils.mem.AllocationTracker;

public final class HugeAtomicBitSet {
    private static final int NUM_BITS = 64;

    private final HugeAtomicLongArray bits;
    private final long numBits;

    public static HugeAtomicBitSet create(long size, AllocationTracker tracker) {
        var wordsSize = BitUtil.ceilDiv(size, NUM_BITS);
        return new HugeAtomicBitSet(HugeAtomicLongArray.newArray(wordsSize, tracker), size);
    }

    private HugeAtomicBitSet(HugeAtomicLongArray bits, long numBits) {
        this.bits = bits;
        this.numBits = numBits;
    }

    /**
     * Returns the state of the bit at the given index.
     */
    public boolean get(long index) {
        assert(index < numBits);
        long wordIndex = index / NUM_BITS;
        int bitIndex = (int) index % NUM_BITS;
        long bitmask = 1L << bitIndex;
        return (bits.get(wordIndex) & bitmask) != 0;
    }

    /**
     * Sets the bit at the given index to true.
     */
    public void set(long index) {
        assert(index < numBits);

        long wordIndex = index / NUM_BITS;
        int bitIndex = (int) index % NUM_BITS;
        long bitmask = 1L << bitIndex;

        while (true) {
            var oldWord = bits.get(wordIndex);
            var newWord = oldWord | bitmask;
            if (bits.compareAndSet(wordIndex, oldWord, newWord)) {
                break;
            }
        }
    }

    /**
     * Sets a bit and returns the previous value.
     * The index should be less than the BitSet size.
     */
    public boolean getAndSet(long index) {
        assert(index < numBits);

        long wordIndex = index / NUM_BITS;
        int bitIndex = (int) index % NUM_BITS;
        long bitmask = 1L << bitIndex;

        while (true) {
            if (!get(index)) {
                var oldWord = bits.get(wordIndex);
                var newWord = oldWord | bitmask;
                if (bits.compareAndSet(wordIndex, oldWord, newWord)) {
                    return false;
                }
            } else {
                return true;
            }
        }
    }

    /**
     * Toggles the bit at the given index.
     */
    public void flip(long index) {
        assert(index < numBits);

        long wordIndex = index / NUM_BITS;
        int bitIndex = (int) index % NUM_BITS;
        long bitmask = 1L << bitIndex;

        while (true) {
            var oldWord = bits.get(wordIndex);
            var newWord = oldWord ^ bitmask;
            if (bits.compareAndSet(wordIndex, oldWord, newWord)) {
                break;
            }
        }
    }

    /**
     * Returns the number of set bits in the bit set.
     * <p>
     * Note: this method is not thread-safe.
     */
    public long cardinality() {
        long setBitCount = 0;

        for (long wordIndex = 0; wordIndex < bits.size() ; wordIndex++) {
            setBitCount += Long.bitCount(bits.get(wordIndex));
        }

        return setBitCount;
    }

    /**
     * Resets all bits in the bit set.
     * <p>
     * Note: this method is not thread-safe.
     */
    public void clear() {
        bits.setAll(0);
    }

    /**
     * Resets the bit at the given index.
     */
    public void clear(long index) {
        assert(index < numBits);

        long wordIndex = index / NUM_BITS;
        int bitIndex = (int) index % NUM_BITS;
        long bitmask = ~(1L << bitIndex);

        while (true) {
            var oldWord = bits.get(wordIndex);
            var newWord = oldWord & bitmask;
            if (bits.compareAndSet(wordIndex, oldWord, newWord)) {
                break;
            }
        }
    }

    /**
     * Returns the number of bits in the bitset.
     */
    public long size() {
        return numBits;
    }
}
