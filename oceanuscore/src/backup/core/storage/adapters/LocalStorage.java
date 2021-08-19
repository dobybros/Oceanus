package core.storage.adapters;

import core.common.InternalTools;
import core.storage.adapters.structure.*;

import java.io.IOException;

public abstract class LocalStorage {
    protected String name;
    protected String path;
    protected InternalTools internalTools;
    public abstract KVOperation getKVOperation() throws IOException;
    public abstract HashOperation getHashOperation() throws IOException;
    public abstract ListOperation getListOperation() throws IOException;
    public abstract SetOperation getSetOperation() throws IOException;
    public abstract ZSetOperation getZSetOperation() throws IOException;
}
