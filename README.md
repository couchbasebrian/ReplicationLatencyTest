# ReplicationLatencyTest

This program was developed using Java 1.8 and Couchbase Java SDK 2.1.3.

Given a source cluster and bucket and a destination cluster and bucket, this program will insert an expiring test document into the source cluster and bucket.

It will then start polling the target bucket.  It will measure the amount of time until the document is available on the target bucket.  Then as a follow-up, it will continue to poll until the document expires and is no longer available.

Finally it outputs both of these values and exits.
