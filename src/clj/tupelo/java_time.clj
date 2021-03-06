(ns tupelo.java-time
  (:refer-clojure :exclude [range])
  (:use tupelo.core)
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [schema.core :as s] )
  (:import
    [java.time DayOfWeek ZoneId ZonedDateTime Instant Period]
    [java.time.format DateTimeFormatter]
    [java.time.temporal TemporalAdjusters Temporal TemporalAmount ChronoUnit] ))

(defn zoned-date-time?
  "Returns true iff arg is an instance of java.time.ZonedDateTime"
  [it] (instance? ZonedDateTime it)) ; #todo test all
(defn instant?
  "Returns true iff arg is an instance of java.time.Instant "
  [it] (instance? Instant it))
(defn joda-instant?
  "Returns true iff arg is an instance of org.joda.time.ReadableInstant "
  [it] (instance? org.joda.time.ReadableInstant it))

(defn fixed-time-point?
  "Returns true iff arg represents a fixed point in time. Examples:

      [java.time       ZonedDateTime  Instant]
      [org.joda.time        DateTime  Instant  ReadableInstant]
  "
  [it]
  (or (zoned-date-time? it)
    (instant? it)
    (joda-instant? it)))

(defn temporal?
  "Returns true iff arg is an instance of java.time.temporal.Temporal "
  [it]
  (instance? Temporal it))

(defn period?
  "Returns true iff arg is an instance of org.joda.time.ReadablePeriod.
  Example:  (period (days 3)) => true "
  [it]
  (instance? Period it))

; #todo: create a namespace java-time.zone ???
(def zoneid-utc            (ZoneId/of "UTC"))
(def zoneid-us-alaska      (ZoneId/of "US/Alaska"))
(def zoneid-us-aleutian    (ZoneId/of "US/Aleutian"))
(def zoneid-us-central     (ZoneId/of "US/Central"))
(def zoneid-us-eastern     (ZoneId/of "US/Eastern"))
(def zoneid-us-hawaii      (ZoneId/of "US/Hawaii"))
(def zoneid-us-mountain    (ZoneId/of "US/Mountain"))
(def zoneid-us-pacific     (ZoneId/of "US/Pacific"))

(defprotocol ToInstant
   (->instant [arg]))
(extend-protocol ToInstant
  Instant (->instant [arg]
            arg)
  ZonedDateTime (->instant [arg]
                  (.toInstant arg))
  org.joda.time.ReadableInstant (->instant [arg]
                                  (-> arg .getMillis Instant/ofEpochMilli)))

(defn ->zoned-date-time ; #todo -> protocol?
  "Coerces a org.joda.time.ReadableInstant to java.time.ZonedDateTime"
  [arg]
  (cond
    (instance? ZonedDateTime arg) arg
    (instance? Instant arg) (ZonedDateTime/of arg, zoneid-utc)
    (instance? org.joda.time.ReadableInstant arg) (it-> arg
                                                    (->instant it)
                                                    (.atZone it zoneid-utc))
    :else (throw (IllegalArgumentException. (str "Invalid type found: " (class arg) " " arg)))))

(def ^:dynamic *zone-id* zoneid-utc)

(defmacro with-zoneid
  [zone-id & forms]
  `(binding [*zone-id* ~zone-id]
     ~@forms))

; "Returns a ZonedDateTime"
(defn zoned-date-time ; #todo add schema & map-version
  "Returns a java.time.ZonedDateTime with the specified parameters and truncated values (day/month=1, hour/minute/sec=0)
  for all other date/time components. Assumes time zone is UTC unless the maximum-arity constructor is used. Usage:

  ; Assumes UTC time zone
  (zoned-date-time)          => current time
  (zoned-date-time year)
  (zoned-date-time year month)
  (zoned-date-time year month day)
  (zoned-date-time year month day hour)
  (zoned-date-time year month day hour minute)
  (zoned-date-time year month day hour minute second)
  (zoned-date-time year month day hour minute second nanos)

  ; Explicit time zone
  (zoned-date-time year month day hour minute second nanos zone-id)

  ; Explicit time zone alternate shortcut arities.
  (with-zoneid zoneid-us-eastern
    (zoned-date-time year month day ...))  ; any arity w/o zone-id

  "
  ([]                                                 (ZonedDateTime/now *zone-id* ))
  ([year]                                             (zoned-date-time year 1     1   0    0      0      0     *zone-id* ))
  ([year month]                                       (zoned-date-time year month 1   0    0      0      0     *zone-id* ))
  ([year month day]                                   (zoned-date-time year month day 0    0      0      0     *zone-id* ))
  ([year month day hour]                              (zoned-date-time year month day hour 0      0      0     *zone-id* ))
  ([year month day hour minute]                       (zoned-date-time year month day hour minute 0      0     *zone-id* ))
  ([year month day hour minute second]                (zoned-date-time year month day hour minute second 0     *zone-id* ))
  ([year month day hour minute second nanos]          (zoned-date-time year month day hour minute second nanos *zone-id* ))
  ([year month day hour minute second nanos zone-id]  (ZonedDateTime/of year month day hour minute second nanos zone-id)))

; #todo: need idempotent ->zoned-date-time-utc (using with-zoneid) & ->instant for ZonedDateTime, Instant, OffsetDateTime
; #todo: need offset-date-time & with-offset
; #todo: need (instant year month day ...) arities

(defn instant
  "Wrapper for java.time.Instant/now "
  [] (java.time.Instant/now))

(defn millis->instant
  "Wrapper for java.time.Instant/ofEpochMilli "
  [millis] (java.time.Instant/ofEpochMilli millis))

(defn secs->instant
  "Wrapper for java.time.Instant/ofEpochSecs "
  [secs] (java.time.Instant/ofEpochSecond secs))

(defn now->zdt
  "Returns the current time as a java.lang.ZonedDateTime (UTC)"
  [] (with-zoneid zoneid-utc (ZonedDateTime/now)))

(defn now->instant
  "Returns the current time as a java.lang.Instant"
  [] (Instant/now))


;----------------------------------------------------------------------------------------
; #todo: Make all use protocol for all Temporal's (ZonedDateTime, OffsetDateTime, Instant, ...?)

; #todo: make work for Clojure `=` ZDT, Instant, etc
(defn same-instant? ; #todo coerce to correct type
  "Returns true iff two ZonedDateTime objects represent the same instant of time, regardless of time zone.
   A thin wrapper over `ZonedDateTime/isEqual`"
  [this & others]
  (cond
    (every? instant? others) (every? truthy?
                               (mapv #(.equals this %) others))

    (every? zoned-date-time? others) (every? truthy?
                                       (mapv #(.isEqual this %) others))

    ; attempt to coerce to Instant
    :else (let [instants (mapv ->instant (prepend this others))]
            (apply same-instant? instants))))

(def DateTimeStamp (s/conditional
                     #(instance? ZonedDateTime %) ZonedDateTime
                     #(instance? Instant %)  Instant))

; #todo need version of < and <= (N-arity) for both ZDT/Instant

(s/defn trunc-to-second
  "Returns a ZonedDateTime truncated to first instant of the second."
  [zdt :- DateTimeStamp]
  (.truncatedTo zdt java.time.temporal.ChronoUnit/SECONDS))

(s/defn trunc-to-minute
  "Returns a ZonedDateTime truncated to first instant of the minute."
  [zdt :- DateTimeStamp]
  (.truncatedTo zdt java.time.temporal.ChronoUnit/MINUTES))

(s/defn trunc-to-hour
  "Returns a ZonedDateTime truncated to first instant of the hour."
  [zdt :- DateTimeStamp]
  (.truncatedTo zdt java.time.temporal.ChronoUnit/HOURS))

(s/defn trunc-to-day
  "Returns a ZonedDateTime truncated to first instant of the day."
  [zdt :- DateTimeStamp]
  (.truncatedTo zdt java.time.temporal.ChronoUnit/DAYS))

(s/defn trunc-to-month
  "Returns a ZonedDateTime truncated to first instant of the month."
  [zdt :- ZonedDateTime]
  (-> zdt
    trunc-to-day
    (.with (TemporalAdjusters/firstDayOfMonth))))

(s/defn trunc-to-year
  "Returns a ZonedDateTime truncated to first instant of the year."
  [zdt :- ZonedDateTime]
  (-> zdt
    trunc-to-day
    (.with (TemporalAdjusters/firstDayOfYear))))

(s/defn trunc-to-sunday-midnight
  "For an instant T, truncate time to midnight and return the first Sunday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/SUNDAY))))

(s/defn trunc-to-monday-midnight
  "For an instant T, truncate time to midnight and return the first Monday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/MONDAY))))

(s/defn trunc-to-tuesday-midnight
  "For an instant T, truncate time to midnight and return the first Tuesday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/TUESDAY))))

(s/defn trunc-to-wednesday-midnight
  "For an instant T, truncate time to midnight and return the first Wednesday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/WEDNESDAY))))

(s/defn trunc-to-thursday-midnight
  "For an instant T, truncate time to midnight and return the first thursday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/THURSDAY))))

(s/defn trunc-to-friday-midnight
  "For an instant T, truncate time to midnight and return the first Friday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/FRIDAY))))

(s/defn trunc-to-saturday-midnight
  "For an instant T, truncate time to midnight and return the first Saturday at or before T."
  [temporal :- Temporal]
  (validate temporal? temporal) ; #todo plumatic schema
  (-> temporal
    trunc-to-day
    (.with (TemporalAdjusters/previousOrSame DayOfWeek/SATURDAY))))

(defn string-date-iso ; #todo maybe inst->iso-date
  "Returns a string like `2018-09-05`"
  [zdt]
  (.format zdt DateTimeFormatter/ISO_LOCAL_DATE))

(defn now->iso-str
  "Returns an ISO string representation of the current time,
  like '2019-02-19T18:44:01.123456Z' "
  []
  (-> (java.time.Instant/now)
    (.toString)))

(defn now->iso-str-simple
  "Returns a canonical string representation of the current time truncated to the current second,
  like '2019-02-19 18:44:01Z' "
  []
  (-> (java.time.Instant/now)
    (.truncatedTo ChronoUnit/SECONDS)
    (str/replace-first \T \space)
    (.toString)))

; #todo rethink these and simplify/rename
(defn string-date-compact
  "Returns a compact date-time string like `2018-09-05 23:05:19.123Z` => `20180905` "
  [timestamp]
  (let [formatter (DateTimeFormatter/ofPattern "yyyyMMdd")]
    (.format timestamp formatter)))

(defn string-date-time-iso ; #todo maybe inst->iso-date-time
  "Returns a ISO date-time string like `2018-09-05T23:05:19.123Z`"
  [timestamp]
  (str (->instant timestamp))) ; uses DateTimeFormatter/ISO_INSTANT

(defn string-date-time-compact
  "Returns a compact date-time string like `2018-09-05 23:05:19.123Z` => `20180905-230519` "
  [timestamp]
  (let [formatter (DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")]
    (.format timestamp formatter)))

(defn string-date-time-hyphens
  "Returns a compact date-time string like `2018-09-05 23:05:19.123Z` => `2018-09-05-23-05-19` "
  [timestamp]
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd-HH-mm-ss")]
    (.format timestamp formatter)))

(defn string-date-time-nice
  "Returns an ISO date-time string like `2018-09-05 23:05:19.123Z`
  (with a space instead of `T`)"
  [timestamp]
  (let [sb (StringBuffer. (string-date-time-iso timestamp))]
    (.setCharAt sb 10 \space)
    (str sb)))

(s/defn iso-str->millis
  "Convert an ISO 8601 string to a java.sql.Date"
  [iso-datetime-str :- s/Str]
  (-> iso-datetime-str
    (Instant/parse)
    (.toEpochMilli)))

(s/defn iso-str->timestamp
  "Convert an ISO 8601 string to a java.sql.Date"
  [iso-datetime-str :- s/Str]
  (java.sql.Timestamp.
    (iso-str->millis iso-datetime-str)))

(defn  walk-timestamp->instant
  "Walks a tree-like data structure, converting any instances of java.sql.Timestamp => java.time.Instant"
  [tree]
  (walk/postwalk
    (fn [item]
      (if (= java.sql.Timestamp (type item))
        (.toInstant item)
        item))
    tree))

(defn walk-instant->timestamp
  "Walks a tree-like data structure, converting any instances of java.sql.Timestamp => java.time.Instant"
  [tree]
  (walk/postwalk
    (fn [item]
      (if (= java.time.Instant (type item))
        (java.sql.Timestamp.
          (.toEpochMilli item))
        item))
    tree))

(defn walk-instant->str
  "Walks a tree-like data structure, calling `.toString` on any instances java.time.Instant"
  [tree]
  (walk/postwalk
    (fn [item]
      (if (= java.time.Instant (type item))
        (.toString item)
        item))
    tree))

;-----------------------------------------------------------------------------
(defn ^:deprecated iso-date-str
  "DEPRECATED: use `date-str-iso`"
  [& args] (apply string-date-iso args))

(defn ^:deprecated iso-date-time-str
  "DEPRECATED: use `date-time-str-iso`"
  [& args] (apply string-date-time-iso args))
;-----------------------------------------------------------------------------

; #todo make work for relative times (LocalDate, LocalDateTime, etc)
(defn stringify-times
  "Will recursively walk any data structure, converting any `fixed-time-point?` object to a string"
  [form]
  (walk/postwalk
    #(if (fixed-time-point? %) (iso-date-time-str %) %)
    form))

(defn range
  "Returns a vector of instants in the half-open interval [start stop) (both instants)
  with increment <step> (a period). Not lazy.  Example:

       (range (zoned-date-time 2018 9 1)
              (zoned-date-time 2018 9 5)
              (Duration/ofDays 1)))  => <vector of 4 ZonedDateTime's from 2018-9-1 thru 2018-9-4>
  "
  [start-inst stop-inst step-dur]
  (validate temporal? start-inst) ; #todo use Plumatic Schema
  (validate temporal? stop-inst) ; #todo use Plumatic Schema
  (verify (instance? TemporalAmount step-dur)) ; #todo -> predicate fn?
  (loop [result    []
         curr-inst start-inst]
    (if (.isBefore curr-inst stop-inst)
      (recur (conj result curr-inst)
        (.plus curr-inst step-dur))
      result)))

(defn interval
  "Returns a map representing an interval in time. Usage:

    (interval start stop)           ; default type => :half-open
    (interval start stop type)      ; type one of [:open :half-open :closed]
  "
  ([start stop] (interval start stop :half-open))
  ([start stop type]
   {:lower-bound   (->instant start)
    :upper-bound   (->instant stop)
    :interval-type (validate #{:open :half-open :closed} type)}))

(defn interval?
  "Returns true iff the arg represents an interval"
  [it] (= (set (keys it)) #{:lower-bound :upper-bound :interval-type}))

(defn interval-contains?
  "Returns true iff the interval contains the instant in time"
  [interval time]
  (validate interval? interval)
  (validate fixed-time-point? time)
  (let [instant              (->instant time)
        lb                   (grab :lower-bound interval)
        ub                   (grab :upper-bound interval)
        within-open-interval (and (.isBefore lb instant) (.isBefore instant ub))
        interval-type       (grab :interval-type interval) ]
    (cond
      (= interval-type :open)           within-open-interval
      (= interval-type :half-open)  (or within-open-interval (.equals instant lb))
      (= interval-type :closed)     (or within-open-interval (.equals instant lb) (.equals instant ub)))))
