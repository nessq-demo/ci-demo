package es.us.isa.restest.apichain.api.util;

import java.util.ArrayList;

/**
 * This interface defines a test writer. The classes that implement this interface should create domain-specific test cases ready to be executed (ex. RESTAssured test cases)
 */
public interface ITestWriter {

	/**
	 * From a collection of domain-independent test cases, the method writes domain-specific ready-to-run test cases using frameworks like RESTAssured.
	 * @param testCases The collection of domain-independent test cases to be instantiated
	 */
	void write(ArrayList<APIObject> apis);

}
