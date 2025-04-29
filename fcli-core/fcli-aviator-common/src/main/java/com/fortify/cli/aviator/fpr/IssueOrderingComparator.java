package com.fortify.cli.aviator.fpr;

import java.util.Comparator;

import com.fortify.cli.aviator.core.model.UserPrompt;

public class IssueOrderingComparator implements Comparator<UserPrompt> {

    @Override
    public int compare(UserPrompt p1, UserPrompt p2) {
        if (p1 == null && p2 == null) return 0;
        if (p1 == null) return -1;
        if (p2 == null) return 1;

        com.fortify.cli.aviator.core.model.StackTraceElement loc1 = p1.getFirstStackTrace().get(0);
        com.fortify.cli.aviator.core.model.StackTraceElement loc2 = p2.getFirstStackTrace().get(0);

        if (loc1 == null && loc2 == null) return 0;
        if (loc1 == null) return -1;
        if (loc2 == null) return 1;

        String filename1 = getShortFileName(loc1.getFilename());
        String filename2 = getShortFileName(loc2.getFilename());

        boolean filename1Empty = isEmpty(filename1);
        boolean filename2Empty = isEmpty(filename2);

        if (filename1Empty && filename2Empty) {

        } else if (filename1Empty) {
            return -1;
        } else if (filename2Empty) {
            return 1;
        } else {
            int fileCompare = filename1.compareToIgnoreCase(filename2);
            if (fileCompare != 0) {
                return fileCompare;
            }
        }

        int line1 = loc1.getLine();
        int line2 = loc2.getLine();
        return Integer.compare(line1, line2);
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String getShortFileName(String fullPath) {
        if (isEmpty(fullPath)) {
            return "";
        }
        int lastSeparatorPos = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));

        if (lastSeparatorPos > -1 && lastSeparatorPos < fullPath.length() - 1) {
            return fullPath.substring(lastSeparatorPos + 1);
        } else {
            return fullPath;
        }
    }
}
