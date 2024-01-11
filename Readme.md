## Gamekins Plugin
This is the repository of the Gamekins Plugin for Jenkins to integrate Gamification into Jenkins.

Gamekins is a neologism consisting of the words Gamification and Jenkins. This describes best what Gamekins is built 
for: The integration of Gamification into Jenkins. Gamekins can be used on project level to add various Gamification 
elements to the Continuous Integration lifecycle of the project. This enables a better and more motivated environment
for developers in the project, especially for testing. Gamekins aims to improve the motivation of developers to test 
their project more thoroughly and therefore to increase the quality of the software.

### Software requirements
- [Java Version 17](https://adoptium.net/temurin/releases/?package=jdk&version=17)
- [Apache Maven](https://maven.apache.org/install.html)

### Project requirements
- [Java](https://adoptium.net/) (fully supported) or [Kotlin](https://kotlinlang.org/) (partially supported)
- [JaCoCo](https://www.eclemma.org/jacoco/)
- [JUnit](https://junit.org/)

### Development
Clone the project and execute ```mvn hpi:run``` on the command line. This will start a Jenkins on
http://localhost:8080/jenkins/ with the plugin installed.

The plugin can also be opened by IntelliJ Idea and started by adding a new configuration with the command line
argument ```hpi:run```. It can also be debugged this way.

Use the file ```Jenkinsfile``` for Continuous Integration without the invocation of Gamekins, 
and ```Jenkinsfile-Gamekins``` with the execution of Gamekins.

### Installation
To install the plugin on a dedicated Jenkins, the Jenkins must have version 2.249.3 or higher. Create the compiled 
plugin by executing ```mvn clean package``` on the command line in the root project folder. The package is located in 
the path ```<project-root>/target/gamekins.hpi```. The packed version can also be found in the current release 
on Github.

To install the plugin on a dedicated Jenkins, the Jenkins must have version 2.249.3 or higher. Create the compiled
plugin by executing ```mvn clean package``` on the command line in the root project folder. The package is located in
the path ```<project-root>/target/gamekins.hpi```.

On the Jenkins, navigate to the **Manage Plugins** settings in **Manage Jenkins**. Go to the **Advanced** tab and upload
the plugin. After the first installation, the plugin should work without restarting the Jenkins, whereas an update of an
existing installation requires a restart of Jenkins.

### Usage
Gamekins support the following project types:
 - Freestyle project
 - Maven project
 - Pipeline
 - Multibranch Pipeline
 - Organization folders with Multibranch Pipelines
 - Folders for grouping projects (Leaderboard only)

#### Non-Pipeline projects
In the configuration page there is a section about Gamekins. The management of the teams and participants is located
here as well as the activation of Gamekins. Some new data is only displayed after a reload of the configuration page. In
addition, the publisher for Gamekins has to be added to the **Post-build Actions** with the following properties:

- Root folder of JaCoCo results: Starting from the project workspace with ```**/``` where the file ```index.html```
  is located (*Mandatory* Property)
- Path to jacoco.csv: Starting from the project workspace with ```**/``` where the file ```jacoco.csv``` is located
  (Note: include also the file name since it can be different from ```jacoco.csv```) (*Mandatory* Property)

#### Pipeline projects
The configuration is the same as before, except that the call to the publisher must be done in the **Jenkinsfile**
in the following or similar way (with example values):
 
```groovy
pipeline {
    post {
        always {
            gamekins jacocoCSVPath: '**/target/site/jacoco/jacoco.csv', jacocoResultsPath: '**/target/site/jacoco/'
        }
    }
}
```

To use the Jenkinsfile, you may need to install additional plugins on your Jenkins.
 
#### Organization folders
Since an organization folder consists of multiple projects, which configuration cannot (easily) be changed, Gamekins 
has to be enabled for each project. To do this, enable Gamekins for the project in the main configuration page of 
the folder. It can be disabled in the same way and now configured in the desired project as described before.

#### Folders
In the configuration of a folder, the Leaderboard can be activated to show the cumulated progress of each user 
participating in one of the sub-projects.

#### Adding teams and users
In the configuration page of the job underneath the checkboxes of the Gamekins section, new teams can be added and 
removed. The newly added team is only displayed after reloading the page. No information will be lost in the Gamekins 
section, even if a popup says otherwise. To add or remove a user to a team, choose both the team and the user in the 
dropdown menus and click on  the corresponding button. Team members can be looked up in the Leaderboard (more later).

#### Advanced options
The Gamekins section in the configuration of a projects provides advanced options. These include the number of
simultaneous current (stored) challenges and quests, the allowance to send challenges, the number of commits to be 
searched and the configuration for PIT.

#### Execution
If configured correctly, Gamekins is executed after each run of the project. Every output of the plugin, including error
messages, will be logged at the end of the **Console Output** of the run.

#### Leaderboard
After activating Gamekins, a new entry named **Leaderboard** on the left side panel of the project is displayed. It
shows the current points of each participant and team for everyone who can see the project. In addition, each signed-in
user, who is participating, can see his/her Challenges as well as a help text explaining the basics of Gamekins.

#### Statistics
In the background, data about the usage of the project and the participants is logged for evaluation purposes. 
By activating the **Statistics** checkbox, another entry on the left side panel is displayed. Currently, the 
information is not sent to the developers, which may come in future with the consent of the owner of the Jenkins 
instance and privacy promises.

#### Additional help
Users who have been added to a team in the job can access a **Help** section in the Leaderboard with a short 
explanation of the Leaderboard itself, and the game in total.

### Extensibility
There are two possible ways to add Challenges to the current version of Gamekins:

#### Pull-Request
You can create a Pull-Request with new Challenges and features, which will be checked by us as soon as possible.

#### Dependency
You can also add Gamekins as a dependencies to your own plugin. 

Add your Challenges to the default Challenges of Gamekins in this way:

```kotlin
org.gamekins.GamePublisherDescriptor.challenges.put(NewChallenge::class.java, weight)
```

The key denotes here the Java Class of the Challenge and the value the weight of the Challenge. Choose
bigger weights for a higher probability that the Challenge is chosen for generation, and lower values for
the opposite. Each third party Challenge must have a constructor with 
```org.gamekins.challenge.Challenge.ChallengeGenerationData``` as only input parameter.

Add your Achievements to the default Achievements of Gamekins in this way:

```kotlin
org.gamekins.GamePublisherDescriptor.challenges.add(achievement)
```

You can of course inherit from ```org.gamekins.achievement.Achievement```, but there is an easier and faster way. 
Use the built-in ```org.gamekins.achievement.AchievementInitializer``` to initialize one or more Achievements from a 
json file with the following format:

```json
[
  {
    "badgePath": "/plugin/<your_plugin>/<path>/file.png",
    "unsolvedBadgePath": "/plugin/gamekins/icons/trophy.png",
    "description": "Solve a CoverageChallenge with at least 80% coverage in the required class",
    "title": "Most of the lines seem familiar",
    "fullyQualifiedFunctionName": "org.gamekins.util.AchievementUtil::haveClassWithXCoverage",
    "secret": false,
    "additionalParameters": {
      "haveCoverage": 0.8
    }
  }
]
```

The ```badgePath``` is the path to the icon for the Achievement if it is solved, ```unsolvedBadgePath``` if not. 
In Jenkins, the files saved in the ```webapp``` folder are available from the Jetty server during runtime. 
In Gamekins the path to the Achievements' icon is ```/plugin/gamekins/icons/trophy.png``` with the real path
```/webapp/icons/trophy.png```. ```description``` and ```title``` are self explaining in this context.

The ```fullyQualifiedFunctionName``` is built in the way ```<full_class_path>::<function_name>```, so that at runtime 
the method to check whether the Achievement is solved, can be found with reflection. You can use the built-in methods 
of Gamekins in the object ```org.gamekins.util.AchievementUtil``` or define your own methods. Keep in mind that each 
method called by the ```isSolved``` method of Achievement must have the following signature:

```kotlin
fun haveClassWithXCoverage(classes: ArrayList<JacocoUtil.ClassDetails>, constants: HashMap<String, String>,
                           run: Run<*, *>, property: GameUserProperty, workspace: FilePath, listener: TaskListener,
                           additionalParameters: HashMap<String, String>): Boolean
```

- The ```classes``` contain all recently changed classes of the current user (that is the user that owns the instance 
of the Achievement)
- The ```constants``` contain information and values about the project with the following keys: **projectName**, 
  **jacocoResultsPath**, **jacocoCSVPath**, **workspace**, **branch**, **projectCoverage**, **projectTests**, 
  **solved** (solved Challenges), **generated** (generated Challenges)
- The ```run``` contains all information about the current Jenkins build
- The ```property``` contains all Gamekins related information for the current user
- The ```workspace``` contains the path and channel to the current workspace
- The ```listener``` is used for logging into the console of the current build
- The ```additionalParameters``` are explained in the next paragraph

Back to the json file, the ```secret``` denotes whether the Achievement is secret and the description should not be 
shown in the Achievements view. The last parameter is the ```additionalParameters``` part, where additional 
key-value-pairs can be defined and propagated to the method defined via ```fullyQualifiedFunctionName```. The value is 
always a String, but it can be converted in the method if necessary.

### Research
This project is funded by the [German Research Foundation](https://www.dfg.de/en/) and developed on the
[Chair of Software Engineering II](https://www.fim.uni-passau.de/lehrstuhl-fuer-software-engineering-ii/) at the 
University of Passau.

### Publications
* "Gamekins: Gamifying Software Testing in Jenkins"
  Philipp Straubinger, Gordon Fraser, ICSE 2022 [[download]](https://arxiv.org/pdf/2202.06562.pdf)
