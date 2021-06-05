# Usage


- `$project_name`
- `$owner/$repo`
- `$gh_basic_auth`
- `$clojars_username`
- `$clojars_password`
- `$slack_webhook_url`
- `$telegram_token`
- `$telegram_to`


``` shell
# clj -M:new


clj -Sdeps '{:deps {seancorfield/clj-new {:mvn/version "1.1.309"}}}' -M -m clj-new.create \
    https://github.com/ajchemist/clj-library-for-github/library-for-github@2fedb256b02c89346c3edc5596ba7537d61b559d \
    $project_name \
    -? \
    -f \
    -- \
    -r $owner/$repo \
    -a $gh_basic_auth \
    --clojars-username $clojars_username \
    --clojars-password $clojars_password \
    --slack-webhook-url $slack_webhook_url \
    --telegram-token $telegram_token \
    --telegram-to $telegram_to
```
