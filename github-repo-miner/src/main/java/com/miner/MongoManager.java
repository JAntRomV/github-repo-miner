package com.miner;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoManager {

  
    private static final String MONGO_URI_ENV = System.getenv("MONGO_URI");

    private static final String MONGO_DB = System.getenv().getOrDefault("MONGO_DB", "shared_catalog");
    private static final String COLLECTION_NAME = "repo_catalog";

    private static MongoClient client;

    public static MongoClient getClient() {
        if (client == null) {
            String uri = (MONGO_URI_ENV != null && !MONGO_URI_ENV.isEmpty())
                ? MONGO_URI_ENV
                : buildLocalUri();
            client = MongoClients.create(uri);
        }
        return client;
    }

    private static String buildLocalUri() {
        String host = System.getenv().getOrDefault("MONGO_HOST", "localhost");
        String port = System.getenv().getOrDefault("MONGO_PORT", "27017");
        String user = System.getenv().getOrDefault("MONGO_USER", "catalog_user");
        String pass = System.getenv().getOrDefault("MONGO_PASS", "catalog_pass");
        return String.format("mongodb://%s:%s@%s:%s/%s?authSource=admin", user, pass, host, port, MONGO_DB);
    }

    public static MongoDatabase getDatabase() {
        return getClient().getDatabase(MONGO_DB);
    }

    public static MongoCollection<Document> getCatalogCollection() {
        return getDatabase().getCollection(COLLECTION_NAME);
    }

    public static void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}