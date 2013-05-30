package helpers;

import java.util.List;

public interface BasicGraph {
	
	public List<?> getKeys();
	
	public List<?> getValues();
	
	public void sort();
	
	public void cutLongTail();
}