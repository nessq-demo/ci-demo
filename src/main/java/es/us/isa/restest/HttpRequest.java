package es.us.isa.restest.apichain.api.util;

public class HttpRequest {
	
	private String body;
	private Header[] headers;
	
	private QueryParameter[] queryparams;
	
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
	public QueryParameter[] getQueryparams() {
		return queryparams;
	}
	public void setQueryparams(QueryParameter[] queryparams) {
		this.queryparams = queryparams;
	}
	
}
