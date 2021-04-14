package core.consensus.model;

import core.net.ContentPacketListener;
import core.net.adapters.data.ContentPacket;
import core.net.data.RequestTransport;
import core.net.data.ResponseTransport;

import java.util.concurrent.CompletableFuture;

public interface NodeCommunicationListener {
    CompletableFuture<ContentPacket<? extends ResponseTransport>> sendContentPacket(ContentPacket<? extends RequestTransport> contentPacket, long serverIdCRC, long timeout);

    <T extends RequestTransport> void addContentPacketListener(Class<T> clazz, ContentPacketListener<T> packetListener);
}
