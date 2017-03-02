# openregister-derivation-spike
A spike for openregister java derivations, forked from https://github.com/michaelabenyohai/openregister-derivation-spike.

```java -jar build/libs/rsf-parser.jar {registerRsf} {indexName} [register record/index record endpoints]```

where `registerVersion` and `indexValue` are optional.

- `registerRsf` is the original register RSF
- `indexName` is the name of the index - any of "record", "current-countries", "local-authority-by-type"
- `[register record/index record endpoints]` are the API endpoints that you want to view

e.g. 

```java -jar build/libs/rsf-parser.jar local_authorities_beta.rsf local-authority-by-type /index/local-authority-by-type```

e.g.

```java -jar build/libs/rsf-parser.jar local_authorities_beta.rsf local-authority-by-type /index/local-authority-by-type/MD```

e.g.

```java -jar build/libs/rsf-parser.jar local_authorities_beta.rsf local-authority-by-type /record/BOL /record/MD /index/local-authority-by-type/BOL /index/local-authority-by-type/NMD```


Note: the code is currently broken if you try and hit a `/record` endpoint for the original register.