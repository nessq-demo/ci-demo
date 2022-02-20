package es.us.isa.restest.main;

import es.us.isa.restest.configuration.pojos.TestConfigurationObject;
import es.us.isa.restest.coverage.CoverageGatherer;
import es.us.isa.restest.coverage.CoverageMeter;
import es.us.isa.restest.generators.ARTestCaseGenerator;
import es.us.isa.restest.generators.AbstractTestCaseGenerator;
import es.us.isa.restest.generators.ConstraintBasedTestCaseGenerator;
import es.us.isa.restest.generators.FuzzingTestCaseGenerator;
import es.us.isa.restest.generators.RandomTestCaseGenerator;
import es.us.isa.restest.reporting.AllureReportManager;
import es.us.isa.restest.reporting.StatsReportManager;
import es.us.isa.restest.runners.RESTestRunner;
import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.testcases.writers.IWriter;
import es.us.isa.restest.testcases.writers.RESTAssuredWriter;
import es.us.isa.restest.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static es.us.isa.restest.configuration.TestConfigurationIO.loadConfiguration;
import static es.us.isa.restest.util.FileManager.createDir;
import static es.us.isa.restest.util.FileManager.deleteDir;
import static es.us.isa.restest.util.Timer.TestStep.ALL;

/*
 * This class show the basic workflow of test case generation -> test case execution -> test reporting
 */
public class TestGenerationAndExecution {

	// Properties file with configuration settings
	private static String propertiesFilePath; 
	private static Integer numTestCases; 								// Number of test cases per operation
	private static Integer numGetTestCases; 								// Number of test cases per GET operation
	
	private static String OAISpecPath; 									// Path to OAS specification file
	private static OpenAPISpecification spec; 							// OAS specification
	private static String confPath; 									// Path to test configuration file
	private static String targetDirJava; 								// Directory where tests will be generated.
	private static String packageName; 									// Package name.
	private static String experimentName; 								// Used as identifier for folders, etc.
	private static String testClassName; 								// Name prefix of the class to be generated
	private static Boolean enableInputCoverage; 						// Set to 'true' if you want the input coverage report.
	private static Boolean enableOutputCoverage; 						// Set to 'true' if you want the input coverage report.
	private static Boolean enableCSVStats; 								// Set to 'true' if you want statistics in a CSV file.
	private static Boolean deletePreviousResults; 						// Set to 'true' if you want previous CSVs and Allure reports.
	private static Float faultyRatio; 									// Percentage of faulty test cases to generate. Defaults to 0.1
	private static Integer totalNumTestCases; 							// Total number of test cases to be generated (-1 for infinite loop)
	private static Integer timeDelay; 									// Delay between requests in seconds (-1 for no delay)
	private static String generator; 									// Generator (RT: Random testing, CBT:Constraint-based testing)
	private static Boolean logToFile;									// If 'true', log messages will be printed to external files
	private static boolean executeTestCases;							// If 'false', test cases will be generated but not executed

	// For Constraint-based testing and AR Testing:
	private static Float faultyDependencyRatio; 						// Percentage of faulty test cases due to dependencies to generate.
	private static Integer reloadInputDataEvery; 						// Number of requests using the same randomly generated input data
	private static Integer inputDataMaxValues; 							// Number of values used for each parameter when reloading input data

	// For AR Testing only:
	private static String similarityMetric;								// The algorithm to measure the similarity between test cases
	private static Integer numberCandidates;							// Number of candidate test cases per AR iteration
	private static String jsonDir;
	private static String testDir;
	

	private static String testEnvironment; 
	
	private static Logger logger = LogManager.getLogger(TestGenerationAndExecution.class.getName());

	public static void main(String[] args) throws Exception 
	{
		testGenerationAndExecution(args);
	}
	
	public static void testGenerationAndExecution(String[] args) throws Exception 
	{
		
		// Read .properties file path. This file contains the configuration parameter
		// for the generation
		if (args != null && args.length > 0)
		{
			if(args[0] != null && args[0].length() > 0)
			{
				propertiesFilePath = "src/test/resources/zycus_new/"+args[0].trim();
			}
		}
		else
		{
			propertiesFilePath = "src/test/resources/zycus_new/zycus.properties";
		}
		System.setProperty("zycus.properties", propertiesFilePath);
		
		// Read parameter values from .properties file
		readParameterValues();

		// This will create the test data needed for Delete APIs
		//DeleteAPITestDataCreator.createTestDataForDeleteAPIs();
		
		Timer.startCounting(ALL);
		jsonDir = targetDirJava+"/TestJSON";
		testDir = targetDirJava+"/Test";
		
		// Create target directory if it does not exists
		createDir(targetDirJava);
		deleteDir(jsonDir);
		createDir(jsonDir);
		createDir(testDir);
		
		// RESTest runner
		AbstractTestCaseGenerator generator = createGenerator(); // Test case generator
		IWriter writer = createWriter(); // Test case writer
		StatsReportManager statsReportManager = createStatsReportManager(); // Stats reporter
		AllureReportManager reportManager = createAllureReportManager(); // Allure test case reporter
		RESTestRunner runner = new RESTestRunner(testClassName, targetDirJava, packageName, generator, writer,
					reportManager, statsReportManager);
		runner.setExecuteTestCases(executeTestCases);


		// Main loop
		int iteration = 1;
		while (totalNumTestCases == -1 || runner.getNumTestCases() < totalNumTestCases) {

			// Introduce optional delay
			if (iteration != 1 && timeDelay != -1)
				delay(timeDelay);

			// Generate unique test class name to avoid the same class being loaded everytime
			String id = IDGenerator.generateId();
			String className = testClassName + "_" + id;
			((RESTAssuredWriter) writer).setClassName(className);
			((RESTAssuredWriter) writer).setTestId(id);
			runner.setTestClassName(className);
			runner.setTestId(id);

			// Test case generation + execution + test report generation
			runner.run();

			logger.info("Iteration {}. {} test cases generated.", iteration, runner.getNumTestCases());
			iteration++;
		}

		Timer.stopCounting(ALL);

		generateTimeReport(iteration-1);
	}

	// Create a test case generator
	private static AbstractTestCaseGenerator createGenerator() {
		
		try
		{
			// Load specification
			spec = new OpenAPISpecification(OAISpecPath);
		}
		catch(Exception e)
		{
			System.out.println("*****Error : swagger.yaml has validation issues. Not able to parse it.");
			System.out.println("*****Error : You can find the validation issues by copying swagger.yaml into editor.swagger.io/");
			System.out.println("Fix the validation issues.Exiting...");
			System.exit(-1);
		}

		// Load configuration
		TestConfigurationObject conf;

		if(generator.equals("FT") && confPath == null) {
			logger.info("No testConf specified. Generating one");
			String[] args = {OAISpecPath};
			CreateTestConf.main(args);

			String specDir = OAISpecPath.substring(0, OAISpecPath.lastIndexOf('/'));
			confPath = specDir + "/testConf.yaml";
			logger.info("Created testConf in '{}'", confPath);
		}

		conf = loadConfiguration(confPath, spec);

		// Create generator
		AbstractTestCaseGenerator gen = null;

		switch (generator) {
		case "FT":
			gen = new FuzzingTestCaseGenerator(spec, conf, numTestCases,numGetTestCases);
			break;
		case "RT":
			gen = new RandomTestCaseGenerator(spec, conf, numTestCases, numGetTestCases);
			((RandomTestCaseGenerator) gen).setFaultyRatio(faultyRatio);
			break;
		case "CBT":
			gen = new ConstraintBasedTestCaseGenerator(spec, conf, numTestCases, numGetTestCases);
			((ConstraintBasedTestCaseGenerator) gen).setFaultyDependencyRatio(faultyDependencyRatio);
			((ConstraintBasedTestCaseGenerator) gen).setInputDataMaxValues(inputDataMaxValues);
			((ConstraintBasedTestCaseGenerator) gen).setReloadInputDataEvery(reloadInputDataEvery);
			gen.setFaultyRatio(faultyRatio);
			break;
		case "ART":
			gen = new ARTestCaseGenerator(spec, conf, numTestCases, numGetTestCases);
			((ARTestCaseGenerator) gen).setFaultyDependencyRatio(faultyDependencyRatio);
			((ARTestCaseGenerator) gen).setInputDataMaxValues(inputDataMaxValues);
			((ARTestCaseGenerator) gen).setReloadInputDataEvery(reloadInputDataEvery);
			((ARTestCaseGenerator) gen).setDiversity(similarityMetric);
			((ARTestCaseGenerator) gen).setNumberOfCandidates(numberCandidates);
			gen.setFaultyRatio(faultyRatio);
			break;
		default:
		}

		return gen;
	}

	// Create a writer for RESTAssured
	private static IWriter createWriter() {
		//String basePath = spec.getSpecification().getServers().get(0).getUrl();
		RESTAssuredWriter writer = new RESTAssuredWriter(OAISpecPath, targetDirJava, testClassName, packageName,
				testEnvironment, logToFile);
		writer.setLogging(true);
		writer.setAllureReport(true);
		writer.setEnableStats(enableCSVStats);
		writer.setEnableOutputCoverage(enableOutputCoverage);
		writer.setAPIName(experimentName);
		return writer;
	}

	// Create an Allure report manager
	private static AllureReportManager createAllureReportManager() {
		AllureReportManager arm = null;
		if(executeTestCases) {
			String allureResultsDir = readParameterValue("allure.results.dir") + "/" + experimentName;
			String allureReportDir = readParameterValue("allure.report.dir") + "/" + experimentName;

			// Delete previous results (if any)
			if (deletePreviousResults) {
				deleteDir(allureResultsDir);
				deleteDir(allureReportDir);
			}

			//Find auth property names (if any)
			List<String> authProperties = AllureAuthManager.findAuthProperties(spec, confPath);

			arm = new AllureReportManager(allureResultsDir, allureReportDir, authProperties);
			arm.setEnvironmentProperties(propertiesFilePath);
			arm.setHistoryTrend(true);
		}
		return arm;
	}

	// Create an statistics report manager
	private static StatsReportManager createStatsReportManager() {
		String testDataDir = readParameterValue("data.tests.dir") + "/" + experimentName;
		String coverageDataDir = readParameterValue("data.coverage.dir") + "/" + experimentName;

		// Delete previous results (if any)
		if (deletePreviousResults) {
			deleteDir(testDataDir);
			deleteDir(coverageDataDir);

			// Recreate directories
			createDir(testDataDir);
			createDir(coverageDataDir);
		}

		return new StatsReportManager(testDataDir, coverageDataDir, enableCSVStats, enableInputCoverage,
				enableOutputCoverage, new CoverageMeter(new CoverageGatherer(spec)));
	}

	private static void generateTimeReport(Integer iterations) {
		String timePath = readParameterValue("data.tests.dir") + "/" + experimentName + "/" + readParameterValue("data.tests.time");
		try {
			Timer.exportToCSV(timePath, iterations);
		} catch (RuntimeException e) {
			logger.error("The time report cannot be generated. Stack trace:");
			logger.error(e.getMessage());
		}
		logger.info("Time report generated.");
	}

	/*
	 * Stop the execution n seconds
	 */
	private static void delay(Integer time) {
		try {
			logger.info("Introducing delay of {} seconds", time);
			TimeUnit.SECONDS.sleep(time);
		} catch (InterruptedException e) {
			logger.error("Error introducing delay", e);
			logger.error(e.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	// Read the parameter values from the .properties file. If the value is not found, the system looks for it in the global .properties file (config.properties)
	private static void readParameterValues() throws Exception{

		logToFile = Boolean.parseBoolean(readParameterValue("logToFile"));
		if(logToFile) {
			setUpLogger();
		}

		logger.info("Loading configuration parameter values");
		
		generator = readParameterValue("generator");
		logger.info("Generator: {}", generator);
		
		OAISpecPath = readParameterValue("oas.path");
		logger.info("OAS path: {}", OAISpecPath);
		
		testEnvironment = readParameterValue("test.environment.url");
		logger.info("Test Environment Name: {}", testEnvironment);
		if(testEnvironment == null || testEnvironment.equals(""))
		{
			throw new Exception("Configuration parameter 'test.environment.url' should be defined and can't be empty or null");
		}
		if(testEnvironment.endsWith("/")){
			testEnvironment = testEnvironment.substring(0,testEnvironment.length()-1);
		}
		
		
		confPath = readParameterValue("conf.path");
		logger.info("Test configuration path: {}", confPath);
		
		targetDirJava = readParameterValue("test.target.dir");
		logger.info("Target dir for test classes: {}", targetDirJava);
		
		experimentName = readParameterValue("experiment.name");
		logger.info("Experiment name: {}", experimentName);
		packageName = experimentName;

		if (readParameterValue("experiment.execute") != null) {
			executeTestCases = Boolean.parseBoolean(readParameterValue("experiment.execute"));
		}
		logger.info("Experiment execution: {}", executeTestCases);
		
		testClassName = readParameterValue("testclass.name");
		logger.info("Test class name: {}", testClassName);

		if (readParameterValue("testsPerPOSTOperation") != null)
			numTestCases = Integer.parseInt(readParameterValue("testsPerPOSTOperation"));
		logger.info("Number of POST test cases per operation: {}", numTestCases);
		
		if (readParameterValue("testsPerGETOperation") != null)
			numGetTestCases = Integer.parseInt(readParameterValue("testsPerGETOperation"));
		logger.info("Number of GET test cases per operation: {}", numGetTestCases);
		
		
		if (readParameterValue("numtotaltestcases") != null)
			totalNumTestCases = Integer.parseInt(readParameterValue("numtotaltestcases"));
		logger.info("Max number of test cases: {}", totalNumTestCases);

		if (readParameterValue("delay") != null)
			timeDelay = Integer.parseInt(readParameterValue("delay"));
		logger.info("Time delay: {}", timeDelay);

		if (readParameterValue("reloadinputdataevery") != null)
			reloadInputDataEvery = Integer.parseInt(readParameterValue("reloadinputdataevery"));
		logger.info("Input data reloading  (CBT): {}", reloadInputDataEvery);

		if (readParameterValue("inputdatamaxvalues") != null)
			inputDataMaxValues = Integer.parseInt(readParameterValue("inputdatamaxvalues"));
		logger.info("Max input test data (CBT): {}", inputDataMaxValues);

		if (readParameterValue("coverage.input") != null)
			enableInputCoverage = Boolean.parseBoolean(readParameterValue("coverage.input"));
		logger.info("Input coverage: {}", enableInputCoverage);

		if (readParameterValue("coverage.output") != null)
			enableOutputCoverage = Boolean.parseBoolean(readParameterValue("coverage.output"));
		logger.info("Output coverage: {}", enableOutputCoverage);

		if (readParameterValue("stats.csv") != null)
			enableCSVStats = Boolean.parseBoolean(readParameterValue("stats.csv"));
		logger.info("CSV statistics: {}", enableCSVStats);

		if (readParameterValue("deletepreviousresults") != null)
			deletePreviousResults = Boolean.parseBoolean(readParameterValue("deletepreviousresults"));
		logger.info("Delete previous results: {}", deletePreviousResults);

		if (readParameterValue("similarity.metric") != null)
			similarityMetric = readParameterValue("similarity.metric");
		logger.info("Similarity metric: {}", similarityMetric);

		if (readParameterValue("art.number.candidates") != null)
			numberCandidates = Integer.parseInt(readParameterValue("art.number.candidates"));
		logger.info("Number of candidates: {}", numberCandidates);

		if (readParameterValue("faulty.ratio") != null)
			faultyRatio = Float.parseFloat(readParameterValue("faulty.ratio"));
		logger.info("Faulty ratio: {}", faultyRatio);

		if (readParameterValue("faulty.dependency.ratio") != null)
			faultyDependencyRatio = Float.parseFloat(readParameterValue("faulty.dependency.ratio"));
		logger.info("Faulty dependency ratio: {}", faultyDependencyRatio);
		
	
	}

	// Read the parameter value from the local .properties file. If the value is not found, it reads it form the global .properties file (config.properties)
	private static String readParameterValue(String propertyName) {

		String value = null;
		if (PropertyManager.readProperty(propertiesFilePath, propertyName) != null) // Read value from local .properties
																					// file
			value = PropertyManager.readProperty(propertiesFilePath, propertyName);
		else if (PropertyManager.readProperty(propertyName) != null) // Read value from global .properties file
			value = PropertyManager.readProperty(propertyName);

		return value;
	}

	private static void setUpLogger() {
		String logPath = readParameterValue("log.path");

		System.setProperty("logFilename", logPath);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		File file = new File("src/main/resources/log4j2-logToFile.properties");
		ctx.setConfigLocation(file.toURI());
		ctx.reconfigure();
	}
}
