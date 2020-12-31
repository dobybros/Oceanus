package com.docker.onlineserver;

import com.docker.data.DockerStatus;
import com.docker.server.OnlineServer;

public class OnlineServerWithStatus extends OnlineServer {
	private Integer statusForBalancer;
	private boolean changingForBalancer;

	public OnlineServerWithStatus() {
		super();
	}

	@Override
	protected DockerStatus generateDockerStatus(Integer port) {
		DockerStatus dockerStatus = super.generateDockerStatus(port);
		dockerStatus.setPublicDomain(baseConfiguration.getPublicDomain());
		return dockerStatus;
	}

}
