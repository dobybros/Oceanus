package com.docker.data;


import script.memodb.ObjectId;

public abstract class DataObject {
	public static final String FIELD_ID = "_id";
	protected String id;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isIdGenerated(){
		if(id == null){
			return false;
		}
		return true;
	}
	
	public void generateId(){
//		CRC32 crc = new CRC32();
//		crc.update(UUID.randomUUID().toString().getBytes());
//		id = colName + "_" + crc.getValue();
//		id = colName + "_" + UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
		if(id == null)
			id = ObjectId.get().toString();
	}

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataObject other = (DataObject) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }
}
