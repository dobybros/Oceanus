package connectors.mongodb;

import com.mongodb.client.MongoCollection;
import connectors.mongodb.annotations.handlers.MongoDBHandler;
import connectors.mongodb.annotations.handlers.MongoDBHandler.CollectionHolder;
import connectors.mongodb.codec.DataObject;
import org.bson.Document;
import script.core.runtime.AbstractRuntimeContext;
import script.core.runtime.groovy.GroovyRuntime;
import script.core.runtime.classloader.MyGroovyClassLoader;

import java.util.HashMap;

public class MongoCollectionHelper {
	protected MongoCollection<DataObject> getMongoCollection() {
		ClassLoader classLoader = this.getClass().getClassLoader().getParent();
		if(classLoader != null && classLoader instanceof MyGroovyClassLoader) {
			MyGroovyClassLoader myGroovyClassLoader = (MyGroovyClassLoader) classLoader;
			AbstractRuntimeContext runtimeContext = myGroovyClassLoader.getRuntimeContext();
			if(runtimeContext != null) {
				MongoDBHandler handler = (MongoDBHandler) runtimeContext.getClassAnnotationHandler(MongoDBHandler.class);
				if(handler != null) {
					HashMap<Class<?>, CollectionHolder> map = handler.getCollectionMap();
					if(map != null) {
						CollectionHolder holder = map.get(this.getClass());
						if(holder != null)
							return holder.getCollection();
					}
				}
			}
		}

		return null;
	};

	protected MongoCollection<Document> getDocumentCollection() {
		ClassLoader classLoader = this.getClass().getClassLoader().getParent();
		if(classLoader != null && classLoader instanceof MyGroovyClassLoader) {
			MyGroovyClassLoader myGroovyClassLoader = (MyGroovyClassLoader) classLoader;
			AbstractRuntimeContext runtimeContext = myGroovyClassLoader.getRuntimeContext();
			if(runtimeContext != null) {
				MongoDBHandler handler = (MongoDBHandler) runtimeContext.getClassAnnotationHandler(MongoDBHandler.class);
				if(handler != null) {
					HashMap<Class<?>, CollectionHolder> map = handler.getCollectionMap();
					if(map != null) {
						CollectionHolder holder = map.get(this.getClass());
						if(holder != null)
							return holder.getDocumentCollection();
					}
				}
			}
		}

		return null;
	};
}
