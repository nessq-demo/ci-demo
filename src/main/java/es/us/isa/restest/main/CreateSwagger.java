package es.us.isa.restest.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;


public class CreateSwagger 
{
	public static Properties propMain = new Properties();
	
	public static String swaggerDir;
	public static String productName;
	public static String swaggerURI;
	public static boolean isBackupNeeded;
	public static String additionalPath;
	public static boolean isRemoveOtherProductAPIs;
	
	public static boolean isCopyListedAPIs;
	public static String copyAPIsFileName;
	
	public static String applicationBaseURI;
	public static String zycusPropertiesDir = "src/test/resources/zycus_new/zycus.properties";
	public static String configPropertiesDir = "src/main/resources/config.properties";
	
	public static boolean isPrintURIs;
	
	public static boolean testEnvironment;
	
	
	public static void main(String args[]) 
	{
		
		
		try {
			
			String subURIs[] = null;
			String subURIsForHarExtraction = null;
			
			
			ArrayList<String> listOfAPIsToBeCopied = new ArrayList<String>();
			
			FileInputStream fisLocal = new FileInputStream(configPropertiesDir);
			propMain.load(fisLocal);
			swaggerDir = propMain.getProperty("process.dir");
			
			FileInputStream fisZycus = new FileInputStream(zycusPropertiesDir);
			Properties zycusProperties = new Properties();
			zycusProperties.load(fisZycus);
			
			subURIsForHarExtraction = zycusProperties.getProperty("sub.uris.for.har.extraction");
			
			if(subURIsForHarExtraction != null)
			{
				subURIs = subURIsForHarExtraction.split(",");
			}
			
			productName = zycusProperties.getProperty("product.name.in.api.uri");
			if(productName == null || productName.equals(""))
			{
				throw new Exception("Configuration parameter 'product.name.in.api.uri' should be defined and can't be empty or null");
			}
			
			swaggerURI = zycusProperties.getProperty("product.swagger.uri");
			if(swaggerURI == null || swaggerURI.equals(""))
			{
				throw new Exception("Configuration parameter 'product.swagger.uri' should be defined and can't be empty or null");
			}
			
			String testEnvironment = zycusProperties.getProperty("test.environment.url");
			if(testEnvironment == null || testEnvironment.equals(""))
			{
				throw new Exception("Configuration parameter 'test.environment.url' should be defined and can't be empty or null");
			}
			if(testEnvironment.endsWith("/")){
				testEnvironment = testEnvironment.substring(0,testEnvironment.length()-1);
			}
			
			
			additionalPath = zycusProperties.getProperty("additional.path");
			if(additionalPath == null)
			{
				throw new Exception("Configuration parameter 'additional.path' should be defined and can't be null");
			}
			
			if(additionalPath.endsWith("/"))
			{
				additionalPath = additionalPath.substring(0, additionalPath.length()-1);
			}
			if(additionalPath.equals(""))
			{
				additionalPath = "";
			}
			
			
			String sIsPrintURIs = zycusProperties.getProperty("print.swagger.uris.in.console");
			
			if(sIsPrintURIs == null || sIsPrintURIs.equals(""))
			{
				isPrintURIs = false;
			}
			else
			{
				if(sIsPrintURIs.equalsIgnoreCase("true"))
				{
					isPrintURIs = true;
				}
				else
				{
					isPrintURIs = false;
				}
			}
			
			String sIsBackupNeeded = zycusProperties.getProperty("is.backup.swagger.yaml");
			
			if(sIsBackupNeeded == null || sIsBackupNeeded.equals(""))
			{
				isBackupNeeded = false;
			}
			else
			{
				if(sIsBackupNeeded.equalsIgnoreCase("true"))
				{
					isBackupNeeded = true;
				}
				else
				{
					isBackupNeeded = false;
				}
			}
			
			String IsRemoveOtherProductAPIs = zycusProperties.getProperty("remove.other.than.product.api");
			if(IsRemoveOtherProductAPIs == null || IsRemoveOtherProductAPIs.equals(""))
			{
				isRemoveOtherProductAPIs = false;
			}
			else
			{
				if(IsRemoveOtherProductAPIs.equalsIgnoreCase("true"))
				{
					isRemoveOtherProductAPIs = true;
				}
				else
				{
					isRemoveOtherProductAPIs = false;
				}
		
			}
			String sIsCopyListedAPIs = zycusProperties.getProperty("is.copy.apis.needed");
			if(sIsCopyListedAPIs == null || sIsCopyListedAPIs.equals(""))
			{
				isCopyListedAPIs = false;
			}
			else
			{
				if(sIsCopyListedAPIs.equalsIgnoreCase("true"))
				{
					isCopyListedAPIs = true;
				}
				else
				{
					isCopyListedAPIs = false;
				}
			}
			
			String copyAPIsFileName = zycusProperties.getProperty("file.name.listing.the.apis.to.be.copied");
			
			if(isCopyListedAPIs)
			{
				if(copyAPIsFileName == null || copyAPIsFileName.equals(""))
				{
					throw new Exception("Configuration parameter 'file.name.listing.the.apis.to.be.copied' should be defined and can't be null or empty");
				}
			}
			
			
			
			fisLocal.close();
			fisZycus.close();
			
			
			System.out.println("Configuration Parameters");
            System.out.println("---------------------------------------------------------------");
            System.out.println();
            
			System.out.println("Test Environment URI = "+testEnvironment);
			System.out.println("product.name.in.api.uri = "+productName);
			System.out.println("product.swagger.uri = "+swaggerURI);
			System.out.println("additional.path = "+additionalPath);
			System.out.println("Swagger Dir = "+swaggerDir);
			System.out.println("is.backup.swagger.yaml = "+isBackupNeeded);
			System.out.println("remove.other.than.product.api = "+isRemoveOtherProductAPIs);
			System.out.println("is.copy.apis.needed = "+isCopyListedAPIs);
			System.out.println("file.name.listing.the.apis.to.be.copied = "+copyAPIsFileName);
			System.out.println("sub.uris.for.har.extraction = "+subURIsForHarExtraction);
			
			
			System.out.println();
			
			File swaggerDirectory = new File(swaggerDir);
			if(!swaggerDirectory.isDirectory())
				new Exception(swaggerDir+" is not a directory. Pls. provide a valid direcory");
			
			File swaggerFile = new File(swaggerDir+File.separator+"swagger.yaml");
			
			if(isBackupNeeded)
			{
				if(swaggerFile.exists())
				{
					LocalDateTime currentDate = LocalDateTime.now();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss");
					String formattedDate = currentDate.format(formatter);
					File copyFile = new File(swaggerDir+File.separator+"swagger_"+formattedDate+".yaml");
					FileUtils.copyFile(swaggerFile, copyFile);
					FileUtils.forceDelete(swaggerFile);
					System.out.println("Swagger.yaml is backed up @ "+swaggerDir+File.separator+"swagger_"+formattedDate+".yaml");
					System.out.println("Existing swagger.yaml is Deleted");
				}
			}
			else
			{
				if(swaggerFile.exists())
				{
					FileUtils.forceDelete(swaggerFile);
					System.out.println("Existing swagger.yaml is Deleted");
				}
			}
			
			File tempFile = new File(swaggerDir+File.separator+"swagger_temp.json");
			FileUtils.copyURLToFile(new URL(swaggerURI), tempFile);
			
			System.out.println("Swagger from '"+testEnvironment+"' downloaded and processing started...");
			
			ObjectMapper objectMapper =  new ObjectMapper();
			YAMLMapper yamlMapper =  new YAMLMapper();
			
			JsonNode node = objectMapper.readTree(tempFile);
			
			String swaggerVersion = null;
			JsonNode swagger = node.get("swagger");
			if(swagger != null)
			{
				swaggerVersion = node.get("swagger").asText();
			}
			else
			{
				swaggerVersion = node.get("openapi").asText();
			}
			
			if(swaggerVersion.startsWith("2"))
			{
				((ObjectNode) node).put("basePath", "");
				((ObjectNode) node).put("host", testEnvironment);
				ArrayNode schemes = objectMapper.createArrayNode();
				schemes.add("https");	
				
				JsonNode oSchemes =  node.path("schemes");
				if(oSchemes == null || oSchemes.isMissingNode())
				{
					((ObjectNode) node).put("schemes", schemes);
					
				}
				
			}
			
			 
			String authSignoutAPI = "/auth/sign_out";
			JsonNode authSignout =  node.path(authSignoutAPI);
			if(!(authSignout == null || authSignout.isMissingNode()))
			{
				((ObjectNode) node).remove(authSignoutAPI);
				
			}
			((ObjectNode) node).remove("responses");
			((ObjectNode) node).remove("parameters");
			((ObjectNode) node).remove("securityDefinitions");
			((ObjectNode) node).remove("security");
			((ObjectNode) node).remove("tags");

			JsonNode basePath = (JsonNode) node.get("paths");
			Iterator<Entry<String, JsonNode>> fields = basePath.fields();
			
			String path;
			
			ArrayList<String> pathsToBeRemoved = new ArrayList<String>();
			HashMap<String, JsonNode> pathsToBeRetained = new HashMap<String, JsonNode>();
 			
			while (fields.hasNext()) 
			{
			        Entry<String, JsonNode> jsonField = fields.next();
			        
			        path = jsonField.getKey();
			        boolean uriFound = false;
			        
			        if(subURIs != null)
					{
			        	
						for(String subURI : subURIs)
						{
							if(path.toLowerCase().contains(subURI.substring(subURI.indexOf("/api")+4)))
							{
								uriFound = true;
							}
							
						}
						if(uriFound)
						{
							pathsToBeRetained.put(path, jsonField.getValue());
						}
						else
						{
							pathsToBeRemoved.add(path);
						}	
						
					}
					else
					{
						if(!(path.toLowerCase().contains("/a/"+productName.toLowerCase()) || path.toLowerCase().contains("/u/"+productName.toLowerCase())))
				        {
				        	pathsToBeRemoved.add(path);
				        }
				        else
				        {
				        	pathsToBeRetained.put(path, jsonField.getValue());
				        }
					}
			        	
			}
			
			if(isRemoveOtherProductAPIs)
			{
				for(String pathToBeRmoved : pathsToBeRemoved)
				{
					((ObjectNode) basePath).remove(pathToBeRmoved);
				}
				System.out.println("Other product APIs are deleted");
			}
			
			if(isPrintURIs)
			{
				System.out.println();
				System.out.println("------------Printing APIs for the product '"+productName+"' in '"+swaggerURI+"'-------------");
				System.out.println();
			}
			
			for (Entry<String, JsonNode> pathToBeRetainedEntry : pathsToBeRetained.entrySet()) 
			{
				String pathToBeRemovedKey = pathToBeRetainedEntry.getKey();
				JsonNode pathToBeRemovedValue = pathToBeRetainedEntry.getValue();
				((ObjectNode) basePath).remove(pathToBeRemovedKey);
				((ObjectNode) basePath).put(additionalPath+pathToBeRemovedKey, pathToBeRemovedValue);
				if(isPrintURIs)
				{
					System.out.println(additionalPath+pathToBeRemovedKey);
				}
				
			}
			
			System.out.println();
			System.out.println("Additional Path Param are added to product APIs...");
			System.out.println();
			if(isCopyListedAPIs)
			{
				listOfAPIsToBeCopied = getListOfAPIsToBeCopied(swaggerDir+File.separator+copyAPIsFileName);
				
				if(listOfAPIsToBeCopied.size() > 0)
				{
					basePath = (JsonNode) node.get("paths");
					fields = basePath.fields();
					pathsToBeRemoved = new ArrayList<String>();
					boolean isApiFound = false;
					while (fields.hasNext()) 
					{
						isApiFound = false;
				        Entry<String, JsonNode> jsonField = fields.next();
				        path = jsonField.getKey();
				        for(String api : listOfAPIsToBeCopied)
				        {
				        	if(path.equalsIgnoreCase(api) || path.toLowerCase().endsWith(api))
					        {
					        	isApiFound = true;
					        	break;
					        	
					        }

				        }
				        if(!isApiFound)
				        {
				        	pathsToBeRemoved.add(path);
				        }
					}
					
					for(String pathToBeRmoved : pathsToBeRemoved)
					{
							((ObjectNode) basePath).remove(pathToBeRmoved);
					}
				}
		
			}

			yamlMapper.writeValue(swaggerFile, node);
			
			System.out.println();
			System.out.println();
			System.out.println("=======================================");
			System.out.println("swagger.yaml created successfully.");
			System.out.println("=======================================");
			if(tempFile.exists())
			{
				FileUtils.forceDelete(tempFile);
			}
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
		
	
	
	public static ArrayList<String> getListOfAPIsToBeCopied(String sFileName) throws Exception
	{
		
		String line;
		ArrayList<String> apisList = new ArrayList<String>();
		
		File file = new File(sFileName); 
		
	    // open input stream test.txt for reading purpose.
        BufferedReader reader = new BufferedReader(new FileReader(file));
        

        while ((line = reader.readLine()) != null) 
        {
           if(line.trim().length() > 0)
           {
        	   apisList.add(line.toLowerCase());   
           }
           
        }
        
        System.out.println("File '"+sFileName+"' read successfully...");
        System.out.println();
        if(apisList.size() > 0)
        {
        	System.out.println();
            System.out.println("The following APIs will be copied and retained in swagger.yaml");
            System.out.println("---------------------------------------------------------------");
            System.out.println();
        }
        else
        {
        	System.out.println();
            System.out.println("File '"+copyAPIsFileName+"' has No APIs to be copied");
            System.out.println("---------------------------------------------------------------");
            System.out.println();
        }
        for(String api : apisList)
        {
        	System.out.println(api);
        }
        
        return apisList;
	}
}			
