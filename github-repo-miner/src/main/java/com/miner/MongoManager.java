package com.miner;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoManager {

    private static final String MONGO_HOST = System.getenv().getOrDefault("MONGO_HOST", "localhost");
    private static final String MONGO_PORT = System.getenv().getOrDefault("MONGO_PORT", "27017");
    private static final String MONGO_DB   = System.getenv().getOrDefault("MONGO_DB", "shared_catalog");
    private static final String MONGO_USER = System.getenv().getOrDefault("MONGO_USER", "catalog_user");
    private static final String MONGO_PASS = System.getenv().getOrDefault("MONGO_PASS", "catalog_pass");

    private static final String COLLECTION_NAME = "repo_catalog";

    private static MongoClient client;

    public static MongoClient getClient() {
        if (client == null) {
            String uri = String.format(
                "mongodb://%s:%s@%s:%s/%s?authSource=admin",
                MONGO_USER, MONGO_PASS, MONGO_HOST, MONGO_PORT, MONGO_DB
            );
            client = MongoClients.create(uri);
        }
        return client;
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