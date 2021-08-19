package oceanus.sdk.core.net.data;

import oceanus.sdk.core.discovery.data.FailedResponse;
import oceanus.sdk.core.discovery.data.TimeoutResponse;
import oceanus.sdk.logger.LoggerEx;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RequestTransport<R extends ResponseTransport> extends Transport {
    private static final String TAG = RequestTransport.class.getSimpleName();
    private static ConcurrentHashMap<Class<?>, Class<? extends ResponseTransport>> cachedMap = new ConcurrentHashMap<>();

    public RequestTransport() {
        transportId = UUID.randomUUID().toString().replace("-", "");
    }

    public RequestTransport renewTransportId() {
        transportId = UUID.randomUUID().toString().replace("-", "");
        return this;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " id " + transportId;
    }

    public R generateResponse() {
        Class<?> clazz = this.getClass();
        Class<? extends Transport> transportClass = getResponseTransportClass(clazz);

//        Class<? extends Transport> transportClass = null;
//        ParameterizedType pType = (ParameterizedType) this.getClass().getGenericSuperclass();
//        Type[] params = pType.getActualTypeArguments();
//        if (params != null && params.length == 1) {
//            transportClass = (Class<? extends Transport>) params[0];
//        }

        try {
            R r = (R) transportClass.getConstructor().newInstance();
//            R r = (R) ClientHelloResponse.class.getConstructor().newInstance();
            r.setTransportId(this.transportId);
            return r;
        } catch (Throwable e) {
            e.printStackTrace();
            LoggerEx.error(TAG, "Generate response failed, " + e.getMessage() + " for transportId " + transportId + " this " + this);
            return null;
        }
    }

    public Class<? extends ResponseTransport> getResponseTransportClass(Class<?> clazz) {
        Class<? extends ResponseTransport> transportClass = cachedMap.get(clazz);
        if(transportClass == null) {
            synchronized (cachedMap) {
                if(transportClass == null) {
                    ParameterizedType pType = (ParameterizedType) this.getClass().getGenericSuperclass();
                    Type[] params = pType.getActualTypeArguments();
                    if (params != null && params.length == 1) {
                        transportClass = (Class<? extends ResponseTransport>) params[0];
                    }
                }
                if(transportClass == null) {
                    throw new IllegalArgumentException("TransportClass doesn't be found for class " + clazz);
                } else {
                    cachedMap.put(clazz, transportClass);
                }
            }
        }
        return transportClass;
    }

    public FailedResponse generateFailedResponse(int code, String message) {
        FailedResponse response = new FailedResponse(code, message);
        response.setTransportId(transportId);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
    public TimeoutResponse generateTimeoutResponse(int code, String message) {
        TimeoutResponse response = new TimeoutResponse(code, message);
        response.setTransportId(transportId);
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
