package oceanus.sdk.rpc;

public abstract class RPCResponse extends RPCBase {
	protected RPCRequest request;

	public RPCResponse(String type) {
		super(type);
	}

	public RPCRequest getRequest() {
		return request;
	}

	public void setRequest(RPCRequest request) {
		this.request = request;
	}
}
