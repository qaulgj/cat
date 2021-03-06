package com.dianping.cat.storage.dump;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.GZIPInputStream;

class MessageBlockReader {
	private RandomAccessFile m_indexFile;

	private RandomAccessFile m_dataFile;

	public MessageBlockReader(File dataFile) throws IOException {
		File indexFile = new File(dataFile.getAbsolutePath() + ".idx");

		m_indexFile = new RandomAccessFile(indexFile, "r");
		m_dataFile = new RandomAccessFile(dataFile, "r");
	}

	public void close() throws IOException {
		synchronized (m_indexFile) {
			m_indexFile.close();
			m_dataFile.close();
		}
	}

	public byte[] readMessage(int index) throws IOException {
		int blockAddress;
		int blockOffset;
		byte[] buf;

		synchronized (m_indexFile) {
			m_indexFile.seek(index * 6);
			blockAddress = m_indexFile.readInt();
			blockOffset = m_indexFile.readShort() & 0xFFFF;
		}

		synchronized (m_dataFile) {
			m_dataFile.seek(blockAddress);
			buf = new byte[m_dataFile.readInt()];
			m_dataFile.readFully(buf);
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(buf);
		DataInputStream in = new DataInputStream(new GZIPInputStream(bais));

		try {
			in.skip(blockOffset);
			
			int len = in.readInt();
			
			byte[] data = new byte[len];

			in.readFully(data);
			return data;
		} finally {
			try {
				in.close();
			} catch (Exception e) {
				// ignore it
			}
		}
	}
}