package com.fortify.cli.aviator.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FPRLoadingUtil {

    private static final Logger logger = LoggerFactory.getLogger(FPRLoadingUtil.class);

    public FPRLoadingUtil() {
    }

    private static final Pattern AUDIT_FVDL_PATTERN = Pattern.compile("^audit\\.fvdl$");
    private static final Pattern SRC_FILE_PATTERN = Pattern.compile("^src(-archive)?(/|(\\\\+))(?!index.xml|ScanUUID).+");

    public static boolean hasSource(File project) throws IOException {
        boolean foundSource = false;
        try (ZipFile projectZip = new ZipFile(project)) {
            Enumeration<? extends ZipEntry> entries = projectZip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry next = entries.nextElement();
                if (next != null && !next.isDirectory()) {
                    String nextName = next.getName();
                    if (nextName != null && SRC_FILE_PATTERN.matcher(nextName).matches()) {
                        foundSource = true;
                        break;
                    }
                }
            }
        }
        return foundSource;
    }

    public static boolean isValidFpr(String fprPath) {
        File fprFile = new File(fprPath);

        if (!fprFile.exists() || !FileUtil.isZipFile(fprFile)) {
            logger.error("FPR file does not exist or is not a zip file: " + fprPath);
            return false;
        }

        boolean foundAuditFvdl = false;
        try (ZipFile zipFile = new ZipFile(fprFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (AUDIT_FVDL_PATTERN.matcher(entry.getName()).matches()) {
                    foundAuditFvdl = true;
                }
                if (foundAuditFvdl) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error accessing FPR file: " + fprPath, e);
            return false;
        }

        if (!foundAuditFvdl) {
            logger.error("FPR file does not contain audit.fvdl in the root directory: " + fprPath);
        }

        return foundAuditFvdl;
    }
}