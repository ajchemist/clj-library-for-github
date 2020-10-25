# Usage


- `$project_name`
- `$owner/$repo`
- `$gh_basic_auth`
- `$clojars_username`
- `$clojars_password`
- `$slack_webhook_url`


``` shell
# clj -M:new


clj -Sdeps '{:deps {seancorfield/clj-new {:mvn/version "1.1.216"}}}' -m clj-new.create \
    https://github.com/ajchemist/clj-library-for-github/library-for-github@0538cd58fe56777b5926971bee6262a5919c94da \
    $project_name \
    -? \
    -f \
    -- \
    -r $owner/$repo \
    -a $gh_basic_auth \
    --clojars-username $clojars_username \
    --clojars-password $clojars_password \
    --slack-webhook-url $slack_webhook_url
```
