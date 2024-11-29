package ru.pulsar.jenkins.library.steps

import hudson.FilePath
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.CoverageUtils
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.StringJoiner
import ru.pulsar.jenkins.library.utils.VRunner

class SmokeTest implements Serializable, Coverable {

    public static final String ALLURE_STASH = 'smoke-allure'
    public static final String COVERAGE_STASH_NAME = 'smoke-coverage'
    public static final String COVERAGE_STASH_PATH = 'build/out/smoke-coverage.xml'
    public static final String COVERAGE_PIDS_PATH = 'build/smoke-pids'

    private final JobConfiguration config

    SmokeTest(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (!config.stageFlags.smoke) {
            Logger.println("Smoke test step is disabled")
            return
        }

        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            steps.installLocalDependencies()
        }

        def options = config.smokeTestOptions
        def env = steps.env()

        def srcDir = config.srcDir
        def workspaceDir = FileUtils.getFilePath("$env.WORKSPACE")

        String vrunnerPath = VRunner.getVRunnerPath()
        String command = "$vrunnerPath xunit --ibconnection \"/F./build/ib\""

        String vrunnerSettings = options.vrunnerSettings
        if (steps.fileExists(vrunnerSettings)) {
            command += " --settings $vrunnerSettings"
        }

        String xddTestRunnerPath = "./oscript_modules/add/xddTestRunner.epf"
        if (steps.fileExists(xddTestRunnerPath)) {
            command += " --pathxunit $xddTestRunnerPath"
        }

        if (steps.fileExists(options.xddConfigPath)) {
            command += " --xddConfig $options.xddConfigPath"
        }

        String junitReport = "build/out/jUnit/smoke/smoke.xml"
        FilePath pathToJUnitReport = FileUtils.getFilePath("$env.WORKSPACE/$junitReport")
        String junitReportDir = FileUtils.getLocalPath(pathToJUnitReport.getParent())

        String allureReport = "build/out/allure/smoke/allure.xml"
        FilePath pathToAllureReport = FileUtils.getFilePath("$env.WORKSPACE/$allureReport")
        String allureReportDir = FileUtils.getLocalPath(pathToAllureReport.getParent())

        StringJoiner reportsConfigConstructor = new StringJoiner(";")

        if (options.publishToJUnitReport) {
            steps.createDir(junitReportDir)

            String junitReportCommand = "ГенераторОтчетаJUnitXML{$junitReport}"

            reportsConfigConstructor.add(junitReportCommand)
        }

        if (options.publishToAllureReport) {
            steps.createDir(allureReportDir)

            String allureReportCommand = "ГенераторОтчетаAllureXMLВерсия2{$allureReport}"

            reportsConfigConstructor.add(allureReportCommand)
        }

        if (reportsConfigConstructor.length() > 0) {
            String reportsConfig = reportsConfigConstructor.toString()
            command += " --reportsxunit \"$reportsConfig\""
        }

        if (steps.isUnix()) {
            command = command.replace(';', '\\;')
        }

        if (!VRunner.configContainsSetting(vrunnerSettings, "testsPath")) {
            String testsPath = "oscript_modules/add/tests/smoke"
            if (!steps.fileExists(testsPath)) {
                testsPath = '$addRoot/tests/smoke'
                if (steps.isUnix()) {
                    testsPath = '\\' + testsPath
                }
            }
            command += " $testsPath"
        }

        def coverageOpts = config.coverageOptions
        def coverageContext = CoverageUtils.prepareContext(config, options)

        steps.lock(coverageContext.lockableResource) {
            if (coverageContext != null) {
                CoverageUtils.startCoverage(steps, coverageOpts, coverageContext, workspaceDir, srcDir, this)
            }

            steps.withEnv(logosConfig) {
                VRunner.exec(command, true)
            }

            if (coverageContext != null) {
                CoverageUtils.stopCoverage(steps, coverageOpts, coverageContext)
            }
        }

        if (options.publishToAllureReport) {
            steps.stash(ALLURE_STASH, "$allureReportDir/**", true)
            steps.archiveArtifacts("$allureReportDir/**")
        }

        if (options.publishToJUnitReport) {
            steps.junit("$junitReportDir/*.xml", true)
            steps.archiveArtifacts("$junitReportDir/**")
        }

        if (options.coverage) {
            steps.stash(COVERAGE_STASH_NAME, COVERAGE_STASH_PATH, true)
        }

    }

    String getCoverageStashPath() {
        return COVERAGE_STASH_PATH
    }

    String getCoveragePidsPath() {
        return COVERAGE_PIDS_PATH
    }
}
