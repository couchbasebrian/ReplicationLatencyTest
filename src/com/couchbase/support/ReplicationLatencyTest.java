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
		
		String sourceClusterAddress = "10.4.2.121", sourceBucketName = "default",
		       targetClusterAddress = "10.4.2.123", targetBucketName = "default";
				
		// Connect to both clusters
		CouchbaseCluster sourceCluster = CouchbaseCluster.create(sourceClusterAddress);
		Bucket sourceBucket = sourceCluster.openBucket(sourceBucketName);

		logMessage("Finished opening source clusters and buckets");

		CouchbaseCluster targetCluster = CouchbaseCluster.create(targetClusterAddress);
		Bucket targetBucket = targetCluster.openBucket(targetBucketName);
		
		logMessage("Finished opening target clusters and buckets");
		
		// Create an expiring document on the source cluster
		int randomIdentifier = (int) (Math.random() * 1000);
		String documentKey = "testDocument" + randomIdentifier;

		long iterationsRequired = 0;
		
		logMessage("Ready to perform tests - document id is " + documentKey);
		
		String jsonDocumentString = "{ \"name\" : \"testDocument\" }";
		JsonObject jsonObject = JsonObject.fromJson(jsonDocumentString);
		int timeToLive = 3; // seconds for document to live
		JsonDocument jsonDocument = JsonDocument.create(documentKey, timeToLive, jsonObject);
		long timeOut = 1000; // 1 second timeout value for the insert operation only 
		TimeUnit timeUnit = TimeUnit.MILLISECONDS;
		sourceBucket.insert(jsonDocument, timeOut, timeUnit);

		long timeWhenDocumentFirstInsertedIntoSourceBucket = System.currentTimeMillis();		

		logMessage("Finished inserting document into source bucket");
		
		long t1 = 0, t2 = 0, timeToAppear = 0, timeToAppear2 = 0, timeToDisappear = 0, 
				timeForCasChange = 0, timeToBeRemoved = 0;
		
		JsonDocument foundDocument = null;
		
		logMessage("Waiting for doc to appear in target bucket...");
		
		iterationsRequired = 0;
		t1 = System.currentTimeMillis();		
		// Poll the target cluster until the doc appears 
		boolean docFound = false;
		while (docFound == false) {
			foundDocument = targetBucket.get(documentKey);
			if (foundDocument != null) { 
				docFound = true; 
			}
			else {
				iterationsRequired++;				
			}
		}
		t2 = System.currentTimeMillis();
		timeToAppear = t2 - t1;
		logMessage("Iterations required: " + iterationsRequired);
		
		logMessage("Waiting for doc to disappear from target bucket... (expecting about " + timeToLive + " seconds)");

		// and disappears
		iterationsRequired = 0;
		t1 = System.currentTimeMillis();		
		docFound = true;
		while (docFound == true) {
			foundDocument = targetBucket.get(documentKey);			
			if (foundDocument == null) { 
				docFound = false; 
			} else {
				iterationsRequired++;
			}
		}
		t2 = System.currentTimeMillis();
		timeToDisappear = t2 - t1;
		logMessage("Iterations required: " + iterationsRequired);
		
		logMessage("Doc has disappeared from target bucket.  Inserting document into source bucket again ( assuming it has expired )");
		
		boolean exceptionUponReinsert = false;
		boolean successfulReinsert = false;
		iterationsRequired = 0;
		t1 = System.currentTimeMillis();
		while (successfulReinsert == false) {
			try {
				// Re-create the document again in the source bucket
				sourceBucket.insert(jsonDocument, timeOut, timeUnit);
				successfulReinsert = true;
			}
			catch (Exception e) {
				iterationsRequired++;		
				exceptionUponReinsert = true;
			}
		}
		t2 = System.currentTimeMillis();
		long timeToReinsert = t2 - t1;
		logMessage("Iterations required: " + iterationsRequired);

		if (exceptionUponReinsert) {
			logMessage("There were exceptions at reinsert.  The total life time in the source bucket was " + 
						(t2 - timeWhenDocumentFirstInsertedIntoSourceBucket) 
						+ " ms.");
		}
		
		logMessage("Reinsert into source bucket successful (took " + timeToReinsert + " ms).  Waiting for doc to appear in target bucket again...");
		
		iterationsRequired = 0;
		t1 = System.currentTimeMillis();		
		// Poll the target cluster until the doc re-appears in the target bucket. 
		docFound = false;
		while (docFound == false) {
			foundDocument = targetBucket.get(documentKey);
			if (foundDocument != null) { docFound = true; } else { iterationsRequired++; }
		}
		t2 = System.currentTimeMillis();
		timeToAppear2 = t2 - t1;
		logMessage("Iterations required: " + iterationsRequired);

		logMessage("The document appeared in the target bucket after being reinserted into the source bucket.");
		logMessage("Now doing an upsert on the document in the source bucket.");
		
		// Change the doc with an upsert to the source bucket
		// and see how long it takes for that to propagate to target bucket

		String newJsonDocumentString = "{ \"name\" : \"bestDocument\" }";
		JsonObject newJsonObject = JsonObject.fromJson(newJsonDocumentString);
		JsonDocument newJsonDocument = JsonDocument.create(documentKey, timeToLive, newJsonObject); // same key and TTL

		t1 = System.currentTimeMillis();		
		sourceBucket.upsert(newJsonDocument, timeOut, timeUnit);
		t2 = System.currentTimeMillis();		
		logMessage("Finished upserting document with key: " + documentKey + 
				" CAS: " + newJsonDocument.cas() + 
				" timeTaken: " + (t2 - t1) + " ms");

		JsonDocument retrievedChangedDocument = sourceBucket.get(documentKey);	
		long retrievedDocumentCas = retrievedChangedDocument.cas();
		logMessage("Retrieved the upserted doc from the source bucket and the CAS is: " + retrievedDocumentCas);

		logMessage("Waiting for doc to change in target bucket...");

		iterationsRequired = 0;
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
			iterationsRequired++;
		}
		t2 = System.currentTimeMillis();
		timeForCasChange = t2 - t1;
		System.out.println();
		logMessage("Iterations required: " + iterationsRequired);
		
		logMessage("Detected the CAS change on the target bucket.  Doing a remove on the source bucket and polling target bucket...");
		
		// Remove the document from the source bucket and see how long it takes to disappear
		// from target bucket
		sourceBucket.remove(documentKey);
		
		iterationsRequired = 0;
		t1 = System.currentTimeMillis();		
		docFound = true;
		while (docFound == true) {
			foundDocument = targetBucket.get(documentKey);			
			if (foundDocument == null) { docFound = false; } else { iterationsRequired++; }
		}
		t2 = System.currentTimeMillis();
		timeToBeRemoved = t2 - t1;
		logMessage("Iterations required: " + iterationsRequired);
		
		logMessage("The document has disappeared from the target bucket.");
		
		// Display final results
		logMessage("Final Results");
		
		System.out.println("Time taken to appear in target bucket:                       " + timeToAppear     + " ms");
		System.out.println("Time taken to expire and disappear from target bucket:       " + timeToDisappear  + " ms after appearing in target");
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
