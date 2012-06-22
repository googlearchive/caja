#!/bin/sh

# Copyright 2012 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

root=${1:-'.'}

# Check for safe case-folding in Java (Turkish i problem)
find "$root/src" -name "*.java" ! -name "Strings.java" -print0 |
xargs -0 egrep 'toLowerCase|toUpperCase|equalsIgnoreCase' |
perl -pe 'END {
  if ($. > 0) {
    die "ERROR: Unsafe case-folding; use Strings.* instead\n";
  }
}
'
