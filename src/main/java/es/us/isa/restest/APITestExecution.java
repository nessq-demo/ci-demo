package es.us.isa.restest.apichain.api;

import es.us.isa.restest.apichain.api.util.ITestWriter;
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
public class APITestExecution {

	// Properties file with configuration settings
	private static String propertiesFilePath = "src/test/resources/zycus_new/zycus.properties";
	private static String configFilePath = "src/main/resources/config.properties";
	

	private static String confPath; 									// Path to test configuration file
	private static String targetDirJava; 								// Directory where tests will be generated.
	private static String packageName; 									// Package name.
	private static String experimentName; 								// Used as identifier for folders, etc.
	private static String testClassName; 								// Name prefix of the class to be generated
	private static Boolean deletePreviousResults; // Set to 'true' if you want previous CSVs and Allure reports.
	private static Boolean logToFile; // If 'true', log messages will be printed to external files
	private static String testEnvironment; 
	private static OpenAPISpecification spec; 							// OAS specification
	
	private static String apiDataFilesDir; 
	
	private static Logger logger = LogManager.getLogger(APITestExecution.class.getName());

	public static void main(String[] args) throws Exception {
		
		// Read parameter values from .properties file
		readParameterValues();

		if (args.length > 0)
			propertiesFilePath = args[0];

		// Create target directory if it does not exists
		createDir(targetDirJava);
		AllureReportManager reportManager = createAllureReportManager(); // Allure test case reporter
		
		ITestWriter writer = createWriter(); // Test case writer
		
		APIChainTestRunner runner = new APIChainTestRunner(testClassName, targetDirJava, packageName, writer, reportManager, apiDataFilesDir);
		runner.setExecuteTestCases(true);
		runner.execute();

	}

	// Create a writer for RESTAssured
	private static ITestWriter createWriter() {
		//String basePath = spec.getSpecification().getServers().get(0).getUrl();
		APIChainRESTAssuredWriter writer = new APIChainRESTAssuredWriter(targetDirJava, testClassName, packageName,testEnvironment, logToFile);
		writer.setLogging(true);
		writer.setAllureReport(true);
		writer.setAPIName(experimentName);
		return writer;
	}

	// Create an Allure report manager
	private static AllureReportManager createAllureReportManager() {
		
		AllureReportManager arm = null;
		String allureResultsDir = readParameterValue("allure.results.dir") + "/" + experimentName;
		String allureReportDir = readParameterValue("allure.report.dir") + "/" + experimentName;

		// Delete previous results (if any)
		if (deletePreviousResults) {
			deleteDir(allureResultsDir);
			deleteDir(allureReportDir);
		}

		List<String> authProperties = AllureAuthManager.findAuthProperties(spec, confPath);

		arm = new AllureReportManager(allureResultsDir, allureReportDir, authProperties);
		arm.setEnvironmentProperties(propertiesFilePath);
		arm.setHistoryTrend(true);
		
		return arm;
	}

	
	
	// Read the parameter values from the .properties file. If the value is not found, the system looks for it in the global .properties file (config.properties)
	private static void readParameterValues() throws Exception{

		logToFile = Boolean.parseBoolean(readParameterValue("logToFile"));
	
		if(logToFile) {
			setUpLogger();
		}

		logger.info("Loading configuration parameter values");
		
		
		
		
		apiDataFilesDir = readConfigParameterValue("apichain.api.data.files");
		logger.info("API Chain - API Data Files Dir : {}", apiDataFilesDir);
		
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

		
		testClassName = readParameterValue("testclass.name");
		logger.info("Test class name: {}", testClassName);

		if (readParameterValue("deletepreviousresults") != null)
			deletePreviousResults = Boolean.parseBoolean(readParameterValue("deletepreviousresults"));
		logger.info("Delete previous results: {}", deletePreviousResults);

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

	private static String readConfigParameterValue(String propertyName) 
	{

		String value = null;
		if (PropertyManager.readProperty(configFilePath, propertyName) != null) // Read value from local .properties
																					// file
			value = PropertyManager.readProperty(configFilePath, propertyName);
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
