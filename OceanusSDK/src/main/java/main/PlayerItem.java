package main;


/**
 * @author <zft>
 * @date 2021/5/19
 * @Description: <奖励（商品）道具信息>
 * @ClassName: BaseItem
 */
public class PlayerItem {
    /**
     * 道具id，包含货币
     */
    private String itemId;
//    private String category;
    /**
     * 总数量
     */
    private Long count;
    /**
     * 更新时间
     */
    private Long updateTime;
    
    public PlayerItem() {
    }

    public PlayerItem(String itemId, Long count) {
        this.itemId = itemId;
        this.count = count;
    }

    public PlayerItem(String itemId, Long count, Long updateTime) {
        this.itemId = itemId;
        this.count = count;
        this.updateTime = updateTime;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
