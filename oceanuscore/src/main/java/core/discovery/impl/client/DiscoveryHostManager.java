package core.discovery.impl.client;

import chat.logs.LoggerEx;
import core.discovery.data.FailedResponse;
import core.discovery.errors.DiscoveryErrorCodes;
import core.discovery.utils.RandomDraw;
import core.net.NetworkCommunicator;
import core.net.adapters.data.ContentPacket;
import core.net.data.RequestTransport;
import core.net.data.ResponseTransport;
import core.utils.ValidateUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DiscoveryHostManager {
    private static final String TAG = DiscoveryHostManager.class.getSimpleName();
    private String host;
    private int port;
    private final List<InetSocketAddress> discoveryAddresses = new ArrayList<>();
    private InetSocketAddress usingAddress;

    public List<InetSocketAddress> getDiscoveryAddresses() {
        return discoveryAddresses;
    }

    public DiscoveryHostManager(String hosts) {
        ValidateUtils.checkNotNull(hosts);

        String[] strs = hosts.split(",");
        for(String str : strs) {
            String[] hostPort = str.split(":");
            if(hostPort.length == 2) {
                String host = hostPort[0];
                String portStr = hostPort[1];
                Integer port = 16666;
                try {
                    port = Integer.parseInt(portStr);
                } catch(Throwable t) {
                    LoggerEx.error(TAG, "Illegal port string " + portStr);
                }
                discoveryAddresses.add(new InetSocketAddress(host, port));
            }
        }

        if(discoveryAddresses.isEmpty())
            throw new IllegalArgumentException("Need configure discovery host \"discovery.host\" in oceanus.properties");
    }

    public void init() {
        //Block to handle aquaman address.
    }

    <K extends RequestTransport<?>, R extends ResponseTransport> NetworkCommunicator.ContentPacketResponseListener<R> generateResponseListener(ContentPacket<K> packet, InetSocketAddress address, List<InetSocketAddress> copied, RandomDraw randomDraw, NetworkCommunicator.ContentPacketResponseListener<R> actualResponseListener, Runnable runnable) {
        return (response, failedResponse, serverIdCRC, theAddress) -> {
            if(response != null) {
                actualResponseListener.responseReceived(response, failedResponse,  serverIdCRC, theAddress);
                usingAddress = address;
                return;
            } else if(failedResponse != null) {
                LoggerEx.info(TAG, "sendRequestTransport packet " + packet + " failedResponse " + failedResponse);
            }
            int nextIndex = randomDraw.next();
            if(nextIndex != -1) {
                InetSocketAddress nextAddress = copied.get(nextIndex);
                if(nextAddress != null) {
                    runnable.run();
                }
            } else {

            }
//                    synchronized (discoveryAddresses) {
//                        retryState.set(state);
//                        discoveryAddresses.notify();
//                    }
        };
    }

    public <K extends RequestTransport<?>, R extends ResponseTransport> void sendRequestTransport(final NetworkCommunicator networkCommunicator, final ContentPacket<K> packet, final Class<R> responseClass, NetworkCommunicator.ContentPacketResponseListener<R> responseListener, long timeout) {
        if(discoveryAddresses.isEmpty())
            throw new IllegalStateException("No available discovery hosts");


        final List<InetSocketAddress> copied = new ArrayList<>(discoveryAddresses);
        if(usingAddress != null) {
            copied.remove(usingAddress);
        }
        final RandomDraw randomDraw = new RandomDraw(copied.size());
        InetSocketAddress address;
        do {
            int index = randomDraw.next();
            address = copied.get(index);
            if(address != null) {
                NetworkCommunicator.ContentPacketResponseListener<R> listener = new NetworkCommunicator.ContentPacketResponseListener<>() {
                    @Override
                    public void responseReceived(ContentPacket<R> response, ContentPacket<? extends FailedResponse> failedResponse, Long serverIdCRC, InetSocketAddress address) {
                        if (response != null) {
                            usingAddress = address;
                            responseListener.responseReceived(response, null, serverIdCRC, address);
                        } else {
                            InetSocketAddress nextAddress;
                            do {
                                int nextIndex = randomDraw.next();
                                if(nextIndex != -1) {
                                    nextAddress = copied.get(nextIndex);
                                    if(nextAddress != null) {
                                        networkCommunicator.sendRequestTransport(packet, nextAddress, responseClass, this, timeout);
                                    }
                                } else {
                                    responseListener.responseReceived(null, ContentPacket.buildWithContent(new FailedResponse(DiscoveryErrorCodes.ERROR_DISCOVERY_HOST_NOT_AVAILABLE)), -1L, address);
                                    break;
                                }
                            } while(nextAddress == null);
                        }
                    }
                };
                networkCommunicator.sendRequestTransport(packet, address, responseClass, listener, timeout);
            }
        } while(address == null);
    }

    public InetSocketAddress getUsingAddress() {
        return usingAddress;
    }
}
