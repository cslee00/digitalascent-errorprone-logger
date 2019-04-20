[Error Prone](https://github.com/google/error-prone) checks and refactorings in support of [Google Flogger](https://github.com/google/flogger) logging API.

Refactorings:
* Migrate from common logging APIs to Google Flogger:
   * Commons Logging
   * Java Logging (java.util.logging)
   * Log4J 
   * Log4J2
   * SLF4J
   * TinyLog 
   * TinyLog2
* Remove unnecessary `toString()` calls on message format arguments
* Remove unnecessary `Arrays.toString()` calls on message format arguments
* Remove unnecessary conditionals around logging statements
* Convert `String.format` and `MessageFormat.format` calls
* Convert string-concatenated messages into parameterized messages
* Unpack extraneous use of `new Object[] { arg1, ... }`
* Make expensive formatting arguments (method invocations, allocations) lazy

Checks
* (TODO) Validate format strings used in log messages
* (TODO) Validate idiomatic creation of Flogger instance
  

# Refactorings
## Migration

### Gradle

Example Gradle build script located [here](blob/master/examples/gradle/build.gradle)

```
./gradlew clean compileJava
```

### Manual steps 

Add Error Prone options to enable patch checks: (https://errorprone.info/docs/patching)

```
-XepPatchChecks:LoggerApiRefactoring
-XepPatchLocation:/full/path/to/your/source/root
```
Add option to select source logging API:
```
-XepOpt:LoggerApiRefactoring:SourceApi=slf4j
```
Available source API names:

| Option Name | Source Logging API |
| --- | --- |
| slf4j | SLF4J API |
| log4j | Log4J 1.x |
| log4j2 | Log4J 2.x |
| commons-logging | Commons Logging API |
| jul | Java Logging (java.util.logging) |
| tinylog | TinyLog 1.x |
| tinylog2| TinyLog 2.x |


The tool attempts to migrate common, idiomatic use cases for the source logging API; not all use cases of the source API are migrated (see Limitations section below).  

### SLF4J

| Log Construct | Migration Notes |
| --- | --- |
| `LoggerFactory.getLogger( Class<?> )` | Migrated to `FluentLogger.forEnclosingClass()`| 
| `LoggerFactory.getLogger( String )` | *Not migrated* |
| Marker parameters | *Marker parameters are silently ignored during migration* |
| Message format | SLF4J parameter placeholders `{}` are migrated to `%s`, honoring escaping logic from SLF4J |
| `Throwable` arguments | Trailing arguments of type `java.lang.Throwable` are migrated to `.withCause(t)`
| `is*Enabled` methods | Migrated to `logger.at*().isEnabled()` |
| Log levels | trace -> finest, debug -> fine, info -> info, warn -> warning, error -> severe |

Notes:
* Only loggers matching the class name are migrated

### Log4J (v1.x)
| Log Construct | Migration Notes |
| --- | --- |
| `LogManager.getLogger( Class<?> )` | Migrated to `FluentLogger.forEnclosingClass()`| 
| `LogManager.getLogger( String )` | Only loggers matching the class name are migrated |
| `Throwable` arguments | Trailing arguments of type `java.lang.Throwable` are migrated to `.withCause(t)`
| `is*Enabled` methods | Migrated to `logger.at*().isEnabled()` |
| Log levels | trace -> finest, debug -> fine, info -> info, warn -> warning, error -> severe, fatal -> severe |

Notes:
* Only loggers matching the class name are migrated
* Classes that use custom log levels are not migrated

### Log4J2 (v2.x)
| Log Construct | Migration Notes |
| --- | --- |
| `LogManager.getLogger( Class<?> )` | Migrated to `FluentLogger.forEnclosingClass()`| 
| `LogManager.getLogger( String )` | Only loggers matching the class name are migrated |
| Marker parameters | *Marker parameters are silently ignored during migration* |
| Message format | Log4J2 parameter placeholders `{}` are migrated to `%s`, honoring escaping logic from Log4J2 |
| `Throwable` arguments | Trailing arguments of type `java.lang.Throwable` are migrated to `.withCause(t)`
| `is*Enabled` methods | Migrated to `logger.at*().isEnabled()` |
| Log levels | trace -> finest, debug -> fine, info -> info, warn -> warning, error -> severe, fatal -> severe |

Notes:
* Only loggers matching the class name are migrated
* Classes that use custom log levels are not migrated
* TODO Migration will attempt to determine which parameter format style is in use and adjust on a per-class basis; 
this may not always work correctly (especially when loggers are inherited).  See the Configuration section for options to
force the resolution to be 'braces' (for `LogManager.getLogger()`) or 'printf' (for `LogManager.getFormatterLogger()`) 


### Java Logging (java.util.logging) 

| Log Construct | Migration Notes |
| --- | --- |
| `Logger.getLogger( String )` | Migrated to `FluentLogger.forEnclosingClass()`| 
| Message format | JUL MessageFormat placeholders `{0}` are migrated to `%s`, honoring escaping logic from MessageFormat |
| `Throwable` arguments | Trailing arguments of type `java.lang.Throwable` are migrated to `.withCause(t)`
| `isEnabledFor` method | Migrated to `logger.at( Level ).isEnabled()` |
| Log levels | directly migrated one-to-one |
| Custom log levels | directly migrated (log levels don't change, they are passed into Flogger) |
| Methods `entering, exiting` | Code is migrated to emit these at FINER level (as JUL logger does) |
| Methods `logp, logrb` | *Not migrated* |
| Method `throwing` | Code is migrated to emit this at FINER level (as JUL logger does) |
| Method `log( LogRecord )` | *Not migrated* |

Notes:
* Only loggers matching the class name are migrated


# Configuration

| Error Prone Option | Arguments |
| --- | --- |
| LoggerApiRefactoring:SourceApi=<api name> | slf4j,log4j,log4j2,tinylog,tinylog2,commons-logging,jul |

