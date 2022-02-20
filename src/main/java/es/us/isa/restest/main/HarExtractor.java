package es.us.isa.restest.main;

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
import org.eclipse.osgi.internal.serviceregistry.ShrinkableEntrySetValueCollection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;

import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.util.PropertyManager;

public class HarExtractor {
	public static Properties propMain = new Properties();
	public static String testDataDir;
	public static String productName;
	public static String configPropertiesDir = "src/main/resources/config.properties";
	public static String zycusPropertiesDir = "src/test/resources/zycus_new/zycus.properties";
	public static Set<String> uniqueURLs = new HashSet<String>();

	public static boolean flagForPrintGET = true;
	public static boolean flagForPrintPOST = true;
	public static boolean printForQueryParamFlag = true;
	
	public static final float MATCH_WEIGHT = 0.40f;

	public static HashMap<String, ArrayList<String>> apis = new HashMap<String, ArrayList<String>>();
	public static List<String> urlListfromSwagger;
	
	public static void main(String args[]) 
	{
		harExtractor();
	}
	
	
	public static void harExtractor()
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
		
		List<String> urlUinqueListfromHAR = new ArrayList<String>();
		JSONObject apiObject = null;
		
		String subURIsForHarExtraction = null;
		
		String subURIs[] = null;
		
		try {
			
			
			FileInputStream fisLocal = new FileInputStream(configPropertiesDir);
			propMain.load(fisLocal);
			filesFolder = new File(propMain.getProperty("harfile.directory.location"));
			testDataDir = propMain.getProperty("json.from.har.location");
			
			
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
			
			System.out.println();
			System.out.println();
			
			System.out.println("------------------------------------PRE-REQISITES Before running HARExtractor--------------------------------------------");
			System.out.println("*** Important Note 1***: In \""+zycusPropertiesDir+"\" properties file, \"product.name.in.api.uri\" field should point to the product name");
			System.out.println("*** Important Note 2***: har files should present in \""+filesFolder+"\" folders");
			System.out.println("*** Important Note 3***: To create test data files for ***GET and PUT APIs***, API should present in both swagger and har file");
			System.out.println("*** Important Note 4***: For DELETE APIs, test data will be created when running TestGenerationAndExecution.java or TestGeneration.java");
			System.out.println("*** Important Note 5***: For DELETE APIs, if you want to do trial run and check it, execute 'DeleteAPITestDataCreator.java'");
			System.out.println("*** Important Note 6***: For DELETE APIs, 'delete_api_configuration.json' should be configured");
			System.out.println("*** Important Note 7***: There could be more than 1 Match between the Swagger URLs and URLs in HAR file.");
			System.out.println("*** Important Note 7 (Continue...)***: In this case, More than 1 test files will be created. You need to review and keep the correct one.");
						
			
			System.out.println("------------------------------------------------------------------------------------------------------------------------");
			System.out.println();
			System.out.println();
			
			
			
			System.out.println("product.name.in.api.uri = "+productName);
			
			File testDataDirectory = new File(testDataDir);
			
			if(testDataDirectory.exists())
			{
				for(File testDataFile : testDataDirectory.listFiles())
				{
					FileUtils.forceDelete(testDataFile);
				}
				System.out.println("Deleted all the existing test data files in \""+testDataDir+"\" and creating new files.....");
				System.out.println();
				System.out.println();
				
			}
			else
			{
				testDataDirectory.mkdir();
			}
			
			File[] listOfFiles = filesFolder.listFiles();
			
			if(listOfFiles == null)
			{
				System.out.println("There are no HAR Files @ "+filesFolder.getAbsolutePath());
				System.out.println("Pls check the folder. Exiting...");
				System.exit(0);
				
			}
			
				// int count=0;
			urlListfromSwagger = new ArrayList<String>(swaggerYamlParse());

			// System.out.println(urlListfromSwagger);

			for (int i = 0; i < listOfFiles.length; i++) {
				// if -1 loop started
				// count=count+1;
				if (listOfFiles[i].getName().toString().endsWith(".har")) {
					System.out.println(listOfFiles[i].getName().toString());
					FileReader reader = new FileReader(listOfFiles[i].getPath());
					Object obj = jsonParser.parse(reader);
					JSONObject jsonObject = (JSONObject) obj;
					apiFlowarrayObj = new JSONArray();
					JSONObject jsonObjectEntries = (JSONObject) jsonObject.get("log");
					intervention = jsonObjectEntries.get("entries");
					if (intervention instanceof JSONArray) {
						// It's an array
						interventionJsonArray = (JSONArray) intervention;
						for (int arryObj = 0; arryObj < interventionJsonArray.size(); arryObj++) {
							JSONObject entryObjects = (JSONObject) interventionJsonArray.get(arryObj);
							JSONObject entryrequest = (JSONObject) entryObjects.get("request");
							JSONObject entryResponce = (JSONObject) entryObjects.get("response");
							if (entryrequest.containsKey("method")) {
								httpMethod = entryrequest.get("method").toString();
							}
							if (entryrequest.containsKey("url")) {
								httpurl = entryrequest.get("url").toString();
								
								//Test Code
//								if(httpurl.toLowerCase().contains("/downloadpurchaseorder?"))
//								{
//									System.out.println("I am in ");
//									System.out.println("I am in ");
//								}
								
								if (!httpurl.contains(".csv") && 
									!httpurl.contains("session") && 
									!httpurl.contains(".js") && 
									!httpurl.contains(".svg") && 
									!httpurl.contains(".png") && 
									!httpurl.contains(".gif") && 
									!httpurl.contains(".css") && 
									!httpurl.contains("dd-icons") && 
									!httpurl.contains("fonts") && 
									!httpurl.contains("js") && 
									!httpurl.contains(".html") && 
									!httpurl.contains(".webmanifest") && 
									!httpurl.contains(".jpg") && 
									!httpurl.contains(".googleapis.com") && 
									!httpurl.contains(".google.com") && 
									!httpurl.contains("css") && 
									!httpurl.contains("beacons")) {
									apiFlowarrayObj.add(entryrequest.get("url"));
									
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
									
									

									//Raj
									//System.out.println( httpMethod + " : " + httpurl);
									uniqueURLs.add(httpMethod + " : " + httpurl);
									
									
									if(httpMethod.equalsIgnoreCase("GET") || httpMethod.equalsIgnoreCase("PUT") || httpMethod.equalsIgnoreCase("POST"))
									{
										if (!urlUinqueListfromHAR.contains(getURIwithoutDomain(entryrequest.get("url").toString())))
										{
											
											urlUinqueListfromHAR.add(getURIwithoutDomain(entryrequest.get("url").toString()));
										}

									}
									
									queryStringProcessor(httpMethod, getURIwithoutDomain(httpurl));
																		/*
									 * else
									 * if(!urlUinqueListfromHAR.contains(java.
									 * net.URLDecoder.decode(entryrequest.get(
									 * "url").toString().replace(
									 * "https://dewdrops-qcvw.zycus.net/cns/api",
									 * ""), "UTF-8"))){
									 * urlUinqueListfromHAR.add(java.net.
									 * URLDecoder.decode(entryrequest.get("url")
									 * .toString().replace(
									 * "https://dewdrops-qcvw.zycus.net/cns/api",
									 * ""), "UTF-8")); } /*else
									 * if(!urlUinqueListfromHAR.contains(java.
									 * net.URLDecoder.decode(entryrequest.get(
									 * "url").toString().replace(
									 * "https://dewdrops-qcvw.zycus.net/cns",
									 * ""), "UTF-8"))){
									 * urlUinqueListfromHAR.add(java.net.
									 * URLDecoder.decode(entryrequest.get("url")
									 * .toString().replace(
									 * "https://dewdrops-qcvw.zycus.net/cns",
									 * ""), "UTF-8")); }
									 * 
									 * else{ // Collections.replaceAll(
									 * urlUinqueListfromHAR,
									 * "https://dewdrops-qcvw.zycus.net/cns/api",
									 * ""); }
									 */

								}

							}
							if (httpMethod.equalsIgnoreCase("POST") || httpMethod.equalsIgnoreCase("PUT")) {
								if (entryrequest.containsKey("postData")) {
									JSONObject bodyRequest = (JSONObject) entryrequest.get("postData");
									if (bodyRequest.containsKey("text")) {
										httpRequestBody = bodyRequest.get("text").toString();
									} else {
										httpRequestBody = entryrequest.get("postData").toString();
									}

								} else if (entryrequest.containsKey("putData")) {
									JSONObject bodyRequest = (JSONObject) entryrequest.get("putData");
									if (bodyRequest.containsKey("text")) {
										httpRequestBody = bodyRequest.get("text").toString();
									} else {
										httpRequestBody = entryrequest.get("putData").toString();
									}

								} else if (entryrequest.containsKey("deleteData")) {
									JSONObject bodyRequest = (JSONObject) entryrequest.get("deleteData");
									httpRequestBody = bodyRequest.get("text").toString();
								}

							} 
							
							if(httpMethod.equalsIgnoreCase("POST") || httpMethod.equalsIgnoreCase("PUT"))
							{
								if (!httpurl.contains(".csv") && !httpurl.contains("session") && !httpurl.contains(".js")
										&& !httpurl.contains(".svg") && !httpurl.contains(".png")
										&& !httpurl.contains(".gif") && !httpurl.contains(".css")
										&& !httpurl.contains("dd-icons") && !httpurl.contains("fonts")
										&& !httpurl.contains(".html")
										&& !httpurl.contains(".webmanifest") && !httpurl.contains(".jpg")
										&& !httpurl.contains(".googleapis.com") && !httpurl.contains(".google.com")
										&& !httpurl.contains("css") && !httpurl.contains("akamaihd")) {

									if (httpurl.contains("u/")) {
										arrOfStr = httpurl.split("u/");

										if (arrOfStr.length > 1) {

											file = new File(propMain.getProperty("json.from.har.location"));
											if (file.exists()) {

											} else {
												FileUtils.forceMkdir(file);
											}
											if (hexCheck1(arrOfStr[1]).toString().contains("queryID")) 
											{
												String queryID = hexCheck1(arrOfStr[1]).toString();
												int random = (int) (Math.random() * (max - min + 1) + min);
												String modifiedUrl = arrOfStr[1].replace(arrOfStr[1].toString(),
														"QUERYID" + random);
												file = new File(propMain.getProperty("json.from.har.location") + "/"
														+ productName+"_api_u_" + modifiedUrl.replaceAll("[^a-zA-Z0-9]", "_") + "_"
														+ httpMethod.toLowerCase() + "_body.json");
												
												String tURL = entryrequest.get("url").toString();
												if(tURL.indexOf("?") != -1)
												{
													tURL = tURL.substring(0,tURL.indexOf("?"));
												}
												String uriWithoutDomain = getURIwithoutDomain(tURL);
												
												ArrayList<String> mURLs = uriMatcher(uriWithoutDomain,urlListfromSwagger);
												
												if(mURLs.size() == 0)
													continue;
												
												for(String mURL : mURLs)
												{
													file = new File(propMain.getProperty("json.from.har.location")+"/"+ mURL + "_"
															+ httpMethod.toLowerCase() + "_body.json");
													
													if (file.createNewFile()) 
													{

														fileWriter = new FileWriter(file.getPath(), true);
														BufferedWriter bw = new BufferedWriter(fileWriter);
														if (!httpMethod.equalsIgnoreCase("GET") && httpRequestBody != null) {
															bw.write(httpRequestBody);
														} else {

															bw.write(queryID.toString());

														}
														
														if(flagForPrintPOST)
														{
															System.out.println("***Test Data Files for POST/PUT APIs***");
															System.out.println("--------------------------------------");
															flagForPrintPOST = false;
														}
														System.out.println("For "+ httpMethod+" API Name \""+httpurl+"\", Test Data File Created @ "+file);
														bw.close();
													}

												}
												
											} else 
											{

												file = new File(
														propMain.getProperty("json.from.har.location") + "/" + productName+"_api_u_"
																+ arrOfStr[1].toString().replaceAll("[^a-zA-Z0-9]", "_")
																+ "_" + httpMethod.toLowerCase() + "_body.json");
												
												String tURL = entryrequest.get("url").toString();
												if(tURL.indexOf("?") != -1)
												{
													tURL = tURL.substring(0,tURL.indexOf("?"));
												}
												String uriWithoutDomain = getURIwithoutDomain(tURL);
												
												ArrayList<String> mURLs = uriMatcher(uriWithoutDomain,urlListfromSwagger);
												
												if(mURLs.size() == 0)
													continue;
												
												for(String mURL : mURLs)
												{
													file = new File(propMain.getProperty("json.from.har.location")+"/"+ mURL + "_"
															+ httpMethod.toLowerCase() + "_body.json");
																								
													if (file.createNewFile()) {
	
														fileWriter = new FileWriter(file.getPath(), true);
														BufferedWriter bw = new BufferedWriter(fileWriter);
														if (!httpMethod.equalsIgnoreCase("GET") && httpRequestBody != null) {
															bw.write(httpRequestBody);
														} else {
															JSONParser parser = new JSONParser();
															JSONObject requestId = (JSONObject) parser
																	.parse(httpRequestBody.toString());
															// System.out.println(httpRequestBody
															// + "---" + requestId +
															// "---" + arrOfStr[1]);
															if (arrOfStr[1].toString()
																	.contains(requestId.get("requestID").toString())) {
																bw.write(requestId.get("requestID").toString());
															}
	
														}
														if(flagForPrintPOST)
														{
															System.out.println("***Test Data for Request Body created for following APIs***");
															System.out.println("--------------------------------------");
															flagForPrintPOST = false;
														}
														System.out.println("For "+ httpMethod+" API Name \""+httpurl+"\", Test Data File Created @ "+file);
														bw.close();
													}
												}	

											}

										}
									} else if (httpurl.contains("a/")) {
									  
										arrOfStr = httpurl.split("a/");

										if (arrOfStr.length > 1) {

											file = new File(propMain.getProperty("json.from.har.location"));
											if (file.exists()) {

											} else {
												FileUtils.forceMkdir(file);
											}

											if (hexCheck1(arrOfStr[1]).toString().contains("queryID")) {
												String queryID = hexCheck1(arrOfStr[1]).toString();
												int random = (int) (Math.random() * (max - min + 1) + min);
												String modifiedUrl = arrOfStr[1].replace(arrOfStr[1].toString(),
														"QUERYID" + random);
												file = new File(propMain.getProperty("json.from.har.location") + "/"
														+ productName+"_api_a_" + modifiedUrl.replaceAll("[^a-zA-Z0-9]", "_") + "_"
														+ httpMethod.toLowerCase() + "_body.json");
												
												String tURL = entryrequest.get("url").toString();
												if(tURL.indexOf("?") != -1)
												{
													tURL = tURL.substring(0,tURL.indexOf("?"));
												}
												String uriWithoutDomain = getURIwithoutDomain(tURL);
												ArrayList<String> mURLs = uriMatcher(uriWithoutDomain,urlListfromSwagger);
												
												if(mURLs.size() == 0)
													continue;
												
												for(String mURL : mURLs)
												{
													file = new File(propMain.getProperty("json.from.har.location")+"/"+ mURL + "_"
															+ httpMethod.toLowerCase() + "_body.json");
														
													if (file.createNewFile()) 
													{
	
														fileWriter = new FileWriter(file.getPath(), true);
														BufferedWriter bw = new BufferedWriter(fileWriter);
														if (!httpMethod.equalsIgnoreCase("GET") && httpRequestBody != null) {
															bw.write(httpRequestBody);
														} else {
	
															bw.write(queryID.toString());
	
														}
														if(flagForPrintPOST)
														{
															System.out.println("***Test Data for Request Body created for following APIs***");
															System.out.println("--------------------------------------");
															flagForPrintPOST = false;
														}
														System.out.println("For "+ httpMethod+" API Name \""+httpurl+"\", Test Data File Created @ "+file);
														bw.close();
													}
												}
											} else {

												file = new File(
														propMain.getProperty("json.from.har.location") + "/" + productName+"_api_a_"
																+ arrOfStr[1].toString().replaceAll("[^a-zA-Z0-9]", "_")
																+ "_" + httpMethod.toLowerCase() + "_body.json");

												String tURL = entryrequest.get("url").toString();
												if(tURL.indexOf("?") != -1)
												{
													tURL = tURL.substring(0,tURL.indexOf("?"));
												}
												String uriWithoutDomain = getURIwithoutDomain(tURL);
												
												ArrayList<String> mURLs = uriMatcher(uriWithoutDomain,urlListfromSwagger);
												
												if(mURLs.size() == 0)
													continue;
												
												for(String mURL : mURLs)
												{
													file = new File(propMain.getProperty("json.from.har.location")+"/"+ mURL + "_"
															+ httpMethod.toLowerCase() + "_body.json");
												
													if (file.createNewFile()) {
	
														fileWriter = new FileWriter(file.getPath(), true);
														BufferedWriter bw = new BufferedWriter(fileWriter);
														if (!httpMethod.equalsIgnoreCase("GET") && httpRequestBody != null) {
															bw.write(httpRequestBody);
														} else {
															JSONParser parser = new JSONParser();
															// System.out.println(httpRequestBody
															// + "---" + "---" +
															// arrOfStr[1]);
															//System.out.println(httpRequestBody.toString());
															JSONObject requestId = (JSONObject) parser
																	.parse(httpRequestBody.toString());
	
															if (arrOfStr[1].toString()
																	.contains(requestId.get("requestID").toString())) {
																bw.write(requestId.get("requestID").toString());
															}
	
														}
														if(flagForPrintPOST)
														{
															System.out.println("***Test Data for Request Body created for following APIs***");
															System.out.println("--------------------------------------");
															flagForPrintPOST = false;
														}
														System.out.println("For "+ httpMethod+" API Name \""+httpurl+"\", Test Data File Created @ "+file);
														bw.close();
													}
												}		
											}

										}
									} else 
									{
										
										file = new File(propMain.getProperty("json.from.har.location"));
										if (file.exists()) {

										} else {
											FileUtils.forceMkdir(file);
										}
										
										String tURL = entryrequest.get("url").toString();
										if(tURL.indexOf("?") != -1)
										{
											tURL = tURL.substring(0,tURL.indexOf("?"));
										}
										String uriWithoutDomain = getURIwithoutDomain(tURL);
										ArrayList<String> mURLs = uriMatcher(uriWithoutDomain,urlListfromSwagger);
											
										if(mURLs.size() == 0)
											continue;
											
										for(String mURL : mURLs)
										{
											file = new File(propMain.getProperty("json.from.har.location")+"/"+ mURL.replaceAll("/", "_") + "_"
														+ httpMethod.toLowerCase() + "_body.json");
											
											if (file.createNewFile()) 
											{
		
												fileWriter = new FileWriter(file.getPath(), true);
												BufferedWriter bw = new BufferedWriter(fileWriter);
												if (!httpMethod.equalsIgnoreCase("GET") && httpRequestBody != null) {
													bw.write(httpRequestBody);
												}
												if(flagForPrintPOST)
												{
													System.out.println("***Test Data for Request Body created for following APIs***");
													System.out.println("--------------------------------------");
													flagForPrintPOST = false;
												}
												System.out.println("For "+ httpMethod+" API Name \""+httpurl+"\", Test Data File Created @ "+file);
												bw.close();
												}
											}		

												
									} //else

								}
							}
							

						}
						fileApi = new File(propMain.getProperty("apiFlowFile.directory.location"));
						if (fileApi.exists()) {

						} else {
							FileUtils.forceMkdir(fileApi);
						}

						file = new File(propMain.getProperty("apiFlowFile.directory.location") + "/"
								+ listOfFiles[i].getName().toString().replace(".", "") + "_"
								+ propMain.getProperty("apiFlowFile.name"));
						// apiObject=new JSONObject();
						if (file.createNewFile()) {
							try {
								// apiObject.put("flow", apiFlowarrayObj);
								fileWriter = new FileWriter(file.getPath(), true);
								BufferedWriter bw = new BufferedWriter(fileWriter);
								// System.out.println(apiFlowarrayObj.toJSONString());
								bw.write(apiFlowarrayObj.toJSONString().replace("\\", ""));

								bw.close();
							} catch (Exception e) {
								System.out.println(e);
							}
						}
					} else if (intervention instanceof JSONObject) {
						// It's an object
						// interventionObject = (JSONObject) intervention;
					} else {
						// It's something else, like a string or number
					}

				} // if -1 loop end
			} // for -1 loop end

		//System.out.println(urlUinqueListfromHAR);
		
		

			paramsMatch = createQueryAndPathParamDictionaryFile(urlListfromSwagger, urlUinqueListfromHAR);
			paramsMatchDictionary = new File(testDataDir+ "/" + "paramsDictionary");
			// apiObject=new JSONObject();
			if (paramsMatchDictionary.createNewFile()) {
				try {
					// apiObject.put("flow", apiFlowarrayObj);
					jsonfileWriter = new FileWriter(paramsMatchDictionary.getPath(), true);
					BufferedWriter bw = new BufferedWriter(jsonfileWriter);
					// System.out.println(apiFlowarrayObj.toJSONString());
					for(Entry<String, String> entry: paramsMatch.entrySet())
					{
						bw.write(entry.getKey()+"="+entry.getValue());
					}
					bw.close();
				} catch (Exception e) {
					System.out.println(e);
				}
			}
			else{
				try {
					// apiObject.put("flow", apiFlowarrayObj);
					jsonfileWriter = new FileWriter(paramsMatchDictionary.getPath(), true);
					BufferedWriter bw = new BufferedWriter(jsonfileWriter);
					// System.out.println(apiFlowarrayObj.toJSONString());
					for(Entry<String, String> entry: paramsMatch.entrySet())
					{
						bw.write(entry.getKey()+"="+entry.getValue());
					}
					bw.close();
				} catch (Exception e) {
					System.out.println(e);
				}
			}
			
		
			System.out.println();
			System.out.println();
			allParamsProcessor(uriMatcher(urlListfromSwagger, urlUinqueListfromHAR));
			
			System.out.println();
			System.out.println();
			System.out.println("Unique API URLs Found in the HAR Files");
			System.out.println("----------------------------------------");
			for(String url : uniqueURLs)
			{
				System.out.println(url);
			}
			
			FormDataProducer.formDataProcessor();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		

	}
	
	public static HashMap<String, String> uriMatcher(List<String> swaggerURLs, List<String> harURLs) 
	{
		String swaggerURL = null;
		String harURL;
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		String[] a1;
		String[] a2;
		ArrayList<Integer> notMatchSequence = null;
		int uriLength = 0;
		
		boolean isMismatch = false;
		
		for(int i=0; i < harURLs.size(); i++)
		{
			harURL = harURLs.get(i);
			
			if(harURL.contains("?"))
			{
				harURL = harURL.substring(0, harURL.indexOf("?"));
			}
					
			if (harURL.startsWith("/")) {
				harURL = harURL.substring(1);
			}
			
			a2 = harURL.split("/");
			
			
			for(int j =0; j < swaggerURLs.size(); j++)
			{
				swaggerURL = swaggerURLs.get(j);
				if (swaggerURL.startsWith("/")) {
					swaggerURL = swaggerURL.substring(1);
				}
				a1 = swaggerURL.split("/");
				
				uriLength = a1.length;
				
				notMatchSequence = new ArrayList<Integer>(); 
				
				if (a1.length != a2.length) {
					continue;
				}
				for (int k = 0; k < a1.length; k++) 
				{
					if (!a1[k].equalsIgnoreCase(a2[k])) 
					{
						if (a1[k].contains("{") && a1[k].contains("}")) 
						{
							notMatchSequence.add(k);
						}
						else
						{
							isMismatch = true;
							break;
					
						}
					}
				}
				
				if(isMismatch)
				{
					isMismatch = false;
					continue;
				}
				
				if(notMatchSequence.size()/uriLength <= MATCH_WEIGHT)
				{
					paramsMap.put(harURL, swaggerURL);
					
				}
				
			}
			
			
		}
		
		return paramsMap;
	
	}
	
	public static ArrayList<String> uriMatcher(String URI, List<String> swaggerURLs) 
	{
		String swaggerURL = null;
		String harURL;
		String url = "";
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		String[] a1;
		String[] a2;
		ArrayList<Integer> notMatchSequence = new ArrayList<Integer>(); 
		int uriLength = 0;
		
		if(URI.contains("?"))
		{
			harURL = URI.substring(0,URI.indexOf("?"));
		}
		else
		{
			harURL = URI;
		}
		
		boolean isMismatch = false;
		ArrayList<String> matchedURLs = new ArrayList<String>(); 
		
		
		for(int i=0; i < swaggerURLs.size(); i++)
		{
			notMatchSequence = new ArrayList<Integer>(); 
			
			swaggerURL = swaggerURLs.get(i);
			if (swaggerURL.startsWith("/")) {
				swaggerURL = swaggerURL.substring(1);
			}
			a1 = swaggerURL.split("/");
			
			uriLength = a1.length;
			
			if (harURL.startsWith("/")) {
				harURL = harURL.substring(1);
			}
			
			a2 = harURL.split("/");

			if (a1.length != a2.length) {
				continue;
			}
			for (int k = 0; k < a1.length; k++) 
			{
				if (!a1[k].equalsIgnoreCase(a2[k])) 
				{
					if (a1[k].contains("{") && a1[k].contains("}")) 
					{
						notMatchSequence.add(k);
					}
					else
					{
						isMismatch = true;
						break;
				
					}
				}
			}
			
			if(isMismatch)
			{
				isMismatch = false;
				continue;
			}
			
			if(notMatchSequence.size()/uriLength <= MATCH_WEIGHT)
			{
				matchedURLs.add(swaggerURL.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", ""));
			}
		}	
			return matchedURLs;
	}
	
	
	public static void allParamsProcessor(HashMap<String, String> paramsMap) 
	{
		String apiName, apiNameWithActualValues;
		HashMap<String, String> params = new HashMap<String, String>();
		String[] a1;
		String[] a2;

		FileWriter writer;
		BufferedWriter buffWriter;
		String fileName = null;

		try {
			for (Entry<String, String> entry : paramsMap.entrySet()) {
				params = new HashMap<String, String>();
				apiName = entry.getValue();
				apiNameWithActualValues =  entry.getKey();
				if (apiName.startsWith("/")) {
					apiName = apiName.substring(1);
				}
				if (apiNameWithActualValues.startsWith("/")) {
					apiNameWithActualValues = apiNameWithActualValues.substring(1);
				}

				a1 = apiName.split("/");
				a2 = apiNameWithActualValues.split("/");

				if (a1.length != a2.length) {
					continue;
				}
				for (int i = 0; i < a1.length; i++) {
					if (!a1[i].equalsIgnoreCase(a2[i])) {
						if (a1[i].contains("{") && a1[i].contains("}")) {
							params.put(a1[i].replaceAll("\\}", "").replaceAll("\\{", ""), a2[i]);
						}
					}
				}
				
				if(params.size() > 0)
				{
					if (flagForPrintGET) {
						System.out.println("***Test Data for URI Param created for following APIs***");
						System.out.println("--------------------------------------");
						flagForPrintGET = false;
					}
				}
				
				for (Entry<String, String> entry1 : params.entrySet()) {
					
					ArrayList<String> methods = new ArrayList<String>(); 
					
					if(apis.containsKey("/"+apiName))
					{
						methods = apis.get("/"+apiName);
						
						for(String method : methods)
						{
							if(method.equalsIgnoreCase("get"))
							{
								fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
										+ entry1.getKey() + "_get.csv";
								if(new File(testDataDir + File.separator + fileName).exists())
								{
									int rand = (int) (Math.random()*10);
									fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
											+ entry1.getKey() + "_get"+rand+".csv";
									
								}
								writer = new FileWriter(testDataDir + File.separator + fileName);
								writer.write(entry1.getValue() + "\n" + getInvalidTestData(entry1.getValue()));
								writer.close();
								System.out.println("For "+method.toUpperCase()+" API Name \"" + apiName + "\", Test Data File Created @ " + fileName);

							}
							else if(method.equalsIgnoreCase("put"))
							{
								fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
										+ entry1.getKey() + "_put.csv";
								if(new File(testDataDir + File.separator + fileName).exists())
								{
									int rand = (int) (Math.random()*10);
									fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
											+ entry1.getKey() + "_put"+rand+".csv";
									
								}
								writer = new FileWriter(testDataDir + File.separator + fileName);
								writer.write(entry1.getValue() + "\n" + getInvalidTestData(entry1.getValue()));
								writer.close();
								System.out.println("For "+method.toUpperCase()+" API Name \"" + apiName + "\", Test Data File Created @ " + fileName);
							}
							else if(method.equalsIgnoreCase("post"))
							{
								fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
										+ entry1.getKey() + "_post.csv";
								if(new File(testDataDir + File.separator + fileName).exists())
								{
									int rand = (int) (Math.random()*10);
									fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
											+ entry1.getKey() + "_post"+rand+".csv";
									
								}
								writer = new FileWriter(testDataDir + File.separator + fileName);
								writer.write(entry1.getValue() + "\n" + getInvalidTestData(entry1.getValue()));
								writer.close();
								System.out.println("For "+method.toUpperCase()+" API Name \"" + apiName + "\", Test Data File Created @ " + fileName);
							}
							
						}
					}
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

	}
	
	public static Object getInvalidTestData(String data)
	{
		if(data.equalsIgnoreCase("true") || data.equalsIgnoreCase("false"))
		{
			if(data.equalsIgnoreCase("true"))
			{
				return new String("TRUE");
			}
			else
			{
				return new String("FALSE");
			}
		}
		else if(isDouble(data))
		{
			return 198765432;
		}
		else
			return "INVALIDDATA";
	}
	
	private static boolean isDouble(String data)
	{
		boolean isDouble = false;
		try
		{
			new Double(data);
			isDouble = true;
			
		}catch(Exception e)
		{
			
		}
		return isDouble;
				
	}
	
	public static void queryStringProcessor(String httpMethod, String httpUrl) 
	{
		ArrayList<String> mURLs = uriMatcher(httpUrl,urlListfromSwagger);
		
		for(int x = 0; x < mURLs.size(); x++)
		{
			httpUrl = java.net.URLDecoder.decode(httpUrl);
			FileWriter writer;
			
			if(httpUrl.indexOf("?") == -1)
			{
				return;
			}
			
			String queryString = httpUrl.substring(httpUrl.indexOf("?")+1);
			//String apiName = httpUrl.substring(0,httpUrl.indexOf("?")); 		
			String apiName = mURLs.get(x); 	
			
			HashMap<String, String> queryParams = new HashMap<>();
			
			String params[] = queryString.split("&");
			String val[] = null;
			
			
			for(String param : params)
			{
				val = param.split("=");
				
				if(val.length == 1)
				{
					System.out.println(param);
					continue;
				}
				
				queryParams.put(val[0], val[1]);
				
			}
			
			String fileName = null;
			try 
			{
				for (Entry<String, String> entry : queryParams.entrySet()) 
				{
					
					if (apiName.startsWith("/")) {
						apiName = apiName.substring(1);
					}
					
					if(httpMethod.equalsIgnoreCase("get"))
					{
						fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
								+ entry.getKey() + "_get.csv";
						if(new File(testDataDir + File.separator + fileName).exists())
						{
							int rand = (int) (Math.random()*10);
							fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
									+ entry.getKey() + "_get"+rand+".csv";
							
						}
						writer = new FileWriter(testDataDir + File.separator + fileName);
						writer.write(entry.getValue() + "\n" + getInvalidTestData(entry.getValue()));
						writer.close();
						System.out.println("For query param '"+entry.getKey()+"' & method '"+httpMethod.toUpperCase()+"' API Name \"" + apiName + "\", Test Data File Created @ " + fileName);

					}
					else if(httpMethod.equalsIgnoreCase("put"))
					{
						fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
								+ entry.getKey() + "_put.csv";
						if(new File(testDataDir + File.separator + fileName).exists())
						{
							int rand = (int) (Math.random()*10);
							fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
									+ entry.getKey() + "_put"+rand+".csv";
							
						}
						writer = new FileWriter(testDataDir + File.separator + fileName);
						writer.write(entry.getValue() + "\n" + getInvalidTestData(entry.getValue()));
						writer.close();
						System.out.println("For query param '"+entry.getKey()+"' & method '"+httpMethod.toUpperCase()+"' API Name \"" + apiName + "\", Test Data File Created @ " + fileName);
					}
					else if(httpMethod.equalsIgnoreCase("post"))
					{
						fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
								+ entry.getKey() + "_post.csv";
						if(new File(testDataDir + File.separator + fileName).exists())
						{
							int rand = (int) (Math.random()*10);
							fileName = apiName.replaceAll("/", "_").replaceAll("\\}", "").replaceAll("\\{", "") + "_"
									+ entry.getKey() + "_post"+rand+".csv";
							
						}
						writer = new FileWriter(testDataDir + File.separator + fileName);
						writer.write(entry.getValue() + "\n" + getInvalidTestData(entry.getValue()));
						writer.close();
						System.out.println("For query param '"+entry.getKey()+"' & method '"+httpMethod.toUpperCase()+"' API Name \"" + apiName + "\", Test Data File Created @ " + fileName);
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
				System.exit(-1);
			}

		}
		
		
		
	}


	public static Boolean hexCheck(String data) {
		int n = data.length();
		if (n > 30) {
			for (int charPosition = 0; charPosition < n; charPosition++) {
				char ch = data.charAt(charPosition);
				if ((ch < '0' || ch > '9') && (ch < 'A' || ch > 'F')) {

				}

			}
			// System.out.println("Yes" + data);
			return true;

		}
		// System.out.println("No" + data);
		return false;
	}

	public static String hexCheck1(String data) {
		String[] splitArrayBasedonSplash = data.split("/");

		for (int index = 0; index < splitArrayBasedonSplash.length; index++) {

			int n = splitArrayBasedonSplash[index].length();
			if (n > 30) {
				for (int charPosition = 0; charPosition < n; charPosition++) {
					char ch = data.charAt(charPosition);
					if ((ch < '0' || ch > '9') && (ch < 'A' || ch > 'F')) {

					}

				}

				// System.out.println("Yes--" + splitArrayBasedonSplash[index]);
				return "{" + "\"queryID\"" + ":\"" + splitArrayBasedonSplash[index].toString() + "\"}";
			}

		}
		// System.out.println("No--" + data);
		return "No";
	}

	public static Set<String> swaggerYamlParse() {

		String[] referenceMethods = {"post","put","delete","get"};
		Yaml yaml = new Yaml();
		InputStream inputStream;
		Map<String, Object> obj = null, obj1 = null, obj2 = null;
		
		try {
			inputStream = new FileInputStream("src/test/resources/zycus_new/swagger.yaml");
			obj = yaml.load(inputStream);
			obj1 = (Map<String, Object>) obj.get("paths");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Iterator<Map.Entry<String, Object>> itr = obj1.entrySet().iterator();
		ArrayList<String> methods = new ArrayList<String>();
		String _method = null;
	    while(itr.hasNext())
        {
             Map.Entry<String, Object> entry = itr.next();
             
             for(int i = 0; i< referenceMethods.length; i++)
             {
            	 _method = referenceMethods[i];
            	 obj2 = (Map<String, Object>) entry.getValue();
                 if(obj2.get(_method) != null)
                 {
                	 methods.add(_method);
                 }
             }
             
             apis.put(entry.getKey(), methods);
             methods = new ArrayList<String>();
        }
	    
		return obj1.keySet();

	}

	public static HashMap<String, String> createQueryAndPathParamDictionaryFile(List<String> swaggerAPIsList,
			List<String> apisfromHarList) throws IOException {
		// File file;
		HashMap<String, String> paramsMatch = new HashMap<String, String>();
		// FileWriter fileWriter;
		for (int index = 0; index < apisfromHarList.size(); index++) {

			String[] apisfromHarListSplitArray = apisfromHarList.get(index).toString().split("/");
			for (int innerIndex = 0; innerIndex < swaggerAPIsList.size(); innerIndex++) {

				String[] swaggerAPIsListSplitArray = swaggerAPIsList.get(innerIndex).toString().split("/");

				if (apisfromHarListSplitArray[apisfromHarListSplitArray.length - 1].toString()
						.equalsIgnoreCase(swaggerAPIsListSplitArray[swaggerAPIsListSplitArray.length - 1].toString())) {
					int swagParamCount = 0;

					for (int swagindex = 0; swagindex < swaggerAPIsListSplitArray.length; swagindex++) {

						if (swaggerAPIsListSplitArray[swagindex].toString().startsWith("{")) {
							swagParamCount = swagParamCount + 1;
						}

					}

					if (((printIntersection(swaggerAPIsListSplitArray, apisfromHarListSplitArray) - 1) * 100
							/ swaggerAPIsListSplitArray.length) > 55 && swagParamCount != 0) {
						paramsMatch.put(swaggerAPIsList.get(innerIndex).toString(),
								apisfromHarList.get(index).toString());
						// System.out.println(swaggerAPIsList.get(innerIndex) +
						// " -- -- " + apisfromHarList.get(index));
					}

				}

				else {

					int swagParamCount = 0;

					for (int swagindex = 0; swagindex < swaggerAPIsListSplitArray.length; swagindex++) {

						if (swaggerAPIsListSplitArray[swagindex].toString().startsWith("{")) {
							swagParamCount = swagParamCount + 1;
						}

					}

					if (((printIntersection(swaggerAPIsListSplitArray, apisfromHarListSplitArray) - 1) * 100
							/ swaggerAPIsListSplitArray.length) > 55 && swagParamCount != 0) {
						paramsMatch.put(swaggerAPIsList.get(innerIndex).toString(),
								apisfromHarList.get(index).toString());
						// System.out.println(swaggerAPIsList.get(innerIndex) + " -- -- " +
						// apisfromHarList.get(index));
					}

				}

			}

		}

		// System.out.println(paramsMatch);
		return paramsMatch;

	}

	static int printIntersection(String[] arr1, String[] arr2) {

		String[] firstArray = arr1;
		String[] secondArray = arr2;

		HashSet<String> set = new HashSet<>();

		set.addAll(Arrays.asList(firstArray));

		set.retainAll(Arrays.asList(secondArray));

		// System.out.println(set);

		// convert to array
		String[] intersection = {};
		intersection = set.toArray(intersection);

		// System.out.println(Arrays.toString(intersection));
		return intersection.length;

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
	
		
}
