package ls;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class FileCache {
	
	private static final Map<File,CachedFile> FILES = new TreeMap<>();
	private static final long MAX_CACHE_SIZE = 1L << 31;
	private static final long MAX_AGE_S = 12 * 60 * 60;
	
	private static long cacheMax;
	
	public static void init() {
		LogSearchUtil.EX.scheduleAtFixedRate(() -> check(), 1, 1, TimeUnit.MINUTES);
		long propMax = Long.parseLong(System.getProperty("ls.maxcache", "0"));
		if (propMax > 0) {
			cacheMax = propMax;
		} else {
			long rtMax = (3 * Runtime.getRuntime().maxMemory()) / 2;
			cacheMax = Math.min(MAX_CACHE_SIZE, rtMax);
		}
		System.out.println("cache max = " + cacheMax);
	}
	
	public static long max() {
		return cacheMax;
	}

	/**
	 * get sum of cached data arrays
	 */
	private static long sum() {
		synchronized (FILES) {
			long v = 0;
			for (CachedFile f : FILES.values()) {
				if (f.data != null) {
					v += f.data.length;
				}
			}
			return v;
		}
	}
	
	/**
	 * return true if cache size less than max
	 */
	public static boolean sumOk () {
		return sum() < cacheMax;
	}
	
	public static CachedFile get (File f) {
		synchronized (FILES) {
			CachedFile cf = FILES.get(f);
			if (cf != null) {
				cf.accessedNs = System.nanoTime();
			}
			return cf;
		}
	}
	
	public static CachedFile put (File f, CachedFile cf) {
		synchronized (FILES) {
			cf.accessedNs = System.nanoTime();
			FILES.put(f, cf);
			return cf;
		}
	}
	
	/**
	 * remove expired files from cache
	 */
	private static void check () {
		long t = System.nanoTime() - (LogSearchUtil.NS_IN_S * MAX_AGE_S);
		synchronized (FILES) {
			Iterator<Entry<File,CachedFile>> i = FILES.entrySet().iterator();
			while (i.hasNext()) {
				Entry<File, CachedFile> e = i.next();
				File k = e.getKey();
				CachedFile v = e.getValue();
				if (v.accessedNs < t) {
					System.out.println("expire " + k + " = " + v);
					i.remove();
				}
			}
			int size = FILES.size();
			if (size > 0) {
				System.out.println("cache size: " + size + " sum: " + LogSearchUtil.formatSize(sum()));
			}
		}
		System.gc();
	}

	private FileCache() {
		//
	}
	
}
