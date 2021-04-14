package script.core.servlets;

import groovy.lang.GroovyObject;

public abstract class GroovyServlet implements GroovyObject{
	public static final String DELETE = "DELETE";
	public static final String GET = "GET";
	public static final String PUT = "PUT";
	public static final String POST = "POST";
	public static final String HEAD = "HEAD";
	public static final String OPTIONS = "OPTIONS";
	public static final String TRACE = "TRACE";
	
//	protected GroovyRuntime groovyRuntime;
	
//	public GroovyRuntime getGroovyRuntime() {
//		return groovyRuntime;
//	}
//	public void setGroovyRuntime(GroovyRuntime groovyRuntime) {
//		this.groovyRuntime = groovyRuntime;
//	}
}
