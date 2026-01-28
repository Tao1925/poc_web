package com.example.quiz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class DatabaseBackupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBackupService.class);
    private static final DateTimeFormatter BACKUP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.db-backup.dir:data/backups}")
    private String backupDir;

    @Value("${app.db-backup.retention-days:30}")
    private int retentionDays;

    public DatabaseBackupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${app.db-backup.cron:0 0 2 * * *}")
    public void backupDatabase() {
        Path dir = Paths.get(backupDir);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            LOGGER.error("Database backup failed: cannot create backup dir {}", dir, e);
            return;
        }

        String timestamp = BACKUP_FORMATTER.format(Instant.now());
        String fileName = "pocdb_" + timestamp + ".zip";
        Path backupFile = dir.resolve(fileName).toAbsolutePath().normalize();

        String escapedPath = backupFile.toString().replace("'", "''");
        String sql = "BACKUP TO '" + escapedPath + "'";
        try {
            jdbcTemplate.execute(sql);
            LOGGER.info("Database backup created: {}", backupFile);
        } catch (Exception e) {
            LOGGER.error("Database backup failed: {}", backupFile, e);
            return;
        }

        cleanupOldBackups(dir);
    }

    private void cleanupOldBackups(Path dir) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("pocdb_"))
                    .filter(path -> path.getFileName().toString().endsWith(".zip"))
                    .forEach(path -> {
                        try {
                            Instant modified = Files.getLastModifiedTime(path).toInstant();
                            if (modified.isBefore(cutoff)) {
                                Files.deleteIfExists(path);
                                LOGGER.info("Database backup deleted: {}", path);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Failed to delete backup: {}", path, e);
                        }
                    });
        } catch (Exception e) {
            LOGGER.warn("Failed to cleanup backups in {}", dir, e);
        }
    }
}
