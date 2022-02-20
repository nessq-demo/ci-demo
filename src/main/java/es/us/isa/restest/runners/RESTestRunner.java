package es.us.isa.restest.runners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import es.us.isa.restest.util.*;
import es.us.isa.restest.util.ClassLoader;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import es.us.isa.restest.generators.AbstractTestCaseGenerator;
import es.us.isa.restest.reporting.AllureReportManager;
import es.us.isa.restest.reporting.StatsReportManager;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.testcases.writers.IWriter;

import static es.us.isa.restest.util.FileManager.checkIfExists;
import static es.us.isa.restest.util.FileManager.getFileNames;
import static es.us.isa.restest.util.Timer.TestStep.*;

/**
 * This class implements a basic test workflow: test generation -&gt; test writing -&gt; class compilation and loading -&gt; test execution -&gt; test report generation -&gt; test coverage report generation
 * @author Sergio Segura
 *
 */
public class RESTestRunner {

	protected String targetDir;							// Directory where tests will be generated
	protected String testClassName;						// Name of the class to be generated
	private String testId;
	private String packageName;							// Package name
	private AbstractTestCaseGenerator generator;   		// Test case generator
	protected IWriter writer;							// RESTAssured writer
	protected AllureReportManager allureReportManager;	// Allure report manager
	protected StatsReportManager statsReportManager;	// Stats report manager
	private boolean executeTestCases;
	private int numTestCases = 0;						// Number of test cases generated so far
	private static final Logger logger = LogManager.getLogger(RESTestRunner.class.getName());

	public RESTestRunner(String testClassName, String targetDir, String packageName, AbstractTestCaseGenerator generator, IWriter writer,
						 AllureReportManager reportManager, StatsReportManager statsReportManager) {
		this.targetDir = targetDir;
		this.packageName = packageName;
		this.testClassName = testClassName;
		this.generator = generator;
		this.writer = writer;
		this.allureReportManager = reportManager;
		this.statsReportManager = statsReportManager;
	}

	public void run() throws RESTestException {

		// Test generation and writing (RESTAssured)
		testGeneration();

		if(executeTestCases) {
			// Test execution
			logger.info("Running tests");
			System.setProperty("allure.results.directory", allureReportManager.getResultsDirPath());
			testExecution(getTestClass());
		}

		generateReports();
	}
	
	private ArrayList<String> getEnabledReleases(String releaseDirectory)
	{
		
		String file = releaseDirectory+"/releases.xlsx";
		String sheetName = "releases";
		
		ArrayList<String> releases = new ArrayList<>();
		
		try 
		{
			boolean flag=checkIfExists(file);
			if(!flag) 
			{
				return releases;
			}
			
			Xls_Reader xls = new Xls_Reader(file);
			for(int i=2;i<=xls.getRowCount(sheetName);i++)
			{
				if(xls.getCellData(sheetName,"Execute",i).equalsIgnoreCase("N"))
				{
					releases.add(xls.getCellData(sheetName,"ReleaseName",i));
				}
			}	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return releases;
	}

	private void copyReleaseTestFiles(String releaseDirectory, String testFilePath) throws Exception
	{
		ArrayList<String> releases = getEnabledReleases(releaseDirectory);
		File releaseDir = new File(releaseDirectory);
		File tempReleaseDir = new File(releaseDirectory+"/temp");
		FileUtils.copyDirectory(releaseDir, tempReleaseDir);
		File relDirTemp; 
		
		for(String releaseName : releases)
		{
			relDirTemp = new File(releaseDirectory+"/temp/"+releaseName);
			if(relDirTemp.exists() && relDirTemp.isDirectory())
			{
				FileUtils.deleteDirectory(relDirTemp);
			}
		}
		String[] extensions = new String[] {"java"};
		Collection<File> files = FileUtils.listFiles( tempReleaseDir , extensions, true);
		for (File file : files) 
		{
            FileUtils.copyFile(file, new File(testFilePath+"/"+file.getName()));
		}
		FileUtils.deleteDirectory(tempReleaseDir);
		
		
	}

	
	public void execute() throws RESTestException, Exception {

		String testFilePath = targetDir+"/Test";
		
		if(executeTestCases) 
		{
			
			if(ReleaseManager.isExecuteReleaseCases())
			{
				String releaseDirectory = targetDir+"/releases";
				File releaseDir = new File(releaseDirectory);
				
				if(releaseDir.exists())
				{	
					copyReleaseTestFiles(releaseDirectory, testFilePath);
					
				}
			}
				
			// Test execution
			logger.info("Running tests");
			System.setProperty("allure.results.directory", allureReportManager.getResultsDirPath());
			//String testFilePath = targetDir;
			List<File> testFileList = getFileNames(testFilePath);
			
			if(testFileList.size() ==0)
			{
				logger.info("===========================================================");
				logger.info("   No Test Class in '"+testFilePath+"' available to Run    ");
				logger.info("===========================================================");
				System.exit(-1);
				
			}
			for(int i=0;i<testFileList.size();i++) {
				testExecution(getTestExecutionClass(testFileList.get(i)));
			}
		}
		generateReports();
		
		logger.info("Test Completed.Cleaning Directory'"+testFilePath+"' ");
		FileUtils.cleanDirectory(new File(testFilePath));
	}
	public void generateTest() throws RESTestException {
		// Test generation and writing (RESTAssured)
		testGeneration();
	}

	
	protected void generateReports() {
		if(executeTestCases) {
			// Generate test report
			logger.info("Generating test report");
			allureReportManager.generateReport();
		}

		// Generate coverage report
		logger.info("Generating coverage report");
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

	private void testGeneration() throws RESTestException {
	    
		// Generate test cases
		logger.info("Generating tests");
		Timer.startCounting(TEST_SUITE_GENERATION);
		Collection<TestCase> testCases = generator.generate();
		Timer.stopCounting(TEST_SUITE_GENERATION);
        this.numTestCases += testCases.size();

        // Pass test cases to the statistic report manager (CSV writing, coverage)
        statsReportManager.setTestCases(testCases);
        
        // Write test cases
        String filePath = targetDir + "/" + testClassName + ".java";
        logger.info("Writing {} test cases to test class {}", testCases.size(), filePath);
        writer.write(testCases);

	}
	
   public String getFilePath() throws RESTestException {
        String filePath = targetDir + "/" + testClassName + ".java";
    return filePath;
	}

	protected void testExecution(Class<?> testClass)  {
		DBConnector db = new DBConnector();
		try {
			db.initConnection(true);
			JUnitCore junit = new JUnitCore();
			//junit.addListener(new TextListener(System.out));
			junit.addListener(new io.qameta.allure.junit4.AllureJunit4());
			Timer.startCounting(TEST_SUITE_EXECUTION);
			Result result = junit.run(testClass);
			Timer.stopCounting(TEST_SUITE_EXECUTION);
			int successfulTests = result.getRunCount() - result.getFailureCount() - result.getIgnoreCount();
			logger.info("{} tests run in {} seconds. Successful: {}, Failures: {}, Ignored: {}", result.getRunCount(), result.getRunTime() / 1000, successfulTests, result.getFailureCount(), result.getIgnoreCount());
		    String className = String.valueOf(testClass).replace("class zycus_new.","").trim();
			db.insertReportTable(result.getRunCount(),successfulTests,result.getFailureCount(), className);
		}catch(Throwable t){
			t.printStackTrace();
		}finally {
			db.closeConnection();
			db=null;
		}
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

	public int getNumTestCases() {
		return numTestCases;
	}
	
	public void resetNumTestCases() {
		this.numTestCases=0;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public void setExecuteTestCases(Boolean executeTestCases) {
		this.executeTestCases = executeTestCases;
	}
}
