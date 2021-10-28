# JParsec ![GitHub Repo stars](https://img.shields.io/github/stars/yuxuanchiadm/jparsec?style=flat-square) ![GitHub](https://img.shields.io/github/license/yuxuanchiadm/jparsec?style=flat-square) ![GitHub Workflow Status](https://img.shields.io/github/workflow/status/yuxuanchiadm/jparsec/Java%20CI%20with%20Maven?style=flat-square) ![GitHub all releases](https://img.shields.io/github/downloads/yuxuanchiadm/jparsec/total?style=flat-square)

Monadic parser combinator library for Java.

# Build

**IMPORTANT**: Please use `OpenJDK 17` with preview features enabled to build this project.

Unfortunately, the most important feature `Pattern Match for switch` is still in preview stage for Java 17.
Therefore, preview features must be enabled for this library to work. Which are already enabled in maven by default.
If you want to enable preview features manually. Pass `--enable-preview` as argument to `javac` and `java`.

# About 2.x

Java is entering a new era. With multiple new language features available. It's times to use them.
All codes are rewritten to use new language features like record, pattern match, sealed, etc.
The API is kept as same as possible. But is not compatible with 1.x. With some necessary changes:

- Replace any old pattern match construct like `match`, `caseof` to use new pattern match feature.
- Remove any `equals`, `hashcode` implementation to use default one provided by record.
- Etc.
