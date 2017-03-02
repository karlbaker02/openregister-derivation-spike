package uk.gov.rsf.indexer;

import uk.gov.rsf.util.Entry;
import uk.gov.rsf.util.HashValue;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a structure that stores index data in the below table format:
 *
 * |    index name     | index value | start entry | end entry |   item hash  |
 * -----------------------------------------------------------------------------
 * | current-countries |      SU     |       1     |      2    |  sha-256:abc |
 * |    record         |      SU     |       1     |      2    |  sha-256:abc |
 * |    record         |      SU     |       2     |           |  sha-256:dfe |
 * |    record         |      GB     |       3     |           |  sha-256:ghi |
 * | current-countries |      GB     |       3     |           |  sha-256:ghi |
 *
 * When something enters the index, it gets a row with a start entry and a null end entry.
 * When something leaves the index, the end entry gets populated with the entry number at which it no longer exists
 * in the index.
 */
public class Index {
    private List<IndexRow> indexRows;

    public Index() {
        this.indexRows = new ArrayList<>();
    }

    public void startValueForIndex(String indexName, String indexValue, int entryStart, HashValue itemHash) {
        indexRows.add(new IndexRow(indexName, indexValue, entryStart, itemHash));
    }

    public void endValueForIndex(String indexName, String indexValue, HashValue itemHash, Entry entry) {
        Optional<IndexRow> toEnd = getCurrentRowsForIndexValue(indexName, Optional.of(indexValue))
                .filter(row -> row.getItemHash().equals(itemHash))
                .findFirst();

        toEnd.get().setEndEntry(entry.getEntryNumber());
    }

//    public List<Entry> getCurrentItemsForIndex(String indexName, Optional<String> indexValue) {
//
//    }

    public Map<String, List<HashValue>> getCurrentItemsForIndex(String indexName, Optional<String> indexValue, Optional<Integer> registerVersion) {
        Stream<IndexRow> indexValueRows = registerVersion.isPresent()
                ? getCurrentRowsForIndexValueAtVersion(indexName, indexValue, registerVersion.get())
                : getCurrentRowsForIndexValue(indexName, indexValue);

        return indexValueRows.collect(Collectors.groupingBy(row -> row.getValue(), Collectors.mapping(row -> row.getItemHash(), Collectors.toList())));
    }

    public List<HashValue> getCurrentItemsForIndexValue(String indexName, String indexValue, Optional<Integer> registerVersion) {
        Stream<IndexRow> indexValueRows = registerVersion.isPresent()
                ? getCurrentRowsForIndexValueAtVersion(indexName, Optional.of(indexValue), registerVersion.get())
                : getCurrentRowsForIndexValue(indexName, Optional.of(indexValue));
        return indexValueRows.map(row -> row.getItemHash()).collect(Collectors.toList());
    }

    public List<IndexRow> getCurrentIndexRowsForIndexValue(String indexName, Optional<String> indexValue, Optional<Integer> registerVersion) {
        Stream<IndexRow> indexValueRows = registerVersion.isPresent()
                ? getCurrentRowsForIndexValueAtVersion(indexName, indexValue, registerVersion.get())
                : getCurrentRowsForIndexValue(indexName, indexValue);
        return indexValueRows.collect(Collectors.toList());
    }

    public Map<IndexRow.IndexValueEntryNumberPair, List<HashValue>> getAllItemsByIndexValueAndOriginalEntryNumber(String indexName, Optional<String> indexValue, Optional<Integer> registerVersion) {
        List<IndexRow> allRowsForIndexValue = (registerVersion.isPresent()
                ? getAllRowsForIndexValueAtVersion(indexName, indexValue, registerVersion.get())
                : getAllRowsForIndexValue(indexName, indexValue))
                .collect(Collectors.toList());

        Stream<IndexValueEvent> indexValueEvents = getAllIndexValueEvents(allRowsForIndexValue, registerVersion);

        return getItemsForIndexValueEntryNumberPair(indexValueEvents);
    }

    private Map<IndexRow.IndexValueEntryNumberPair, List<HashValue>> getItemsForIndexValueEntryNumberPair(Stream<IndexValueEvent> indexValueEvents) {
        Map<IndexRow.IndexValueEntryNumberPair, List<HashValue>> itemsForEachIndexValueAtOriginalEntryNumber = new HashMap<>();
        indexValueEvents.forEach(indexValueEvent -> {
            List<HashValue> hashes = getCurrentItemsForIndexValue(indexValueEvent.getOriginalIndexRow().getName(), indexValueEvent.getOriginalIndexRow().getValue(), Optional.of(indexValueEvent.getOriginalIndexRow().getTransaction(indexValueEvent.isStartEvent()).getEntryNumber()));
            itemsForEachIndexValueAtOriginalEntryNumber.put(indexValueEvent.getOriginalIndexRow().getTransaction(indexValueEvent.isStartEvent()), hashes);
        });
        return itemsForEachIndexValueAtOriginalEntryNumber;
    }

    private Stream<IndexValueEvent> getAllIndexValueEvents(List<IndexRow> indexValueRows, Optional<Integer> registerVersion) {
        return Stream.concat(
                indexValueRows.stream()
                        .filter(r -> !registerVersion.isPresent() || r.getStartEntry() <= registerVersion.get())
                        .map(r -> new IndexValueEvent(r, true)),
                indexValueRows.stream()
                        .filter(r -> !r.isCurrent() && (!registerVersion.isPresent() || r.getEndEntry() <= registerVersion.get()))
                        .map(r -> new IndexValueEvent(r, false)));
    }

    private Stream<IndexRow> getAllRowsForIndexValueAtVersion(String indexName, Optional<String> indexValue, int version) {
        return  getAllRowsForIndexValue(indexName, indexValue)
                .filter(ir -> ((ir.isCurrent() && ir.getStartEntry() <= version) || (!ir.isCurrent() && ir.getStartEntry() <= version)));
    }

    private Stream<IndexRow> getCurrentRowsForIndexValueAtVersion(String indexName, Optional<String> indexValue, int version) {
        return getAllRowsForIndexValue(indexName, indexValue)
                .filter(row -> (row.isCurrent() && row.getStartEntry() <= version)
                        || (!row.isCurrent() && row.getStartEntry() <= version && row.getEndEntry() > version));
    }

    private Stream<IndexRow> getCurrentRowsForIndexValue(String indexName, Optional<String> indexValue) {
        return getAllRowsForIndexValue(indexName, indexValue)
                .filter(row -> row.isCurrent());
    }

    private Stream<IndexRow> getAllRowsForIndexValue(String indexName, Optional<String> indexValue) {
        return indexRows.stream()
                .filter(row -> row.getName().equals(indexName))
                .filter(row -> !indexValue.isPresent() || row.getValue().equals(indexValue.get()));
    }

    @Override
    public String toString() {
        return String.join("\n", indexRows.stream().map(IndexRow::toString).collect(Collectors.toList()));
    }

    private class IndexValueEvent {
        public IndexRow originalIndexRow;
        public boolean start;

        public IndexValueEvent(IndexRow indexRow, boolean start) {
            this.originalIndexRow = indexRow;
            this.start = start;
        }

        public IndexRow getOriginalIndexRow() {
            return originalIndexRow;
        }

        public boolean isStartEvent() {
            return  start;
        }
    }
}


