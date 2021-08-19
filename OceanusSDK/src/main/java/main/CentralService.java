package main;

import java.util.List;

public interface CentralService {
    String hello();
    String error();
    List<PlayerItem> getPlayerItems(PlayerItem playerItem, List<PlayerItem> pis);
}
