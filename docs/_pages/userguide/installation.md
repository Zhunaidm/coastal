---
layout: page
title: How to install DEEPSEA
permalink: /userguide/installation/
---

There are three main sections to this page

## Requirements

- Z3

~~~
RUN git clone https://github.com/Z3Prover/z3.git
RUN apt-get update && \
    apt-get install -y build-essential && \
    apt-get install -y gcc && \
    apt-get install -y m4 && \
    apt-get install -y g++
WORKDIR /z3
RUN python scripts/mk_make.py --java
WORKDIR /z3/build
RUN make && \
    make install
~~~

## Installation for CLI

  - git clone
  - settings

## Installation in Eclipse

  - git clone
  - gradle import

