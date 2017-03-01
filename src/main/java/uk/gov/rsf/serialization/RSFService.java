package uk.gov.rsf.serialization;

import com.google.common.collect.ImmutableMap;
import uk.gov.rsf.indexer.function.*;
import uk.gov.rsf.serialization.handlers.AddItemCommandHandler;
import uk.gov.rsf.serialization.handlers.AppendEntryCommandHandler;
import uk.gov.rsf.util.Entry;
import uk.gov.rsf.util.HashValue;
import uk.gov.rsf.util.Register;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final Pattern recordPattern = Pattern.compile("^/record/(.+)");
    private static final Pattern indexPattern = Pattern.compile("^/index/(.+)/(.+)");

    private static final Map<String, Pattern> patterns = ImmutableMap.of(
            "/record", Pattern.compile("^/record/(.+)"),
            "/index", Pattern.compile("^/index/(.+)/(.+)"));

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

//            patterns.keySet().forEach(k -> {
//                if (request.startsWith(k)) {
//                    Matcher matcher = patterns.get(k).matcher(request);
//
//                    if (matcher.find()) {
//
//                    }
//                }
//            });

            if (recordPattern.matcher(request).find()) {
                matcher = recordPattern.matcher(request);
                matcher.find();
                String pk = matcher.group(1);

                Map<String, List<HashValue>> records = register.getRecordsForIndex("record", Optional.of(pk), Optional.empty());

                printRecords(request, records, register);

            }
            else if (indexPattern.matcher(request).find()) {
                matcher = indexPattern.matcher(request);
                matcher.find();
                String recordIndexName = matcher.group(1);
                String pk = matcher.group(2);

                Map<String, List<HashValue>> records = register.getRecordsForIndex(recordIndexName, Optional.of(pk), Optional.empty());

                printRecords(request, records, register);
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

    private static void printRecords(String request, Map<String, List<HashValue>> records, Register register) {
        System.out.println(request + ":");

        records.entrySet().stream().sorted((es1, es2) -> es1.getKey().compareTo(es2.getKey())).forEach(f -> {
//            System.out.println(f.getKey() + ":");
            f.getValue().forEach(itemHash -> System.out.println("\t" + register.getItem(itemHash).getContent()));
        });
    }

    private static void printEntries(List<Entry> entries, Register register) {
        System.out.println(String.join("\n", entries.stream().flatMap(e -> e.getSha256hex().stream()).collect(Collectors.toSet())
                .stream().map(itemHash -> "add-item\t" + register.getItem(itemHash).getContent()).collect(Collectors.toList())));
        entries.stream().forEach(System.out::println);
    }

    public static void main(String[] args) throws FileNotFoundException {
        InputStream in = new FileInputStream(args[0]);
        String indexName = args[1];
        List<String> recordRequests = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

        morc(in, indexName, recordRequests);

//        String indexRender = args[1];
//        Optional<Integer> registerVersion = args.length > 2 ? Optional.of(Integer.valueOf(args[2])) : Optional.empty();
//        Optional<String> indexValue = args.length > 3 ? Optional.of(args[3]) : Optional.empty();
//        morc(System.in, indexName, indexRender, indexValue, registerVersion);
    }
}
