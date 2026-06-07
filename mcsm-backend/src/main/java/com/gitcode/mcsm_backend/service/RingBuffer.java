package com.gitcode.mcsm_backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 线程安全的环形缓冲区，用于存储服务器控制台日志
 */
public class RingBuffer {

    private final int capacity;
    private final LogLine[] buffer;
    private final AtomicLong lineCounter;
    private final ReadWriteLock lock;
    private int writePos;
    private int count;

    public RingBuffer() {
        this(1000);
    }

    public RingBuffer(int capacity) {
        this.capacity = Math.max(capacity, 100);
        this.buffer = new LogLine[this.capacity];
        this.lineCounter = new AtomicLong(0);
        this.lock = new ReentrantReadWriteLock();
        this.writePos = 0;
        this.count = 0;
    }

    /**
     * 追加一行日志
     */
    public void addLine(String content) {
        long lineNum = lineCounter.getAndIncrement();
        LogLine logLine = new LogLine(lineNum, content);
        lock.writeLock().lock();
        try {
            buffer[writePos] = logLine;
            writePos = (writePos + 1) % capacity;
            if (count < capacity) count++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取指定行号之后的所有新行（用于增量拉取）
     * @param sinceLineNumber 起始行号（不含），-1 表示获取最近 100 行
     */
    public ConsoleData getLinesSince(long sinceLineNumber) {
        lock.readLock().lock();
        try {
            List<LogLine> result = new ArrayList<>();
            long latestLine = lineCounter.get() - 1;

            if (count == 0) {
                return new ConsoleData(result, latestLine);
            }

            // since == -1：首次拉取，返回最近 100 行
            if (sinceLineNumber < 0) {
                int fetchCount = Math.min(100, count);
                int startPos = count < capacity ? Math.max(0, count - fetchCount) : (writePos - fetchCount + capacity) % capacity;
                for (int i = 0; i < fetchCount; i++) {
                    int idx = (startPos + i) % capacity;
                    LogLine line = buffer[idx];
                    if (line != null) result.add(line);
                }
                return new ConsoleData(result, latestLine);
            }

            // 遍历缓冲区，找出 sinceLineNumber 之后的行
            int startPos = count < capacity ? 0 : writePos;
            for (int i = 0; i < count; i++) {
                int idx = (startPos + i) % capacity;
                LogLine line = buffer[idx];
                if (line != null && line.lineNumber > sinceLineNumber) {
                    result.add(line);
                }
            }
            return new ConsoleData(result, latestLine);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ========== 内部类 ==========

    /** 单行日志 */
    public record LogLine(long lineNumber, String content) {}

    /** 拉取结果 */
    public record ConsoleData(List<LogLine> lines, long latestLineNumber) {}
}
