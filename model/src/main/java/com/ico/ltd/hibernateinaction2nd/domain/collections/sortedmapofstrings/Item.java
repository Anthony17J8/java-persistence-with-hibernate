package com.ico.ltd.hibernateinaction2nd.domain.collections.sortedmapofstrings;

import com.ico.ltd.hibernateinaction2nd.domain.Constants;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

@Entity
public class Item {

    @Id
    @GeneratedValue(generator = Constants.ID_GENERATOR)
    protected Long id;

    @ElementCollection
    @CollectionTable(name = "IMAGE")
    @MapKeyColumn(name = "FILENAME")
    @Column(name = "IMAGENAME")
    @org.hibernate.annotations.SortComparator(ReverseStringComparator.class)
    protected SortedMap<String, String> images = new TreeMap<>();

    public SortedMap<String, String> getImages() {
        return images;
    }

    public void setImages(SortedMap<String, String> images) {
        this.images = images;
    }

    public static class ReverseStringComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            return o2.compareTo(o1);
        }
    }
}
