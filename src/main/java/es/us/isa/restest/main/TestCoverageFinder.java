package es.us.isa.restest.main;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.isa.restest.util.APICoverage;
import es.us.isa.restest.util.DateProcessor;
import es.us.isa.restest.util.FieldCoverage;
import es.us.isa.restest.util.PropertyManager;




public class TestCoverageFinder {
	
	private static Logger logger = LogManager.getLogger(TestCoverageFinder.class.getName());
	private static Map<String,String> fields = new HashMap<>();
	
	public static final int java_lang_Integer = 5;
	public static final int java_lang_Long = 5;
	public static final int java_lang_Double = 5;
	public static final int java_lang_String = 7;
	public static final int org_json_JSONObject = 1;
	public static final int java_lang_Boolean = 4;
	public static final int org_json_JSONArray = 1;
	public static final int Null = 2;
	public static final int Others = 0;
	public static final boolean debug = false;
	
	
	public static void main(String args[]) throws Exception
	{
		ArrayList<APICoverage> expectedAPICoverages = getExpectedAPICoverages();
		System.out.println();
	}
	
	
	public static ArrayList<APICoverage> getExpectedAPICoverages() throws Exception
	{
		ArrayList<APICoverage> expectedAPICoverages = new ArrayList<>(); 
		
		String stestDataDir = PropertyManager.readProperty("testdata.dir");
		String processDir = PropertyManager.readProperty("process.dir");
		String apiName = null;
		String methodName = null;
		APICoverage  apiCov = null;
		ArrayList<FieldCoverage> fieldCoverages = new ArrayList<>();
		FieldCoverage fieldCov = null;
 		File testDataDir = new File(stestDataDir);
 		
 		if(!testDataDir.isDirectory())
 		{
 			throw new Exception("testdata.dir in config.properties is not directory!");
 			
 		}
 		else
 		{
 			org.json.simple.JSONObject content;
 			JSONObject rootJO = null;
 			for(String sFile : testDataDir.list())
 			{
 				if(sFile.endsWith(".json"))
 				{
 					try 
 					{
 						Reader in = new FileReader(testDataDir+File.separator+sFile);
 						if(debug)
 						{
 							System.out.println();
 	 	 					System.out.println("File Name = "+sFile);
 		 					System.out.println("==========================================================");
 		 					
 						}
 						 
 	 				    if(new BufferedReader(new FileReader(testDataDir+File.separator+sFile)).readLine() == null )
 	 				    {
 	 				    	continue;
 	 				    }
 	 				    
 	 				    else
 	 				    {
 	 				    	apiCov = new APICoverage();
 	 				    	fieldCoverages = new ArrayList<>();
 	 				    	fields = new HashMap<>();
 	 				    	JSONParser parser = new JSONParser();
 	 	 				    content = (org.json.simple.JSONObject) parser.parse(in);
 	 	 				    rootJO = new JSONObject(content.toString());
 	 	 				    in.close();
 	 	 				    loopThroughJson(rootJO);
 	 				    }
 	 				    
 	 				    
 	 				    if(sFile.indexOf("_post") != -1)
 	 				    {
 	 				    	apiName = sFile.substring(0, sFile.indexOf("_post"));
 	 				    	methodName = "post";
 	 				    }
 	 				    else if(sFile.indexOf("put") != -1)
 	 				    {
 	 				    	apiName = sFile.substring(0, sFile.indexOf("_put"));
 	 				    	methodName = "put";
 	 				    }
 	 				    else
 	 				    {
 	 				    	String errMessage = "***Error : Issue with the API, mostly naming convention @ '"+sFile+"'"; 
 	 				    	System.out.println(errMessage);
 	 				    	throw new Exception(errMessage);
 	 				    }
 	 				    apiName = "/"+apiName.replaceAll("_", "/");
 	 				    
 	 				    apiCov.setApiName(apiName);
 	 				    apiCov.setMethodName(methodName);
 	 				    
 	 				    int counter = 1;
 	 				    
 	 				    if(debug)
						{
 	 				    	System.out.println("Total # of Fields "+fields.size());
						}
 	 					int totalTestCasesPossible = 0;
 	 				    int totalFieldTestCasesPossible = 0;
	 	 				for(Entry<String, String> field: fields.entrySet())
	 	 				{
	 	 				
	 	 					if(debug)
	 	 						System.out.println(counter++ +") Type of '"+field.getKey()+"' = "+field.getValue());
	 	 					totalFieldTestCasesPossible = TestCoverageFinder.class.getField(field.getValue().replace(".", "_")).getInt(null);
	 	 					totalTestCasesPossible += totalFieldTestCasesPossible; 	 							  
	 	 					  
	 	 					fieldCov = new FieldCoverage();
	 	 					fieldCov.setFieldName(field.getKey());
	 	 					fieldCov.setExpectedTestCaseCount(totalFieldTestCasesPossible);
	 	 					fieldCov.setFieldType(field.getValue());
	 	 					fieldCoverages.add(fieldCov);
	 	 					
	 	 				}
	 	 				apiCov.setFieldCoverages(fieldCoverages);
	 	 				apiCov.setExpectedTestCaseCount(totalTestCasesPossible);
	 	 				if(debug)
	 	 					System.out.println("Total TC # "+ totalTestCasesPossible);	
	 	 				expectedAPICoverages.add(apiCov);
	 	 				
 	 				}catch(Exception e)
 					{
 	 					//e.printStackTrace();
 					}
 				}
 			}
 		}
 		return expectedAPICoverages;
	}
	

	public static void loopThroughJson(Object input) throws JSONException {

	    if (input instanceof JSONObject) {

	        Iterator<?> keys = ((JSONObject) input).keys();

	        while (keys.hasNext()) {

	            String key = (String) keys.next();
	            
	            if (!(((JSONObject) input).get(key) instanceof JSONArray))
	            	
	            	if (((JSONObject) input).get(key) instanceof JSONObject) {
	            		
	            		fields.put(key, getType(((JSONObject) input).get(key)));
	                    loopThroughJson(((JSONObject) input).get(key));
	                } else
	                {
	                	Object obj = ((JSONObject) input).get(key); 
	                	
	                	
	                	if(obj != null)
	                	{
	                		if(isJSON(obj.toString()))
	                		{
	                			JSONObject jsonObj = new JSONObject(obj.toString());
	                			loopThroughJson(jsonObj);
	                		}
	                	}
	                	
	                	fields.put(key, getType(obj));
	                	//System.out.println("Type of '"+key+"' = "+getType(obj));
	                	
	       	        }
	                    
	            else
	            {
	            	fields.put(key, getType(((JSONObject) input).get(key)));
	            	//System.out.println("Type of '"+key+"' = "+getType(((JSONObject) input).get(key)));
	            	loopThroughJson(((JSONObject) input).get(key));
	            	
	            }
	                
	        }
	    }

	    if (input instanceof JSONArray) {
	    	
	    	
	    	
	        for (int i = 0; i < ((JSONArray) input).length(); i++) 
	        {
	        	if (((JSONArray) input).get(i) instanceof JSONObject) {
                    loopThroughJson(((JSONArray) input).getJSONObject(i));
                } else
                {
                	//System.out.println(((JSONArray) input).get(i));
                }
                   
	        }
	    }

	}
	
	
	  public static boolean isJSON(String stringToBeTested) 
	  {
	        JsonNode jsonData = null;
	        boolean isJSON = true;
	        try {
	           
	            ObjectMapper objectMapper = new ObjectMapper();
	            jsonData = objectMapper.readTree(stringToBeTested);
	            
	            if(!(jsonData.isArray() || jsonData.isObject()))
	            {
	            	isJSON = false;
	            }
	            
	            
	        } catch (Exception ex) {
	        	isJSON = false;
	        }
	        return isJSON;
	  }
	
	private static String getType(Object obj)
	{
		
		if(obj instanceof JSONObject)
		{
			return "org.json.JSONObject";
		}
		if(obj instanceof JSONArray)
		{
			return "org.json.JSONArray";
		}
		if(obj instanceof Long)
		{
			return "java.lang.Long";
		}
		if(obj instanceof Double)
		{
			return "java.lang.Double";
		}
		if(obj instanceof BigDecimal)
		{
			return "java.lang.Double";
		}
		if(obj instanceof Float)
		{
			return "java.lang.Float";
		}
		if(obj instanceof Integer)
		{
			return "java.lang.Integer";
		}
		if(obj instanceof String)
		{
			return "java.lang.String";
		}
		if(obj instanceof Boolean)
		{
			return "java.lang.Boolean";
		}
		if(obj==null)
		{
			return "Null";
		}
		else
		{
			return "Others";
		}
	}
	
}
