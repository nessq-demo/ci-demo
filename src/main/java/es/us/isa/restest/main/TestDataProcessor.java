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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import es.us.isa.restest.util.DateProcessor;
import es.us.isa.restest.util.PropertyManager;



public class TestDataProcessor {
	
	private static Logger logger = LogManager.getLogger(TestDataProcessor.class.getName());
	private static List<String> fields = new ArrayList<String>();
	private static Properties processorProp = new Properties();
	
	public static void main(String args[]) throws Exception
	{
		testDataProcessor();
	}
	public static void testDataProcessor() throws Exception
	{
		String stestDataDir = PropertyManager.readProperty("testdata.dir");
		String processDir = PropertyManager.readProperty("process.dir");
		
		System.out.println("testDataDir-"+stestDataDir);
		System.out.println("processDir-"+processDir);
		
		try
		{
			processorProp.load(new FileReader(processDir+File.separator+"fields.properties"));
		}
		catch(Exception e)
		{
			logger.error("Error reading property file: {}", e.getMessage());
			logger.error("Exception: ", e);
		}
		String field = null;
		for(Entry<Object, Object> set: processorProp.entrySet() )
		{
			//System.out.println(set.getKey()+":"+set.getValue());
			
 			field = ((String)set.getKey()).split("\\.")[0];
			if(!fields.contains(field))
			{
				fields.add(field);
			}
		}
 		
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
 					try (Reader in = new FileReader(testDataDir+File.separator+sFile)) {
 	 				    
 	 					System.out.println("File Name = "+sFile);
 	 				    if(new BufferedReader(new FileReader(testDataDir+File.separator+sFile)).readLine() == null )
 	 				    {
 	 				    	continue;
 	 				    }
 	 				    JSONParser parser = new JSONParser();
 	 				    content = (org.json.simple.JSONObject) parser.parse(in);
 	 				    rootJO = new JSONObject(content.toString());
 	 				    in.close();
 	 				}
	 				
	 				for( String f: fields)
 	 				{
 	 					Object mValue = processor(f);
 	 					loopThroughJson(rootJO, f, mValue);
 	 					
 	 				}
	 				
	 				try (Writer out = new FileWriter(testDataDir+File.separator+sFile)) {
 	 		 				out.write(rootJO.toString());
 	 		 				out.close();
 	 		 		}
 				}
 				
 			}
 		}
 	
	}
	
	public static Object processor(String field)
	{
		Object processedValue = null;
		
		switch(((String)processorProp.get(field+".datatype")).toLowerCase()) 
		{
			case "date":
				processedValue = dateProcessor((String)processorProp.get(field+".type"),(String)processorProp.get(field+".format"),(String)processorProp.get(field+".increment.days"));
				break;
			case "string":
				processedValue = stringProcessor((String)processorProp.get(field+".type"),(String)processorProp.get(field+".prefix"),(String)processorProp.get(field+".length"));
				break;
		}
		
		return processedValue;
		
	}
	
	public static Long dateProcessor(String type, String format, String increment)
	{
		long date = 0;
		switch(type.toLowerCase()) 
		{
			case "future":
				date =  DateProcessor.getFutureDateInMilliseconds(increment);
				break;
		}
		return date;
		
	}
	
	public static String stringProcessor(String type, String prefix, String length)
	{
		String value = null;
		switch(type.toLowerCase()) 
		{
			case "unique":
				int addLength = Integer.parseInt(length) - prefix.length();
				value =  prefix + generateRandomNumber(addLength);
			
		}
		return value;
		
	}
	
	public static String generateRandomNumber(int charLength) {
        return String.valueOf(charLength < 1 ? 0 : new Random()
                .nextInt((9 * (int) Math.pow(10, charLength - 1)) - 1)
                + (int) Math.pow(10, charLength - 1));
    }
	
	
	public static void loopThroughJson(Object input, String fieldName, Object newFieldValue) throws JSONException {

	    if (input instanceof JSONObject) {

	        Iterator<?> keys = ((JSONObject) input).keys();

	        while (keys.hasNext()) {

	            String key = (String) keys.next();
	            
	            System.out.println(key);
	            if (!(((JSONObject) input).get(key) instanceof JSONArray))
	                if (((JSONObject) input).get(key) instanceof JSONObject) {
	                    loopThroughJson(((JSONObject) input).get(key),fieldName, newFieldValue);
	                } else
	                {
	                	System.out.println(key + "- old Value =" + ((JSONObject) input).get(key));
	                	
	                	if(key.equalsIgnoreCase(fieldName))
	                	{
	                		((JSONObject) input).put(key, newFieldValue);
	                	}
	                	
	                	System.out.println(key + "- new Value =" + ((JSONObject) input).get(key));
	                	
	                }
	                    
	            else
	            {
	            	//loopThroughJson(new JSONArray(((JSONObject) input).get(key).toString()),fieldName, newFieldValue);
	            	loopThroughJson(((JSONObject) input).get(key),fieldName, newFieldValue);
	            	
	            }
	                
	        }
	    }

	    if (input instanceof JSONArray) {
	        for (int i = 0; i < ((JSONArray) input).length(); i++) 
	        {
	        		
	        	if (((JSONArray) input).get(i) instanceof JSONObject) {
                    loopThroughJson(((JSONArray) input).getJSONObject(i),fieldName, newFieldValue);
                } else
                   System.out.println(((JSONArray) input).get(i));
	        }
	    }

	}
	
	
}
