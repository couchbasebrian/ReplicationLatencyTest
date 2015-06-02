# ReplicationLatencyTest

This program was developed using Java 1.8 and Couchbase Java SDK 2.1.3.

Given a source cluster and bucket and a destination cluster and bucket, this program will insert an expiring test document into the source cluster and bucket.

It will then start polling the target bucket.  It will measure the amount of time until the document is available on the target bucket.  Then as a follow-up, it will continue to poll until the document expires and is no longer available.

Finally it outputs both of these values and exits.

Sample output

    === Replication Latency Test ===
    === Finished opening source clusters and buckets ===
    === Finished opening target clusters and buckets ===
    === Finished inserting document ===
    === Waiting for doc to appear in target bucket... ===
    === Waiting for doc to disappear from target bucket... (expecting about 3 seconds) ===
    === Waiting for doc to appear in target bucket again... ===
    === Finished upserting document with key: testDocument2 cas: 0 ===
    === Retrieved the upserted doc and the cas is: 65687996628451 ===
    === Waiting for doc to change in target bucket... ===
    xxxxxxxxxxxxxxxxxxxxx
    === Final Results ===
    Time taken to appear in target bucket:                       11 ms
    Time taken to expire and disappear from target bucket:       10797 ms
    Time taken to reappear in target after recreating in source: 9 ms
    Time for upsert to propagate to target bucket:               12 ms
    Time taken for remove to take effect on target bucket:       5 ms
    === Goodbye. ===
