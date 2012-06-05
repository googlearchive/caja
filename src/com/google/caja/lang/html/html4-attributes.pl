#!/usr/bin/perl

# Extracts JSON from http://www.w3.org/TR/html4/index/attributes.html

use strict;

my %TYPE_CLASSES = (
  'CDATA' => 'NULL', 'CLASSES' => 'CLASSES', 'ID' => 'ID',
  'IDREF' => 'IDREF', 'IDREFS' => 'IDREFS', 'NAME' => 'NAME',
  'GLOBAL_NAME' => 'GLOBAL_NAME', 'LOCAL_NAME' => 'LOCAL_NAME',
  '%CAlign;' => 'NULL', '%Charset;' => 'NULL', '%Charsets;' => 'NULL',
  '%ContentTypes;' => 'NULL', '%ContentType;' => 'NULL',
  '%Datetime;' => 'NULL', '%Fragment' => 'URI_FRAGMENT',
  '%FrameTarget;' => 'FRAME_TARGET', '%IAlign;' => 'NULL', '%LAlign;' => 'NULL',
  '%LIStyle;' => 'NULL', '%LanguageCode;' => 'NULL', '%LinkTypes;' => 'NULL',
  '%MediaDesc;' => 'NULL', '%MultiLength;' => 'NULL',
  '%MultiLengths;' => 'NULL', '%OLStyle;' => 'NULL',
  '%Scope;' => 'NULL', '%Script;' => 'SCRIPT', '%Shape;' => 'NULL',
  '%StyleSheet;' => 'STYLE', '%TAlign;' => 'NULL', '%TFrame;' => 'NULL',
  '%TRules;' => 'NULL', '%Text;' => 'NULL', '%ULStyle;' => 'NULL',
  '%URI;' => 'URI',
  );

my %PATTERNS_FOR_TYPE = (
  '%Character;' => '.',
  '%Color;' => '\\w+|#[0-9A-Fa-f]{6}',
  '%Pixels;' => '[0-9]+',
  '%Length;' => '[0-9]+%?',
  '%Coords;' => '[0-9]+(?:,[0-9]+)*',
  '%Fragment;' => '#.*',
  'NUMBER' => '[0-9]+',
  '%InputType;'
  => 'TEXT|PASSWORD|CHECKBOX|RADIO|SUBMIT|RESET|FILE|HIDDEN|IMAGE|BUTTON',
  );

my %MIME_TYPES = (
  'FORM::ACTION' => 'application/x-www-form-urlencoded',
  'BODY::BACKGROUND' => 'image/*',
  '*::CITE' => '*/*',
  'OBJECT::CLASSID' => 'application/*',
  '*::CODEBASE' => 'application/*',
  'OBJECT::DATA' => 'application/*',
  'A::HREF' => '*/*',
  'AREA::HREF' => '*/*',
  'LINK::HREF' => 'text/css',
  'BASE::HREF' => 'text/html',
  '*::LONGDESC' => '*/*',
  'HEAD::PROFILE' => 'application/*',
  'SCRIPT::SRC' => 'text/javascript',
  'INPUT::SRC' => 'image/*',
  'FRAME::SRC' => 'text/html',
  'IFRAME::SRC' => 'text/html',
  'IMG::SRC' => 'image/*',
  );

my %TYPE_OVERRIDES = (
  '*::USEMAP' => '%Fragment;',  # Not really a URI
  '*::CLASS' => 'CLASSES',
  'BUTTON::NAME' => 'LOCAL_NAME',
  'INPUT::NAME' => 'LOCAL_NAME',
  'META::NAME' => 'LOCAL_NAME',
  'PARAM::NAME' => 'LOCAL_NAME',
  'SELECT::NAME' => 'LOCAL_NAME',
  'TEXTAREA::NAME' => 'LOCAL_NAME',
  '*::NAME' => 'GLOBAL_NAME',
  );

my %DEFAULT_OVERRIDES = (
  '*::TARGET' => '_self',
  );


my $headers = <STDIN>;
while (<STDIN>) {
  chomp;
  my ($name, $elements, $type, $default, $deprecated, $dtd, $desc)
      = split / +\t/, $_;
  die qq'$.: name: $_'       unless $name =~ /^[a-z-]+$/;
  $name = qq'\U$name';

  die qq'$.: elements: $_'
      unless $elements =~ /^(All elements but )?([A-Z0-9]+(?:, [A-Z0-9]+)*)$/;

  my $negatedElementSet = $1;
  my @elements = split m/, /, $2;

  # In the input, the value in the Type column for the "checked" attribute is
  # "(checked)" meaning it can only assume that value if present, and
  # similarly for other valueless attributes like selected, multiple, etc.
  my $valueless = ("\L$type" eq "\L($name)");

  if (exists($TYPE_OVERRIDES{qq'*::$name'})) {
    $type = $TYPE_OVERRIDES{qq'*::$name'};
  }

  if (exists($DEFAULT_OVERRIDES{qq'*::$name'})) {
    $default = $DEFAULT_OVERRIDES{qq'*::$name'};
  }

  if ($default eq '%HTML.Version;') {
    $default = '#IMPLIED';
  }
  die qq'$.: default: $_'
      unless $default =~ /^(\#REQUIRED|\#IMPLIED|\w+|\"[^\"]*\")$/;
  die qq'$.: deprecated: $_' unless $deprecated =~ /^D?$/;
  die qq'$.: dtd: $_'        unless $dtd =~ /^[LF]?$/;

  my @elNames = @elements;
  if ($negatedElementSet) {
    @elNames = ('*');
  }

  foreach my $elName (sort(@elNames)) {
    my $elType = $type;
    my $elAndAttr = qq'$elName\E::$name';
    print STDERR "$elAndAttr\n" if $valueless;
    if (exists($TYPE_OVERRIDES{$elAndAttr})) {
      $elType = $TYPE_OVERRIDES{$elAndAttr};
    }

    my $typeName;
    my @typeValues;
    my $typePattern;
    my $typeClass;

    if ($elType =~ /^(?:%(?:\w+);|[A-Z_]+)$/) {
      $typeName = $elType;
      $typePattern = $PATTERNS_FOR_TYPE{$elType};
      $typeClass = $TYPE_CLASSES{$typeName};
      @typeValues = ();
    } elsif($elType =~ /^\(([\w\/]+(?: \| [\w\/]+)*)\)$/) {
      @typeValues = split / \| /, $1;
      $typePattern = join '|', @typeValues;
      $typeClass = $typeName = undef;
    }
    die qq'$.: type: $elType'
        unless defined($typePattern) or defined($typeClass);

    my $mimeTypes = undef;
    if ('URI' eq $typeClass) {
      $mimeTypes = $MIME_TYPES{$elAndAttr}
          || $MIME_TYPES{qq'*\E::$name'};
      die qq'$.: mimeTypes: $_' unless $mimeTypes;
    }
    my $descStr = $desc; $descStr =~ s/[\"\\]/\\$&/g;
    print qq'      { \"key\": \"$elAndAttr\",';
    print qq' \"description\": \"$descStr\",' if $descStr =~ /\S/;
    print qq'\n       ';
    print qq' \"mimeTypes\": \"$mimeTypes\",' if $mimeTypes;
    print qq' \"type\": \"$typeClass\",'
        if $typeClass && $typeClass ne 'NULL';
    if (@typeValues) {
      my $typeValueStr = join ',', @typeValues;
      print qq' \"values\": \"$typeValueStr\",' if $typeValueStr;
    } else {
      my $typePatternStr = $typePattern;
      $typePatternStr =~ s/[\"\\]/\\$&/g;
      print qq' \"pattern\": \"$typePatternStr\",' if $typePatternStr;
    }
    if ($default !~ /^\#/) {
      my $defaultValueStr = $default;
      $defaultValueStr =~ s/\"(.*)\"/$1/g;
      $defaultValueStr =~ s/[\"\\]/\\$&/g;
      print qq' \"default\": \"$defaultValueStr\",'
    }
    print qq' \"valueless\": true,' if $valueless;
    my $optional = $default eq '#REQUIRED' ? 'false' : 'true';
    print qq' \"optional\": $optional';
    print qq' },\n';
  }
}
