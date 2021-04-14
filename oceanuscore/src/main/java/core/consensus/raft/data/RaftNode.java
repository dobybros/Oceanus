package core.consensus.raft.data;

public class RaftNode {
    public static final int ROLE_FOLLOWER = 1;
    public static final int ROLE_CANDIDATE = 3;
    public static final int ROLE_LEADER = 5;
    private int role = ROLE_FOLLOWER;

    private String serverName;
    private Long createTime;
    private Long updateTime;

}