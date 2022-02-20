package es.us.isa.restest.apichain.api;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import es.us.isa.restest.util.PropertyManager;
import io.restassured.response.Response;


public class APIChainMapper {
	
	private static Logger logger = LogManager.getLogger(APIChainMapper.class.getName());
	
	private static String propertiesFilePath = "src/test/resources/zycus_new/zycus.properties";
	private static String configFilePath = "src/main/resources/config.properties";
	
	private static String targetDir;
	private static String packageName;
	private static String responseWriteDir;
	private static String projectConfigDir;
	
	
	private static String[] getPrefixInAlbhapeticalOrder()
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
	
	
	public static void main(String[] args) throws Exception
	{
		
		String[] prefixes = getPrefixInAlbhapeticalOrder();
		
		for(int i = 0; i < prefixes.length; i++)
		{
			System.out.println((i+1) + " - "+prefixes[i]);
		}
		
//		if(responseWriteDir == null)
//		{
//			targetDir = readParameterValue("test.target.dir");
//			///logger.info("Target dir for test classes: {}", targetDir);
//			String experimentName = readConfigParameterValue("experiment.name");
//			logger.info("Experiment name: {}", experimentName);
//			packageName = experimentName;
//			responseWriteDir = targetDir+File.separator+"TestJSON"+File.separator+"response";
//			
//		}
//		String testName = "24_POST_cns_api_a_cns_notifications";
//		
//		FileReader reader = new FileReader(new File(targetDir+File.separator+"TestJSON"+File.separator+"request"+File.separator+testName+"_request.json"));
//		JSONParser parser = new JSONParser();
//		JSONObject root = (JSONObject) parser.parse(reader);
//		ObjectMapper objectMapper = new ObjectMapper();
//	    JsonNode jsonNode = objectMapper.readTree(root.toString());
//	    
//		fillMappings(testName, jsonNode);
	}
	
	public static JsonNode fillMappings(String testName, JsonNode node) throws Exception
	{
		
		if(responseWriteDir == null)
		{
			targetDir = readParameterValue("test.target.dir");
			///logger.info("Target dir for test classes: {}", targetDir);
			String experimentName = readConfigParameterValue("experiment.name");
			logger.info("Experiment name: {}", experimentName);
			packageName = experimentName;
			responseWriteDir = targetDir+File.separator+"TestJSON"+File.separator+"response";
			
		}
		
		projectConfigDir = PropertyManager.readProperty("process.dir");
		FileReader reader = new FileReader(projectConfigDir+File.separator+"api_mapping.json");
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(reader);
		JSONArray mappings = (JSONArray) root.get("mappings");
		
		JSONObject mapping;
		JSONArray modifications;
		String test;
		JSONObject modification;
		String source;
		String source_field;
		String destination_field;
		
		boolean apiFound = false;
		
		for(int k = 0; k < mappings.size(); k++)
        {
			mapping = (JSONObject) mappings.get(k);
			test = (String) mapping.get("test");
			
			if(test.equalsIgnoreCase(testName))
			{
				apiFound = true;
			}
		
        }
		
		if(!apiFound)
		{
			return node;
		}
        
		for(int i = 0; i < mappings.size(); i++)
        {
			mapping = (JSONObject) mappings.get(i);
			test = (String) mapping.get("test");
			
			if(test.equalsIgnoreCase(testName))
			{
				System.out.println("Test Name = "+test);
				modifications = (JSONArray) mapping.get("modifications");
				
				for(int j = 0; j < modifications.size(); j++)
		        {
					modification = 	(JSONObject)modifications.get(j);
					source = (String)modification.get("source");
					source_field = (String)modification.get("source_field");
					destination_field = (String)modification.get("destination_field");
					
					System.out.println("Source = "+source);
					System.out.println("Field Name @Source = "+source_field);
					System.out.println("Field@Destination = "+destination_field);
					if(!getSourceFieldValue(source, source_field).equals(""))
					{
						node = setDestinationValue(node, destination_field, getSourceFieldValue(source, source_field));
					}
			
		        }
				
			}
			System.out.println("------------------------------");
			
        }
		
		return node;
		
	}
	
	public static JsonNode setDestinationValue(JsonNode node, String destination_field, String newValue) throws Exception
	{
		String newJSON = JsonPath.parse(node.toString()).set(destination_field, newValue).jsonString();
		ObjectMapper objectMapper = new ObjectMapper();
	    JsonNode jsonNode = objectMapper.readTree(newJSON);
	    
	    System.out.println("!!!!! Changed To '"+ newValue +"' in Destination '"+ destination_field+"'");
	    
	    return jsonNode;
		
	}
	
	public static String getSourceFieldValue(String sourceFileName, String source_field) throws Exception
	{
		File sourceFile = new File(responseWriteDir+File.separator+sourceFileName+".json");
		
		if(!sourceFile.exists())
			return "";
		
		FileReader reader = new FileReader(sourceFile);
		JSONParser parser = new JSONParser();
		JSONObject root = (JSONObject) parser.parse(reader);
		ObjectMapper objectMapper = new ObjectMapper();
	    JsonNode jsonNode = objectMapper.readTree(root.toString());
	    String sourceFieldValue;
	    
	    JsonNode tempNode = jsonNode.at(source_field);
	    if(!tempNode.isMissingNode())
	    {
	    	sourceFieldValue = jsonNode.at(source_field).asText();
	    }
	    else
	    {
	    	
	    	throw new Exception("Source Field Name : "+source_field + " in Source File Name : "+sourceFileName +" is not correct");
	    }
	    
		return sourceFieldValue;
	}
	

	
	public static void writeResponse(String testName, Response response) throws Exception
	{
		if(responseWriteDir == null)
		{
			targetDir = readParameterValue("test.target.dir");
			///logger.info("Target dir for test classes: {}", targetDir);
			String experimentName = readConfigParameterValue("experiment.name");
			logger.info("Experiment name: {}", experimentName);
			packageName = experimentName;
			responseWriteDir = targetDir+File.separator+"TestJSON"+File.separator+"response";
		}
		
		if(!new File(responseWriteDir).exists())
		{
			new File(responseWriteDir).mkdir();
		}

		File file = new File(responseWriteDir+File.separator+testName+".json");
		FileWriter writer = new FileWriter(file);
		writer.write(response.getBody().asString());
		writer.close();
		
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
}
