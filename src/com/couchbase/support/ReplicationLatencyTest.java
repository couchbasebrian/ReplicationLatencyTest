// May 28, 2015
// Replication Latency Test
// Brian Williams ( brian.williams@couchbase.com )

package com.couchbase.support;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;

public class ReplicationLatencyTest {

	public static void main(String[] args) {

		logMessage("Replication Latency Test");
		
		try {
			
		// Define two clusters, a source and a target ( local and remote )
		
		String sourceClusterAddress = "192.168.0.101", sourceBucketName = "beer-sample",
		       targetClusterAddress = "192.168.0.102", targetBucketName = "beer-sample";
				
		// Connect to both clusters
		CouchbaseCluster sourceCluster = CouchbaseCluster.create(sourceClusterAddress);
		Bucket sourceBucket = sourceCluster.openBucket(sourceBucketName);

		logMessage("Finished opening source clusters and buckets");

		CouchbaseCluster targetCluster = CouchbaseCluster.create(targetClusterAddress);
		Bucket targetBucket = targetCluster.openBucket(targetBucketName);
		
		logMessage("Finished opening target clusters and buckets");
		
		// Create an expiring document on the source cluster
		String documentKey = "testDocument2";
		String jsonDocumentString = "{ \"name\" : \"testDocument\" }";
		JsonObject jsonObject = JsonObject.fromJson(jsonDocumentString);
		int timeToLive = 3; // 3 seconds for document to live
		JsonDocument jsonDocument = JsonDocument.create(documentKey, timeToLive, jsonObject);
		long timeOut = 1000; // 1 second timeout value for the insert operation only 
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		sourceBucket.insert(jsonDocument, timeOut, timeUnit);
		
		logMessage("Finished inserting document");
		
		long t1 = 0, t2 = 0, timeToAppear = 0, timeToDisappear = 0;
		
		JsonDocument foundDocument = null;
		
		logMessage("Waiting for doc to appear in target bucket...");
		
		t1 = System.currentTimeMillis();		
		// Poll the target cluster until the doc appears 
		boolean docFound = false;
		while (docFound == false) {
			foundDocument = targetBucket.get(documentKey);
			if (foundDocument != null) { docFound = true; }
		}
		t2 = System.currentTimeMillis();
		timeToAppear = t2 - t1;

		logMessage("Waiting for doc to disappear from target bucket...");

		// and disappears
		t1 = System.currentTimeMillis();		
		docFound = true;
		while (docFound == true) {
			foundDocument = targetBucket.get(documentKey);			
			if (foundDocument == null) { docFound = false; }
		}
		t2 = System.currentTimeMillis();
		timeToDisappear = t2 - t1;
		
		// Determine how much time passed
		
		System.out.println("Time taken to appear:     " + timeToAppear + " ms");
		System.out.println("Time taken to disappear:  " + timeToDisappear + " ms");
		
		} catch (Exception e) {
			System.out.println("Exception occurred: " + e);
		}
		
		logMessage("Goodbye.");
	}

	static void logMessage(String s) {
		System.out.println("=== " + s + " ===");
	}
	
}
