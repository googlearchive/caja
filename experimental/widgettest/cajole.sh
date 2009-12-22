function cajole {
  input=$1;
  output=$( echo $1 | sed s/\.js/.out.js/g );
  ../../bin/cajole_html --input $input --output_js $output --output_html /dev/null --renderer debugger
}

cajole widget.js
cajole libSquare.js
cajole libCube.js
cajole factorial.js