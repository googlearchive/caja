#!/usr/bin/bash
rm -rf ./test/unit/tmp/*
rake caja:cajole_gadgets CAJA_SRC_PATH=../..
open -a /Applications/Firefox\ 3.app http://localhost:8000/test/unit/tmp/dom_test.html