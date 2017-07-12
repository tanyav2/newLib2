package edu.illinois.mitra.cyphyhouse.interfaces;

import java.util.List;
import edu.illinois.mitra.cyphyhouse.objects.*;

public interface DSM extends Cancellable{
	void start();
	void stop();
	void reset();
	List<DSMVariable> getAll(String name, String owner);
	DSMVariable get_V(String name, String owner);
	String get(String name, String owner);
	String get(String name, String owner, String attr);
	boolean put(DSMVariable tuple);
	boolean putAll(List<DSMVariable> tuples);
	boolean put(String name, String owner, int value);
	boolean put(String name, String owner, String attr, int value);
	boolean put(String name, String owner, String ... attr_and_value);
	boolean createMW(String name, int value);
	boolean createMW(String name, String ... attr_and_value);
}