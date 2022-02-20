package es.us.isa.restest.generators;

import static es.us.isa.restest.util.SpecificationVisitor.hasDependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import es.us.isa.restest.configuration.pojos.Operation;
import es.us.isa.restest.configuration.pojos.TestConfigurationObject;
import es.us.isa.restest.mutation.TestCaseMutation;
import es.us.isa.restest.specification.OpenAPISpecification;
import es.us.isa.restest.testcases.TestCase;
import es.us.isa.restest.util.RESTestException;

/**
 *  This class implements a simple random test case generator
 * @author Sergio Segura
 *
 */
public class RandomTestCaseGenerator extends AbstractTestCaseGenerator {
	
	public static final String INDIVIDUAL_PARAMETER_CONSTRAINT = "individual_parameter_constraint";

	public RandomTestCaseGenerator(OpenAPISpecification spec, TestConfigurationObject conf, int nTests, int nGetTests) {
		super(spec, conf, nTests, nGetTests);
	}

	@Override
	protected Collection<TestCase> generateOperationTestCases(Operation testOperation) throws RESTestException {

		List<TestCase> testCases = new ArrayList<>();

		// Reset counters for the current operation
		resetOperation();

		boolean fulfillsDependencies = !hasDependencies(testOperation.getOpenApiOperation());
		
		while (hasNext(testOperation.getMethod())) {

			// Create test case with specific parameters and values
			//Timer.startCounting(TEST_CASE_GENERATION);
			TestCase test = generateNextTestCase(testOperation);
			test.setFulfillsDependencies(fulfillsDependencies);
			//Timer.stopCounting(TEST_CASE_GENERATION);
			
			// Set authentication data (if any)
			authenticateTestCase(test);

			// Add test case to the collection
			testCases.add(test);
			
			// Update indexes
			updateIndexes(test);

		}
		
		return testCases;
	}
	

	// Generate the next test case
	public TestCase generateNextTestCase(Operation testOperation) throws RESTestException {

		TestCase test = null;

		// If more faulty test cases need to be generated, try generating one
		if (nFaulty < (int) (faultyRatio * numberOfTests))
			test = generateFaultyTestCaseDueToIndividualConstraints(testOperation);
		if (test != null)
			return test;

		// If no more faulty test cases need to be generated, or one could not be generated, generate one nominal
		return generateRandomValidTestCase(testOperation);
	}

	// Returns true if there are more test cases to be generated
	protected boolean hasNext(String testMethod) {
		return nTests < numberOfTests;
	}
	
}
