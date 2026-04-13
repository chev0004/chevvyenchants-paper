#!/usr/bin/env fish

set script_dir (dirname (status filename))
set project_root (dirname $script_dir)
set props_file "$project_root/gradle.properties"
set paper_dir (grep '^paperRunDirectory=' $props_file | string replace 'paperRunDirectory=' '')

cd $project_root

./gradlew build -Pchevvyenchants.autoDeployLangPack=false
or exit 1

set jar (ls build/libs/chevvyenchants-*.jar 2>/dev/null | grep -v -- -sources | grep -v -- -javadoc | head -1)
if test -z "$jar"
	echo "No plugin jar found under build/libs" >&2
	exit 1
end
rm -f $paper_dir/plugins/chevvyenchants-*.jar
cp -f $jar $paper_dir/plugins/chevvyenchants.jar
echo "Installed: $jar"

fish $script_dir/apply-lang-pack-sha-to-server-properties.fish $paper_dir/server.properties

cd $paper_dir
exec java -jar paper.jar --nogui
