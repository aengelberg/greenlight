(ns greenlight.report
  "Code for generating human-readable reports from test results. Each reporter
  takes a collection of test results as input and should produce some output,
  depending on the report type."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.test :as ctest]
    [greenlight.step :as step]
    [greenlight.test :as test]))


(def ^:private sgr-code
  "Map of symbols to numeric SGR (select graphic rendition) codes."
  {:none        0
   :bold        1
   :underline   3
   :black      30
   :red        31
   :green      32
   :yellow     33
   :blue       34
   :magenta    35
   :cyan       36
   :white      37})


(defn- sgr
  "Returns an ANSI escope string which will apply the given collection of SGR
  codes."
  [codes]
  (let [codes (map sgr-code codes codes)
        codes (str/join \; codes)]
    (str \u001b \[ codes \m)))


(defn- color
  "Wraps the given string with SGR escapes to apply the given codes, then reset
  the graphics."
  [codes string]
  (let [codes (if (coll? codes) codes [codes])]
    (str (sgr codes) string (sgr [:none]))))


(defn- state-color
  "Return the color keyword for the given outcome state."
  [outcome]
  (case outcome
    :pass :green
    :fail :red
    :error :red
    :timeout :yellow
    :magenta))


(defn handle-test-event
  "Print out a test report event."
  [options event]
  (let [color (if (:print-color options)
                color
                (fn [codes string] string))]
    (case (:type event)
      :test-start
        (let [test-case (:test event)]
          (printf "\n\n %s Testing %s\n"
                  (color :magenta "*")
                  (color [:bold :magenta] (::test/title test-case)))
          (when (::test/ns test-case)
            (printf " %s %s:%s\n"
                    (color :magenta "|")
                    (::test/ns test-case)
                    (::test/line test-case -1)))
          (when-let [desc (::test/description test-case)]
            (printf " %s %s\n"
                    (color :magenta "|")
                    (color :yellow (::test/description test-case))))
          (printf " %s\n" (color :magenta "|")))

      :step-start
        (let [step (:step event)]
          (printf " %s %s\n"
                  (color :blue "+->>")
                  (::step/title step)))

      :step-end
        (let [result (:step event)]
          (run! ctest/report (::step/reports result))
          (when-let [message (::step/message result)]
            (printf " %s %s\n"
                    (color :blue "|")
                    (color (state-color (::step/outcome result))
                           (::step/message result))))
          (printf " %s [%s] (%s seconds)\n"
                  (color :blue "|")
                  (let [outcome (::step/outcome result "???")]
                    (color
                      [:bold (state-color outcome)]
                      (str/upper-case (name outcome))))
                  (color :cyan (format "%.3f" (::step/elapsed result))))
          (printf " %s\n" (color :blue "|")))

      :cleanup-resource
        (let [{:keys [resource-type parameters]} event]
          (printf " %s Cleaning %s resource %s\n"
                  (color :yellow "+->>")
                  (color [:bold :yellow] resource-type)
                  (color :magenta (pr-str parameters))))

      :cleanup-error
        (let [{:keys [error resource-type parameters]} event]
          (printf " %s Failed to cleanup %s resource %s: %s\n"
                  (color :yellow "|")
                  (color [:bold :yellow] resource-type)
                  (color :magenta (pr-str parameters))
                  (color :red error)))

      :test-end
        (let [result (:test event)]
          (printf " %s %s (%s seconds)\n"
                  (color
                    [:bold (state-color (::test/outcome result))]
                    "*")
                  (::test/title result)
                  (color :cyan (format "%.3f" (test/elapsed result))))
          (when-let [message (::test/message result)]
            (printf "   %s\n" message)))

      (println "Unknown report event type:" (pr-str event)))))


(defn print-console-results
  "Render a set of test results out to the console."
  [results options]
  (let [,,,]
    ; TODO: report results better
    #_(clojure.pprint/pprint results)))


(defn write-junit-results
  "Render a set of test results to a JUnit XML file."
  [report-path results options]
  ; TODO: implement junit reporting (#6)
  (println "WARN: JUnit XML reporting is not available yet"))


(defn write-html-results
  "Render a set of test results to a human-friendly HTML file."
  [report-path results options]
  ; TODO: implement html reporting (#7)
  (println "WARN: HTML reporting is not available yet"))
