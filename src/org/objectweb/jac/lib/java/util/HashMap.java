/**
 * This class delegates to java.util.HashMap
 * This file was automatically generated by JAC (-g option)
 * DO NOT MODIFY
 * Author: Renaud Pawlak (pawlak@cnam.fr)
 */

package org.objectweb.jac.lib.java.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.objectweb.jac.util.Strings;

public class HashMap extends java.util.HashMap {

    //private java.util.HashMap delegate = new java.util.HashMap();

    public Object clone() {
        System.out.println("HashMap.clone");
        Object result = null;
        try { 
            result = super.clone(); 
        } catch(Exception e) {
        };
        //((HashMap)result).delegate = (java.util.HashMap)delegate.clone();
        return result;
    }

    public Object put(Object p0, Object p1) {
        return super.put(p0, p1);
    }

    public Object get(Object p0) {
        return super.get(p0);
    }

    public int size() {
        return super.size();
    }

    public Object remove(Object p0) {
        return super.remove(p0);
    }

    public Collection values() {
        return super.values();
    }

    public void clear() {
        super.clear();
    }

    public Set keySet() {
        return super.keySet();
    }

    public Set entrySet() {
        return super.entrySet();
    }

    public boolean isEmpty() {
        return super.isEmpty();
    }

    public boolean containsValue(Object p0) {
        return super.containsValue(p0);
    }

    public boolean containsKey(Object p0) {
        return super.containsKey(p0);
    }

    public void putAll(Map p0) {
        super.putAll(p0);
    }
    
    public boolean equals(Object o) {
        return this == o;
    }

    public int hashCode() {
        return System.identityHashCode(this);
    }

    public String toString() {
        return getClass().getName()+"@"+System.identityHashCode(this);
    }
}
