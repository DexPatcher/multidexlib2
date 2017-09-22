# multidexlib2
### Multi-dex extensions for dexlib2.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.lanchon.dexpatcher/multidexlib2/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.lanchon.dexpatcher/multidexlib2)
[![Build Status](https://travis-ci.org/DexPatcher/multidexlib2.svg?branch=master)](https://travis-ci.org/DexPatcher/multidexlib2)

This is a free software (GPLv3+) library on top of [dexlib2](https://github.com/JesusFreke/smali/tree/master/dexlib2) that features:
- Multi-dex reading and writing.
- Optional management of the content of the main dex file when writing multi-dex.
- Simplified read/write access to multi-dex containers as plain-old dexlib2 `DexFile` objects, making it trivial to add multi-dex support to existing non-multi-dex-aware dexlib2 clients.
- Faster dex reads than plain dexlib2.
- Optional multi-threaded multi-dex writes that deliver greatly increased write performance.
- Automatic management of dex version, dex opcodes, and API level.
- Configurable naming of multi-dex member files.
- Progress logging to a client-provided logger.

Limitations:
- Only supports dex files. (Does not support odex/oat files.)

### DISTRIBUTION

Releases of multidexlib2 are distributed with coordinates `com.github.lanchon.dexpatcher:multidexlib2` through the [Maven Central Repository](https://search.maven.org/#search%7Cga%7C1%7Ccom.github.lanchon.dexpatcher.multidexlib2), with version numbers starting at `2.2.0` and matching the version of dexlib2 they were built against. Release changelog is available [here](https://github.com/DexPatcher/multidexlib2/releases).

### USAGE

Interface:
- [__MultiDexIO:__](https://github.com/DexPatcher/multidexlib2/blob/master/src/main/java/lanchon/multidexlib2/MultiDexIO.java) read and write potentially multi-dex containers.
  - __readDexFile():__ read potentially multi-dex container and return a single, merged `DexFile`.
  - __readMultiDexContainer():__ read potentially multi-dex container and return a `MultiDexContainer`.
  - __writeDexFile():__ write potentially multi-dex container from a single `DexFile`.
- [__RawDexIO:__](https://github.com/DexPatcher/multidexlib2/blob/master/src/main/java/lanchon/multidexlib2/RawDexIO.java) read and write single dex files (supersedes invoking dexlib2 directly).

Parameters:
- __boolean multiDex:__ `true` to enable multi-dex support.
- __File file:__ file or directory to read or write.
- __DexFileNamer namer:__ set to `new BasicDexFileNamer()`.
- __Opcodes opcodes:__ `null` to auto-detect.
- __int maxDexPoolSize:__ set to `DexIO.DEFAULT_MAX_DEX_POOL_SIZE`.
- __int threadCount:__ thread count for multi-threaded multi-dex writes.
  - `1` to force single-threaded writes.
  - `0` to auto-detect optimum thread count.
- __int minMainDexClassCount, boolean minimalMainDex:__ main dex file content management.
  - `0, false` to disable main dex management.
- __[DexIO](https://github.com/DexPatcher/multidexlib2/blob/master/src/main/java/lanchon/multidexlib2/DexIO.java).Logger logger:__ `null` to disable logging.

Sample:

DexPatcher's [file processor](https://github.com/DexPatcher/dexpatcher-tool/blob/master/tool/src/main/java/lanchon/dexpatcher/Processor.java) is a simple yet production-quality client of multidexlib2.
