/*
 * Sortable.java
 *
 * Created on 20. April 2007, 14:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package rahmann.util;

/**
 *
 * @author Sven Rahmann
 */

public interface Sortable {
	int length();
	int compare(int i, int j);
	void swap(int i, int j);
}

