package core.discovery.node;

import java.util.Map;

/**
 * Created by wenqi on 2018/12/4
 */
public class ServiceAnnotation {
    private String type; //serviceAnnotationType, name of Annotation, like TransactionTry
    private Map<String, Object> annotationParams;
    private String className;
    private String methodName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getAnnotationParams() {
        return annotationParams;
    }

    public void setAnnotationParams(Map<String, Object> annotationParams) {
        this.annotationParams = annotationParams;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

}
