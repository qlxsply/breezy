package com.corwin.system.diagnostic.infrastructure.runtime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Corwin 2026/4/16
 */
public class DiagnosticRingBuffer<T> {

    private final ArrayDeque<T> deque = new ArrayDeque<>();
    private int capacity;

    public DiagnosticRingBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    public synchronized void configureCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        trimIfNecessary();
    }

    public synchronized void add(T item) {
        if (item == null) {
            return;
        }
        deque.addLast(item);
        trimIfNecessary();
    }

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

    public synchronized T latest() {
        return deque.peekLast();
    }

    public synchronized void clear() {
        deque.clear();
    }

    private void trimIfNecessary() {
        while (deque.size() > capacity) {
            deque.pollFirst();
        }
    }
}
