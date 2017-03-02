package uk.gov.rsf.serialization;

import com.google.common.collect.ImmutableMap;
import uk.gov.rsf.indexer.function.*;
import uk.gov.rsf.serialization.handlers.AddItemCommandHandler;
import uk.gov.rsf.serialization.handlers.AppendEntryCommandHandler;
import uk.gov.rsf.util.Entry;
import uk.gov.rsf.util.HashValue;
import uk.gov.rsf.util.Item;
import uk.gov.rsf.util.Register;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RSFService {

    private static final List<Function<Register, IndexFunction>> availableIndexFunctions = Arrays.asList(
            r -> new CurrentCountriesIndexFunction(r),
            r -> new LocalAuthorityByTypeIndexFunction(r),
            r -> new SchoolByAgeIndexFunction(r));

    private static final List<IndexFunction> defaultIndexFunctions = Arrays.asList(
            new RecordIndexFunction());

    private static final Pattern recordPattern = Pattern.compile("^/record/([a-zA-Z-]+)");
    private static final Pattern indexPattern = Pattern.compile("^/index/([a-zA-Z-]+)/?([a-zA-Z-]+)?");

    private static final Map<String, Pattern> patterns = ImmutableMap.of(
            "/record", Pattern.compile("^/record/(.+)"),
            "/index", Pattern.compile("^/index/(.+)/?(.+)?"));

//    public static void morc(InputStream inputStream, String indexName, String indexRender, Optional<String> indexValue, Optional<Integer> registerVersion) {
//        Register register = new Register();
//        if (!indexName.equals("indexTable")
//                && !defaultIndexFunctions.stream().anyMatch(func -> func.getName().equals(indexName))) {
//            register.registerIndex(availableIndexFunctions.stream().filter(func -> func.apply(register).getName().equals(indexName)).findFirst().get().apply(register));
//        }
//
//        parseRsf(inputStream, register);
//
//        if (indexRender.equals("indexTable")) {
//            System.out.println(register.getIndex().toString());
//        } else {
//            if (indexRender.equals("current")) {
//                printRecords("", register.getRecordsForIndex(indexName, indexValue, registerVersion), register);
//            } else if (indexRender.equals("rsf")) {
//                printEntries(register.getRsfEntries(indexName, indexValue, registerVersion), register);
//            }
//        }
//    }

    public static void morc(InputStream inputStream, String indexName, List<String> recordRequests) {
        Register register = new Register();
        if (!defaultIndexFunctions.stream().anyMatch(func -> func.getName().equals(indexName))) {
            register.registerIndex(availableIndexFunctions.stream().filter(func -> func.apply(register).getName().equals(indexName)).findFirst().get().apply(register));
        }

        parseRsf(inputStream, register);

        recordRequests.forEach(request -> {
            Matcher matcher;

            if (recordPattern.matcher(request).find()) {
                matcher = recordPattern.matcher(request);
                matcher.find();
                String pk = matcher.group(1);

                Map<String, List<HashValue>> records = register.getRecordsForIndex("record", Optional.of(pk), Optional.empty());

                printRecords(indexName, request, records, register);

            }
            else if (request.startsWith("/index")) {
                matcher = indexPattern.matcher(request);
                matcher.find();
                String recordIndexName = matcher.group(1);
                Optional<String> pk = matcher.group(2) == null ? Optional.empty() : Optional.of(matcher.group(2));

                Map<String, List<HashValue>> records = register.getRecordsForIndex(recordIndexName, pk, Optional.empty());
                List<Entry> rsfEntries = register.getRsfEntries(recordIndexName, pk, Optional.empty());



//                List<IndexRow> indexRows = register.getCurrentEntries(recordIndexName, pk);

                printRecords(indexName, request, records, register);
            }
            else {
                // No such endpoint exists, swallow exception for now
            }
        });
    }

    private static void parseRsf(InputStream inputStream, Register register) {
        RSFExecutor rsfExecutor = new RSFExecutor(register);
        rsfExecutor.register(new AddItemCommandHandler());
        rsfExecutor.register(new AppendEntryCommandHandler());

        RegisterSerialisationFormatService rsfService = new RegisterSerialisationFormatService(rsfExecutor);
        RegisterSerialisationFormat rsf = rsfService.readFrom(inputStream, new RSFFormatter());
        rsfService.process(rsf);
    }

    private static void printRecords(String indexName, String request, Map<String, List<HashValue>> records, Register register) {
        System.out.println(request + ":");

        records.forEach((key, r) -> {
            List<Entry> entries = register.getRsfEntries(indexName, Optional.of(key), Optional.empty());
            Entry record = entries.get(entries.size() - 1);

            System.out.println("\t\"" + key + "\": {");
            System.out.println("\t\tentry-number:" + record.getEntryNumber() + ",");
            System.out.println("\t\tentry-timestamp:" + record.getTimestampAsISOFormat() + ",");


            List<Item> items = new ArrayList<>();
            r.forEach(itemHash -> items.add(register.getItem(itemHash)));

            String itemString = String.join(",", items.stream().map(
                    item -> item.getSha256hex() + ":" + item.getContent()).collect(Collectors.toList()));

            System.out.println("\t\titem-hash: ["+ itemString +"]");
            System.out.println("\t}");
        });

//        records.entrySet().stream().sorted((es1, es2) -> es1.getKey().compareTo(es2.getKey())).forEach(f -> {
//            Entry entry = register.getRecord(f.getKey()).get();
//
//            System.out.println("\t\"" + f.getKey() + "\": {");
//            System.out.println("\t\tentry-number:" + entry.getEntryNumber() + ",");
//            System.out.println("\t\tentry-timestamp:" + entry.getTimestampAsISOFormat() + ",");
//
//
//            List<Item> items = new ArrayList<>();
//            f.getValue().forEach(itemHash -> items.add(register.getItem(itemHash)));
//
//            f.getValue().forEach(itemHash -> {
//                Item item = register.getItem(itemHash);
////                System.out.println("\t\t\t" + item.getSha256hex() + ":" + item.getContent());
//
//            });
//
//            String itemString = String.join(",", items.stream().map(
//                    item -> "\t\t\t" + item.getSha256hex() + ":" + item.getContent()).collect(Collectors.toList()));
//
//            System.out.println("\t\titem-hash: ["+ itemString +"]");
//            System.out.println("\t}");
////            f.getValue().forEach(itemHash -> System.out.println("\t\t" + register.getItem(itemHash).getContent()));
//        });
    }

//    private static void printEntries(List<Entry> entries, Register register) {
//        System.out.println(String.join("\n", entries.stream().flatMap(e -> e.getSha256hex().stream()).collect(Collectors.toSet())
//                .stream().map(itemHash -> "add-item\t" + register.getItem(itemHash).getContent()).collect(Collectors.toList())));
//        entries.stream().forEach(System.out::println);
//    }

    public static void main(String[] args) throws FileNotFoundException {
        InputStream in = new FileInputStream(args[0]);
        String indexName = args[1];
        List<String> recordRequests = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

        morc(in, indexName, recordRequests);
    }
}
