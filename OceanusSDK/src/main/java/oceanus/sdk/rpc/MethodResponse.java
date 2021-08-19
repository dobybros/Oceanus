package oceanus.sdk.rpc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import oceanus.sdk.errors.ChatErrorCodes;
import oceanus.apis.CoreException;
import oceanus.sdk.logger.LoggerEx;
import oceanus.sdk.rpc.remote.MethodMapping;
import oceanus.sdk.rpc.remote.stub.RpcCacheManager;
import oceanus.sdk.rpc.remote.stub.ServiceStubManager;
import oceanus.sdk.server.OnlineServer;
import oceanus.sdk.utils.DataInputStreamEx;
import oceanus.sdk.utils.DataOutputStreamEx;
import oceanus.sdk.utils.GZipUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MethodResponse extends RPCResponse {
    private static final String TAG = MethodResponse.class.getSimpleName();
    private byte version = 1;
    private Long crc;
    private Object returnObject;
    private CoreException exception;

    private String returnTmpStr;

    public static final String FIELD_RETURN = "return";
    public static final String FIELD_ERROR = "error";

    public MethodResponse() {
        super(MethodRequest.RPCTYPE);
    }

    public MethodResponse(Object returnObj, CoreException exception) {
        super(MethodRequest.RPCTYPE);
        this.returnObject = returnObj;
        this.exception = exception;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(MethodResponse.class.getSimpleName());
//		builder.append(": ").append(server);
        return builder.toString();
    }

    @Override
    public void resurrect() throws CoreException {
        byte[] bytes = getData();
        Byte encode = getEncode();
        if (bytes != null) {
            if (encode != null) {
                switch (encode) {
                    case ENCODE_JAVABINARY:
                        ByteArrayInputStream bais = null;
                        DataInputStreamEx dis = null;
                        try {
                            bais = new ByteArrayInputStream(bytes);
                            dis = new DataInputStreamEx(bais);
                            version = dis.readByte();
                            crc = dis.readLong();
                            if (crc == null || crc == 0 || crc == -1)
                                throw new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_CRC_ILLEGAL, "CRC is illegal for MethodRequest,service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));

                            ServiceStubManager serviceStubManager = null;
                            MethodRequest methodRequest = (MethodRequest) request;
                            if (request != null) {
                                serviceStubManager = methodRequest.getServiceStubManager();
                            }
                            if (serviceStubManager == null) {
                                serviceStubManager = OnlineServer.getInstance().getServiceStubManagerFactory().get();
                            }

                            MethodMapping methodMapping = serviceStubManager.getMethodMapping(crc);
//						if(methodMapping == null)
//							throw new CoreException(ChatErrorCodes.ERROR_METHODREQUEST_METHODNOTFOUND, "Method doesn't be found by crc " + crc);

                            int returnLength = dis.readInt();
                            if (returnLength > 0) {
                                byte[] returnBytes = new byte[returnLength];
                                dis.readFully(returnBytes);
                                try {
                                    byte[] data = GZipUtils.decompress(returnBytes);
                                    String json = new String(data, "utf8");
                                    returnTmpStr = json;
                                    Class<?> returnClass = ((MethodRequest)request).getSpecifiedReturnClass();
                                    if(returnClass != null && !returnClass.equals(Object.class)) {
                                        returnObject = JSON.parseObject(json, returnClass);
                                    } else {
                                        if (methodMapping == null || methodMapping.getReturnClass().equals(Object.class)) {
                                            returnObject = JSON.parse(json);
                                        } else {
                                            returnObject = JSON.parseObject(json, methodMapping.getGenericReturnClass());
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    LoggerEx.error(TAG, "Parse return bytes failed, " + e.getMessage() + ",service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                                }
                            }

                            int execeptionLength = dis.readInt();
                            if (execeptionLength > 0) {
                                byte[] exceptionBytes = new byte[execeptionLength];
                                dis.readFully(exceptionBytes);
                                try {
                                    byte[] data = GZipUtils.decompress(exceptionBytes);
                                    String json = new String(data, "utf8");
                                    JSONObject jsonObj = (JSONObject) JSON.parse(json);
                                    if (jsonObj != null) {
                                        Integer code = jsonObj.getInteger("code");
                                        String message = jsonObj.getString("message");
                                        String logLevel = jsonObj.getString("logLevel");
                                        if (code != null) {
                                            exception = new CoreException(code, message, logLevel);
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    LoggerEx.error(TAG, "Parse exception bytes failed, " + e.getMessage() + ",service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw new CoreException(ChatErrorCodes.ERROR_RPC_ENCODE_FAILED, "PB parse data failed, " + e.getMessage() + ",service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                        } finally {
                            IOUtils.closeQuietly(bais);
                            IOUtils.closeQuietly(dis.original());
                        }
                        break;
                    default:
                        throw new CoreException(ChatErrorCodes.ERROR_RPC_ENCODER_NOTFOUND, "Encoder type doesn't be found for resurrect,service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                }
            }
        }
    }

    @Override
    public void persistent() throws CoreException {
        Byte encode = getEncode();
        if (encode == null)
            throw new CoreException(ChatErrorCodes.ERROR_RPC_ENCODER_NULL, "Encoder is null for persistent,service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
        switch (encode) {
            case ENCODE_JAVABINARY:
                ByteArrayOutputStream baos = null;
                DataOutputStreamEx dis = null;
                try {
                    baos = new ByteArrayOutputStream();
                    dis = new DataOutputStreamEx(baos);
                    dis.writeByte(version);
                    dis.writeLong(crc);

                    byte[] returnBytes = null;
                    if (returnObject != null) {
                        String returnStr = null;
                        if (returnTmpStr == null)
                            returnStr = JSON.toJSONString(returnObject);
                        else
                            returnStr = returnTmpStr;
                        try {
                            returnBytes = GZipUtils.compress(returnStr.getBytes("utf8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "Generate return " + returnStr + " to bytes failed, " + e.getMessage() + ",service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                        }
                    }
                    if (returnBytes != null) {
                        dis.writeInt(returnBytes.length);
                        dis.write(returnBytes);
                    } else {
                        dis.writeInt(0);
                    }

                    byte[] exceptionBytes = null;
                    if (exception != null) {
                        JSONObject json = new JSONObject();
                        json.put("code", exception.getCode());
                        json.put("message", exception.getMessage());
                        json.put("logLevel", exception.getLogLevel());
                        String errorStr = json.toJSONString();//JSON.toJSONString(exception);
                        try {
                            exceptionBytes = GZipUtils.compress(errorStr.getBytes("utf8"));
                        } catch (IOException e) {
                            e.printStackTrace();
                            LoggerEx.error(TAG, "Generate error " + errorStr + " to bytes failed, " + e.getMessage() + ",service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                        }
                    }
                    if (exceptionBytes != null) {
                        dis.writeInt(exceptionBytes.length);
                        dis.write(exceptionBytes);
                    } else {
                        dis.writeInt(0);
                    }

                    byte[] bytes = baos.toByteArray();
                    setData(bytes);
                    setEncode(ENCODE_JAVABINARY);
                    setType(MethodRequest.RPCTYPE);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new CoreException(ChatErrorCodes.ERROR_RPC_ENCODE_FAILED, "PB parse data failed, " + t.getMessage() + ",service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
                } finally {
                    IOUtils.closeQuietly(baos);
                    IOUtils.closeQuietly(dis.original());
                }
                break;
            default:
                throw new CoreException(ChatErrorCodes.ERROR_RPC_ENCODER_NOTFOUND, "Encoder type doesn't be found for persistent,service_class_method: " + RpcCacheManager.getInstance().getMethodByCrc(crc));
        }
    }

    public Long getCrc() {
        return crc;
    }

    public void setCrc(Long crc) {
        this.crc = crc;
    }

    public Object getReturnObject() {
        return returnObject;
    }

    public void setReturnObject(Object returnObject) {
        this.returnObject = returnObject;
    }

    public CoreException getException() {
        return exception;
    }

    public void setException(CoreException exception) {
        this.exception = exception;
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public String getReturnTmpStr() {
        return returnTmpStr;
    }

    public void setReturnTmpStr(String returnTmpStr) {
        this.returnTmpStr = returnTmpStr;
    }
}
