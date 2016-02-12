# ReplicationLatencyTest

This program was developed using Java 1.8 and [Couchbase Java SDK 2.1.3](http://packages.couchbase.com/clients/java/2.1.3/Couchbase-Java-Client-2.1.3.zip).

Given a source cluster and bucket and a destination cluster and bucket, this program will insert an expiring test document into the source cluster and bucket.

It will then start polling the target bucket.  It will measure the amount of time until the document is available on the target bucket.  Then as a follow-up, it will continue to poll until the document expires and is no longer available.

After that it re-creates the original document in the source bucket, and does an upsert, and polls to see how long it takes for the change to appear in the target bucket ( by monitoring CAS value ).

Next, the program removes the document from the source bucket and observes how long it takes to be removed from the target bucket.

Finally it outputs all of the measured time values and exits.

Sample output ( from continuous mode )

    === Ready to perform tests - document id is testDocument997 ===
    === Finished inserting document into source bucket default ===
    === Waiting for doc to appear in target bucket default... ===
    === Number of get iterations required: 6 ===
    === Waiting for doc to disappear from target bucket... (expecting approximately 10 seconds based on TTL) ===
    === Iterations required: 30601 ===
    === Doc has disappeared from target bucket.  Inserting document into source bucket again ( assuming it has expired ) ===
    === Iterations required: 0 ===
    === Reinsert into source bucket successful (took 10 ms).  Waiting for doc to appear in target bucket again... ===
    === Iterations required: 0 ===
    === The document appeared in the target bucket after being reinserted into the source bucket. ===
    === Now doing an upsert on the document in the source bucket. ===
    === Finished upserting document with key: testDocument997 CAS: 0 timeTaken: 2 ms ===
    === Retrieved the upserted doc from the source bucket and the CAS is: 45121999447122 ===
    === Waiting for doc to change in target bucket... ===
    xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    === Iterations required: 31 ===
    === Detected the CAS change on the target bucket.  Doing a remove on the source bucket and polling target bucket... ===
    === Iterations required: 14 ===
    === The document has disappeared from the target bucket. ===
    === Final Results: 2016-02-12T12:16:15.161 ===
    Time taken to appear in target bucket:                       14 ms
    Time taken to expire and disappear from target bucket:       10526 ms after appearing in target
    Time taken to reappear in target after recreating in source: 0 ms
    Time for upsert to propagate to target bucket:               11 ms
    Time taken for remove to take effect on target bucket:       6 ms
    CB Version of Source:     3.1.0-1776-rel-enterprise
    CB Version of Target:     3.1.0-1776-rel-enterprise
