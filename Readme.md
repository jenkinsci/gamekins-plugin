## Gamekins Plugin
This is the repository of the Gamekins Plugin for Jenkins to add Gamifcation to Jenkins. 

### Software requirements
- Java Version 8 or 11
- Apache Maven

### Project requirements
- Java or Kotlin
- JaCoCo
- JUnit

### Development
Clone the project and execute ```mvn hpi:run``` on the command line. This will start a Jenkins on 
http://localhost:8080/jenkins/ with the plugin installed.

The plugin can also be opened by IntelliJ Idea and started by adding a new configuration with the command line 
argument ```hpi:run```. It can also be debugged this way.

### Installation
To install the plugin on a dedicated Jenkins, the Jenkins must have version 2.249.3 or higher. Create the compiled 
plugin by executing ```mvn clean package``` on the command line in the root project folder. The package is located in 
the path ```<project-root>/target/gamekins.hpi```.

On the Jenkins, navigate to the **Manage Plugins** settings in **Manage Jenkins**. Go to the **Advanced** tab and 
upload the plugin. After the first installation, the plugin should work without restarting the Jenkins, whereas an 
update of an existing installation requires a restart of Jenkins.

### Usage

#### Supported project types
 - Freestyle project
 - Maven project
 - Pipeline
 - Multibranch Pipeline
 - Organization folders with Multibranch Pipelines
 
#### Non-Pipeline projects
In the configuration page there is a section about Gamekins. The management of the teams and participants is located 
here as well as the activation of Gamekins. Some new data is only displayed after a reload of the configuration page. 
In addition, the publisher for Gamekins has to be added to the **Post-build Actions** with the following properties:

- Root folder of JaCoCo results: Starting from the project workspace with ```**/``` where the file ```index.html``` 
is located
- Path to jacoco.csv: Starting from the project workspace with ```**/``` where the file ```jacoco.csv``` is located 
(Note: include also the file name since it can be different from ```jacoco.csv```)
- Commits to be searched: Mainly for performance reasons (50 per default)
 
#### Pipeline projects
The configuration is the same as before, except that the call to the publisher must be done in the **Jenkinsfile** 
in the following or similar way (with example values):
 
 ```groovy
pipeline {
    post {
        always {
            gamekins jacocoCSVPath: '**/target/site/jacoco/jacoco.csv', jacocoResultsPath: '**/target/site/jacoco/', searchCommitCount: 50
        }
    }
}
```
 
#### Organization folders
Since a organization folder consists of multiple projects, which configuration cannot (easily) be changed, Gamekins 
has to be enabled for each project. To do this, enable Gamekins for the project in the main configuration page of 
the folder. It can be disabled in the same way and now configured in the desired project as described before.
 
#### Execution
If configured correctly, Gamekins is executed after each run of the project. Every output of the plugin, including 
error messages, will be logged at the end of the **Console Output** of the run.

#### Leaderboard
After activating Gamekins, a new entry named **Leaderboard** on the left side panel of the project is displayed. 
It shows the current points of each participant and team for everyone who can see the project. In addition, each 
signed-in user, who is participating, can see his/her Challenges as well as a help text explaining the basics of 
Gamekins.

#### Statistics
In the background, data about the usage of the project and the participants is logged for evaluation purposes. 
By activating the **Statistics** checkbox, another entry on the left side panel is displayed. Currently, the 
information is not send to the developers, which will come in future with the consens of the owner of the Jenkins 
instance and privacy promises.