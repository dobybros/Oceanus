package connectors.mongodb;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import connectors.mongodb.annotations.handlers.MongoDBHandler;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.GroovyRuntime;
import script.core.runtime.classloader.MyGroovyClassLoader;

import java.util.HashMap;


public class MongoDatabaseHelper {
	protected ClientSession startSession() {
		return startSession(null);
	}

	protected ClientSession startSession(ClientSessionOptions option) {
		ClassLoader classLoader = this.getClass().getClassLoader().getParent();
		if(classLoader != null && classLoader instanceof MyGroovyClassLoader) {
			MyGroovyClassLoader myGroovyClassLoader = (MyGroovyClassLoader) classLoader;
			AbstractRuntimeContext runtimeContext = myGroovyClassLoader.getRuntimeContext();
			if (runtimeContext != null) {
				MongoDBHandler handler = (MongoDBHandler) runtimeContext.getClassAnnotationHandler(MongoDBHandler.class);
				MongoClientHelper helper = handler.getMongoClientHelper();
				if(handler != null && helper != null) {
					HashMap<Class<?>, MongoDatabase> map = handler.getDatabaseMap();
					if(map != null) {
						return helper.startSession(option != null ? option : ClientSessionOptions.builder().build());
					}
				}
			}
		}
		return null;
	}

	protected MongoDatabase getMongoDatabase() {
		ClassLoader classLoader = this.getClass().getClassLoader().getParent();
		if(classLoader != null && classLoader instanceof MyGroovyClassLoader) {
			MyGroovyClassLoader myGroovyClassLoader = (MyGroovyClassLoader) classLoader;
			AbstractRuntimeContext runtimeContext = myGroovyClassLoader.getRuntimeContext();
			if (runtimeContext != null) {
				MongoDBHandler handler = (MongoDBHandler) runtimeContext.getClassAnnotationHandler(MongoDBHandler.class);
				if(handler != null) {
					HashMap<Class<?>, MongoDatabase> map = handler.getDatabaseMap();
					if(map != null) {
						MongoDatabase database = map.get(this.getClass());
						return database;
					}
				}
			}
		}
		return null;
	}
}
