package chat.errors;



/**
 * code -29999~-20000
 */
public interface GroovyErrorCodes {
	int CODE_CORE = -20000;

	//Groovy related codes. 
    int ERROR_GROOVY_CLASSNOTFOUND = CODE_CORE - 1;
	int ERROR_GROOY_NEWINSTANCE_FAILED = CODE_CORE - 2;
	int ERROR_GROOY_CLASSCAST = CODE_CORE - 3;
	int ERROR_GROOVY_INVOKE_FAILED = CODE_CORE - 4;
	int ERROR_GROOVYSERVLET_SERVLET_NOT_INITIALIZED = CODE_CORE - 5;
	int ERROR_URL_PARAMETER_NULL = CODE_CORE - 6;
	int ERROR_URL_VARIABLE_NULL = CODE_CORE - 7;
	int ERROR_GROOVY_PARSECLASS_FAILED = CODE_CORE - 8;
	int ERROR_GROOVY_UNKNOWN = CODE_CORE - 9;
	int ERROR_GROOVY_CLASSLOADERNOTFOUND = CODE_CORE - 10;
	int ERROR_JAVASCRIPT_LOADFILE_FAILED = CODE_CORE - 11;
	int ERROR_URL_HEADER_NULL = CODE_CORE - 12;

}
