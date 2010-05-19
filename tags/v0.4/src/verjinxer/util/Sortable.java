/*
 * Sortable.java
 *
 * Created on 20. April 2007, 14:24
 *
 */

package verjinxer.util;

/**
 * a simple interface for sortable classes: we need to determine the length 
 * of the container to be sorted, we need to be compare the objects at any positions i and j
 * in the container, an we need to be able to swap such objects.
 * @author Sven Rahmann
 */

public interface Sortable {
	int length();
	int compare(int i, int j);
	void swap(int i, int j);
}

