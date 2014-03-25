package com.example.vaadinfilelist.singleton;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.WriteConcern;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class MongoConnector {

  private static MongoConnector myInstance = null;
  private DB db;
  private MongoClient mongoClient;
  private GridFS myFS;

  public static MongoConnector getConnector() {
    if (myInstance == null) {
      System.out.println("Erzeuge Singleton...");
      myInstance = new MongoConnector();
    }
    return myInstance;
  }

  private MongoConnector() {
    if (db == null) {
     db =  connectToMongo();
    }
  }

  private DB connectToMongo() {
    System.out.println("Connecting to mongo...");
    try {
      mongoClient = new MongoClient("localhost", 27017);
      mongoClient.setWriteConcern(WriteConcern.JOURNALED);
      db = mongoClient.getDB("vaadinDb");
      myFS = new GridFS(db, "uploads");
    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // or, to connect to a replica set, with auto-discovery of the primary, supply a seed list of
    // members
    return db;
  }

  public void closeConnection() {
    mongoClient.close();
  }

  public boolean firstTestInsert() {
    Set<String> colls = db.getCollectionNames();
    for (String currentCollection : colls) {
      System.out.println("Collections: " + currentCollection);
    }
    DBCollection coll = db.getCollection("testCollection");
    BasicDBObject doc = new BasicDBObject("name", "MongoDB").
        append("type", "database").
        append("count", 1).
        append("info", new BasicDBObject("x", 203).append("y", 102));

    coll.insert(doc);
    return true;
  }

  public boolean storeFile(File f) {
    System.out.println("store File " + f);
    try {
      GridFSInputFile createFile = myFS.createFile(f);
      createFile.setFilename(f.getName());
      BasicDBObject basicDBObject = new BasicDBObject("target_field", "mandant");
      createFile.setMetaData(basicDBObject);
      createFile.save();

    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public List<GridFSDBFile> findFiles() {
    BasicDBObject query = new BasicDBObject("metadata.target_field", "mandant");
    db.requestStart();
    List<GridFSDBFile> fileList = myFS.find(query);
    db.requestDone();
    System.out.println("Habe " + fileList.size() + " gefunden..");
    return fileList;
  }

}
