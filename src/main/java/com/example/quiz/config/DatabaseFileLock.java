package com.example.quiz.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class DatabaseFileLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseFileLock.class);
    private final Environment environment;
    private FileChannel channel;
    private FileLock lock;

    public DatabaseFileLock(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void acquireLock() {
        String url = environment.getProperty("spring.datasource.url", "");
        if (!url.startsWith("jdbc:h2:file:")) {
            return;
        }
        String rawPath = url.substring("jdbc:h2:file:".length());
        int semicolon = rawPath.indexOf(';');
        if (semicolon >= 0) {
            rawPath = rawPath.substring(0, semicolon);
        }
        Path dbPath = Paths.get(rawPath);
        if (!dbPath.isAbsolute()) {
            dbPath = Paths.get(System.getProperty("user.dir")).resolve(dbPath).normalize();
        }
        Path lockPath = Paths.get(dbPath.toString() + ".process.lock");
        try {
            channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null) {
                throw new IllegalStateException("数据库文件正在被其他进程使用: " + dbPath);
            }
            LOGGER.info("Database lock acquired: {}", lockPath);
        } catch (Exception e) {
            throw new IllegalStateException("无法获取数据库锁: " + lockPath, e);
        }
    }

    @PreDestroy
    public void releaseLock() {
        try {
            if (lock != null && lock.isValid()) {
                lock.release();
            }
        } catch (Exception ignored) {
        }
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception ignored) {
        }
    }
}
