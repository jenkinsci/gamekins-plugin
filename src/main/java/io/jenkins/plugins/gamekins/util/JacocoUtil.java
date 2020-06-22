package io.jenkins.plugins.gamekins.util;

import hudson.FilePath;
import hudson.model.Run;
import hudson.tasks.junit.TestResultAction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class JacocoUtil {

    public static HashMap<String, UUID> classMapping = new HashMap<>();

    private JacocoUtil() {}

    public static double getProjectCoverage(String workspace, String csvName) {
        ArrayList<FilePath> files = getFilesInAllSubDirectories(workspace, csvName);
        List<List<String>> records = new ArrayList<>();
        int instructionCount = 0;
        int coveredInstructionCount = 0;
        for (FilePath file : files) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file.getRemote()));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    records.add(Arrays.asList(values));
                }
                for (List<String> coverageLine : records ) {
                    //TODO: Improve
                    if (!coverageLine.get(2).equals("CLASS")) {
                        coveredInstructionCount += Double.parseDouble(coverageLine.get(4));
                        instructionCount += (Double.parseDouble(coverageLine.get(3))
                                + Double.parseDouble(coverageLine.get(4)));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return coveredInstructionCount / (double) instructionCount;
    }

    public static double getCoverageInPercentageFromJacoco(String className, File csv) {
        List<List<String>> records = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(csv));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
            for (List<String> coverageLine : records ) {
                //TODO: Improve
                if (className.contains(coverageLine.get(2))) {
                    return Double.parseDouble(coverageLine.get(4))
                            / (Double.parseDouble(coverageLine.get(3))
                            + Double.parseDouble(coverageLine.get(4)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public static int getTestCount(HashMap<String, String> constants, Run<?, ?> run) {
        if (run != null) {
            TestResultAction action = run.getAction(TestResultAction.class);
            if (action != null) {
                return action.getTotalCount();
            }
        }
        if (constants == null) return 0;
        return getTestCount(constants);
    }

    public static int getTestCount(HashMap<String, String> constants) {
        try {
            List<FilePath> files = getFilesInAllSubDirectories(constants.get("workspace"), "TEST-.+\\.xml");
            int testCount = 0;
            for (FilePath file : files) {
                StringBuilder xml = new StringBuilder();
                try (Stream<String> stream = Files.lines( Paths.get(file.getRemote()), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> xml.append(s).append("\n"));
                }

                Document document = Jsoup.parse(xml.toString(), "", Parser.xmlParser());
                Elements elements = document.select("testsuite");
                testCount += Integer.parseInt(elements.first().attr("tests"));
            }
            return testCount;
        } catch (IOException ignored) { }

        return 0;
    }

    private static ArrayList<FilePath> getFilesInAllSubDirectories(String directory, String regex) {
        FilePath rootPath = new FilePath(new File(directory));
        ArrayList<FilePath> files = new ArrayList<>();
        try {
            for (FilePath path : rootPath.list()) {
                if (path.isDirectory()) {
                    files.addAll(getFilesInAllSubDirectories(path.getRemote(), regex));
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

    public static Document generateDocument(String filePath, String charset) throws IOException {
        return Jsoup.parse(new File(filePath), charset);
    }

    public static Document generateDocument(File file, String charset) throws IOException {
        return Jsoup.parse(file, charset);
    }

    public static Elements getLines(File jacocoSourceFile) throws IOException {
        Document document = Jsoup.parse(jacocoSourceFile, "UTF-8");
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

    public static ArrayList<CoverageMethod> getNotFullyCoveredMethodEntries(File jacocoMethodFile) throws IOException {
        ArrayList<CoverageMethod> methods = getMethodEntries(jacocoMethodFile);
        methods.removeIf(method -> method.missedLines == 0);
        return methods;
    }

    public static ArrayList<CoverageMethod> getMethodEntries(File jacocoMethodFile) throws IOException {
        Elements elements = generateDocument(jacocoMethodFile.getPath(), "UTF-8").select("tr");
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

    public static class ClassDetails {

        final String className;
        final String extension;
        final String packageName;
        final File jacocoMethodFile;
        final File jacocoSourceFile;
        final File jacocoCSVFile;
        final File file;
        final double coverage;
        final UUID id;

        /**
         *
         * @param workspace Workspace of the project
         * @param shortFilePath Path of the file, starting in the workspace root directory
         * @param shortJacocoPath Path of the JaCoCo root directory, beginning with ** / (without space)
         * @param shortJacocoCSVPath Path of the JaCoCo csv file, beginning with ** / (without space)
         */
        public ClassDetails(String workspace,
                            String shortFilePath,
                            String shortJacocoPath,
                            String shortJacocoCSVPath) {
            ArrayList<String> pathSplit = new ArrayList<>(Arrays.asList(shortFilePath.split("/")));
            this.className = pathSplit.get(pathSplit.size() - 1).split("\\.")[0];
            this.extension = pathSplit.get(pathSplit.size() - 1).split("\\.")[1];
            this.packageName = computePackageName(shortFilePath);
            StringBuilder jacocoPath = new StringBuilder(workspace);
            int i = 0;
            while (!pathSplit.get(i).equals("src")) {
                if (!pathSplit.get(i).isEmpty()) jacocoPath.append("/").append(pathSplit.get(i));
                i++;
            }
            this.jacocoCSVFile = new File(jacocoPath + shortJacocoCSVPath.substring(2));
            jacocoPath.append(shortJacocoPath.substring(2)).append(this.packageName).append("/");
            this.jacocoMethodFile = new File(jacocoPath + this.className + ".html");
            this.jacocoSourceFile = new File(jacocoPath + this.className + "." + this.extension + ".html");
            this.file = new File(workspace + shortFilePath);
            this.coverage = getCoverageInPercentageFromJacoco(this.className, this.jacocoCSVFile);
            if (JacocoUtil.classMapping.containsKey(this.className)) {
                this.id = JacocoUtil.classMapping.get(this.className);
            } else {
                this.id = UUID.randomUUID();
                JacocoUtil.classMapping.put(this.className, this.id);
            }
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

        public File getFile() {
            return file;
        }

        public double getCoverage() {
            return coverage;
        }

        public UUID getId() {
            return id;
        }
    }
}
