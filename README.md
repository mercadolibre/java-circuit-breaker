# Circuit breaker

Circuit breaker for Java 1.7+.<br/>

For questions and support please contact [services@mercadolibre.com](mailto:services@mercadolibre.com)

# Contents

   * [Dependencies](#dependencies)

# Dependencies

You must define the repository resolver

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

And the dependencies themselves

```xml
<dependency>
    <groupId>com.mercadolibre.resilience</groupId>
    <artifactId>resilience-core</artifactId>
    <version>0.0.1</version>
</dependency>
```