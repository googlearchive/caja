#!/bin/bash

host="$BROWSER_SCOPE_HOST"
if [ -z "$host" ]; then
  host=www.browserscope.org
fi

function url_esc() {
  perl -e '$s = $ARGV[0]; $s =~ s/[^A-Za-z0-9\-._~]/sprintf("%%%02x", ord($&))/ge; print $s' "$1"
}

root="$(dirname $0)"

tmp_file="$(mktemp "$TMP/temp.XXXXXX")"
tmp_list_file="$(mktemp "$TMP/temp.XXXXXX")"
for user_agent in "MSIE" "MSIE 6.0" "MSIE 7.0" "MSIE 8.0" "Firefox 2" "Firefox 3" "Firefox 3.0" "Firefox 3.4" "Firefox 3.5" "Firefox 3.6" "Chrome" "Chrome 4" "Chrome 5" "Opera" "Opera 9" "Opera 10"; do
  echo Reading "$user_agent" from "$host"
  curl -o "$tmp_file" \
      "http://$host/jskb/json?ua=$(url_esc "$user_agent")&ot=application/json"
  local_file="$(echo -n "$user_agent" | tr ' /:;' '____').env.json"
  abs_file="$root/$local_file"
  if [ "$?" -eq "0" ] && [ "$(stat -f "%z" "$tmp_file")" -gt 100 ] ; then
    echo "$user_agent" OK
    mv "$tmp_file" "$abs_file"
  else
    echo "$user_agent" BAD
    head -3 -- "$tmp_file"
  fi
  if [ -e "$abs_file" ]; then
    echo "$local_file" >> "$tmp_list_file"
  fi
  echo ; echo
done
rm -f "$tmp_file"
cat -- "$tmp_list_file" | sort > "$root/env.json.list.txt"
rm "$tmp_list_file"
