package core.net.data;

import core.discovery.data.FailedResponse;
import core.discovery.data.TimeoutResponse;
import core.log.LoggerHelper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RequestTransport<R extends ResponseTransport> extends Transport {
    private static final AtomicLong counter = new AtomicLong(0);

    private static ConcurrentHashMap<Class<?>, Class<? extends ResponseTransport>> cachedMap = new ConcurrentHashMap<>();

    private Long requestCounter;

    public RequestTransport() {
        requestCounter = counter.getAndIncrement();
        transportId = UUID.randomUUID().toString().replace("-", "");
    }

    public RequestTransport renewTransportId() {
        transportId = UUID.randomUUID().toString().replace("-", "");
        return this;
    }

    public Long getRequestCounter() {
        return requestCounter;
    }

    public void setRequestCounter(Long requestCounter) {
        this.requestCounter = requestCounter;
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
            LoggerHelper.logger.error("Generate response failed, " + e.getMessage() + " for transportId " + transportId + " this " + this);
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
