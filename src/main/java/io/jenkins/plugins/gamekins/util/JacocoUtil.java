package io.jenkins.plugins.gamekins.util;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResultAction;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class JacocoUtil {

    private JacocoUtil() {}

    public static double getProjectCoverage(FilePath workspace, String csvName) {
        ArrayList<FilePath> files;
        try {
            files = workspace.act(new FilesOfAllSubDirectoriesCallable(workspace, csvName));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return 0.0;
        }

        int instructionCount = 0;
        int coveredInstructionCount = 0;
        for (FilePath file : files) {
            try {
                String content = file.readToString();
                String[] lines = content.split("\n");
                for (String coverageLine : lines ) {
                    //TODO: Improve
                    List<String> entries =  Arrays.asList(coverageLine.split(","));
                    if (!entries.get(2).equals("CLASS")) {
                        coveredInstructionCount += Double.parseDouble(entries.get(4));
                        instructionCount += (Double.parseDouble(entries.get(3))
                                + Double.parseDouble(entries.get(4)));
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        return coveredInstructionCount / (double) instructionCount;
    }

    public static double getCoverageInPercentageFromJacoco(String className, FilePath csv) {
        try {
            String content = csv.readToString();
            String[] lines = content.split("\n");
            for (String coverageLine : lines ) {
                //TODO: Improve
                List<String> entries =  Arrays.asList(coverageLine.split(","));
                if (className.contains(entries.get(2))) {
                    return Double.parseDouble(entries.get(4))
                            / (Double.parseDouble(entries.get(3))
                            + Double.parseDouble(entries.get(4)));
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static int getTestCount(FilePath workspace, Run<?, ?> run) {
        if (run != null) {
            TestResultAction action = run.getAction(TestResultAction.class);
            if (action != null) {
                return action.getTotalCount();
            }
        }
        if (workspace == null) return 0;
        return getTestCount(workspace);
    }

    public static int getTestCount(FilePath workspace) {
        try {
            List<FilePath> files = workspace.act(
                    new JacocoUtil.FilesOfAllSubDirectoriesCallable(workspace, "TEST-.+\\.xml"));
            int testCount = 0;
            for (FilePath file : files) {
                Document document = Jsoup.parse(file.readToString(), "", Parser.xmlParser());
                Elements elements = document.select("testsuite");
                testCount += Integer.parseInt(elements.first().attr("tests"));
            }
            return testCount;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static ArrayList<FilePath> getFilesInAllSubDirectories(FilePath directory, String regex) {
        ArrayList<FilePath> files = new ArrayList<>();
        try {
            for (FilePath path : directory.list()) {
                if (path.isDirectory()) {
                    files.addAll(getFilesInAllSubDirectories(path, regex));
                } else {
                    if (path.getName().matches(regex)) files.add(path);
                }
            }
        } catch (IOException | InterruptedException ignored) {
            return new ArrayList<>();
        }
        return files;
    }

    public static int calculateCoveredLines(Document document, String modifier) {
        Elements elements = document.select("span." + modifier);
        return elements.size();
    }

    public static Document generateDocument(FilePath file) throws IOException, InterruptedException {
        return Jsoup.parse(file.readToString());
    }

    public static Elements getLines(FilePath jacocoSourceFile) throws IOException, InterruptedException {
        Document document = Jsoup.parse(jacocoSourceFile.readToString());
        Elements elements = document.select("span." + "pc");
        elements.addAll(document.select("span." + "nc"));
        elements.removeIf(e -> e.text().contains("{")
                || e.text().contains("}")
                || e.text().contains("class")
                || e.text().contains("void")
                || e.text().contains("public")
                || e.text().contains("private")
                || e.text().contains("protected")
                || e.text().contains("static")
                || e.text().equals("(")
                || e.text().equals(")"));
        return elements;
    }

    public static ArrayList<CoverageMethod> getNotFullyCoveredMethodEntries(FilePath jacocoMethodFile)
            throws IOException, InterruptedException {
        ArrayList<CoverageMethod> methods = getMethodEntries(jacocoMethodFile);
        methods.removeIf(method -> method.missedLines == 0);
        return methods;
    }

    public static ArrayList<CoverageMethod> getMethodEntries(FilePath jacocoMethodFile)
            throws IOException, InterruptedException {
        Elements elements = generateDocument(jacocoMethodFile).select("tr");
        ArrayList<CoverageMethod> methods = new ArrayList<>();
        for (Element element : elements) {
            boolean matches = false;
            for (Node node : element.childNodes()) {
                for (Attribute attribute : node.attributes()){
                    if (attribute.getKey().equals("id") && attribute.getValue().matches("a\\d+")) {
                        matches = true;
                        break;
                    }
                }
                if (matches) break;
            }
            if (matches) {
                String methodName = "";
                int lines = 0;
                int missedLines = 0;
                for (Node node : element.childNodes()) {
                    for (Attribute attribute : node.attributes()){
                        if (attribute.getKey().equals("id")) {
                            if (attribute.getValue().matches("a\\d+")) {
                                methodName = node.childNode(0).childNode(0).toString();
                            } else if (attribute.getValue().matches("h\\d+")) {
                                missedLines = Integer.parseInt(node.childNode(0).toString());
                            } else if (attribute.getValue().matches("i\\d+")) {
                                lines = Integer.parseInt(node.childNode(0).toString());
                            }
                            break;
                        }
                    }
                }
                methods.add(new CoverageMethod(methodName, lines, missedLines));
            }
        }
        return methods;
    }

    static String computePackageName(String shortFilePath) {
        ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(shortFilePath.split("/")));
        StringBuilder packageName = new StringBuilder();
        for (int i = pathSplit.size() - 2; i >= 0; i--) {
            if (pathSplit.get(i).equals("src") || pathSplit.get(i).equals("main") || pathSplit.get(i).equals("java")) {
                packageName = new StringBuilder(packageName.substring(1));
                break;
            }
            packageName.insert(0, "." + pathSplit.get(i));
        }
        return packageName.toString();
    }

    public static FilePath getJacocoFileInMultiBranchProject(Run<?, ?> run, HashMap<String, String> constants,
                                                         FilePath jacocoFile, String oldBranch) {
        if (run.getParent().getParent() instanceof WorkflowMultiBranchProject
                && constants.get("branch").equals(oldBranch)) {
            return new FilePath(jacocoFile.getChannel(), jacocoFile.getRemote().replace(
                    constants.get("projectName") + "_" + oldBranch,
                    constants.get("projectName") + "_" + constants.get("branch")));
        } else {
            return jacocoFile;
        }
    }

    public static FilePath calculateCurrentFilePath(FilePath workspace, File file, String oldWorkspace) {
        if (!oldWorkspace.endsWith("/")) oldWorkspace += "/";
        String remote = workspace.getRemote();
        if (!remote.endsWith("/")) remote += "/";
        return new FilePath(workspace.getChannel(), file.getAbsolutePath().replace(oldWorkspace, remote));
    }

    public static FilePath calculateCurrentFilePath(FilePath workspace, File file) {
        return new FilePath(workspace.getChannel(), file.getAbsolutePath());
    }

    public static class FilesOfAllSubDirectoriesCallable extends MasterToSlaveCallable<ArrayList<FilePath>, IOException> {

        private final FilePath directory;
        private final String regex;

        public FilesOfAllSubDirectoriesCallable(FilePath directory, String regex) {
            this.directory = directory;
            this.regex = regex;
        }

        /**
         * Performs computation and returns the result,
         * or throws some exception.
         */
        @Override
        public ArrayList<FilePath> call() {
            return getFilesInAllSubDirectories(this.directory, this.regex);
        }
    }

    public static class CoverageMethod {

        final String methodName;
        final int lines;
        final int missedLines;

        CoverageMethod (String methodName, int lines, int missedLines) {
            this.methodName = methodName;
            this.lines = lines;
            this.missedLines = missedLines;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getLines() {
            return lines;
        }

        public int getMissedLines() {
            return missedLines;
        }
    }

    public static class ClassDetails implements Serializable {

        final String className;
        final String extension;
        final String packageName;
        final File jacocoMethodFile;
        final File jacocoSourceFile;
        final File jacocoCSVFile;
        final double coverage;
        final ArrayList<GitUtil.GameUser> changedByUsers;
        final String workspace;

        /**
         *
         * @param workspace Workspace of the project
         * @param shortFilePath Path of the file, starting in the workspace root directory
         * @param shortJacocoPath Path of the JaCoCo root directory, beginning with ** / (without space)
         * @param shortJacocoCSVPath Path of the JaCoCo csv file, beginning with ** / (without space)
         */
        public ClassDetails(FilePath workspace,
                            String shortFilePath,
                            String shortJacocoPath,
                            String shortJacocoCSVPath,
                            TaskListener listener) {
            this.workspace = workspace.getRemote();
            ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(shortFilePath.split("/")));
            this.className = pathSplit.get(pathSplit.size() - 1).split("\\.")[0];
            this.extension = pathSplit.get(pathSplit.size() - 1).split("\\.")[1];
            this.packageName = computePackageName(shortFilePath);
            StringBuilder jacocoPath = new StringBuilder(workspace.getRemote());
            int i = 0;
            while (!pathSplit.get(i).equals("src")) {
                if (!pathSplit.get(i).isEmpty()) jacocoPath.append("/").append(pathSplit.get(i));
                i++;
            }
            this.jacocoCSVFile = new File(jacocoPath + shortJacocoCSVPath.substring(2));
            if (!this.jacocoCSVFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCoCSVPath: " + this.jacocoCSVFile.getAbsolutePath());
            }
            jacocoPath.append(shortJacocoPath.substring(2));
            if (!jacocoPath.toString().endsWith("/")) jacocoPath.append("/");
            jacocoPath.append(this.packageName).append("/");
            this.jacocoMethodFile = new File(jacocoPath + this.className + ".html");
            if (!this.jacocoMethodFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCoMethodPath: "
                        + this.jacocoMethodFile.getAbsolutePath());
            }
            this.jacocoSourceFile = new File(jacocoPath + this.className + "." + this.extension + ".html");
            if (!this.jacocoSourceFile.exists()) {
                listener.getLogger().println("[Gamekins] JaCoCoSourcePath: "
                        + this.jacocoSourceFile.getAbsolutePath());
            }
            this.coverage = getCoverageInPercentageFromJacoco(this.className,
                    calculateCurrentFilePath(workspace, this.jacocoCSVFile));
            this.changedByUsers = new ArrayList<>();
        }

        public String getClassName() {
            return className;
        }

        public String getExtension() {
            return extension;
        }

        public String getPackageName() {
            return packageName;
        }

        public File getJacocoMethodFile() {
            return jacocoMethodFile;
        }

        public File getJacocoSourceFile() {
            return jacocoSourceFile;
        }

        public File getJacocoCSVFile() {
            return jacocoCSVFile;
        }

        public double getCoverage() {
            return coverage;
        }

        public ArrayList<GitUtil.GameUser> getChangedByUsers() {
            return this.changedByUsers;
        }

        public void addUser(GitUtil.GameUser user) {
            this.changedByUsers.add(user);
        }

        public String getWorkspace() {
            return this.workspace;
        }
    }
}
