package es.us.isa.restest.main;

import static es.us.isa.restest.util.FileManager.readFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import com.atlassian.oai.validator.model.SimpleRequest.Builder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.us.isa.restest.mutation.operators.JsonMutatorLocal;
import groovy.io.FileType;
import groovy.lang.Buildable;

public class FormDataProducer {

	public static String testDataDir= "src/test/resources/zycus_new/testdata";
	public static String fieldType = "NotAFile";
	
	public static void main(String[] args)
	{
		formDataProcessor();
	}
	
	public static void formDataProcessor()
	{
		
		ArrayList<String> fields; 
		try
		{
			File tdDir = new File(testDataDir);
			for(String file : tdDir.list())
			{
				if(file.endsWith(".json"))
				{
					fields = getFormDataFields(file);
						
					for(String field : fields)
					{
						String content = getFormDataContent(file, field);
						
						if(fieldType.equals("File"))
						{
							System.out.println("@@@@Field '"+field+"' - "+fieldType);
							content = getAFileNameRandomly();
							
						}
						
						boolean isJSON = isJSON(content);
						//System.out.println(content +"===>"+isJSON(content));
						
						if(isJSON)
						{
							content = getAllJSONMutations(content);
						}
						
						writeFormDataContentIntoTestFile(file, field, content); 
					}
					
				}		

			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	  public static String getAllJSONMutations(String jsonString) {
	        
		  	JsonMutatorLocal mutator = new JsonMutatorLocal();
		  	JsonNode json = null;
	        HashSet<String> testData = new HashSet<>(); 
	        //ArrayList<String> testData = new ArrayList<>();
	        String ls = System.getProperty("line.separator");
	        String mutatedJSON;
	        StringBuilder builder = new StringBuilder();
	        try {
	           
	            ObjectMapper objectMapper = new ObjectMapper();
	            json = objectMapper.readTree(jsonString);
	            int testCasesCount = 10* json.size();
	            
	            for(int i = 0; i< 10000; i++)
	            {
	            	if(testData.size() == testCasesCount)
	            	{
	            		break;
	            	}
	            	
	            	mutatedJSON = mutator.mutateJson(jsonString, true);
	            	System.out.println(mutatedJSON);
	            	//Get mutated JSON
	            	testData.add(mutatedJSON);
	            	
	            }
	            
	            
	            for(String data : testData)
	            {
	            	builder.append(data);
	            	builder.append(ls);
	            }
	            
	            
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
	        
	        return builder.toString();
	    }
	
	
	public static void writeFormDataContentIntoTestFile(String fileName, String fieldName, String content) 
	{
		
		String httpMethod=null;
		if(fileName.contains("post"))
		{
			httpMethod="post";
		}
		else if(fileName.contains("put"))
		{
			httpMethod="put";
		}
		
		int index = fileName.indexOf(httpMethod);
		
		fileName = fileName.substring(0, index)+fieldName+"_"+httpMethod+".csv";
		try
		{
			FileWriter writer = new FileWriter(testDataDir + File.separator + fileName);
			writer.write(content);
			writer.close();

		}catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exiting...");
			System.exit(-1);
		}
	}
	
	
	
	public static String getFormDataContent(String fileName, String field) {
		
		fieldType = "NotAFile";
		BufferedReader reader; 
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		boolean fieldFound = false;
		try
		{
			reader = new BufferedReader(new FileReader(new File(testDataDir)+File.separator+fileName));
			while ((line = reader.readLine()) != null) {
			
				if(line.contains("form-data") && line.contains(field))
				{
					fieldFound = true;
					if(line.contains("filename="))
					{
						fieldType = "File";	
					}
					
				}
				
				if(fieldFound && line.contains("form-data"))
				{
					continue;
				}
				if(fieldFound && line.contains("------"))
				{
					break;
				}
				
				if(fieldFound)
				{
					stringBuilder.append(line.trim());
				}
				
			}
			reader.close();
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}	
		
		return stringBuilder.toString();

	}
	
	public static ArrayList<String> getFormDataFields(String fileName) {
		
		ArrayList<String> fields = new ArrayList<>();
		BufferedReader reader; 
		String line = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(new File(testDataDir)+File.separator+fileName));
			while ((line = reader.readLine()) != null) {
			
				if(line.contains("form-data") && line.contains("name="))
				{
					//System.out.println(line);
					int start = line.indexOf("name=")+6;
					int end = line.indexOf("\"", start);
					String field = line.substring(start, end);
					fields.add(field);
					
				}
			}
			reader.close();
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}	
		for(int i =0; i < fields.size(); i++)
		{
			System.out.println(fields.get(i));
		}
		
		return fields;

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
	  
	  public static String getAFileNameRandomly() 
	  {
		  String dir = testDataDir.substring(0,testDataDir.indexOf("/testdata")); 
		  
		  File filesDir = new File(dir+File.separator+"files");
		  
		  if(!filesDir.exists())
		  {
			  System.out.println("******ERROR For File field in Form Data, Dir '"+filesDir+"' should be present.^^^^^^");
			  System.out.println("You need to create the Dir '"+filesDir+"' and add one or more files.");
			  System.exit(0);
		  }
		  
		  String[] listofFiles = filesDir.list();
		  Random rand = new Random();  
		  int randomNo = rand.nextInt(listofFiles.length);
		  return dir+File.separator+"files"+File.separator+listofFiles[randomNo];
		  
	  }  
	  	
}
