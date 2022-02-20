package es.us.isa.restest.apichain.api;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Random;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;

import es.us.isa.restest.apichain.api.util.APIObject;
import es.us.isa.restest.apichain.api.util.Header;

import es.us.isa.restest.apichain.api.util.ITestWriter;
import es.us.isa.restest.apichain.api.util.QueryParameter;
import es.us.isa.restest.mutation.operators.JsonMutatorLocal;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.testcases.writers.IWriter;
import es.us.isa.restest.util.authentication;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static es.us.isa.restest.inputs.perturbation.ObjectPerturbator.beforeMutate;
import static es.us.isa.restest.util.FileManager.*;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;

/** This class defines a test writer for the REST Assured framework. It creates a Java class with JUnit test cases
 * ready to be executed.
 * 
 * @author Sergio Segura &amp; Alberto Martin-Lopez
 *
 */
public class APIChainRESTAssuredWriter implements ITestWriter
{
	
	
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

	private static final Logger logger = LogManager.getLogger(APIChainRESTAssuredWriter.class.getName());
	
	private String testcaseID;
	private String testName;
	private String[] prefixes;
	private int counter;
	
	public APIChainRESTAssuredWriter(String testFilePath, String className, String packageName, String testEnvironment, Boolean logToFile) {
		this.testFilePath = testFilePath;
		this.className = className;
		this.packageName = packageName;
		this.baseURI = testEnvironment;
		this.logToFile = logToFile;
		
		prefixes = getPrefixInAlbhapeticalOrder();
	}
	
	/* (non-Javadoc)
	 * @see es.us.isa.restest.testcases.writers.IWriter#write(java.util.Collection)
	 */
	
	public void write(ArrayList<APIObject> apis) {
		
		// Initializing content
		String contentFile = "";
		
		// Generating imports
		contentFile += generateImports(packageName);
		
		// Generate className
		contentFile += generateClassName(className);
		
		// Generate attributes
		contentFile += generateAttributes(specPath);
		
		// Generate variables to be used.
		contentFile += generateSetUp(baseURI);


		// Generate tests
		int ntest=1;
		int count=0;
		for(APIObject api : apis) {
			if(count == 0) {
				contentFile += generateTest(api, ntest++, 0);
				count++;
			}
			else {
				contentFile += generateTest(api, ntest++, count);
			}

		}
		// Close class
		contentFile += "}\n";

		String s = authentication.authenticate(contentFile);
		//Save to file
		saveToFile(testFilePath,className,s);

		/* Test Compile
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		compiler.run(System.in, System.out, System.err, TEST_LOCATION + this.specification.getInfo().getTitle().replaceAll(" ", "") + "Test.java");
		*/
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
				+  "import static org.junit.Assert.assertTrue;\n"
				+  "import org.junit.runners.MethodSorters;\n"
		        +  "import io.qameta.allure.restassured.AllureRestAssured;\n"
				+  "import es.us.isa.restest.testcases.restassured.filters.StatusCode5XXFilter;\n"
				+  "import es.us.isa.restest.testcases.restassured.filters.NominalOrFaultyTestCaseFilter;\n"
				+  "import java.io.File;\n"
				+  "import static es.us.isa.restest.util.FileManager.*;\n"
				+  "import java.util.*;\n"
		        +  "import java.util.Map;\n"
				+ "import es.us.isa.restest.apichain.api.APIChainMapper;\n";
		
		// OAIValidation (Optional)
//		if (OAIValidation)
		content += 	"import es.us.isa.restest.testcases.restassured.filters.ResponseValidationFilter;\n";

//		// Coverage filter (optional)
//		if (enableOutputCoverage)
//			content += 	"import es.us.isa.restest.testcases.restassured.filters.CoverageFilter;\n";

		// Coverage filter (optional)
		//if (enableStats || enableOutputCoverage)
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
	
	private String generateClassName(String className) {
		return "@FixMethodOrder(MethodSorters.NAME_ASCENDING)\n"
				 + "public class " + className + " {\n\n";

	}
	
	
	private String[] getPrefixInAlbhapeticalOrder()
	{
		String str;
		int counter=0;
		String[] prefixes = new String[500];
		String[] rawList = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
		for(int i = 0; i <= 25; i++)
		{
			if(counter == 500)
				break;
			for(int j=(i+1); j <= 25; j++ )
			{
				if(counter == 500)
					break;
				for(int k=j; k <= 25; k++ )
				{
					if(counter == 500)
						break;
					str = rawList[i]+rawList[j]+rawList[k];
					prefixes[counter++]=str;
				}
			}
		}
		return prefixes;
		
	}
	
	private String generateAttributes(String specPath) {
		String content = "";
		
//		if (OAIValidation)
		content += "\tprivate static final String OAI_JSON_URL = \"" + specPath + "\";\n"
				+  "\tprivate static final StatusCode5XXFilter statusCode5XXFilter = new StatusCode5XXFilter();\n"
				+  "\tprivate static final NominalOrFaultyTestCaseFilter nominalOrFaultyTestCaseFilter = new NominalOrFaultyTestCaseFilter();\n";
				//+  "\tprivate static final ResponseValidationFilter validationFilter = new ResponseValidationFilter(OAI_JSON_URL);\n";

		if (logToFile) {
			content +=  "\tprivate static RequestLoggingFilter requestLoggingFilter;\n"
					+   "\tprivate static ResponseLoggingFilter responseLoggingFilter;\n"
					+   "\tprivate static Logger logger;\n";
		}


		if (allureReport)	
			content += "\tprivate static final AllureRestAssured allureFilter = new AllureRestAssured();\n";

		//if (enableStats || enableOutputCoverage) { // This is only needed to export output data to the proper folder
			content += "\tprivate static final String APIName = \"" + APIName + "\";\n"
					+  "\tprivate static final String testId = \"" + testId + "\";\n"
					+  "\tprivate static final CSVFilter csvFilter = new CSVFilter(APIName, testId);\n";
		//}

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

	private String generateTest(APIObject api, int instance, int count) {
		String content="";
		
		// Generate test method header
		content += generateMethodHeader(api,instance);

		// Generate test case ID (only if stats enabled)
		content += generateTestCaseId(api.getId());

		// Generate initialization of filters for those that need it
		//content += generateFiltersInitialization(api);

		// Generate the start of the try block
		content += generateTryBlockStart();

		content += addTestCaseIDAsVariable();
		
		// Generate all stuff needed before the RESTAssured request
		content += generatePreRequest(api);
		
		// Generate RESTAssured object pointing to the right path
		content += generateRESTAssuredObject(api,count);
		
		// Generate header parameters
		content += generateHeaderParameters(api);
		
		// Generate query parameters
		content += generateQueryParameters(api);
		
		// Generate path parameters
		//content += generatePathParameters(api);

		//Generate form-data parameters
		//content += generateFormParameters(api);

		// Generate body parameter
		content += generateBodyParameter(api);

		// Generate filters
		content += generateFilters(api);
		
		// Generate HTTP request
		content += generateHTTPRequest(api);
		
		// Generate basic response validation
		//if(!OAIValidation)
		content += generateResponseValidation(api);

		// Generate all stuff needed after the RESTAssured response validation
		content += generatePostResponseValidation(api);

		// Generate the end of the try block, including its corresponding catch
		content += generateTryBlockEnd();
		
		// Close test method
		content += "\t}\n\n";
		
		return content;
	}


	private String generateMethodHeader(APIObject t, int instance) {

		/*Object name = JsonMutatorLocal.propertytype.toArray()[c];
		//System.out.println(c +"--"+name+"--"+t.getId());*/
		
		Random random = new Random();   
		testcaseID = t.getId().substring(2).replaceAll("-", "")+"_"+random.nextInt(10000);
		testName = t.getId();
		return "\t@Test\n" +
				"\t@DisplayName("+"\"" + t.getId() + "\""+") \n" +
				"\tpublic void " + prefixes[counter++] +"_"+testcaseID +"() {\n";
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

		content += "\n";

		return content;
	}

	private String generateTryBlockStart() {
		return "\t\ttry {\n";
	}

	private String addTestCaseIDAsVariable() {
		return "\t\t\tString testName = \""+testName+"\";\n System.out.println(\"CHECKCHECK\"+testName);\n";
	}
	
	private String generatePreRequest(APIObject api) {
		String content = "";

		if (api.getRequest().getBody() != null) {
			content += generateJSONtoObjectConversionNew(api);
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

	private String generateJSONtoObjectConversionNew(APIObject api) {
		String content = "";
		String bodyParameter = (escapeJava(api.getRequest().getBody())).replace("\\","");
		String testName = api.getId();

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

		if(api.getMethod().equalsIgnoreCase("POST") || api.getMethod().equalsIgnoreCase("PUT"))
		{
			content += "\t\t\tjsonBody = APIChainMapper.fillMappings(testName, jsonBody);\n";
		}
		
		return content;
	}

	private String getDifferenceJSON(TestCase t) {
		String content = "";
		String bodyParameter = (escapeJava(t.getBodyParameter())).replace("\\", "");
		String testName = t.getId();

        //System.out.println("testSourceData ->"+ testSourceData);
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode actualJson = beforeMutate;
			JsonNode mutatedJson = objectMapper.readTree(bodyParameter);
			JsonNode patch = JsonDiff.asJson(actualJson, mutatedJson);
			String diffs = patch.toString().replace("[","").replace("]","").replace("//","");
			System.out.println(diffs);
			JsonNode mutations = objectMapper.readTree(diffs);
			JsonNode op = mutations.get("op");
			JsonNode path = mutations.get("path");
			JsonNode value = mutations.get("value");
			String val = "";
			if(value == null)
				val = "null";
			else
				val = value.asText();
			if(op!=null && path!=null)
				content = (op+"_"+path+"_"+val).replace("/","").replace("\"","");

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return content;
	}

	private String generateRESTAssuredObject(APIObject t,int count) {
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
	
	private String generateHeaderParameters(APIObject api) {
		String content = "";
		
		if(api.getRequest().getHeaders() != null)
		{
			for(Header param: api.getRequest().getHeaders())
				content += "\t\t\t\t.header(\"" + param.getHeaderName() + "\", \"" + escapeJava(param.getHeaderValue()) + "\")\n";
		}
		return content;
	}
	
	private String generateQueryParameters(APIObject api) {
		String content = "";
		
		if(api.getRequest().getQueryparams() != null)
		{
			for(QueryParameter param: api.getRequest().getQueryparams())
				content += "\t\t\t\t.queryParam(\"" + param.getQueryParameterName() + "\", \"" + escapeJava(param.getQueryParameterValue()) + "\")\n";
		}
		
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

		if(t.getFormParameters().entrySet().stream().anyMatch(x -> checkIfExists(x.getValue())))
			content += "\t\t\t\t.contentType(\"multipart/form-data\")\n";
		else if(!t.getFormParameters().isEmpty())
			content += "\t\t\t\t.contentType(\"application/x-www-form-urlencoded\")\n";

		for(Entry<String,String> param : t.getFormParameters().entrySet()) {
			content += checkIfExists(param.getValue())? "\t\t\t\t.multiPart(\"" + param.getKey() +  "\", new File(\"" + escapeJava(param.getValue()) + "\"))\n"
					: "\t\t\t\t.formParam(\"" + param.getKey() + "\", \"" + escapeJava(param.getValue()) + "\")\n";
		}

		return content;
	}

	private String generateBodyParameter(APIObject api) {
		String content = "";

		if (api.getRequest().getBody() != null &&
				(api.getMethod().equals(HttpMethod.POST) || api.getMethod().equals(HttpMethod.PUT)
				|| api.getMethod().equals(HttpMethod.PATCH) || api.getMethod().equals(HttpMethod.DELETE)))
			content += "\t\t\t\t.contentType(\"application/json\")\n";
		if (api.getRequest().getBody() != null) {
			content += "\t\t\t\t.body(jsonBody)\n";
		}

		return content;
	}

	private String generateFilters(APIObject api) {
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
		//content += "\t\t\t\t.filter(validationFilter)\n";
		if (enableStats || enableOutputCoverage) // CSV filter
			content += "\t\t\t\t.filter(csvFilter)\n";

		return content;
	}
	
	private String generateHTTPRequest(APIObject api) {
		String content = "\t\t\t.when()\n";

		content +=	 "\t\t\t\t." + api.getMethod().toLowerCase() + "(\"" + api.getName() + "\");\n";
		
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
	
	
	private String generateResponseValidation(APIObject api) {
		String content = "";
		String expectedStatusCode = null;
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
			expectedStatusCode = "200";
//		}
//
//		// Assert status code only if it was found among possible status codes. Otherwise, only JSON structure will be validated
//		//TODO: Improve oracle of status code
////		if (expectedStatusCode != null) {
				content = "\t\t\tresponse.then().statusCode("
						+ expectedStatusCode
						+ ");\n\n";
////		}
////		content = "\t\t\tassertTrue(\"Received status 500. Server error found.\", response.statusCode() < 500);\n";
//
		return content;
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
	}

	private String generatePostResponseValidation(APIObject api) {
		
		
		String content = "\t\t\tAPIChainMapper.writeResponse(testName, response);\n"
					+"\t\t\tSystem.out.println(\"Test passed.\");\n";

		if (api.getRequest().getBody() != null) {
			content += "\t\t} catch (IOException e) {\n"
					+  "\t\t\te.printStackTrace();\n";
		}	

		return content;
	}

	private String generateTryBlockEnd() {
		
				
		String content = "\t\t} catch (RuntimeException ex) {\n"
				+  "\t\t\tSystem.err.println(ex.getMessage());\n"
				+  "\t\t\tfail(ex.getMessage());\n"
				+ "\t\t} catch (Exception e) {\n"
				+  "\t\t\tSystem.err.println(e.getMessage());\n"
				+  "\t\t\tfail(e.getMessage());\n"
				+  "\t\t}\n";
	
		return content;
		
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
