package com.docker.storage.adapters.impl;

import chat.errors.ChatErrorCodes;
import chat.errors.CoreException;
import com.docker.data.SDocker;
import com.docker.storage.DBException;
import com.docker.storage.adapters.SDockersService;
import com.docker.storage.mongodb.daos.SDockerDAO;
import com.docker.oceansbean.BeanFactory;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.bson.Document;

public class SDockersServiceImpl implements SDockersService {
    private SDockerDAO sDockerDAO;

    @Override
    public Document getSDockerConf(String ip, Integer port) throws CoreException {
        try {
            FindIterable<Document> iterable = sDockerDAO.query(new Document().append(SDocker.FIELD_SDOCKER_IP, ip).append(SDocker.FIELD_SDOCKER_PORT, port));
            MongoCursor<Document> cursor = iterable.iterator();
            if(cursor.hasNext())
                return cursor.next();
        } catch (DBException e) {
            e.printStackTrace();
            throw new CoreException(ChatErrorCodes.ERROR_SDOCKER_QUERY_FAILED, "Query SDocker config for " + ip + ":" + port  + " failed, " + e.getMessage());
        }
        return null;

    }

    public void setsDockerDAO(SDockerDAO sDockerDAO) {
        this.sDockerDAO = sDockerDAO;
    }
}
