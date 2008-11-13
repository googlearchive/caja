#!/usr/bin/perl -w

use strict;
use Carp;

open(F,'status') || croak "status: $!";

my($tt, $at, $ft, $et);
while(my $line=<F>) {
    chomp $line;

    next if $line !~ /(.+): (\d+) tests, (\d+) assertions, (\d+) failures, (\d+) errors/;

    my($m, $t, $a, $f, $e) = ($1, $2, $3, $4, $5);

    print "$m: $t tests, $a assertions, $f failures, $e errors\n";

    $tt+=$t;
    $at+=$a;
    $ft+=$f;
    $et+=$e;
}

print "\nTotal: $tt tests, $at assertions, $ft failures, $et errors\n\n";

printf "Assertion failures: %.2f%%\n", $ft*100./$at;
printf "Errors            : %.2f%%\n", $et*100./$tt;
