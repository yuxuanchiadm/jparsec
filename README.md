# JParsec [![Build Status](https://travis-ci.com/yuxuanchiadm/jparsec.svg?branch=master)](https://travis-ci.com/yuxuanchiadm/jparsec)

Monadic parser combinator library for Java.

# Build

**IMPORTANT**: Please use `OpenJDK 11` to build this project.

Although all sourcecode is written in `Java 8` and compiled to `Java 8` bytecode.
But `OpenJDK 8` has some bugs in type inference.
So some polymorphic type variables won't inferred correctly.
Therefore some code won't compile using `OpenJDK 8`.