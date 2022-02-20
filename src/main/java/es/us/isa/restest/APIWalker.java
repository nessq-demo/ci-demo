package es.us.isa.restest.apichain.api;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.h;
import org.eclipse.osgi.internal.serviceregistry.ShrinkableEntrySetValueCollection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;

import es.us.isa.restest.apichain.api.util.APIObject;
import es.us.isa.restest.apichain.api.util.Header;
import es.us.isa.restest.apichain.api.util.HttpRequest;
import es.us.isa.restest.apichain.api.util.HttpResponse;
import es.us.isa.restest.apichain.api.util.QueryParameter;
import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.util.PropertyManager;

public class APIWalker {
	public static Properties propMain = new Properties();
	public static String testDataDir;
	public static String apiDataDir; 
	public static String productName;
	public static String configPropertiesDir = "src/main/resources/config.properties";
	public static String zycusPropertiesDir = "src/test/resources/zycus_new/zycus.properties";
	public static Set<String> uniqueURLs = new HashSet<String>();

	public static boolean flagForPrintGET = true;
	public static boolean flagForPrintPOST = true;
	public static boolean printForQueryParamFlag = true;
	
	public static final float MATCH_WEIGHT = 0.40f;

	public static ArrayList<APIObject> apis = new ArrayList<>();
	
	public static void main(String args[]) 
	{
		harWalker();
	}
	
	public static void harWalker()
	{
		JSONParser jsonParser = new JSONParser();
		int min = 1;
		int max = 900;
		Object intervention;
		JSONArray interventionJsonArray;

		String httpRequestBody = null;
		String httpurl = null;
		String httpMethod = null;
		File file, filesFolder, fileApi, paramsMatchDictionary;
		FileWriter fileWriter, jsonfileWriter;
		HashMap<String, String> paramsMatch;
		String[] arrOfStr;
		JSONArray apiFlowarrayObj = null;
		List<String> urlListfromSwagger;
		List<String> urlUinqueListfromHAR = new ArrayList<String>();
		
		APIObject api;
		HttpRequest request;
		HttpResponse response;
		String apiNameWithoutDomainName;
		
		String subURIsForHarExtraction = null;
		String subURIs[] = null;
		
		try 
		{
			
			FileInputStream fisLocal = new FileInputStream(configPropertiesDir);
			propMain.load(fisLocal);
			filesFolder = new File(propMain.getProperty("harfile.directory.location"));
			testDataDir = propMain.getProperty("json.from.har.location");
			
			apiDataDir = testDataDir.substring(0,testDataDir.lastIndexOf("/"))+File.separator+"apis";
			
			FileInputStream fisZycus = new FileInputStream(zycusPropertiesDir);
			Properties zycusProperties = new Properties();
			zycusProperties.load(fisZycus);
			productName = zycusProperties.getProperty("product.name.in.api.uri");
		
			subURIsForHarExtraction = zycusProperties.getProperty("sub.uris.for.har.extraction");
			
			if(subURIsForHarExtraction != null)
			{
				subURIs = subURIsForHarExtraction.split(",");
			}
			fisZycus.close();
			
			System.out.println("product.name.in.api.uri = "+productName);
			
			File apiDataDirectory = new File(apiDataDir);
			
			if(apiDataDirectory.exists())
			{
				for(File testDataFile : apiDataDirectory.listFiles())
				{
					FileUtils.forceDelete(testDataFile);
				}
				System.out.println("Deleted all the existing test data files in \""+apiDataDir+"\" and creating new files.....");
				System.out.println();
				System.out.println();
				
			}
			else
			{
				apiDataDirectory.mkdir();
			}
			
			createRequestDirectory(apiDataDirectory);
			createResponseDirectory(apiDataDirectory);
			
			File[] listOfFiles = filesFolder.listFiles();
			
			if(listOfFiles == null)
			{
				System.out.println("There are no HAR Files @ "+filesFolder.getAbsolutePath());
				System.out.println("Pls check the folder. Exiting...");
				System.exit(0);
				
			}
			
			for (int i = 0; i < listOfFiles.length; i++) 
			{
				if (listOfFiles[i].getName().toString().endsWith(".har")) 
				{
					System.out.println(listOfFiles[i].getName().toString());
					FileReader reader = new FileReader(listOfFiles[i].getPath());
					Object obj = jsonParser.parse(reader);
					JSONObject jsonObject = (JSONObject) obj;
					apiFlowarrayObj = new JSONArray();
					JSONObject jsonObjectEntries = (JSONObject) jsonObject.get("log");
					intervention = jsonObjectEntries.get("entries");
					if (intervention instanceof JSONArray) 
					{
						interventionJsonArray = (JSONArray) intervention;
						for (int arryObj = 0; arryObj < interventionJsonArray.size(); arryObj++) 
						{
							JSONObject entryObjects = (JSONObject) interventionJsonArray.get(arryObj);
							JSONObject entryrequest = (JSONObject) entryObjects.get("request");
							JSONObject entryResponse = (JSONObject) entryObjects.get("response");
							if (entryrequest.containsKey("method")) {
								httpMethod = entryrequest.get("method").toString();
							}
							if (entryrequest.containsKey("url")) 
							{
								httpurl = entryrequest.get("url").toString();
								
								boolean uriFound = false;
								if(subURIs != null)
								{
									for(String subURI : subURIs)
									{
										if(httpurl.contains(subURI))
										{
											uriFound = true;
										}
										
									}
									if(!uriFound)
										continue;
									
								}
								else
								{
									if(!httpurl.toLowerCase().contains("/"+productName.toLowerCase()+"/api/"))
										
										continue;
								}
								
								uniqueURLs.add(httpMethod + " : " + httpurl);
								api = new APIObject();
								request = new HttpRequest();
								response = new HttpResponse();
								httpRequestBody = null;
								
								apiNameWithoutDomainName = 	getURIwithoutDomain(httpurl);
								request.setBody(getRequestBody(entryrequest));
								request.setQueryparams(getQueryParameters(httpurl));
								//request.setHeaders(getRequestHeaders(entryrequest));
								
								response.setBody(getResponseBody(entryResponse));
								
								api.setName(apiNameWithoutDomainName);
								api.setMethod(httpMethod);
								api.setDomainName(getDomainName(httpurl));
								api.setRequest(request);
								api.setResponse(response);
								apis.add(api);
							}
							
						}
					}
				}//Single Har File
			}//All Files
			
			createAPIJSONs(apis);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	

	public static void createRequestDirectory(File apiDataDirectory) throws Exception
	{
		File requestDirectory = new File(apiDataDirectory.getPath()+File.separator+"request");
		
		if(requestDirectory.exists())
		{
			for(File requestFile : requestDirectory.listFiles())
			{
				FileUtils.forceDelete(requestFile);
			}
			System.out.println();
			System.out.println();
			
		}
		else
		{
			requestDirectory.mkdir();
		}
	}
	

	public static void createResponseDirectory(File apiDataDirectory) throws Exception
	{
		File responseDirectory = new File(apiDataDirectory.getPath()+File.separator+"response");
		
		if(responseDirectory.exists())
		{
			for(File responseFile : responseDirectory.listFiles())
			{
				FileUtils.forceDelete(responseFile);
			}
			System.out.println();
			System.out.println();
			
		}
		else
		{
			responseDirectory.mkdir();
		}
	}
	
	
	public static void createAPIJSONs(ArrayList<APIObject> apis)
	{
		FileWriter writer;
		APIObject api;
		String fileName;
		boolean print = true;
		try
		{
			for(int i=0; i< apis.size(); i++)
			{
				api = apis.get(i);
				
				JSONObject root = new JSONObject();
				root.put("name",api.getName());
				root.put("method",api.getMethod());
				root.put("body",api.getRequest().getBody());
				
				
//				if(api.getRequest().getHeaders() != null)
//				{
//					JSONArray headers = new JSONArray();
//					JSONObject header;
//					Header h;
//					
//					for(int j = 0; j <api.getRequest().getHeaders().length; j++)
//					{
//						h = api.getRequest().getHeaders()[j];
//						header = new JSONObject();
//						header.put(h.getHeaderName(), h.getHeaderValue());
//						headers.add(header);
//					}
//					root.put("headers",headers);
//					
//				}
				
				if(api.getRequest().getQueryparams() != null)
				{
					JSONArray queryParameters = new JSONArray();
					JSONObject queryParameter;
					QueryParameter q;
					
					for(int j =0; j <api.getRequest().getQueryparams().length; j++)
					{
						q = api.getRequest().getQueryparams()[j];
						queryParameter = new JSONObject();
						queryParameter.put(q.getQueryParameterName(), q.getQueryParameterValue());
						queryParameters.add(queryParameter);
					}
					root.put("querystring",queryParameters);
				}
				if(print)
				{
					System.out.println("*****Creating the API JSON Files***");
					System.out.println("----------------------------------------");
					print = false;				}
				
				String name = (i+1)+"_"+api.getMethod().toUpperCase()+"_"+api.getName().substring(1).replaceAll("/", "_");
				api.setId(name);
				fileName = name +".json";
				
				writeRequestBody(name, api);
				writeResponseBody(name, api);
				
				System.out.println(fileName);
				writer = new FileWriter(apiDataDir + File.separator + fileName);
				writer.write(root.toString());
				writer.close();
				
			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
				
	}
	
	
	public static void writeRequestBody(String fileName, APIObject api) throws Exception
	{
		if(api.getRequest().getBody() != null)
		{
			fileName = fileName+"_request.json";
			FileWriter writer = new FileWriter(apiDataDir + File.separator + "request"+ File.separator + fileName);
			writer.write(api.getRequest().getBody());
			writer.close();
		}
	}
	
	public static void writeResponseBody(String fileName, APIObject api) throws Exception
	{
		if(api.getResponse().getBody() != null)
		{
			fileName = fileName+"_response.json";
			FileWriter writer = new FileWriter(apiDataDir + File.separator +"response" + File.separator + fileName);
			writer.write(api.getResponse().getBody());
			writer.close();
		}
	}
	
	public static Header[] getRequestHeaders(JSONObject request)
	{
		JSONArray jsonHeaders = (JSONArray) request.get("headers");
		JSONObject headerObject;
		ArrayList<Header> headers = new ArrayList<Header>();
		Header header;
		
		for(int i = 0; i<jsonHeaders.size(); i++)
		{
			header = new Header();
			headerObject = (JSONObject) jsonHeaders.get(i);
			header.setHeaderName((String)headerObject.get("name"));
			header.setHeaderValue((String)headerObject.get("value"));
			headers.add(header);
		}
			
		Header[] _headers = new Header[headers.size()];
		_headers = headers.toArray(_headers);
		return _headers;
		
	}
	
	
	public static String getRequestBody(JSONObject request)
	{
		String httpRequestBody = null;
		
		if (request.containsKey("postData")) 
		{
		
			JSONObject bodyRequest = (JSONObject) request.get("postData");
			if (bodyRequest.containsKey("text")) 
			{
				httpRequestBody = bodyRequest.get("text").toString();
			} else 
			{
				httpRequestBody = request.get("postData").toString();
			}

		} else if (request.containsKey("putData")) 
		{
			JSONObject bodyRequest = (JSONObject) request.get("putData");
			if (bodyRequest.containsKey("text")) 
			{
				httpRequestBody = bodyRequest.get("text").toString();
			} else 
			{
				httpRequestBody = request.get("putData").toString();
			}

		} else if (request.containsKey("deleteData")) 
		{
			JSONObject bodyRequest = (JSONObject) request.get("deleteData");
			httpRequestBody = bodyRequest.get("text").toString();
		}
		
		return httpRequestBody;
	}
	
	public static String getResponseBody(JSONObject response) throws Exception
	{
		JSONObject responseBody = null;
		JSONObject content;
		JSONObject text;
		if (response.containsKey("content")) 
		{
			content = (JSONObject) response.get("content");
			if(content.containsKey("text"))
			{
				JSONParser jsonParser = new JSONParser();
				Object obj = jsonParser.parse((String) content.get("text"));
				text = (JSONObject) obj;
				if(text.containsKey("data"))
				{
					responseBody = (JSONObject) text.get("data");
				}
			}
		}
		return responseBody.toString();
	}

	
	public static QueryParameter[] getQueryParameters(String httpUrl) 
	{
		
	
		httpUrl = java.net.URLDecoder.decode(httpUrl);
		FileWriter writer;
		
		if(httpUrl.indexOf("?") == -1)
		{
			return null;
		}
			
		ArrayList<QueryParameter> queryParameters = new ArrayList<>();
		QueryParameter queryParam = null;
		
		
		String queryString = httpUrl.substring(httpUrl.indexOf("?")+1);
		HashMap<String, String> queryParams = new HashMap<>();
		
		String params[] = queryString.split("&");
		String val[] = null;
		
		for(String param : params)
		{
			queryParam = new QueryParameter();
			
			val = param.split("=");
			queryParam.setQueryParameterName(val[0]);
			
			if(val.length == 2)
			{
				queryParam.setQueryParameterValue(val[1]);
			}
			
			queryParameters.add(queryParam);
			
		}
		
		QueryParameter[] _queryParameters = new QueryParameter[queryParameters.size()];
		_queryParameters = queryParameters.toArray(_queryParameters);
		return _queryParameters;
		
		

	}

	// Read the parameter value from the local .properties file. If the value is not
	// found, it reads it form the global .properties file (config.properties)
	private static String readParameterValue(String propertyName) {

		String value = null;
		if (PropertyManager.readProperty(zycusPropertiesDir, propertyName) != null) // Read value from local .properties
																					// file
			value = PropertyManager.readProperty(zycusPropertiesDir, propertyName);
		else if (PropertyManager.readProperty(propertyName) != null) // Read value from global .properties file
			value = PropertyManager.readProperty(propertyName);

		return value;
	}
	
	private static String getURIwithoutDomain(String uri)
	{
		uri = java.net.URLDecoder.decode(uri);
		String[] subURIs = uri.split("/");
		String uriWithoutDomain = "";
		
		for(int i=3; i< subURIs.length; i++)
		{
			uriWithoutDomain= uriWithoutDomain+"/"+subURIs[i];
		}
		
		return uriWithoutDomain;
	}
	
	
	
	private static String getDomainName(String uri)
	{
		uri = java.net.URLDecoder.decode(uri);
		String[] subURIs = uri.split("/");
		return subURIs[0]+"//"+subURIs[2];
		
	}	
}
