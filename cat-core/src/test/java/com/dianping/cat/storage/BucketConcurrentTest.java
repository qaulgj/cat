package com.dianping.cat.storage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.unidal.lookup.ComponentTestCase;

@RunWith(JUnit4.class)
@Ignore
public class BucketConcurrentTest extends ComponentTestCase {
	@BeforeClass
	public static void beforeClass() {
		new File("target/bucket/concurrent/bytes").delete();
		new File("target/bucket/concurrent/message").delete();
		new File("target/bucket/concurrent/data").delete();
	}

	@Test
	public void testStringBucket() throws Exception {
		long timestamp = System.currentTimeMillis();
		BucketManager manager = lookup(BucketManager.class);
		final Bucket<String> bucket = manager.getReportBucket(timestamp, "concurrent/data");
		ExecutorService pool = Executors.newFixedThreadPool(10);

		for (int p = 0; p < 10; p++) {
			final int num = p;

			pool.submit(new Runnable() {
				public void run() {
					try {
						for (int i = 0; i < 100; i++) {
							int seq = num * 100 + i;
							String id = "id" + seq;
							String t1 = "value" + seq;
							boolean success = bucket.storeById(id, t1);

							if (!success) {
								Assert.fail("Data failed to store at " + seq + ".");
							}
						}
					} catch (IOException e) {
						Assert.fail(e.getMessage());
					}
				}
			});
		}

		pool.awaitTermination(5000, TimeUnit.MILLISECONDS);

		final Bucket<String> bucket2 = manager.getReportBucket(timestamp, "concurrent/data");

		for (int p = 0; p < 10; p++) {
			final int num = p;

			pool.submit(new Runnable() {
				public void run() {
					try {
						for (int i = 0; i < 100; i++) {
							int seq = num * 100 + i;
							String id = "id" + seq;
							String t1 = "value" + seq;
							String t2 = bucket2.findById(id);

							Assert.assertEquals("Unable to find data after stored it.", t1, t2);
						}
					} catch (IOException e) {
						Assert.fail(e.getMessage());
					}
				}
			});
		}

		pool.awaitTermination(5000, TimeUnit.MILLISECONDS);

		// store it and load it
		for (int i = 0; i < 1000; i++) {
			String id = "id" + i;
			String t1 = "value" + i;
			String t2 = bucket.findById(id);

			Assert.assertEquals("Unable to find data after stored it.", t1, t2);
		}
	}
}
