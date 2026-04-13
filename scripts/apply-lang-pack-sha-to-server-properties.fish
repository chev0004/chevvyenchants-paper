#!/usr/bin/env fish

set script_dir (dirname (status filename))
set project_root (dirname $script_dir)
set sha_file "$project_root/build/distributions/chevvyenchants-lang.zip.sha1"
set props $argv[1]

if test -z "$props"
	echo "Usage: "(status filename)" <path/to/server.properties>" >&2
	echo "Run ./gradlew chevvyenchantsResourcePack first." >&2
	exit 1
end
if not test -f "$sha_file"
	echo "Missing $sha_file — run ./gradlew chevvyenchantsResourcePack first." >&2
	exit 1
end
if not test -f "$props"
	echo "Not a file: $props" >&2
	exit 1
end

set sha (string trim (cat $sha_file))

if grep -qE '^resource-pack-sha1=' $props
	sed -i "s|^resource-pack-sha1=.*|resource-pack-sha1=$sha|" $props
else
	echo "resource-pack-sha1=$sha" >> $props
end

echo "Set resource-pack-sha1=$sha in $props"
