# Resilience

Utilities for web service resilience. Java 1.7+.<br/>

For questions and support please contact [services@mercadolibre.com](mailto:services@mercadolibre.com)

# Contents

* [Dependency](#dependency)
* [Circuit Breaker](#circuit-breaker)
    * [Operation](#operation)
    * [Configuration](#configuration)
        * [Parameters](#parameters)
  
  
# Dependency

```xml
<dependency>
    <groupId>com.mercadolibre.resilience</groupId>
    <artifactId>resilience-core</artifactId>
    <version>0.0.5</version>
</dependency>
```

You can find it in our repo

```xml
<repository>
	<id>java-circuit-breaker-mvn-repo</id>
	<url>https://raw.github.com/mercadolibre/java-circuit-breaker/mvn-repo/</url>
	<snapshots>
	    <enabled>true</enabled>
	    <updatePolicy>always</updatePolicy>
	</snapshots>
</repository>
```

# Circuit Breaker

A circuit breaker is intended to be placed between an app and an external service, to be opened automatically upon service malfunction and later close again, based on predefined operational metrics.

This is useful to return quickly a response (partial or failure) in case of known external services shortage, and to prevent unnecessary overload to these services which probably difficult their recovery procedure.

## Operation

An instance of `CircuitBreaker` begins its operation in closed state, and for each request that goes through it, counts a success or failure, based on user defined parameters.

It groups these metrics in fixed time width buckets. Upon rolling of a bucket, it recalculates a service score as a weighted moving average, which is compared against a user predefined minimum acceptable score.

If the calculated score is less than the user defined one, circuit is open for a fixed amount of time and every request will get a `RejectedExecutionException`.

When the open window is elapsed, the circuit breaker closes again in try mode, in which it'll check for a definite amount of time whether the backend service is normal again. In this case, it'll resume normal operation, otherwise it'll open and restart this procedure.

Therefore, a typical breaker cycle shall look as this:

![Typical breaker operation](https://github.com/mercadolibre/java-meli-toolkit/blob/restclient-breaker/resilience/breaker.png)


## Configuration

We provide a factory class `CircuitBreakers` to make breaker creation easier. You can choose between two strategies for score calculation: fixed and exponential.

Using a fixed strategy means you must provide and array of bucket weights, from newer to older.

An exponential strategy weights samples with a decreasing exponential function. You must provide a number between 0 and 1, for the exponential base.

There are many parameters concerning the circuit breaker itself, independent of score strategy, which are detailed as follows

### Parameters

- _interval_: amount of time in ms the breaker should remain open after triggered.
- _tryWindow_: amount of time in ms the breaker should check backend service after _interval_ is elapsed. It should allow al least a full evaluation window (_buckets_*_bucketWidthMs_).
- _buckets_: Amount of buckets to be considered for a score evaluation window
- _bucketWidthMs_: With of each bucket in ms.
- _minScore_: Minimum acceptable score to remain closed. Should be a number between 0 and 1.
- _staleInterval_: Amount expressed as bucket count after which a bucket is to be considered staled and not taken into account for score calculation. A _staleInterval_ of 10 and a _bucketWidthMs_ of 2 means that every bucket older than 20ms before actual time will be discarded. 
- _minMeasures_: Minimal amount of measures for a bucket to be considered eligible for score evaluation.
