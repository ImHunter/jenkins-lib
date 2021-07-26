package ru.pulsar.jenkins.library

import org.jenkinsci.plugins.pipeline.utility.steps.fs.FileWrapper
import org.jenkinsci.plugins.workflow.support.actions.EnvironmentAction

interface IStepExecutor {

    boolean isUnix()
    
    int sh(String script, boolean returnStatus, String encoding)
    
    int bat(String script, boolean returnStatus, String encoding)

    String libraryResource(String path)

    FileWrapper[] findFiles(String glob)

    FileWrapper[] findFiles(String glob, String excludes)

    String readFile(String file, String encoding)

    void echo(message)

    int cmd(String script, boolean returnStatus)

    int cmd(String script)

    void tool(String toolName)

    void withSonarQubeEnv(String installationName, Closure body)

    EnvironmentAction env()

    void createDir(String path)

    def withEnv(List<String> strings, Closure body)

    def archiveArtifacts(String path)

    def stash(String name, String includes)

    def stash(String name, String includes, boolean allowEmpty)

    def unstash(String name)

    def zip(String dir, String zipFile)

    def zip(String dir, String zipFile, String glob)

    def zip(String dir, String zipFile, String glob, boolean archive)

    def unzip(String dir, String zipFile)

    def unzip(String dir, String zipFile, quiet)

    def catchError(Closure body)

    def httpRequest(String url, String outputFile, String responseHandle, boolean wrapAsMultipart)

    def error(String errorMessage)

    def allure(List<String> results)

    def installLocalDependencies()
}