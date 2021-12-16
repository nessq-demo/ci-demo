package es.us.isa.restest.main;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

public class GraphWalkerJsonBuilder {
	public static Properties propMain = new Properties();
	public static void main(String args[]){
		
		JSONParser jsonParser = new JSONParser();
		
		
		JSONArray jsonModelArray=new JSONArray();
		JSONObject jsonObjectInner;
		JSONArray jsonObjectVertices = null;
		 
		JSONArray jsonObjectEdges ;
		File file,fileapi,generatedApiFlowFolder=null;
		FileWriter fileWriter;
		try{
			
			
			FileInputStream fisLocal = new FileInputStream("src/main/resources/config.properties");
			propMain.load(fisLocal);
			file = new File(propMain.getProperty("base.graphBuilder.file.loation")+"/"+propMain.getProperty("base.graphBuilder.file.name"));
			
			//filesFolder = new File(propMain.getProperty("base.graphBuilder.file.loation"));
			fileapi = new File(propMain.getProperty("apiFlowFile.directory.location"));
			File[] listOfFiles = fileapi.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				generatedApiFlowFolder = new File(propMain.getProperty("generated.graphBuilder.file.loation"));
				if (generatedApiFlowFolder.exists()) {

				} else {
					FileUtils.forceMkdir(generatedApiFlowFolder);
				}
				File fileGraphWalker = new File(propMain.getProperty("generated.graphBuilder.file.loation") + "/"
					+listOfFiles[i].getName().toString().replace(".","")+"fileGraphWalker.json");
			
			
				FileReader reader = new FileReader(file.getPath());
				
			FileReader readerapi = new FileReader(listOfFiles[i].getPath());
			//Object obj = jsonParser.parse(reader);
			//System.out.println(listOfFiles[i].getPath());
			JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);
			JSONArray jsonapiArray=(JSONArray)jsonParser.parse(readerapi);
			if(jsonObject.containsKey("models") ){
				jsonModelArray=(JSONArray)jsonObject.get("models");
				
					
						
						 jsonObjectInner = (JSONObject) jsonModelArray.get(0);
						 jsonObjectVertices = (JSONArray) jsonObjectInner.get("vertices");
						 jsonObjectEdges = (JSONArray) jsonObjectInner.get("edges");
						
					for(int indexApi=0;indexApi<jsonapiArray.size();indexApi++){
						JSONObject apiObjectVertex = new JSONObject();
						JSONObject apiObjectVertexPrperties = new JSONObject();
						 apiObjectVertex.put("name", jsonapiArray.get(indexApi).toString().replaceAll("[^a-zA-Z0-9]", "-"));
						 apiObjectVertex.put("id", "v"+indexApi);
						 apiObjectVertexPrperties.put("x", 2106);
						 apiObjectVertexPrperties.put("y", 6332);
						 apiObjectVertex.put("properties", apiObjectVertexPrperties);
						 //System.out.println(apiObjectVertex);
							jsonObjectVertices.add(apiObjectVertex);
					}
					for(int indexApi=0;indexApi<jsonapiArray.size()-1;indexApi++){
						JSONObject apiObjectEdges = new JSONObject();
						apiObjectEdges.put("targetVertexId", "v"+(indexApi+1));
						apiObjectEdges.put("name", "action_"+indexApi);
						apiObjectEdges.put("id", "e"+indexApi);
						apiObjectEdges.put("sourceVertexId", "v"+indexApi);
						jsonObjectEdges.add(apiObjectEdges);
					}

							//jsonObjectVertices.add(jsonObject.toJSONString().replace("\\",""));
						
					
					//System.out.println(jsonModelArray);
					/*apiObject.put("apiObject1", "1");
					apiObject.put("apiObject2", "2");
					apiObject.put("apiObject3", "3");
					apiObject.put("apiObject4", "4");
					//jsonModelArray.add(jsonObject.toJSONString());
					jsonModelArray.add(apiObject);*/
					fileGraphWalker.createNewFile();
					fileWriter = new FileWriter(fileGraphWalker.getPath(), false);
					jsonObject.writeJSONString(fileWriter);
					//fileWriter.write(jsonObject.toJSONString());
					fileWriter.close();
					//fileWriter.write(jsonObjectVertices.toJSONString());
					//BufferedWriter bw = new BufferedWriter(fileWriter);
					//System.out.println(jsonModelArray.toJSONString());
					//bw.write(jsonObject.toJSONString());

					//bw.close(); 
				
				
			}
			
			//System.out.println(jsonModelArray);
			//fileWriter = new FileWriter(file.getPath(), true);
			
			
			}
			
			
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
