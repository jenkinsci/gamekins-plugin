package io.jenkins.plugins.gamekins.util;

import hudson.FilePath;

import java.util.List;

public class PublisherUtil {

    private PublisherUtil() { }

    public static boolean doCheckJacocoResultsPath(String workspace, String jacocoResultsPath) {
        if (!jacocoResultsPath.endsWith("/")) jacocoResultsPath += "/";
        if (jacocoResultsPath.startsWith("**")) jacocoResultsPath = jacocoResultsPath.substring(2);
        List<FilePath> files = JacocoUtil.getFilesInAllSubDirectories(workspace, "index.html");
        for (FilePath file : files) {
            String path = file.getRemote();
            if (path.substring(0, path.length() - 10).endsWith(jacocoResultsPath)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doCheckJacocoCSVPath(String workspace, String jacocoCSVPath) {
        if (jacocoCSVPath.startsWith("**")) jacocoCSVPath = jacocoCSVPath.substring(2);
        String[] split = jacocoCSVPath.split("/");
        List<FilePath> files = JacocoUtil.getFilesInAllSubDirectories(
                workspace, split[split.length - 1]);
        for (FilePath file : files) {
            if (file.getRemote().endsWith(jacocoCSVPath)) {
                return true;
            }
        }
        return false;
    }
}
