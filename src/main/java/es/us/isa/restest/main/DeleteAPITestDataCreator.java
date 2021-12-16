package es.us.isa.restest.main;


import es.us.isa.restest.configuration.pojos.TestParameter;
import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;



/*
 * This class show the basic workflow of test case generation -> test case execution -> test reporting
 */
public class DeleteAPITestDataCreator {

	
	
	private static Logger logger = LogManager.getLogger(DeleteAPITestDataCreator.class.getName());

	private static String testEnvironment;
	private static String sassToken;
	private static String testDataDir; 
	private static String workingDir;
	private static String loginUserName;
	private static String loginPassword;

	private static String saasTokenName = "SAAS_COMMON_BASE_TOKEN_ID";
	private static String configFile = "src/main/resources/config.properties";
	private static String propertiesFilePath = "src/test/resources/zycus_new/zycus.properties";
	private static String loginAPI = "/api/u/tms/auth/login";
	private static String deleteAPIConfigFile = "delete_api_configuration.json";
	
	private static String testParameter;
	private static String ID;
	private static String deleteAPIName;
	private static String postAPIName;
	private static String fieldPathInPOSTAPI;
	private static String fieldNameInPOSTAPI;
	
	private static boolean debug = true;
	
	
	private static ArrayList<String> errors = new ArrayList<String>(); 
	private static ArrayList<String> successes = new ArrayList<String>(); 
	
	public static void main(String[] args)
	{
		//System.out.println("*********** Test Data Preparation for DELETE APIs - STARTS  ********");
		
		createTestDataForDeleteAPIs();
		//deleteNotification();
		
		//System.out.println("*********** Test Data Preparation for DELETE APIs - ENDS  ********");
		
	}
	
	public static void createTestDataForDeleteAPIs()
	{
		try
		{
			readParameterValues();
			
			FileInputStream fisLocal = new FileInputStream(configFile);
			Properties prop = new Properties();
			prop.load(fisLocal);
			testDataDir = prop.getProperty("testdata.dir");
			workingDir = prop.getProperty("process.dir");
			
			if(testEnvironment == null || testEnvironment.equals(""))
			{
				throw new Exception("Configuration parameter 'test.environment.url' should be defined and can't be empty or null");
			}
			if(testEnvironment.endsWith("/")){
				testEnvironment = testEnvironment.substring(0,testEnvironment.length()-1);
			}
			
			if(debug)
				System.out.println("Test Environment = "+testEnvironment);
			
			readSAASToken();

			createTestData();

		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	
	public static void createTestData() throws Exception
	{
		
		String deleteConfigFile = workingDir+File.separator+deleteAPIConfigFile;
		JSONParser jsonParser = new JSONParser();
		FileReader reader = new FileReader(deleteConfigFile);
		
        Object obj = jsonParser.parse(reader);
	    JSONObject config = (JSONObject) obj;
		
	    JSONArray apis = (JSONArray) config.get("apis");
	    
	  
	    JSONObject node;
	    
	    for(int i=0; i< apis.size(); i++)
		{
	    	node = (JSONObject) apis.get(i);
	    	
	    	deleteAPIName = (String) node.get("delete-api-name");
	    	
	    	testParameter = getTestParameter(deleteAPIName);
	    	postAPIName = (String) node.get("post-api-name");
	    	fieldNameInPOSTAPI = (String) node.get("field-name-in-post-api");
	    	fieldPathInPOSTAPI = (String) node.get("field-name-path");
	    	
	    	String param = getPathParam(postAPIName, fieldNameInPOSTAPI, fieldPathInPOSTAPI);
	    	
	    	if(param == null)
	    	{
	    		continue;
	    	}
	    	if(debug)
	    		System.out.println(fieldNameInPOSTAPI+" = "+param);
			
	    	createTestFile(deleteAPIName, param);
		}
	    
	    System.out.println("\n-------------------------------------------------------\n");
	    
	    for(String error: errors)
	    {
	    	System.out.println(error);
	    	
	    }
	    
	    System.out.println("\n-------------------------------------------------------\n");
	    
	    
	    for(String success: successes)
	    {
	    	System.out.println(success);
	    	
	    }
	    
	    System.out.println("\n-------------------------------------------------------\n");
	    
	}
	
	public static String getTestParameter(String apiName)
	{
		String testParameter=null;
		if(apiName.startsWith("/"))
		{
			apiName = apiName.substring(1);
		}
		
		String[] a = apiName.split("/");
		
		for(int i = 0; i< a.length; i++)
		{
			if(a[i].contains("{") && a[i].contains("}"))
			{
				testParameter = a[i].replaceAll("\\}", "").replaceAll("\\{", "");
			}
			
		}
		
		return testParameter;
	}
	
	
	public static void createTestFile(String apiName, String param) throws Exception
	{
	
		String testDataFileName = null; 
		if(apiName.startsWith("/"))
		{
			testDataFileName = apiName.substring(1);
		}
		testDataFileName = testDataFileName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "")+"_"+testParameter+"_delete.csv";
		FileWriter writer = new FileWriter(testDataDir+File.separator+testDataFileName);
		writer.write(param+"\n"+getInvalidTestData(param));
		writer.close();
		
		successes.add("Successfully created Test Data File : "+testDataFileName);
	}
	
	public static Object getInvalidTestData(String data)
	{
		if(StringUtils.isNumeric(data))
		{
			return 198765432;
		}
		else
			return "INVALIDDATA";
	}
	
	public static String getPathParam(String postAPIName, String field, String path) throws Exception
	{
		
		String testDataFileName = null; 
		if(postAPIName.startsWith("/"))
		{
			testDataFileName = postAPIName.substring(1);
		}
		testDataFileName = testDataFileName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "")+"_post_body.json";
		
		String postBody =  readTestData(testDataDir+File.separator+testDataFileName);
		
		HashMap<String, String> headers = new HashMap<String, String>();
		
		String cookieString = saasTokenName+"="+sassToken;
		if(debug)
			System.out.println("Cookie STRING = "+cookieString);
		headers.put("Content-Type", "application/json");
		headers.put("Cookie",cookieString);
		

		HttpResponse response = doPost(testEnvironment+postAPIName, headers, postBody);
		String sResponse = EntityUtils.toString(response.getEntity());

		
		if(response.getStatusLine().getStatusCode() != 200)
		{
			throw new Exception("POST API - "+testEnvironment+postAPIName+" Failed - Status is "+response.getStatusLine().toString()+" - Response is "+sResponse);
		}
		
	    ObjectMapper objectMapper = new ObjectMapper();
	    JsonNode jsonNode = objectMapper.readTree(sResponse);
	    
	    if(debug)
	    	System.out.println("Response ----\n"+sResponse);
        
	    String param = null;
	    
	    JsonNode dataNode = jsonNode.at(path);
	    
	    if(!dataNode.isMissingNode())
	    {
	    	param = jsonNode.at(path).asText();
	    }
	    else
	    {
	    	
	    	errors.add("Failed to create Test Data File : Path \""+path+"\" for the field \""+field+"\" in the post API \""+postAPIName+"\" is incorrect");
	    	return null;
	    }
	    
	    return param;
	    
	}
	
	
	public static String readTestData(String fileName) throws Exception
	{
		StringBuilder resultStringBuilder = new StringBuilder();
		BufferedReader br = null;
		try
		{
			
		    br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName))));
		    {
		        String line;
		        while ((line = br.readLine()) != null) {
		            resultStringBuilder.append(line).append("\n");
		        }
		    }
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally 
		{
			br.close();
		}
		
		return resultStringBuilder.toString();
	}
	

	public static void deleteNotification()
	{
		
		System.out.println("Delete API");
	    
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", "application/json");
			
			String cookieString = saasTokenName+"="+sassToken;
			headers.put("Cookie",cookieString);
			
			CloseableHttpResponse response = null;
			try
			{
				HttpClientContext context = HttpClientContext.create();
				
				String uri = testEnvironment+"/cns/api/a/cns/notifications/"+ID;
				System.out.println(uri);
				HttpDelete delete = new HttpDelete(uri);
				
				for(Map.Entry<String,String> entry : headers.entrySet())
				{
					delete.addHeader(entry.getKey(),entry.getValue());
				}
				
				CloseableHttpClient httpClient = HttpClients.createDefault();
			    response = httpClient.execute(delete, context);
			    
			    System.out.println(uri);
			    System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
			    if(response.getStatusLine().getStatusCode() != 200)
				{
					throw new Exception("Login Failed - Check whether "+testEnvironment+" up Or check the credentials used");
				}
			    List<Cookie> cookies = context.getCookieStore().getCookies();
			    
			    
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		
		
	}
	
	

	
	public static void readSAASToken()
	{
			HashMap<String, String> headers = new HashMap<String, String>();
			headers.put("Content-Type", "application/json");
			StringBuilder jsonBody = new StringBuilder();
			jsonBody.append("{");
			jsonBody.append("\"emailAddress\":\""+loginUserName+"\",");
			jsonBody.append("\"password\":\""+loginPassword+"\"");
			jsonBody.append("}");
			
			CloseableHttpResponse response = null;
			try
			{
				HttpClientContext context = HttpClientContext.create();
				
				String uri = testEnvironment+loginAPI;
				if(debug)
				{
					System.out.println(uri);
					System.out.println(jsonBody);
				}
				HttpPost post = new HttpPost(uri);
				post.setEntity(new StringEntity(jsonBody.toString()));

				for(Map.Entry<String,String> entry : headers.entrySet())
				{
					post.addHeader(entry.getKey(),entry.getValue());
				}
				
				CloseableHttpClient httpClient = HttpClients.createDefault();
			    response = httpClient.execute(post, context);
			    if(debug)
			    {
				    System.out.println(jsonBody);
				    System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
			    }
			    if(response.getStatusLine().getStatusCode() != 200)
				{
					throw new Exception("Login Failed - Check whether "+testEnvironment+" up Or check the credentials used");
				}
			    List<Cookie> cookies = context.getCookieStore().getCookies();
			    for(Cookie cookie : cookies)
			    {
			    	if(cookie.getName().equals(saasTokenName))
			    	{
			    		sassToken = cookie.getValue();
			    		if(debug)
			    			System.out.println("SASS Token = ***"+sassToken+"***");
			    	}
			    	
			    }
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(-1);
			}
		
		
	}
	
	public static HttpResponse doPost(String url, HashMap<String,String> headers, String jsonBody)
	{
		String result=null;
		HttpResponse response = null;
		try
		{
			//HttpClientContext context = HttpClientContext.create();
			HttpPost post = new HttpPost(url);
			if(debug)
			{
				System.out.println(url);
				System.out.println(jsonBody);
				
			}
			post.setEntity(new StringEntity(jsonBody.toString()));
			
			for(Map.Entry<String,String> entry : headers.entrySet())
			{
				post.addHeader(entry.getKey(),entry.getValue());
			}
			
			BasicCookieStore cookieStore = new BasicCookieStore();
//			BasicClientCookie cookie = new BasicClientCookie("SAAS_COMMON_BASE_TOKEN_ID", sassToken);
//			cookie.setDomain(".zycus.net");
//		    cookie.setPath("/");
//			cookieStore.addCookie(cookie);
			
			//context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
			//CloseableHttpClient httpClient = HttpClients.createDefault();
			
			HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
			response = httpClient.execute(post);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return response;

	}
		// Read the parameter values from the .properties file. If the value is not found, the system looks for it in the global .properties file (config.properties)
	private static void readParameterValues() {


		logger.info("Loading configuration parameter values");
		
		
		testEnvironment = readParameterValue("test.environment.url");
		logger.info("Test Environment: {}", testEnvironment);
		
		loginUserName = readParameterValue("login.credentials.username");
		logger.info("Login UserName: {}", loginUserName);
		
		loginPassword = readParameterValue("login.credentials.password");
		logger.info("Login Password: {}", loginPassword);
	
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
}
