package io.jenkins.plugins.gamekins.util;

import hudson.FilePath;

import java.io.IOException;
import java.util.List;

public class PublisherUtil {

    private PublisherUtil() { }

    public static boolean doCheckJacocoResultsPath(FilePath workspace, String jacocoResultsPath) {
        if (!jacocoResultsPath.endsWith("/")) jacocoResultsPath += "/";
        if (jacocoResultsPath.startsWith("**")) jacocoResultsPath = jacocoResultsPath.substring(2);
        List<FilePath> files;
        try {
            files = workspace.act(new JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, "index.html"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        for (FilePath file : files) {
            String path = file.getRemote();
            if (path.substring(0, path.length() - 10).endsWith(jacocoResultsPath)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doCheckJacocoCSVPath(FilePath workspace, String jacocoCSVPath) {
        if (jacocoCSVPath.startsWith("**")) jacocoCSVPath = jacocoCSVPath.substring(2);
        String[] split = jacocoCSVPath.split("/");
        List<FilePath> files;
        try {
            files = workspace.act(
                    new JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, split[split.length - 1]));
        } catch (IOException | InterruptedException ignored) {
            return false;
        }
        for (FilePath file : files) {
            if (file.getRemote().endsWith(jacocoCSVPath)) {
                return true;
            }
        }
        return false;
    }
}
