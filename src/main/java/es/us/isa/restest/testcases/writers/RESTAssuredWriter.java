package es.us.isa.restest.testcases.writers;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JacksonInject.Value;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;

import es.us.isa.restest.main.TestCoverageFinder;
import es.us.isa.restest.mutation.operators.JsonMutatorLocal;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.util.*;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.SystemPropertiesPropertySource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static es.us.isa.restest.generators.AbstractTestCaseGenerator.bParameter;
import static es.us.isa.restest.inputs.perturbation.ObjectPerturbator.beforeMutate;
import static es.us.isa.restest.util.FileManager.*;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

/** This class defines a test writer for the REST Assured framework. It creates a Java class with JUnit test cases
 * ready to be executed.
 * 
 * @author Sergio Segura &amp; Alberto Martin-Lopez
 *
 */
public class RESTAssuredWriter implements IWriter {
	
	
	private boolean OAIValidation = true;
	private boolean logging = false;				// Log everything (ONLY IF THE TEST FAILS)
	private boolean allureReport = false;			// Generate request and response attachment for allure reports
	private boolean enableStats = false;			// If true, export test results data to CSV
	private boolean enableOutputCoverage = false;	// If true, export output coverage data to CSV

	private String specPath;						// Path to OAS specification file
	private String testFilePath;					// Path to test configuration file
	private String className;						// Test class name
	private String testId;							// Test suite ID
	private String packageName;						// Package name
	private String baseURI;							// API base URI
	private boolean logToFile;						// If 'true', REST-Assured requests and responses will be logged into external files
	private String APIName;// API name (necessary for folder name of exported data)
	private boolean authFlag = true;
	private int c = 0;
	private int classNameCounter = 0;
	private List<String> displayName = new ArrayList<String>();
	private String valGlobal ;
	private List<String> eleGlobal = new ArrayList<String>();
	private Hashtable<String,String> eleGlobalTable = new Hashtable<String,String>();
	private String sheetName = "API";
	private static Xls_Reader xls;
	private Hashtable<String,String> endpoint = new Hashtable<String,String>();
	private boolean valFlag = false ;
	Set<String> display = new HashSet<String>();
	private int tCount = 0;
	private Set<String> coverage = new HashSet<String>();
	
	private boolean deDupe2ndLayer = false;

	private static final Logger logger = LogManager.getLogger(RESTAssuredWriter.class.getName());
	
	public RESTAssuredWriter(String specPath, String testFilePath, String className, String packageName, String baseURI, Boolean logToFile) {
		this.specPath = specPath;
		this.testFilePath = testFilePath;
		this.className = className;
		this.packageName = packageName;
		this.baseURI = baseURI;
		this.logToFile = logToFile;
	}
	
	/* (non-Javadoc)
	 * @see es.us.isa.restest.testcases.writers.IWriter#write(java.util.Collection)
	 */
	@Override
	public void write(Collection<TestCase> testCases) {
		
		if(deDupe2ndLayer)
		{
			System.out.println("********************************************");
			
			System.out.println("Initial Cases :"+testCases.size());
	
			ArrayList<TestCase> uniqueCases = new ArrayList<>();
			HashMap<String, TestCase> nonUniqueMap = new HashMap<>();
			
			for(TestCase tc : testCases)
			{
				//System.out.println("Operator : "+ tc.getOperationApplied() + " - Element to Mutate : "+tc.getElementToMutate());
				//System.out.println(tc.getOperationApplied()+"_"+tc.getPropertyMutated());
				
				if(tc.getPropertyMutated() != null)
				{
					nonUniqueMap.put(tc.getOperationApplied()+"_"+tc.getPropertyMutated(), tc);
					
				}
				else
				{
					uniqueCases.add(tc);
				}
				
			}
			
			System.out.println("Unique Cases :"+uniqueCases.size());
			System.out.println("NonUnique Cases :"+nonUniqueMap.size());
	
			Collection<TestCase> nonUniqueCases = nonUniqueMap.values();
			uniqueCases.addAll(nonUniqueCases);
			
			
			
			testCases = new ArrayList<TestCase>();
			testCases.addAll(uniqueCases);
	
			uniqueCases = null;
			nonUniqueCases = null;
			nonUniqueCases = null;
			
			System.out.println("Final Cases :"+testCases.size());
			
			for(TestCase tc : testCases)
			{
				//System.out.println("Operator : "+ tc.getOperationApplied() + " - Element to Mutate : "+tc.getElementToMutate());
				System.out.println(tc.getId()+"," +tc.getOperationApplied() + ","+tc.getPropertyMutated());
				
							
			}
			
			System.out.println("********************************************");
	
		}
		
		Object[] test = testCases.toArray();
		
		// Initializing content
		String contentFile = "";
		
		// Generating imports
		contentFile += generateImports(packageName);
		
		// Generate className
		contentFile += generateClassName(className,test);
		
		// Generate attributes
		contentFile += generateAttributes(specPath);
		
		// Generate variables to be used.
		contentFile += generateSetUp(baseURI);


		// Generate tests
		int ntest=1;
		int count=0;
		for(TestCase t: testCases) {
			
			if(!endpoint.containsKey(t.getPath())) {
				count = 0;
			}
			endpoint.put(t.getPath(),t.getMethod().toString());
			if (count == 0) {
				contentFile += generateTest(t, ntest++, 0);
				count++;
			} else {
				contentFile += generateTest(t, ntest++, count);
			}

		}
		// Close class
		contentFile += "}\n";

		String s = authentication.authenticate(contentFile);
		//Save to file
		saveToFile(testFilePath,className,s);
		writeExcel("Config");
		/* Test Compile
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		compiler.run(System.in, System.out, System.err, TEST_LOCATION + this.specification.getInfo().getTitle().replaceAll(" ", "") + "Test.java");
		*/
		getTestCoverage();
		
	}

	private void getTestCoverage()
	{
		try
		{
			ArrayList<APICoverage> expectedAPICoverages = TestCoverageFinder.getExpectedAPICoverages();
			Iterator<String> it = coverage.iterator();
			
			String coverageRecord;
			String field;
			String api;
			while(it.hasNext())
			{
				coverageRecord = it.next();
				
				String[] records = coverageRecord.split("###");
				
				if(records.length == 1)
				{
					System.out.println("Skipping :"+coverageRecord);
					continue;
				}
				
				api = records[0];
				String[] fieldRecords = records[1].split("_");
				if(fieldRecords.length == 1)
				{
					System.out.println("Skipping :"+coverageRecord);
					continue;
				}
				
				String[] fields = fieldRecords[1].split("~~~");
				
				field = fields[fields.length-1];
				
				if(NumberUtils.isCreatable(field) || field.equals("-"))
				{
					field = fields[fields.length-2];
				}
				
				APICoverage apiCov = null;
				FieldCoverage fieldCov = null;
				ArrayList<FieldCoverage> fieldCoverages = null;
				int testCaseCounter = 0;
				boolean apiFound = false;
				
				for(int i = 0; i < expectedAPICoverages.size(); i++)
				{
					//ArrayList<APICoverage> expectedAPICoverages = TestCoverageFinder.getExpectedAPICoverages();
					apiCov = expectedAPICoverages.get(i);
					if(!apiCov.getApiName().equals(api))
					{
						continue;
					}
					
					apiFound = true;
					fieldCoverages = apiCov.getFieldCoverages();
					
					for(int j = 0; j < fieldCoverages.size(); j++)
					{
						fieldCov = fieldCoverages.get(j);
						if(!fieldCov.getFieldName().equals(field))
						{
							continue;
						}
						
						fieldCov.setActualTestCaseCount(fieldCov.getActualTestCaseCount()+1);
						fieldCov.getTestData().add(records[1].replaceAll("~~~", ""));
						
					
					}
					apiCov.setApiFoundInExecution(apiFound);
					
					for(int j = 0; j < fieldCoverages.size(); j++)
					{
						fieldCov = fieldCoverages.get(j);
						testCaseCounter += fieldCov.getActualTestCaseCount();
					
					}
					apiCov.setActualTestCaseCount(testCaseCounter);
				}
				
				
			}
			
			//printCoverage(expectedAPICoverages);
			writeTestCoverage(expectedAPICoverages);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		
	}
	
	private void printCoverage(ArrayList<APICoverage> expectedAPICoverages)
	{
		APICoverage apiCov = null;
		FieldCoverage fieldCov = null;
		ArrayList<FieldCoverage> fieldCoverages = null;

		System.out.println("==============Coverage======================");
		
		for(int i = 0; i < expectedAPICoverages.size(); i++)
		{
			apiCov = expectedAPICoverages.get(i);
			
			System.out.println();
			System.out.println("API Name : "+apiCov.getApiName());
			System.out.println("=====================================================");
			System.out.print("Expected TC# : "+apiCov.getExpectedTestCaseCount()+" , ");
			
			if(apiCov.isApiFoundInExecution())
			{
				int actual = apiCov.getActualTestCaseCount();
				int expected = apiCov.getExpectedTestCaseCount();
				
				float covPercentage = 0;
				
				if(actual > expected)
				{
					covPercentage = 100;
				}
				else
				{
					covPercentage = actual*100/expected; 
				}
				System.out.println("Actual TC# : "+actual+", Test Coverage : "+covPercentage+"%");
				
			}
			else
			{
				System.out.println("<No TestCase Generated since API is not in swagger.yaml>");
			}
			
			fieldCoverages = apiCov.getFieldCoverages();
			
			for(int j = 0; j < fieldCoverages.size(); j++)
			{
				fieldCov = fieldCoverages.get(j);
				System.out.println();
				System.out.println("Field Name : "+fieldCov.getFieldName());
				System.out.println("------------------------------------------------------------");
				System.out.print("Expected TC# : "+fieldCov.getExpectedTestCaseCount()+" , ");
				
				if(apiCov.isApiFoundInExecution())
				{
					int actual = fieldCov.getActualTestCaseCount();
					int expected = fieldCov.getExpectedTestCaseCount();
					
					float covPercentage = 0;
					
					if(actual > expected)
					{
						covPercentage = 100;
					}
					else
					{
						covPercentage = actual*100/expected; 
					}
					System.out.println("Actual TC# : "+actual+", Field Test Coverage : "+covPercentage+"%");
				}
				else
				{
					System.out.println("<NA>");
				}
				
				if(fieldCov.getTestData().size() > 0)
				{
					System.out.println("Test Data Generated : ");
					
				}
				
				for(int k = 0; k < fieldCov.getTestData().size(); k++)
				{
					System.out.println("\t\t\t"+fieldCov.getTestData().get(k));
				}
				
			}
			
		}
		System.out.println("==============Coverage======================");
		
		
	}
	
	
	private void writeTestCoverage(ArrayList<APICoverage> expectedAPICoverages)
	{
		try 
		{
			String sFile = testFilePath+File.separator+"test-coverage.xlsx";
			boolean flag=checkIfExists(sFile);
			if(flag) 
			{
				FileUtils.forceDelete(new File(sFile));
			
			}	
			XSSFWorkbook workbook = new XSSFWorkbook();
			
		    
			APICoverage apiCov = null;
			FieldCoverage fieldCov = null;
			ArrayList<FieldCoverage> fieldCoverages = null;
			
			for(int i = 0; i < expectedAPICoverages.size(); i++)
			{
				apiCov = expectedAPICoverages.get(i);
				String fileName = apiCov.getApiName().replaceAll("/", "_")+"_"+apiCov.getMethodName();
						
				while (fileName.length() > 30)
				{
					fileName = fileName.substring(5, fileName.length());
				
				}
				
				if(apiCov.isApiFoundInExecution())
				{
					
					XSSFSheet sheet = workbook.createSheet(fileName);
					CellStyle cellStyle = workbook.createCellStyle();
				    Font font = sheet.getWorkbook().createFont();
				    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
				    cellStyle.setFont(font);
				    cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
				    
				    CellStyle cellStyleAlignment = workbook.createCellStyle();
				    cellStyleAlignment.setAlignment(CellStyle.ALIGN_CENTER);
				    
				    
					int rowCount = 0;
					int columnCount  = 0;
					Row row = sheet.createRow(++rowCount);
					Cell cell = row.createCell(++columnCount);
					cell.setCellValue("API");
					cell.setCellStyle(cellStyle);
					
					cell = row.createCell(++columnCount);
					cell.setCellValue("Name");
					cell.setCellStyle(cellStyle);
					 
					cell = row.createCell(++columnCount);
					cell.setCellValue("Expected TC#");
					cell.setCellStyle(cellStyle);
					 
					cell = row.createCell(++columnCount);
					cell.setCellValue("Actual TC#");
					cell.setCellStyle(cellStyle);
					 
					cell = row.createCell(++columnCount);
					cell.setCellValue("Test Coverage (%)");
					cell.setCellStyle(cellStyle);
					 
					cell = row.createCell(++columnCount);
					cell.setCellValue("Test Cases Generated");
					cell.setCellStyle(cellStyle);
					 
					columnCount = 0;
					row = sheet.createRow(++rowCount);
					cell = row.createCell(++columnCount);
					cell.setCellValue("");
					
					cell = row.createCell(++columnCount);
					cell.setCellValue(apiCov.getApiName());
					cell.setCellStyle(cellStyleAlignment);
					
					cell = row.createCell(++columnCount);
					cell.setCellValue(new Integer(apiCov.getExpectedTestCaseCount()));
					cell.setCellStyle(cellStyleAlignment);
					
					int actual = apiCov.getActualTestCaseCount();
					int expected = apiCov.getExpectedTestCaseCount();
					
					float covPercentage = 0;
					
					if(actual > expected)
					{
						covPercentage = 100;
					}
					else
					{
						covPercentage = actual*100/expected; 
					}
					cell = row.createCell(++columnCount);
					cell.setCellValue(new Integer(actual));
					cell.setCellStyle(cellStyleAlignment);
					
					Cell totalCoverageCell = row.createCell(++columnCount);
					totalCoverageCell.setCellValue(covPercentage+"%");
					totalCoverageCell.setCellStyle(cellStyleAlignment);
					
					fieldCoverages = apiCov.getFieldCoverages();
					
					float totalTestCoverage =0;
					
					if(fieldCoverages.size() >0)
					{
						columnCount = 0;
						row = sheet.createRow(++rowCount);
						cell = row.createCell(++columnCount);
						cell.setCellValue("Fields");
						cell.setCellStyle(cellStyle);
					}
					
					for(int j = 0; j < fieldCoverages.size(); j++)
					{
					
						columnCount = 0;
						row = sheet.createRow(++rowCount);
						cell = row.createCell(++columnCount);
						cell.setCellValue("");
			
						
						fieldCov = fieldCoverages.get(j);
						int actualFieldCount = fieldCov.getActualTestCaseCount();
						int expectedFieldCount = fieldCov.getExpectedTestCaseCount();
						
						if(expectedFieldCount == 0)
						{
							expectedFieldCount = 1;
						}
						
						cell = row.createCell(++columnCount);
						cell.setCellValue(fieldCov.getFieldName());
						
						cell = row.createCell(++columnCount);
						cell.setCellValue(new Integer(fieldCov.getExpectedTestCaseCount()));
						cell.setCellStyle(cellStyleAlignment);
						
						float fieldCovPercentage = 0;
						if(actualFieldCount > expectedFieldCount)
						{
							fieldCovPercentage = 100;
						}
						else
						{
							fieldCovPercentage = actualFieldCount*100/expectedFieldCount; 
						}
						
						totalTestCoverage += fieldCovPercentage;
						
						cell = row.createCell(++columnCount);
						cell.setCellValue(new Integer(fieldCov.getActualTestCaseCount()));
						cell.setCellStyle(cellStyleAlignment);
						
						cell = row.createCell(++columnCount);
						cell.setCellValue(fieldCovPercentage+"%");
						cell.setCellStyle(cellStyleAlignment);
						
						
						StringBuffer testData = new StringBuffer();
						
						for(int k = 0; k < fieldCov.getTestData().size(); k++)
						{
							testData.append(fieldCov.getTestData().get(k)+"\n");
						}
						
						cell = row.createCell(++columnCount);
						cell.setCellValue(testData.toString());
					}
					
					float totalCoverage = totalTestCoverage/fieldCoverages.size();
					totalCoverageCell.setCellValue(totalCoverage+"%");
					
					
					for (int n = 0; n < 10; n++)
					{
						   sheet.autoSizeColumn(n);
					}
					
				}
				
				
				
			}
			try (FileOutputStream outputStream = new FileOutputStream(sFile)) 
			{
				workbook.write(outputStream);
				System.out.println("====================================================================");
				System.out.println("Writing Test Coverage......in...."+sFile);
				System.out.println("====================================================================");
		    }
				
		}catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
		
	}

	private void writeExcel(String sheetName){
		DBConnector db = new DBConnector();
		try {
			db.initConnection(true);
			boolean flag=checkIfExists(testFilePath+"/XLSData/APTData.xlsx");
			if(!flag) {
				XSSFWorkbook wb = new XSSFWorkbook();
				File excel = new File(testFilePath + "/XLSData/APTData.xlsx");
				excel.getParentFile().mkdirs();
				FileOutputStream out = new FileOutputStream(excel);
				wb.write(out);
			}
			xls = new Xls_Reader(testFilePath+"/XLSData/APTData.xlsx");
			if(!flag) {
				xls.addSheet(sheetName);
				xls.addColumn(sheetName, "Endpoint");
				xls.addColumn(sheetName, "Method");
				xls.addColumn(sheetName, "Execute");
			}
			Set<String> keys = endpoint.keySet();
			Iterator<String> it = keys.iterator();
			int c = 2;
			String key = "";
			String val = "";
			while(it.hasNext()){
				key = it.next();
				val = endpoint.get(key);
				boolean f = false;
				for(int i=1;i<=xls.getRowCount(sheetName);i++){
					if(!xls.getCellData(sheetName,"Endpoint",i).equals(key)){
						f=true;
					}
				}
				db.insertExecutionStatus(key);
				if(f) {
					xls.setCellData(sheetName, "Endpoint", c, key);
					xls.setCellData(sheetName, "Method", c, val);
					if(!xls.getCellData(sheetName, "Execute", c).equalsIgnoreCase("N"))
						xls.setCellData(sheetName, "Execute", c, "Y");
					c++;
				}
			}


		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			db.closeConnection();
			db=null;
		}
	}

	private String generateImports(String packageName) {
		String content = "";
		
		if (packageName!=null)
			content += "package " + packageName + ";\n\n";

		content += "import org.junit.*;\n"
				+  "import io.restassured.RestAssured;\n"
				+  "import io.restassured.response.Response;\n"
				+  "import com.fasterxml.jackson.databind.JsonNode;\n"
				+  "import com.fasterxml.jackson.databind.ObjectMapper;\n"
				+  "import java.io.IOException;\n"
				+  "import org.junit.FixMethodOrder;\n"
				+  "import static org.junit.Assert.fail;\n"
				+  "import static org.junit.Assert.*;\n"
				+  "import static org.junit.Assume.assumeTrue;\n"
				+  "import org.junit.runners.MethodSorters;\n"
				+  "import io.qameta.allure.restassured.AllureRestAssured;\n"
				+  "import es.us.isa.restest.testcases.restassured.filters.StatusCode5XXFilter;\n"
				+  "import es.us.isa.restest.testcases.restassured.filters.NominalOrFaultyTestCaseFilter;\n"
				+  "import java.io.File;\n"
				+  "import static es.us.isa.restest.util.FileManager.*;\n"
				+  "import es.us.isa.restest.util.Xls_Reader;\n"
				+  "import java.util.*;\n"
				+  "import java.util.Map;\n";


		
		// OAIValidation (Optional)
//		if (OAIValidation)
		content += 	"import es.us.isa.restest.testcases.restassured.filters.ResponseValidationFilter;\n";

//		// Coverage filter (optional)
//		if (enableOutputCoverage)
//			content += 	"import es.us.isa.restest.testcases.restassured.filters.CoverageFilter;\n";

		// Coverage filter (optional)
		if (enableStats || enableOutputCoverage)
			content += 	"import es.us.isa.restest.testcases.restassured.filters.CSVFilter;\n"
		                +   "import io.qameta.allure.junit4.DisplayName;\n";

		if (logToFile) {
			content +=  "import org.apache.logging.log4j.LogManager;\n"
					+   "import org.apache.logging.log4j.Logger;\n"
					+   "import org.apache.logging.log4j.io.IoBuilder;\n"
					+   "import io.restassured.filter.log.RequestLoggingFilter;\n"
					+   "import io.restassured.filter.log.ResponseLoggingFilter;\n"
					+   "import java.io.PrintStream;\n";
		}
		
		content +="\n";
		
		return content;
	}
	
	private String generateClassName(String className,Object[] test) {
		String classNameGenerator = "@FixMethodOrder(MethodSorters.NAME_ASCENDING)\n"
			 + "public class " + className + " {\n\n";
		/*String filePath = testFilePath+"/"+className+".java";
		String renamePath = testFilePath+"/"+test[classNameCounter]+".java";
		System.out.println(test[classNameCounter]);
		File filename = new File(renamePath);
		File changeFilename = new File(renamePath);
		boolean flag = filename.renameTo(changeFilename);
		System.out.println("Flag:"+flag);
		if(flag) {
			   classNameGenerator = "@FixMethodOrder(MethodSorters.NAME_ASCENDING)\n"
			   + "public class " +  test[classNameCounter] + " {\n\n";
		}
		    classNameCounter++;*/
       return classNameGenerator;
	}
	
	private String generateAttributes(String specPath) {
		String content = "";
		
//		if (OAIValidation)
		content += "\tprivate static final String OAI_JSON_URL = \"" + specPath + "\";\n"
				+  "\tprivate static final StatusCode5XXFilter statusCode5XXFilter = new StatusCode5XXFilter();\n"
				+  "\tprivate static final NominalOrFaultyTestCaseFilter nominalOrFaultyTestCaseFilter = new NominalOrFaultyTestCaseFilter();\n"
				+  "\tprivate static final ResponseValidationFilter validationFilter = new ResponseValidationFilter(OAI_JSON_URL);\n";

		if (logToFile) {
			content +=  "\tprivate static RequestLoggingFilter requestLoggingFilter;\n"
					+   "\tprivate static ResponseLoggingFilter responseLoggingFilter;\n"
					+   "\tprivate static Logger logger;\n";
		}


		if (allureReport)
			content += "\tprivate static final AllureRestAssured allureFilter = new AllureRestAssured();\n";

		if (enableStats || enableOutputCoverage) { // This is only needed to export output data to the proper folder
			content += "\tprivate static final String APIName = \"" + APIName + "\";\n"
					+  "\tprivate static final String testId = \"" + testId + "\";\n"
					+  "\tprivate static final CSVFilter csvFilter = new CSVFilter(APIName, testId);\n"
					+  "\tprivate static Xls_Reader xls;\n";
		}

		content += "\n";
		
		return content;
	}
	
	private String generateSetUp(String baseURI) {
		String content = "";

		content += "\t@BeforeClass\n "
				+  "\tpublic static void setUp() {\n"
			  	+  "\t\tRestAssured.baseURI = " + "\"" + baseURI + "\";\n";

		if (logToFile) {
			content +=  "\t\tSystem.setProperty(\"logFilename\", \"" + System.getProperty("logFilename") + "\");"
					+   "\t\tlogger = LogManager.getLogger(" + className + ".class.getName());\n"
					+   "\t\tPrintStream logStream = IoBuilder.forLogger(logger).buildPrintStream();\n"
					+   "\t\trequestLoggingFilter = RequestLoggingFilter.logRequestTo(logStream);\n"
					+   "\t\tresponseLoggingFilter = new ResponseLoggingFilter(logStream);\n";
		}

		content += "\t\txls = new Xls_Reader( " + "\"" + testFilePath+"/XLSData/APTData.xlsx" + "\");\n";

		if (enableStats || enableOutputCoverage) {
			content += "\t\tstatusCode5XXFilter.setAPIName(APIName);\n"
					+  "\t\tstatusCode5XXFilter.setTestId(testId);\n"
					+  "\t\tnominalOrFaultyTestCaseFilter.setAPIName(APIName);\n"
					+  "\t\tnominalOrFaultyTestCaseFilter.setTestId(testId);\n"
					+  "\t\tvalidationFilter.setAPIName(APIName);\n"
					+  "\t\tvalidationFilter.setTestId(testId);\n";
		}

		content += "\t}\n\n";

		return content;
	}

	private String generateTest(TestCase t, int instance, int count) {

		String content="";
		
		// Generate test method header
		content += generateMethodHeader1(t,instance,count);

		// Generate test case ID (only if stats enabled)
		content += generateTestCaseId(t.getId());

		// Generate initialization of filters for those that need it
		content += generateFiltersInitialization(t);

		// Generate the start of the try block
		content += generateTryBlockStart();

		// Generate all stuff needed before the RESTAssured request
		content += generatePreRequest(t);
		
		// Generate RESTAssured object pointing to the right path
		content += generateRESTAssuredObject(t,count);
		
		// Generate header parameters
		content += generateHeaderParameters(t);
		
		// Generate query parameters
		content += generateQueryParameters(t);
		
		// Generate path parameters
		content += generatePathParameters(t);

		//Generate form-data parameters
		content += generateFormParameters(t);

		// Generate body parameter
		content += generateBodyParameter(t);

		// Generate filters
		content += generateFilters(t);
		
		// Generate HTTP request
		content += generateHTTPRequest(t);
		
		// Generate basic response validation
		//if(!OAIValidation)
//			content += generateResponseValidation(t);

		// Generate all stuff needed after the RESTAssured response validation
		content += generatePostResponseValidation(t);

		// Generate the end of the try block, including its corresponding catch
		content += generateTryBlockEnd();
		
		// Close test method
		content += "\t}\n\n";
		
		
		return content;
	}


	private String generateMethodHeader(TestCase t, int instance) {
		String name = getDifferenceJSON(t);
		
		return "\t@Test\n" +
				"\t@DisplayName("+"\"" + name + "\""+") \n" +
				"\tpublic void " + t.getId()+"() {\n";

	}
	
	
	

	private String generateMethodHeader1(TestCase t, int instance,int count) {
		String val = getDifferenceJSON(t);
		String valCoverage = val;
		val = val.replace("~~~", "");
		
		String endpoint = t.getPath();
		endpoint = val+"_"+endpoint;
		
		String API_NAME = t.getId().split("_")[2];
		
		if(val.startsWith("remove") || val.startsWith("add") || val.startsWith("replace"))
		{
			//coverage.add(val+":"+endpoint+":"+API_NAME);
			coverage.add(t.getPath()+"###"+valCoverage);
		}
		
		//String annotation = "";
		String annotation = "\t@Test\n" +
				"\t@DisplayName(" + "\"" + API_NAME + "-" + val + "\"" + ") \n" +
				"\tpublic void " + t.getId() + "() {\n";

		if(count==0){
			String NEW_API_NAME=API_NAME+"_UNAUTHORIZED";
			return annotation.replace(API_NAME,NEW_API_NAME);
		}else {
			if (display.contains(endpoint)) {
				annotation = "\t//@Test\n" +
						"\t//@DisplayName(" + "\"" + API_NAME + "-" + val + "\"" + ") \n" +
						"\tpublic void " + t.getId() + "() {\n";
			}
		}
		display.add(endpoint);


		return annotation;
	}

	private String generateTestCaseId(String testCaseId) {
		String content = "";

		if (enableStats || enableOutputCoverage) {
			content += "\t\tString testResultId = \"" + testCaseId + "\";\n" +
					   "\t\t//delay(1);\n" ;
		}

		return content;
	}

	private String generateFiltersInitialization(TestCase t) {
		String content = "";

		content += "\t\tnominalOrFaultyTestCaseFilter.updateFaultyData(" + t.getFaulty() + ", " + t.getFulfillsDependencies() + ", \"" + t.getFaultyReason() + "\");\n" +
				"\t\tstatusCode5XXFilter.updateFaultyData(" + t.getFaulty() + ", " + t.getFulfillsDependencies() + ", \"" + t.getFaultyReason() + "\");\n";

		if (enableStats || enableOutputCoverage)
			content += "\t\tcsvFilter.setTestResultId(testResultId);\n" +
					"\t\tstatusCode5XXFilter.setTestResultId(testResultId);\n" +
					"\t\tnominalOrFaultyTestCaseFilter.setTestResultId(testResultId);\n" +
					"\t\tvalidationFilter.setTestResultId(testResultId);\n";


		content += "\t\tString endpointCheck = \"" + t.getPath() + "\";\n" ;

		content += "\t\tboolean execute=getXLSValue(xls,endpointCheck);\n";
		content += "\t\tassumeTrue(execute);\n";
		content += "\t\tassertEquals(\"run\", \"RUN\".toLowerCase());\n";


		content += "\n";

		return content;
	}

	private String generateTryBlockStart() {
		return "\t\ttry {\n";
	}

	private String generatePreRequest(TestCase t) {
		String content = "";

		/*if (t.getBodyParameter() != null) {
			content += generateJSONtoObjectConversion(t);
		}*/
		if (t.getBodyParameter() != null) {
			content += generateJSONtoObjectConversionNew(t);
		}

		return content;
	}

	private String generateJSONtoObjectConversion(TestCase t) {
		String content = "";
		String bodyParameter = escapeJava(t.getBodyParameter());
		System.out.println(bodyParameter);
		String testName = t.getId();
		createFileIfNotExists(testFilePath+ "/TestJSON/" + testName);
		content += "\t\t\tObjectMapper objectMapper = new ObjectMapper();\n"
				+  "\t\t\tJsonNode jsonBody =  objectMapper.readTree(\""
				+  bodyParameter
				+  "\");\n\n";

		return content;
	}

	private String generateJSONtoObjectConversionNew(TestCase t) {
		String content = "";
		//String bodyParameter = (escapeJava(t.getBodyParameter())).replace("\\","");
		String bodyParameter = t.getBodyParameter();
		String testName = t.getId();

        String filePath = testFilePath+ "/TestJSON/" + testName;
		createFileIfNotExists(filePath);
		writeFile(filePath,bodyParameter);

		content += "\t\t\tObjectMapper objectMapper = new ObjectMapper();\n"
				+  "\t\t\tString json =  readFile(\""
				+   filePath
				+  "\");\n\n"
				+  "\t\t\tJsonNode jsonBody =  objectMapper.readTree("
				+  "json"
				+  ");\n\n";

		return content;
	}

	private String getDifferenceJSON(TestCase t) 
	{
		String content = t.getId();
		
		boolean isBodyParameterEmptyForPUTAndPOST = false;
		
		if(t.getMethod().toString().toLowerCase().equals("post") || t.getMethod().toString().toLowerCase().equals("put")) 
		{
			//String bodyParameter = (escapeJava(t.getBodyParameter())).replace("\\", "");
			
			if(!t.getInputFormat().contains("form-data"))
			{
				String bodyParameter = t.getBodyParameter();
				
				if(!(bodyParameter == null || bodyParameter.equalsIgnoreCase("{}")))
				{
					System.out.println("Modified ====>"+bodyParameter);
					
					String testName = t.getId();

					//System.out.println("testSourceData ->"+ testSourceData);
					ObjectMapper objectMapper = new ObjectMapper();
					try {
						//JsonNode actualJson = objectMapper.readTree(bParameter.get(tCount));
						JsonNode actualJson = objectMapper.readTree(t.getTestdataBeforeModification());
						System.out.println("Actual ====>"+actualJson.toString());
						tCount++;
						JsonNode mutatedJson = objectMapper.readTree(bodyParameter);
						JsonNode patch = JsonDiff.asJson(actualJson, mutatedJson);
						//System.out.println("1diffs \n"+patch.toString());
						//String diffs = patch.toString().replace("[", "").replace("]", "").replace("//", "");
						
						String difference = patch.toString();
						String diffs = difference.substring(1, difference.length() - 1).replace("//", "");
						JsonNode mutations = objectMapper.readTree(diffs);
						
						JsonNode op = mutations.get("op");
						JsonNode path = mutations.get("path");
						JsonNode secondPath = null;
						JsonNode value_node = mutations.get("value");
						
						if(value_node != null && !value_node.asText().equals("") && value_node.asText().contains("{") && value_node.asText().contains("}"))
						{
							String sPath= path.asText();
							String sMutatedSubNode = mutatedJson.at(sPath).toString();
							String sActualSubNode = actualJson.at(sPath).toString();
							
							sMutatedSubNode = sMutatedSubNode.substring(1, sMutatedSubNode.length()-1).replace("\\", "");
							sActualSubNode = sActualSubNode.substring(1, sActualSubNode.length()-1).replace("\\", "");
							
							JsonNode mutatedSubNode = objectMapper.readTree(sMutatedSubNode);
							JsonNode actualSubNode = objectMapper.readTree(sActualSubNode);
							
							//System.out.println("R M--->"+mutatedSubNode.toString());
							//System.out.println("R A--->"+ actualSubNode.toString());
							patch = JsonDiff.asJson(actualSubNode, mutatedSubNode);
							//diffs = patch.toString().replace("[", "").replace("]", "").replace("//", "");
							
							difference = patch.toString();
							diffs = difference.substring(1, difference.length() - 1).replace("//", "");
							
							//System.out.println("2diffs \n"+diffs);
							mutations = objectMapper.readTree(diffs);
							//System.out.println("3diffs \n"+patch.toString());
							value_node = mutations.get("value");
							secondPath = mutations.get("path");
							
						}
						
						String val = "";
						if (value_node == null || value_node.isNull())
							val = "null";
						else if (value_node.asText().equals(""))
							val = "blank";
						else {
							val = value_node.asText();
						}

						valGlobal = t.getPath();
						String storedValue = "";
						if (op != null && path != null) {

							String ele = path.asText()+System.getProperty("endpoint");
							
							if(secondPath != null)
							{
								ele = path.asText()+secondPath.asText()+System.getProperty("endpoint");
							}
							
							if (eleGlobalTable.containsKey(ele)) {
								storedValue = eleGlobalTable.get(ele);
								if (val.length() >= 17)
									val = "Variable";
								else if (storedValue.length() == val.length() && !val.equals("null"))
									val = "Var";
								else if (storedValue.length() > val.length()+2 && !val.equals("null"))
									val = "Var";
								else if (storedValue.length() < val.length()-2 && !val.equals("null"))
									val = "Var";
								else if (storedValue.length()+2 > val.length() && !val.equals("null"))
									val = "Var";
								else if (storedValue.length()-2 < val.length() && !val.equals("null"))
									val = "Var";
								else if (storedValue.length() - val.length() >= 1 && !val.contains("-"))
									val = "Var";
							}
							
							String mPath = path.toString().replace("/", "~~~");
							content = (op + "_" + mPath +"_" + val).replace("/", "").replace("\"", "");
							if(secondPath != null)
							{
								String mSeconPath = secondPath.toString().replace("/", "~~~");
								content = (op + "_" + mPath +mSeconPath+"_" + val).replace("/", "").replace("\"", "");
							}
							else
							{
								content = (op + "_" + mPath +"_" + val).replace("/", "").replace("\"", "");
							}
							
							System.out.println("content \n"+content);	
							eleGlobalTable.put(ele, val);

						}
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}
				else
				{
					isBodyParameterEmptyForPUTAndPOST = true;
				}
			}
			else
			{
				StringBuffer temp = new StringBuffer();
				temp.append(t.getPath().substring(1).replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "")+"_");
				boolean firstValue= true;
				
				for( Entry<String,String> entry: t.getFormParameters().entrySet())
				{
					String formParameterValue = entry.getValue();
					formParameterValue = formParameterValue.replaceAll("\"", "");
					
					if(firstValue)
					{
						temp.append(entry.getKey()+":"+formParameterValue);
						firstValue = false;
					}
					else
					{
						temp.append("-"+entry.getKey()+":"+formParameterValue);
					}
					
					
				}
				content = temp.toString();
			}
		}
		if(t.getMethod().toString().toLowerCase().equals("get") || t.getMethod().toString().toLowerCase().equals("delete") || isBodyParameterEmptyForPUTAndPOST)
		{
			StringBuffer temp = new StringBuffer();
			temp.append(t.getPath().substring(1).replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "")+"_");
			boolean firstValue= true;
			
			for( Entry<String,String> entry: t.getPathParameters().entrySet())
			{
				if(firstValue)
				{
					temp.append(entry.getKey()+":"+entry.getValue());
					firstValue = false;
				}
				else
				{
					temp.append("-"+entry.getKey()+":"+entry.getValue());
				}
				
				
			}
			firstValue= true;
			for( Entry<String,String> entry: t.getQueryParameters().entrySet())
			{
				if(firstValue)
				{
					temp.append(entry.getKey()+":"+entry.getValue());
					firstValue = false;
				}
				else
				{
					temp.append("-"+entry.getKey()+":"+entry.getValue());
				}
			}
			
			content = temp.toString();
			
		}
		
		return content;
			
	}

	private String generateRESTAssuredObject(TestCase t,int count) {
		String content = "";
		if(count == 0) {
			content += "\t\t\tResponse response = RestAssured\n"
					+  "\t\t\t.given().cookies(Collections.singletonMap(\"SAAS_COMMON_BASE_TOKEN_ID\",\"560d9b63-150d-4c83-9524\"))\n";
		}else {
			content += "\t\t\tResponse response = RestAssured\n"
					+ "\t\t\t.given().cookies(cookies)\n";
		}
//		if (logging)
//			content +="\t\t\t\t.log().ifValidationFails()\n";
		if (logging)
			content +="\t\t\t\t.log().all()\n";

		return content;
	}
	
	private String generateHeaderParameters(TestCase t) {
		String content = "";
		
		for(Entry<String,String> param: t.getHeaderParameters().entrySet())
			content += "\t\t\t\t.header(\"" + param.getKey() + "\", \"" + escapeJava(param.getValue()) + "\")\n";
		
		return content;
	}
	
	private String generateQueryParameters(TestCase t) {
		String content = "";
		
		for(Entry<String,String> param: t.getQueryParameters().entrySet())
			content += "\t\t\t\t.queryParam(\"" + param.getKey() + "\", \"" + escapeJava(param.getValue()) + "\")\n";
		
		return content;
	}
	
	private String generatePathParameters(TestCase t) {
		String content = "";
		
		for(Entry<String,String> param: t.getPathParameters().entrySet())
			content += "\t\t\t\t.pathParam(\"" + param.getKey() + "\", \"" + escapeJava(param.getValue()) + "\")\n";
		
		return content;
	}

	private String generateFormParameters(TestCase t) {
		String content = "";
		content += "\t\t\t\t.contentType(\""+t.getInputFormat()+"\")\n";
		
//		if(t.getFormParameters().entrySet().stream().anyMatch(x -> checkIfExists(x.getValue())))
//			content += "\t\t\t\t.contentType(\"multipart/form-data\")\n";
//		else if(!t.getFormParameters().isEmpty())
//			content += "\t\t\t\t.contentType(\"application/x-www-form-urlencoded\")\n";

		
		for(Entry<String,String> param : t.getFormParameters().entrySet()) {
			content += checkIfExists(param.getValue())? "\t\t\t\t.multiPart(\"" + param.getKey() +  "\", new File(\"" + escapeJava(param.getValue()) + "\"))\n"
					: "\t\t\t\t.multiPart(\"" + param.getKey() + "\", \"" + escapeJava(param.getValue()) + "\")\n";
		}

		return content;
	}

	private String generateBodyParameter(TestCase t) {
		String content = "";

		if ((t.getFormParameters() == null || t.getFormParameters().size() == 0) &&
				t.getBodyParameter() != null &&
				(t.getMethod().equals(HttpMethod.POST) || t.getMethod().equals(HttpMethod.PUT)
				|| t.getMethod().equals(HttpMethod.PATCH) || t.getMethod().equals(HttpMethod.DELETE)))
			content += "\t\t\t\t.contentType(\"application/json\")\n";
		if (t.getBodyParameter() != null) {
			content += "\t\t\t\t.body(jsonBody)\n";
		}

		return content;
	}

	private String generateFilters(TestCase t) {
		String content = "";

		if(logToFile) {
			content += "\t\t\t\t.filter(requestLoggingFilter)\n"
					+  "\t\t\t\t.filter(responseLoggingFilter)\n";
		}

//		if (enableOutputCoverage) // Coverage filter
//			content += "\t\t\t\t.filter(new CoverageFilter(testResultId, APIName))\n";
		if (allureReport) // Allure filter
			content += "\t\t\t\t.filter(allureFilter)\n";
		// 5XX status code oracle:
		content += "\t\t\t\t.filter(statusCode5XXFilter)\n";
		// Validation of nominal and faulty test cases
		content += "\t\t\t\t.filter(nominalOrFaultyTestCaseFilter)\n";
//		if (OAIValidation)
//		content += "\t\t\t\t.filter(validationFilter)\n";
		if (enableStats || enableOutputCoverage) // CSV filter
			content += "\t\t\t\t.filter(csvFilter)\n";

		return content;
	}
	
	private String generateHTTPRequest(TestCase t) {
		String content = "\t\t\t.when()\n";

		content +=	 "\t\t\t\t." + t.getMethod().name().toLowerCase() + "(\"" + t.getPath() + "\");\n";
		
		// Create response log
//		if (logging) {
//			content += "\n\t\t\tresponse.then().log().ifValidationFails();"
//			         + "\n\t\t\tresponse.then().log().ifError();\n";
//		}
		content += "\n\t\t\tresponse.then()";
		if (logging)
			content += ".log().all()";
		content += ";\n";
		
//		if (OAIValidation)
//			content += "\t\t} catch (RuntimeException ex) {\n"
//					+  "\t\t\tSystem.err.println(\"Validation results: \" + ex.getMessage());\n"
//					+  "\t\t\tfail(\"Validation failed\");\n"
//					+	"\t\t}\n";
		
		//content += "\n";
		
		return content;
	}

	private String getEndpointAsString(TestCase t){
		return t.getPath().replace("//","_");
	}
	
	
//	private String generateResponseValidation(TestCase t) {
//		String content = "";
//		String expectedStatusCode = null;
//		boolean thereIsDefault = false;
//
//		// Get status code of the expected response
//		for (Entry<String, Response> response: t.getExpectedOutputs().entrySet()) {
//			if (response.getValue().equals(t.getExpectedSuccessfulOutput())) {
//				expectedStatusCode = response.getKey();
//			}
//			// If there is a default response, use it if the expected status code is not found
//			if (response.getKey().equals("default")) {
//				thereIsDefault = true;
//			}
//		}
//
//		if (expectedStatusCode == null && !thereIsDefault) {
//			// Default expected status code to 200
//			expectedStatusCode = "200";
//		}
//
//		// Assert status code only if it was found among possible status codes. Otherwise, only JSON structure will be validated
//		//TODO: Improve oracle of status code
////		if (expectedStatusCode != null) {
////			content = "\t\t\tresponse.then().statusCode("
////					+ expectedStatusCode
////					+ ");\n\n";
////		}
////		content = "\t\t\tassertTrue(\"Received status 500. Server error found.\", response.statusCode() < 500);\n";
//
//		return content;
//
//		/*String content = "\t\tswitch(response.getStatusCode()) {\n";
//		boolean hasDefaultCase = false;
//
//		for(Entry<String, Response> response: t.getExpectedOutputs().entrySet()) {
//
//			// Default response
//			if (response.getKey().equals("default")) {
//				content += "\t\t\tdefault:\n";
//				hasDefaultCase = true;
//			} else		// Specific HTTP code
//				content += "\t\tcase " + response.getKey() + ":\n";
//
//
//				content += "\t\t\tresponse.then().contentType(\"" + t.getOutputFormat() + "\");\n";
//
//				//TODO: JSON validation
//				content += "\t\t\tbreak;\n";
//		}
//
//		if (!hasDefaultCase)
//			content += "\t\tdefault: \n"
//					+ "\t\t\tSystem.err.println(\"Unexpected HTTP code: \" + response.getStatusCode());\n"
//					+ "\t\t\tfail();\n"
//					+ "\t\t\tbreak;\n";
//
//		// Close switch sentence
//		content += "\t\t}\n";
//
//		return content;*/
//	}

	private String generatePostResponseValidation(TestCase t) {
		
		String content = "\t\t\tSystem.out.println(\"Test passed.\");\n";

		if (t.getBodyParameter() != null) {
			content += "\t\t} catch (IOException e) {\n"
					+  "\t\t\te.printStackTrace();\n";
		}

		return content;
	}

	private String generateTryBlockEnd() {
		return "\t\t} catch (RuntimeException ex) {\n"
				+  "\t\t\tSystem.err.println(ex.getMessage());\n"
				+  "\t\t\tfail(ex.getMessage());\n"
				+  "\t\t}\n";
	}
		
	private String saveToFile(String path, String className, String contentFile) {
		String path1 = path + "/" + className + ".java";
		try(FileWriter testClass = new FileWriter(path1)) {
			testClass.write(contentFile);
			testClass.flush();
		} catch(Exception ex) {
			logger.error("Error writing test file");
			logger.error("Exception: ", ex);
		}
		return path1;
	}

	private String getPath(String path, String className) {
	//	String path1 = path + "/" + className + ".java";
		String path1 = className;
		return path1;
	}

	public boolean OAIValidation() {
		return OAIValidation;
	}

	public void setOAIValidation(boolean oAIValidation) {
		OAIValidation = oAIValidation;
	}

	public boolean isLogging() {
		return logging;
	}

	public void setLogging(boolean logging) {
		this.logging = logging;
	}
	
	public boolean allureReport() {
		return allureReport;
	}

	public void setAllureReport(boolean ar) {
		this.allureReport = ar;
	}

	public boolean getEnableStats() {
		return enableStats;
	}

	public void setEnableStats(boolean enableStats) {
		this.enableStats = enableStats;
	}

	public boolean isEnableOutputCoverage() {
		return enableOutputCoverage;
	}

	public void setEnableOutputCoverage(boolean enableOutputCoverage) {
		this.enableOutputCoverage = enableOutputCoverage;
	}

	public String getSpecPath() {
		return specPath;
	}

	public void setSpecPath(String specPath) {
		this.specPath = specPath;
	}

	public String getTestFilePath() {
		return testFilePath;
	}

	public void setTestFilePath(String testFilePath) {
		this.testFilePath = testFilePath;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getBaseURI() {
		return baseURI;
	}

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	public String getAPIName() {
		return APIName;
	}

	public void setAPIName(String APIName) {
		this.APIName = APIName;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}
}
