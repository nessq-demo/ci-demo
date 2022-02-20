package es.us.isa.restest.apichain.api.util;

public class HttpResponse {
	
	private String body;
	private Header[] headers;
	private String statusCode;
	private String statusMessage;
	
	
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public Header[] getHeaders() {
		return headers;
	}
	public void setHeaders(Header[] headers) {
		this.headers = headers;
	}
	
	
}
