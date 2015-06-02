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
		int timeToLive = 3; // seconds for document to live
		JsonDocument jsonDocument = JsonDocument.create(documentKey, timeToLive, jsonObject);
		long timeOut = 1000; // 1 second timeout value for the insert operation only 
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		sourceBucket.insert(jsonDocument, timeOut, timeUnit);
		
		logMessage("Finished inserting document");
		
		long t1 = 0, t2 = 0, timeToAppear = 0, timeToAppear2 = 0, timeToDisappear = 0, 
				timeForCasChange = 0, timeToBeRemoved = 0;
		
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
		
		logMessage("Waiting for doc to disappear from target bucket... (expecting about " + timeToLive + " seconds)");

		// and disappears
		t1 = System.currentTimeMillis();		
		docFound = true;
		while (docFound == true) {
			foundDocument = targetBucket.get(documentKey);			
			if (foundDocument == null) { docFound = false; }
		}
		t2 = System.currentTimeMillis();
		timeToDisappear = t2 - t1;
	
		// Re-create the document again in the source bucket
		sourceBucket.insert(jsonDocument, timeOut, timeUnit);

		logMessage("Waiting for doc to appear in target bucket again...");
		
		t1 = System.currentTimeMillis();		
		// Poll the target cluster until the doc re-appears in the target bucket. 
		docFound = false;
		while (docFound == false) {
			foundDocument = targetBucket.get(documentKey);
			if (foundDocument != null) { docFound = true; }
		}
		t2 = System.currentTimeMillis();
		timeToAppear2 = t2 - t1;

		// Change the doc with an upsert to the source bucket
		// and see how long it takes for that to propagate to target bucket

		String newJsonDocumentString = "{ \"name\" : \"bestDocument\" }";
		JsonObject newJsonObject = JsonObject.fromJson(newJsonDocumentString);
		JsonDocument newJsonDocument = JsonDocument.create(documentKey, timeToLive, newJsonObject); // same key and TTL

		sourceBucket.upsert(newJsonDocument, timeOut, timeUnit);
		logMessage("Finished upserting document with key: " + documentKey + " cas: " + newJsonDocument.cas());

		JsonDocument retrievedChangedDocument = sourceBucket.get(documentKey);	
		long retrievedDocumentCas = retrievedChangedDocument.cas();
		logMessage("Retrieved the upserted doc and the cas is: " + retrievedDocumentCas);

		logMessage("Waiting for doc to change in target bucket...");

		t1 = System.currentTimeMillis();		
		// Poll the target cluster until the cas changes 
		boolean casChanged = false;
		while (casChanged == false) {
			foundDocument = targetBucket.get(documentKey);
			if (foundDocument == null) {
				System.out.print(".");
			}
			else {
				if (foundDocument.cas() == retrievedDocumentCas) { 
					casChanged = true; 
				}
				else {
					System.out.print("x");
				}
			}
		}
		t2 = System.currentTimeMillis();
		timeForCasChange = t2 - t1;
		System.out.println();
		// System.out.println("Retrieved upserted doc from target cluster: " + foundDocument.toString());

		// Remove the document from the source bucket and see how long it takes to disappear
		// from target bucket
		sourceBucket.remove(documentKey);
		
		t1 = System.currentTimeMillis();		
		docFound = true;
		while (docFound == true) {
			foundDocument = targetBucket.get(documentKey);			
			if (foundDocument == null) { docFound = false; }
		}
		t2 = System.currentTimeMillis();
		timeToBeRemoved = t2 - t1;
		
		// Display final results
		logMessage("Final Results");
		
		System.out.println("Time taken to appear in target bucket:                       " + timeToAppear     + " ms");
		System.out.println("Time taken to expire and disappear from target bucket:       " + timeToDisappear  + " ms");
		System.out.println("Time taken to reappear in target after recreating in source: " + timeToAppear2    + " ms");		
		System.out.println("Time for upsert to propagate to target bucket:               " + timeForCasChange + " ms");
		System.out.println("Time taken for remove to take effect on target bucket:       " + timeToBeRemoved  + " ms");
		
		} catch (Exception e) {
			System.out.println("Exception occurred: " + e);
			e.printStackTrace();
		}
		
		logMessage("Goodbye.");
	}

	static void logMessage(String s) {
		System.out.println("=== " + s + " ===");
	}
	
}
