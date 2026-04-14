#!/usr/bin/env fish

set script_dir (dirname (status filename))
set project_root (dirname $script_dir)
set props_file "$project_root/gradle.properties"
set paper_dir (rg '^paperRunDirectory=' "$props_file" | string replace 'paperRunDirectory=' '')

cd "$project_root"

./gradlew build -Pchevvyenchants.autoDeployLangPack=false
or exit 1

set jar_candidates (rg --files build/libs -g 'chevvyenchants-*.jar' | string match -rv '(-sources|-javadoc)\.jar$')
set jar $jar_candidates[1]
if test -z "$jar"
	echo "No plugin jar found under build/libs" >&2
	exit 1
end

mkdir -p "$paper_dir/plugins"
set old_plugin_jars (rg --files "$paper_dir/plugins" -g 'chevvyenchants-*.jar')
if test (count $old_plugin_jars) -gt 0
	rm -f $old_plugin_jars
end
cp -f "$jar" "$paper_dir/plugins/chevvyenchants.jar"
echo "Installed: $jar"

fish "$script_dir/apply-lang-pack-sha-to-server-properties.fish" "$paper_dir/server.properties"

cd "$paper_dir"
exec java -jar paper.jar --nogui
