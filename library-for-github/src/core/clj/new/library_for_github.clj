(ns clj.new.library-for-github
  (:require
   [clojure.string :as str]
   [clojure.java.io :as jio]
   [clojure.java.shell :as jsh]
   [clojure.tools.cli :as cli]
   [clj.new.templates :as t]
   [user.tools.deps.util.shell :refer [*sh-dir* dosh]]
   [user.tools.github.alpha :as github]
   ))


;; * CLI


(def cli-options
  [["-d" "--target-dir TARGET_DIR" "Target directory"
    :id :dir
    :default-fn (fn [{:keys [github-repository]}] (str (doto (jio/file (System/getProperty "user.dir") (name (keyword github-repository))) (jio/make-parents))))]
   ["-r" "--github-repository REPO" "Github repository symbol"
    :validate-fn [string? (complement str/blank?) #(number? (str/index-of % "/"))]]
   ["-a" "--basic-auth BASIC_AUTH" "Basic authentication token"
    :validate [#(number? (str/index-of % ":")) "Basic-auth string must be a \"user:password\""]]
   [nil "--clojars-username USERNAME" "Clojars username"]
   [nil "--clojars-password PASSWORD" "Clojars password"]
   [nil "--slack-webhook-url SLACK_WEBHOOK_URL" "Slack webhook url"]])


;; * fns


(defn strip-quotes
  [s]
  (nth (re-matches #"\s*\"([^\"]*)\"\s*" s) 1))


;; * sh


(defn sh-exit
  "Returns a map of jsh/sh return"
  [{:keys [exit out] :as sh-return}]
  (println out)
  (when-not (zero? exit)
    (throw (ex-info "Non-zero exit." sh-return)))
  sh-return)


;; ** ssh


(defn ssh-keygen
  [target key-title]
  (jio/make-parents target)
  (sh-exit (jsh/sh "ssh-keygen" "-t" "ecdsa" "-b" "521" "-P" "" "-N" "" "-C" key-title "-f" (str target) :in "y")))


;; * Render


(def render (t/renderer "library-for-github"))


(defn project-data
  [name]
  (let [data (t/project-data name)]
    (if (str/index-of (:name data) ".")
      (let [main-ns (t/multi-segment (t/sanitize-ns (:name data)))]
        (assoc data
          :namespace main-ns
          :original-namespace (:namespace data)
          :nested-dirs (t/name-to-path main-ns)
          :original-nested-dirs (:nested-dirs data)))
      data)))


;;


(defn github-repo-actions-put-secret
  "Workaround IllegalAccessError when using JNI library"
  [owner repo secret-name secret-value key key-id basic-auth]
  (dosh "clojure" "-Srepro" "-Sdeps" "'{:deps {ajchemist/tools.github.alpha {:git/url \"https://github.com/ajchemist/tools.github.alpha\" :sha \"75338a47308667240b6760992a0b5c04a76f0cf7\"}}}'"
        "-m" "user.tools.github.alpha.script.repo-actions" "put-secret"
        "--owner" owner
        "--repo" repo
        "--secret-name" secret-name
        "--secret-value" secret-value
        "--key" key
        "--key-id" key-id
        "--basic-auth" basic-auth))


;;


(defn configure-ci
  "Provide additional project data"
  [{:keys
    [dir
     basic-auth
     github-repository
     clojars-username
     clojars-password
     slack-webhook-url]
    :as _options}]
  (let [[owner repo] (str/split github-repository #"/" 2)]
    ;; validate parsed-opts
    (jio/make-parents (jio/file dir ".ci"))
    (github/create-repo
      {:basic-auth  basic-auth
       :form-params {"name" repo}})
    (let [{:strs [key key_id] :as _actions-pub-key}
          (github/actions-get-repo-pub-key
            {:github/owner owner
             :github/repo  repo
             :basic-auth   basic-auth})]
      (binding [*sh-dir* (System/getProperty "java.io.tmpdir")]
        (github-repo-actions-put-secret owner repo "CLOJARS_USERNAME" clojars-username key key_id basic-auth)
        (github-repo-actions-put-secret owner repo "CLOJARS_PASSWORD" clojars-password key key_id basic-auth)
        (github-repo-actions-put-secret owner repo "SLACK_WEBHOOK_URL" slack-webhook-url key key_id basic-auth))
      (binding [*sh-dir* dir]
        (dosh "git" "init")
        (dosh "git" "remote" "add" "-f" "origin" (str "git@github.com:" github-repository))))))


(defn library-for-github
  "Create a clojure library template for github"
  [name & args]
  (let [data (project-data name)]
    (t/->files
      data
      ;;
      [".ci/settings.xml" (render ".ci/settings.xml" data)]
      [".gitignore" (render ".gitignore" data)]
      [".dir-locals.el" (render ".dir-locals.el" data)]
      ;;
      ["deps.edn" (render "deps.edn" data)]
      "src/core"
      "src/test"
      ["src/test/user.clj" (render "user.clj" data)])
    (let [{:keys [options] :as _parsed} (cli/parse-opts args cli-options)
          data'                         (merge data (configure-ci options))]
      (println "CI data:" (pr-str data'))
      (t/->files
        data'
        ["README.md" (render "README.md" data')]
        [".github/workflows/main.yml" (render ".github/workflows/main.yml" data')]
        [".github/workflows/lint.yml" (render ".github/workflows/lint.yml" data')]))))
