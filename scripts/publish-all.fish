#!/usr/bin/env fish

set script_dir (dirname (status filename))
set project_root (dirname $script_dir)
set props_file "$project_root/gradle.properties"
set paper_dir (grep '^paperRunDirectory=' $props_file | string replace 'paperRunDirectory=' '')

cd $project_root

./gradlew build
or exit 1

fish $script_dir/apply-lang-pack-sha-to-server-properties.fish $paper_dir/server.properties
