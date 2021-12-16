package es.us.isa.restest.util;




import es.us.isa.restest.specification.OpenAPISpecification;
import io.restassured.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static es.us.isa.restest.util.FileManager.checkIfExists;
import static es.us.isa.restest.util.FileManager.createDir;


public class XlsManager {

    // Properties file with configuration settings
    private static String propertiesFilePath = "src/test/resources/zycus_new/zycus.properties";
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
    private static String xlsDir;
    private static String excelBaseDir;

    private static Logger logger = LogManager.getLogger(XlsManager.class.getName());

    public static String testDataDirectoryPath;
    public static Xls_Reader xls;
    public static final String ENDPOINT = "Endpoint";
    public static final String DEPENDENCY = "Dependency_Endpoint";
    public static final String DEPENDENCY_METHOD = "Dep_Method";
    public static final String PARAMETER = "Path_Param";
    public static final String QUERYPARAM = "Query_Param";
    public static final String SheetName = "API";



    public static void main(String[] args) throws IOException {
        String filePath = "D:\\Code\\API-RESTTEST\\RestTest_10_AUG_NEW\\src\\test\\java\\CNS\\TestJSON\\cns_api_a_cns_notificationgroups";
        readParameterValues();
        xlsDir = targetDirJava+"/XLSData";
        createDir(xlsDir);
        FileUtils.copyFile(new File(excelBaseDir+"/APTData.xlsx"),new File(xlsDir+"/APIData.xlsx"));
        xls = new Xls_Reader(xlsDir+"/APIData.xlsx");
        xls.addSheet("API_NEW");
        xls.addColumn("API_NEW","Endpoint");

     /*   xls = new Xls_Reader(testDataDirectoryPath+"/APTData.xlsx");
        try
        {
            Response response = null;
            List<HashMap<String,String>> map = getTestParameters("/cns/api/a/cns/notificationgroups",xls);
            for(int i=0;i<map.size();i++) {
                System.out.println(map.get(i));
            }
          //  String endpoint = buildEndpoint(map);
           // System.out.println(endpoint);
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
    }
    public static Boolean createFileIfNotExists(String path) {
        File file = new File(path);
        try {
            file.createNewFile();
            file.setWritable(true);
            return true;
        } catch (IOException e) {
            logger.error("Exception: ", e);
        }
        return null;
    }

    public XlsManager() {
        readParameterValues();
        xls = new Xls_Reader(xlsDir+"/APIData.xlsx");
    }

    public static Xls_Reader createXLS(String source, String destination) {
        Xls_Reader xls = null;
        try {
            String path = System.getProperty("user.dir")+"/";
            String fileName = "/APIData.xlsx";
            if(!checkIfExists(destination+fileName)) {
                FileUtils.copyFile(new File(path+ source + fileName), new File(path+ destination + fileName));
            }
                xls = new Xls_Reader(destination + fileName);
                xls.addSheet("API");
                xls.addColumn("API", "Endpoint");
                xls.addColumn("API", "Method");
                xls.addColumn("API", "Execute");

        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
        return xls;
    }
    /*public static HashMap<String,String> getTestParameters(String endpoint,Xls_Reader xls){

        int row = xls.getRowCount(SheetName);
        HashMap<String,String> map = new HashMap<String,String>();
        String url = "";
        for(int i=2;i<row;i++){
           url = xls.getCellData(SheetName,ENDPOINT,i);
           if(url.trim().toLowerCase().equals(endpoint.toLowerCase())){
               for(int j=1;j<=xls.getColumnCount(SheetName);j++){
                   if(xls.getCellData(SheetName,j,1).toLowerCase().contains("reqvar")){
                       map.put(xls.getCellData(SheetName,j,1),xls.getCellData(SheetName,j,i));
                       map.put(xls.getCellData(SheetName,j+1,1),xls.getCellData(SheetName,j+1,i));
                   }
                       map.put(DEPENDENCY,xls.getCellData(SheetName,DEPENDENCY,i));
                       map.put(DEPENDENCY_METHOD,xls.getCellData(SheetName,DEPENDENCY_METHOD,i).toLowerCase());
                       map.put(PARAMETER,xls.getCellData(SheetName,PARAMETER,i));
                       map.put(QUERYPARAM,xls.getCellData(SheetName,QUERYPARAM,i));

               }
           }
        }
        return map;
    }*/

    public static HashMap<String,String> getTestParameter(String endpoint,Xls_Reader xls){

        int row = xls.getRowCount(SheetName);
        HashMap<String,String> map = new HashMap<String,String>();
        String url = "";
        String columnVal="";
        String rowVal="";
        for(int i=2;i<row;i++) {
            url = xls.getCellData(SheetName, ENDPOINT, i);
            if (url.trim().toLowerCase().equals(endpoint.toLowerCase())) {
                for (int j = 1; j <= xls.getColumnCount(SheetName); j++) {
                    columnVal = xls.getCellData(SheetName, j, 1);
                    rowVal = xls.getCellData(SheetName, j, i);
                    map.put(columnVal, rowVal);
                    break;
                }
            }
        }
        return map;
    }

    public static List<HashMap<String,String>> getTestParameters(String endpoint,Xls_Reader xls){
        HashMap<String,String> map;
        int row = xls.getRowCount(SheetName);
        List<HashMap<String,String>> mapList = new ArrayList<HashMap<String,String>>();
        String url = "";
        String columnVal="";
        String rowVal="";
        for(int i=2;i<=row;i++) {
            url = xls.getCellData(SheetName, ENDPOINT, i);
            if (url.trim().toLowerCase().equals(endpoint.toLowerCase())) {
                map = new HashMap<String,String>();
                for (int j = 0; j <= xls.getColumnCount(SheetName); j++) {
                    columnVal = xls.getCellData(SheetName, j, 1);
                    rowVal = xls.getCellData(SheetName, j, i);
                    map.put(columnVal, rowVal);
                }
                mapList.add(map);
            }

        }
        return mapList;
    }

    public static List<String> getReplaceJsonObjectKeys(HashMap<String,String> map){
        List<String> replace = new ArrayList<String>();
        String key="";
        if(!map.isEmpty()){
            Set<String> keys = map.keySet();
            Iterator<String> it = keys.iterator();
            while(it.hasNext()){
                key=it.next();
                if(key.toLowerCase().contains("ReqVar".toLowerCase()))
                    replace.add(map.get(key));
            }
        }
        return replace;
    }

    public static List<String> getValidationKeys(HashMap<String,String> map){
        List<String> replace = new ArrayList<String>();
        String key="";
        if(!map.isEmpty()){
            Set<String> keys = map.keySet();
            Iterator<String> it = keys.iterator();
            while(it.hasNext()){
                key=it.next();
                if(key.toLowerCase().contains("ValidationKey".toLowerCase()))
                    replace.add(map.get(key));
            }
        }
        return replace;
    }

    public static List<String> getValidationResponseKeys(HashMap<String,String> map){
        List<String> replace = new ArrayList<String>();
        String key="";
        if(!map.isEmpty()){
            Set<String> keys = map.keySet();
            Iterator<String> it = keys.iterator();
            while(it.hasNext()){
                key=it.next();
                if(key.toLowerCase().contains("ResponseKey".toLowerCase()))
                    replace.add(map.get(key));
            }
        }
        return replace;
    }

    public static List<String> getReplaceJsonObjectVal(HashMap<String,String> map){
        List<String> replace = new ArrayList<String>();
        String key="";
        if(!map.isEmpty()){
            Set<String> keys = map.keySet();
            Iterator<String> it = keys.iterator();
            while(it.hasNext()){
                key=it.next();
                if(key.toLowerCase().contains("ReqVal".toLowerCase()))
                    replace.add(map.get(key));
            }
        }
        return replace;
    }

    public static List<String> getValidationVals(HashMap<String,String> map){
        List<String> replace = new ArrayList<String>();
        String key="";
        if(!map.isEmpty()){
            Set<String> keys = map.keySet();
            Iterator<String> it = keys.iterator();
            while(it.hasNext()){
                key=it.next();
                if(key.toLowerCase().contains("ValidationVal".toLowerCase()))
                    replace.add(map.get(key));
            }
        }
        return replace;
    }

    public static String extractPathParam(Response resp,String path){
        String param = "null";
        if(resp!=null)
             param = resp.jsonPath().getString(path);
        return param;
    }

    public static String buildEndpoint(String url,String pathParam,String queryParam){
        String endpoint = "";
        if(queryParam.equals("null"))
            queryParam="";

            if (url.contains("{") && queryParam.equals("")) {
                endpoint = url.substring(0, url.indexOf("{")) + pathParam + url.substring(url.indexOf("}") + 1);
            } else if (url.contains("{") && !queryParam.equals("") ) {
                endpoint = url.substring(0, url.indexOf("{")) + pathParam + url.substring(url.indexOf("}") + 1) + "?" + queryParam;
            } else if (!url.contains("{") && !queryParam.equals(""))
                endpoint = url + "?" + queryParam;
            else
                endpoint = url;

        return endpoint;
    }

    public static String getDependencyMethod(HashMap<String,String> map){
        return map.get(map.get("Dep_Method"));
    }

    public static String getPathParam(HashMap<String,String> map){
        return map.get(map.get("Path_Param"));
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

    // Read the parameter values from the .properties file. If the value is not found, the system looks for it in the global .properties file (config.properties)
    private static void readParameterValues() {

        logToFile = Boolean.parseBoolean(readParameterValue("logToFile"));
        if(logToFile) {
            setUpLogger();
        }

        logger.info("Loading configuration parameter values");

        generator = readParameterValue("generator");
        logger.info("Generator: {}", generator);

        OAISpecPath = readParameterValue("oas.path");
        logger.info("OAS path: {}", OAISpecPath);

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

        if (readParameterValue("data.xls.dir.base") != null) {
            excelBaseDir = readParameterValue("data.xls.dir.base");
        }

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
