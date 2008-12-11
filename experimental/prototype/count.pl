#!/usr/bin/perl -w

use strict;
use Carp;

open(F,'status') || croak "status: $!";

my($tt, $at, $ft, $et);
my($ptt, $pat, $pft, $pet);

while(my $line = <F>) {
    chomp $line;

    if ($line =~ /(.+): (\d+) tests, (\d+) assertions, (\d+) failures, (\d+) errors/) {
	my($m, $t, $a, $f, $e) = ($1, $2, $3, $4, $5);

	print "$m: $t tests, $a assertions, $f failures, $e errors\n";

	$tt += $t;
	$at += $a;
	$ft += $f;
	$et += $e;

	$line = <F>;
	chomp $line;
	print "$line\n";
	croak if $line !~ /\((\d+) tests, (\d+) assertions, (\d+) failures, (\d+) errors\)/;

	($t, $a, $f, $e) = ($1, $2, $3, $4);

	$ptt += $t;
	$pat += $a;
	$pft += $f;
	$pet += $e;
    } 
}

print "\nTotal: $tt tests, $at assertions, $ft failures, $et errors";
print "\n      ($ptt tests, $pat assertions, $pft failures, $pet errors)\n\n";

croak if $tt != $ptt;

printf "Assertion failures: %.2f%% (%.2f%%)\n", $ft*100./$pat, $pft*100./$pat;
printf "Errors            : %.2f%% (%.2f%%)\n", $et*100./$tt, $pet*100./$tt;
