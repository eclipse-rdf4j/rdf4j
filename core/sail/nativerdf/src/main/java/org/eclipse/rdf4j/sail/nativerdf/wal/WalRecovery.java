package org.eclipse.rdf4j.sail.nativerdf.wal;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WalRecovery {

	public Map<Integer, WalRecord> replay(WalReader reader) throws IOException {
		WalReader.ScanResult scan = reader.scan();
		Map<Integer, WalRecord> dictionary = new LinkedHashMap<>();
		for (WalRecord record : scan.records()) {
			dictionary.putIfAbsent(record.id(), record);
		}
		return dictionary;
	}
}
