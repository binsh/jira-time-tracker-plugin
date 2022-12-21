package com.kolhozcustoms.jira.plugin.timetracker.utils;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class FindUtil {
    public static <T> T findByProperty(Collection<T> col, Predicate<T> filter) {
        return col.stream().filter(filter).findFirst().orElse(null);
    }

    public static <T> List<T> findListByFilter(Collection<T> col, Predicate<T> filter) {
        return col.stream().filter(filter).collect(Collectors.toList());
    } 
}