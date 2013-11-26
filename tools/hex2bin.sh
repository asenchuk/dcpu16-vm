#!/bin/bash

if [ ! $# -eq 2 ]; then
    echo "Usage: $0 hex_words_list.txt output.bin"
    exit 1
fi

# create (clear) output file
echo -n > "$2"

for word in $(cat "$1"); do
	word=$(echo $word | sed 's/\0x\(.\{2\}\)\(.\{2\}\)/\\x\1\\x\2/g')
	echo -en $word >> "$2"
done
