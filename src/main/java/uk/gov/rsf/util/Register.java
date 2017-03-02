package uk.gov.rsf.util;

import uk.gov.rsf.indexer.*;
import uk.gov.rsf.indexer.function.IndexFunction;
import uk.gov.rsf.indexer.function.RecordIndexFunction;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Register {
    private final Map<HashValue, Item> items;
    private final Map<Integer, Entry> entries;

    private final IndexDriver indexDriver;
    private final List<IndexFunction> indexFunctions;

    public Register() {
        items = new HashMap<>();
        entries = new HashMap<>();
        indexDriver = new IndexDriver(this);
        this.indexFunctions = new ArrayList<>();
    }

    public void registerIndex(IndexFunction indexFunction) {
        indexFunctions.add(indexFunction);
    }

    public void putItem(Item item) {
        items.put(item.getSha256hex(), item);
    }

    public void appendEntry(Entry entry) {
        entries.put(entry.getEntryNumber(), entry);
        indexFunctions.forEach(func -> indexDriver.indexEntry(entry, func));
        indexDriver.indexEntry(entry, new RecordIndexFunction());
    }

    public Item getItem(HashValue hashValue) {
        return items.get(hashValue);
    }

    public int getLatestEntryNumber() {
        return entries.keySet().stream().max((i,j) -> Integer.compare(i,j)).orElse(0);
    }

    public Optional<Entry> getRecord(String key) {
        List<Entry> records = getRsfEntries("record", Optional.of(key), Optional.empty());
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(records.size() - 1));
    }

    public Optional<Entry> getRecordForIndex(String indexName, String indexValue) {
//        indexDriver.getIndex().get
        return null;
    }

    public Map<String, List<HashValue>> getRecordsForIndex(String indexName, Optional<String> indexValue, Optional<Integer> registerVersion) {
        return indexDriver.getIndex().getCurrentItemsForIndex(indexName, indexValue, registerVersion);
    }

//    public List<Entry> getRecordsForIndex(String indexName, Optional<String> indexValue) {
//        return indexDriver.getIndex().getCurrentItemsForIndex(indexName, indexValue);
//    }

    public List<Entry> getRsfEntries(String indexName, Optional<String> indexValue, Optional<Integer> registerVersion) {
        AtomicInteger entryNumber = new AtomicInteger();

        Map<IndexRow.IndexValueEntryNumberPair, List<HashValue>> result = indexDriver.getIndex().getAllItemsByIndexValueAndOriginalEntryNumber(indexName, indexValue, registerVersion);
        return result.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e1.getKey().getEntryNumber(), e2.getKey().getEntryNumber()))
                .map(e -> {
                    Entry originalEntry = entries.get(e.getKey().getEntryNumber());
                    return new Entry(entryNumber.incrementAndGet(), e.getValue(), originalEntry.getTimestamp(), e.getKey().getIndexValue());
                }).collect(Collectors.toList());
    }

//    public Map<String, List<HashValue>> getRsfEntries2(String indexName, Optional<String> indexValue, Optional<Integer> registerVersion) {
//        AtomicInteger entryNumber = new AtomicInteger();
//
//        Map<IndexRow.IndexValueEntryNumberPair, List<HashValue>> result = indexDriver.getIndex().getAllItemsByIndexValueAndOriginalEntryNumber(indexName, indexValue, registerVersion);
//        result.entrySet().stream()
//                .sorted((e1, e2) -> Integer.compare(e1.getKey().getEntryNumber(), e2.getKey().getEntryNumber()))
//                .map(e -> {
//                    Entry originalEntry = entries.get(e.getKey().getEntryNumber());
//                    return new Entry(entryNumber.incrementAndGet(), e.getValue(), originalEntry.getTimestamp(), e.getKey().getIndexValue());
//                })
//                .collect(Collectors.groupingBy(e -> e.getKey(), Collectors.mapping(e -> e.getSha256hex()));
//    }

//    public List<IndexRow> getCurrentEntries(String indexName, Optional<String> indexValue) {
//        AtomicInteger entryNumber = new AtomicInteger();
//        List<IndexRow> result = indexDriver.getIndex().getCurrentIndexRowsForIndexValue(indexName, indexValue, Optional.empty());
//
//        return result.stream()
//                .sorted((e1, e2) -> Integer.compare(e1.getKey().getEntryNumber(), e2.getKey().getEntryNumber()))
//                .map(e -> {
//                    Entry originalEntry = entries.get(e.getKey().getEntryNumber());
//                    return new Entry(entryNumber.incrementAndGet(), e.getValue(), originalEntry.getTimestamp(), e.getKey().getIndexValue());
//                }).collect(Collectors.toList());
//    }

    public Index getIndex() {
        return indexDriver.getIndex();
    }
}
