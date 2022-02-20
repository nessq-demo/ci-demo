package es.us.isa.restest.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import es.us.isa.restest.util.PropertyManager;

public class Restest 
{
	private static String propertiesFilePath = "src/test/resources/zycus_new/zycus.properties";
	private static Logger logger = LogManager.getLogger(Restest.class.getName());
	private static String executionType;
	
	public static void main(String[] args) throws Exception
	{
		restest(args);
	}
	
	public static void restest(String[] args) throws Exception
	{
		
		
		String argument = null;
		readParameterValues();
		
		if(args.length != 0)
		{
			argument = args[0].trim();
		}
		else if(System.getenv("Execution_Type") != null) 
		{
			argument = System.getenv("Execution_Type");
		}
		else if(executionType != null)
		{
			argument = executionType;
		}
		
		if(argument == null)
		{
			logger.info("================================================");
			logger.info("Restest Failed to execute since no options mentioned");
			logger.info("args[]  = {}", args);
			logger.info("Execution_Type in pipeline  = {}", System.getenv("Execution_Type"));
			logger.info("execution.type in Zycus.properties  = {}", System.getenv("Execution_Type"));
			logger.info("================================================");
		}
		else
		{
			execute(argument);
		}
		
	}
	
	private static void execute(String arg) throws Exception
	{
		if(arg.equals("TestGeneration"))
		{
			TestGeneration.testGeneration(null);
		}
		else if(arg.equals("TestExecution"))
		{
			TestExecution.testExecution(null);
		}
		else if(arg.equals("GenerationExecution"))
		{
			TestGeneration.testGeneration(null);
			TestExecution.testExecution(null);
		}
		else if(arg.equals("TestGenerationAndExecution"))
		{
			TestGenerationAndExecution.testGenerationAndExecution(null);
		}
		else if(arg.equals("HarExtractor"))
		{
			HarExtractor.harExtractor();
		}
		else if(arg.equals("CreateSwagger"))
		{
			CreateSwagger.createSwaggerXML();
		}
		else if(arg.equals("CreateTestConf"))
		{
			CreateTestConf.createTestConf(null);
		}
		else if(arg.equals("TestDataProcessor"))
		{
			TestDataProcessor.testDataProcessor();
		}
		else
		{
			logger.info("================================================");
			logger.info("Failed to execute Restest");
			logger.info("Not Supported : {} ", arg);
			logger.info("Supported  : ");
			logger.info("\t TestGeneration");
			logger.info("\t TestExecution");
			logger.info("\t GenerationExecution");
			logger.info("\t TestGenerationAndExecution");
			logger.info("\t HarExtractor");
			logger.info("\t CreateSwagger");
			logger.info("\t CreateTestConf");
			logger.info("\t TestDataProcessor");
			logger.info("================================================");
			
		}
		
			
	}
	
	
	// Read the parameter values from the .properties file. If the value is not found, the system looks for it in the global .properties file (config.properties)
	private static void readParameterValues() throws Exception
	{
		
		executionType =  readParameterValue("execution.type");
		logger.info("execution.type: {}", executionType);
		
	}	
	
	// Read the parameter value from the local .properties file. If the value is not found, it reads it form the global .properties file (config.properties)
	private static String readParameterValue(String propertyName) 
	{

		String value = null;
		if (PropertyManager.readProperty(propertiesFilePath, propertyName) != null) // Read value from local .properties
																					// file
			value = PropertyManager.readProperty(propertiesFilePath, propertyName);
		else if (PropertyManager.readProperty(propertyName) != null) // Read value from global .properties file
			value = PropertyManager.readProperty(propertyName);

		return value;
	}
	
	
}
