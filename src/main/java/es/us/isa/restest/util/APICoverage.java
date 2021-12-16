package es.us.isa.restest.util;

import java.util.ArrayList;

public class APICoverage 
{
	private String apiName;
	private ArrayList<FieldCoverage> fieldCoverages;
	private String methodName;
	private int actualTestCaseCount;
	private int expectedTestCaseCount;
	private boolean isApiFoundInExecution = false;

	public boolean isApiFoundInExecution() {
		return isApiFoundInExecution;
	}
	public void setApiFoundInExecution(boolean isApiFoundInExecution) {
		this.isApiFoundInExecution = isApiFoundInExecution;
	}
	public int getActualTestCaseCount() {
		return actualTestCaseCount;
	}
	public void setActualTestCaseCount(int actualTestCaseCount) {
		this.actualTestCaseCount = actualTestCaseCount;
	}
	public int getExpectedTestCaseCount() {
		return expectedTestCaseCount;
	}
	public void setExpectedTestCaseCount(int expectedTestCaseCount) {
		this.expectedTestCaseCount = expectedTestCaseCount;
	}
	public ArrayList<FieldCoverage> getFieldCoverages() {
		return fieldCoverages;
	}
	public void setFieldCoverages(ArrayList<FieldCoverage> fieldCoverages) {
		this.fieldCoverages = fieldCoverages;
	}
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	public String getApiName() {
		return apiName;
	}
	public void setApiName(String apiName) {
		this.apiName = apiName;
	}

}
