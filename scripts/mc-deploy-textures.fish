#!/usr/bin/env fish

set script_dir (dirname (status filename))
set project_root (dirname $script_dir)
set source "$project_root/build/distributions/chevvyenchants-lang.zip"
set filename "chevvyenchants-lang.zip"

set deploy_dir $argv[1]
if test -z "$deploy_dir"
	if set -q CHEVVY_NETLIFY_DEPLOY_DIR
		set deploy_dir $CHEVVY_NETLIFY_DEPLOY_DIR
	else
		echo "Usage: "(status filename)" <deploy_dir>" >&2
		echo "Or set CHEVVY_NETLIFY_DEPLOY_DIR to the Netlify --dir folder." >&2
		exit 1
	end
end

if not test -f "$source"
	echo "Missing $source — run ./gradlew chevvyenchantsResourcePack first." >&2
	exit 1
end

cp "$source" "$deploy_dir/$filename"

echo "Deploying $filename..."
netlify deploy --dir="$deploy_dir" --prod

if set -q CHEVVY_NETLIFY_PUBLIC_URL
	echo "Done! URL: $CHEVVY_NETLIFY_PUBLIC_URL/$filename"
else
	echo "Deploy finished. Set CHEVVY_NETLIFY_PUBLIC_URL to print the download URL next time."
end
