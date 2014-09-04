
$ git flow release start XXX
$ ./scripts/bump-version-number.sh
Android studio -> Build variants (tab in bottom left) -> Set both to release
Android studio -> Build -> Generate Signed APK

Upload app/app.apk to Google Play

$ git flow release finish XXX
$ git push
$ git push --tags
