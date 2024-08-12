# Droid Graph #

Droid Graph is a Java command line application that generates an extended control flow model of an Android
application. Both static and dynamic analysis techniques are used to model the applications control flow 
including the applications interface elements and callback methods.

#### Publications:
```
@inproceedings{doyle2023modelling,
  title={Modelling Android applications through static analysis and systematic exploratory testing},
  author={Doyle, Jordan and Laurent, Thomas and Ventresque, Anthony},
  booktitle={2023 10th International Conference on Dependable Systems and Their Applications (DSA)},
  pages={94--104},
  year={2023},
  organization={IEEE}
}
```
```
@inproceedings{doyle2024padraig,
  title={PADRAIG: Precise AnDRoid Automated Input Generation},
  author={Doyle, Jordan and Laurent, Thomas and Ventresque, Anthony},
  booktitle={2024 International Conference on Software Quality, Reliability, and Security (QRS)},
  year={2024},
  note={Status: accepted and presented at QRS.}
}
```

### Build & Run ###

This is a Maven project developed in JetBrains Intellij IDE. You can clone this project and open it in JetBrains
Intellij IDE as a maven project, or you can clone the project and build a JAR file using the maven package command
below:

```
$ cd DroidGraph
$ mvn package
```

The maven package command will build a JAR file with dependencies included. Run the project using the JAR file
and the sample input APK using the following commands:

```
$ cd target
$ java -jar DroidGraph-2.0-SNAPSHOT-jar-with-dependencies.jar -a "samples/activity_lifecycle_1.apk" -i "samples/activity_lifecycle_1.gml"
```
