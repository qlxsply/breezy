package com.corwin.system.diagnostic.infrastructure.runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A thread-safe ring buffer with configurable capacity that retains the most recently added items.
 *
 * @param <T> the element type
 * @author Corwin 2026/4/16
 */
public class DiagnosticRingBuffer<T> {

    private final ArrayDeque<T> deque = new ArrayDeque<>();
    private int capacity;

    /**
     * Creates a ring buffer with the given initial capacity.
     *
     * @param capacity the maximum number of items to retain
     */
    public DiagnosticRingBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    /**
     * Reconfigures the maximum capacity, trimming excess items if necessary.
     *
     * @param capacity the new capacity
     */
    public synchronized void configureCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        trimIfNecessary();
    }

    /**
     * Adds an item to the end of the buffer, trimming old items if the capacity is exceeded.
     *
     * @param item the item to add; null items are ignored
     */
    public synchronized void add(T item) {
        if (item == null) {
            return;
        }
        deque.addLast(item);
        trimIfNecessary();
    }

    /**
     * Returns the most recent items up to the given limit, in reverse chronological order (newest first).
     *
     * @param limit the maximum number of items to return
     * @return a list of recent items
     */
    public synchronized List<T> latest(int limit) {
        if (deque.isEmpty()) {
            return List.of();
        }
        int size = Math.max(0, limit);
        if (size == 0) {
            size = deque.size();
        }
        List<T> list = new ArrayList<>(deque);
        int fromIndex = Math.max(0, list.size() - size);
        List<T> view = list.subList(fromIndex, list.size());
        List<T> result = new ArrayList<>(view);
        Collections.reverse(result);
        return List.copyOf(result);
    }

    /**
     * Returns the single most recently added item, or null if the buffer is empty.
     *
     * @return the latest item, or null
     */
    public synchronized T latest() {
        return deque.peekLast();
    }

    /**
     * Removes all items from the buffer.
     */
    public synchronized void clear() {
        deque.clear();
    }

    private void trimIfNecessary() {
        while (deque.size() > capacity) {
            deque.pollFirst();
        }
    }
}
