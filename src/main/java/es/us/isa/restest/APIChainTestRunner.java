package es.us.isa.restest.apichain.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import es.us.isa.restest.util.*;
import es.us.isa.restest.util.ClassLoader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import es.us.isa.restest.apichain.api.util.APIObject;
import es.us.isa.restest.apichain.api.util.ITestWriter;
import es.us.isa.restest.generators.AbstractTestCaseGenerator;
import es.us.isa.restest.reporting.AllureReportManager;
import es.us.isa.restest.reporting.StatsReportManager;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.testcases.writers.IWriter;

import static es.us.isa.restest.util.FileManager.getFileNames;
import static es.us.isa.restest.util.Timer.TestStep.*;

/**
 * This class implements a basic test workflow: test generation -&gt; test writing -&gt; class compilation and loading -&gt; test execution -&gt; test report generation -&gt; test coverage report generation
 * @author Sergio Segura
 *
 */
public class APIChainTestRunner {

	protected String targetDir;							// Directory where tests will be generated
	protected String testClassName;						// Name of the class to be generated
	private String testId;
	private String packageName;							// Package name
	protected ITestWriter writer;							// RESTAssured writer
	protected AllureReportManager allureReportManager;	// Allure report manager
	private boolean executeTestCases;
	private String apiDataDir;
	
	private static final Logger logger = LogManager.getLogger(APIChainTestRunner.class.getName());

	public APIChainTestRunner(String testClassName, String targetDir, String packageName, ITestWriter writer,
						 AllureReportManager reportManager, String apiDataDir) {
		this.targetDir = targetDir;
		this.packageName = packageName;
		this.testClassName = testClassName;
		this.writer = writer;
		this.allureReportManager = reportManager;
		this.apiDataDir = apiDataDir;
	}

	public void run() throws RESTestException {

		testGeneration();

		if(executeTestCases) {
			// Test execution
			logger.info("Running tests");
			System.setProperty("allure.results.directory", allureReportManager.getResultsDirPath());
			testExecution(getTestClass());
		}

		generateReports();
	}

	public void execute() throws RESTestException {

		if(executeTestCases) {
			// Test execution
			logger.info("Running tests");
			System.setProperty("allure.results.directory", allureReportManager.getResultsDirPath());
			String testFilePath = targetDir+"/Test";
			//String testFilePath = targetDir;
			List<File> testFileList = getFileNames(testFilePath);
			for(int i=0;i<testFileList.size();i++) {
				testExecution(getTestExecutionClass(testFileList.get(i)));
			}
		}
		generateReports();
	}

	protected void generateReports() {
		if(executeTestCases) {
			// Generate test report
			logger.info("Generating test report");
			allureReportManager.generateReport();
		}

		// Generate coverage report
		//logger.info("Generating coverage report");
		//statsReportManager.generateReport(testId, executeTestCases);
	}

	protected Class<?> getTestClass() {
		// Load test class
		String filePath = targetDir + "/" + testClassName + ".java";
		String className = packageName + "." + testClassName;
		logger.info("Compiling and loading test class {}.java", className);
		return ClassLoader.loadClass(filePath, className);
	}

	protected Class<?> getTestExecutionClass(File file) {
		// Load test class
		String filePath = file.getPath();
		//System.out.println("filePath ->"+filePath);
		String testFileName = file.getName();
		//System.out.println("testFileName ->"+testFileName);
		String classNameGenerated = file.getName().substring(0,testFileName.length()-5);
		String className = packageName + "." + classNameGenerated;
		logger.info("Compiling and loading test class {}.java", className);
		//System.out.println("classNameGenerated ->"+classNameGenerated);
		//System.out.println("className ->"+className);
		return ClassLoader.loadClass(filePath, className);
	}

	@SuppressWarnings("deprecation")
	private void testGeneration() throws RESTestException 
	{

//		logger.info("Generating tests");
//		Collection<TestCase> testCases = generator.generate();
//		String filePath = targetDir + "/" + testClassName + ".java";
//        logger.info("Writing {} test cases to test class {}", testCases.size(), filePath);
//        writer.write(testCases);
		
		APIWalker.harWalker();
		ArrayList<APIObject> apis = APIWalker.apis;
		writer.write(apis);
		
	}
	
   public String getFilePath() throws RESTestException {
        String filePath = targetDir + "/" + testClassName + ".java";
    return filePath;
	}

	protected void testExecution(Class<?> testClass)  {
		
		JUnitCore junit = new JUnitCore();
		//junit.addListener(new TextListener(System.out));
		junit.addListener(new io.qameta.allure.junit4.AllureJunit4());
		Timer.startCounting(TEST_SUITE_EXECUTION);
		Result result = junit.run(testClass);
		Timer.stopCounting(TEST_SUITE_EXECUTION);
		int successfulTests = result.getRunCount() - result.getFailureCount() - result.getIgnoreCount();
		logger.info("{} tests run in {} seconds. Successful: {}, Failures: {}, Ignored: {}", result.getRunCount(), result.getRunTime()/1000, successfulTests, result.getFailureCount(), result.getIgnoreCount());

	}
	
	public String getTargetDir() {
		return targetDir;
	}
	
	public void setTargetDir(String targetDir) {
		this.targetDir = targetDir;
	}
	
	public String getTestClassName() {
		return testClassName;
	}
	
	public void setTestClassName(String testClassName) {
		this.testClassName = testClassName;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public void setExecuteTestCases(Boolean executeTestCases) {
		this.executeTestCases = executeTestCases;
	}
}
