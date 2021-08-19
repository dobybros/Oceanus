package oceanus.sdk.core.discovery.utils;

import oceanus.sdk.logger.LoggerEx;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryDomainFormatUtils {
    private static final String TAG = DiscoveryDomainFormatUtils.class.getSimpleName();
    public static List<InetSocketAddress> parseAddresses(String discoveryDomainFormat) {
        boolean pass = false;
        if(discoveryDomainFormat != null) {
            final String startStr = "discovery_{";
            int pos = discoveryDomainFormat.indexOf(startStr);
            int portPos = discoveryDomainFormat.lastIndexOf(":");
            if(pos > 0 && portPos > 0) {
                int endPos = discoveryDomainFormat.indexOf("}", pos + startStr.length());
                if(endPos > 0) {
                    String numberRange = discoveryDomainFormat.substring(pos + startStr.length(), endPos);
                    String[] numberRangeStrings = numberRange.split("~");
                    if(numberRangeStrings.length == 2) {
                        String portStr = discoveryDomainFormat.substring(portPos + 1);
                        int min, max, port;
                        try {
                            min = Integer.parseInt(numberRangeStrings[0]);
                            max = Integer.parseInt(numberRangeStrings[1]);
                            port = Integer.parseInt(portStr);
                            List<InetSocketAddress> discoveryAddresses = new ArrayList<>();
                            if(max >= min && port > 0) {
                                String firstPart = discoveryDomainFormat.substring(0, pos + startStr.length() - 1);
                                String tailPart = discoveryDomainFormat.substring(endPos + 1, portPos);
                                for(int i = min; i <= max; i++) {
                                    InetSocketAddress inetSocketAddress = new InetSocketAddress(firstPart + i + tailPart, port);
                                    discoveryAddresses.add(inetSocketAddress);
                                    LoggerEx.info(TAG, "Found possible discovery address " + inetSocketAddress.getHostName() + ":" + inetSocketAddress.getPort());
                                }
                                LoggerEx.info(TAG, "DiscoveryDomainFormat is " + discoveryDomainFormat + " at port " + port);
//                                pass = true;
                                return discoveryAddresses;
                            }
                        } catch(Throwable ignored) { }
                    }
                }
            }
        }

        LoggerEx.error(TAG, "DiscoveryDomainFormat is illegal " + discoveryDomainFormat + ". Expect format like discovery_{0~2}.seastarnet.cn:36666, domain format must contains \"discovery_{[min]~[max]}\"");
        return null;
    }
}
