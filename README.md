[Error Prone](https://github.com/google/error-prone) checks and refactorings in support of [Google Flogger](https://github.com/google/flogger) logging API.

Refactorings:
* Migrate from common logging APIs to Google Flogger:
   * Commons Logging
   * Java Logging (java.util.logging)
   * Log4J / Log4J2
   * SLF4J
   * TinyLog / TinyLog2
* (TODO) remove unnecessary conditionals around logging statements
* (TODO) convert string-concatenated formatting messages into parameters
* (TODO) make complex formatting arguments lazy

Checks
* (TODO) Validate format strings used in log messages
* (TODO) Validate idiomatic creation of Flogger instance
  

# Refactorings
## Migration

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
* Classes with multiple logger member variables are not migrated
  * e.g. `LoggerFactory.getLogger(getClass())` & `LoggerFactory.getLogger("myLogger")` would not be migrated

### Log4J (v1.x)
| Log Construct | Migration Notes |
| --- | --- |
| `LogManager.getLogger( Class<?> )` | Migrated to `FluentLogger.forEnclosingClass()`| 
| `LogManager.getLogger( String )` | *Not migrated* |
| `Throwable` arguments | Trailing arguments of type `java.lang.Throwable` are migrated to `.withCause(t)`
| `is*Enabled` methods | Migrated to `logger.at*().isEnabled()` |
| Log levels | trace -> finest, debug -> fine, info -> info, warn -> warning, error -> severe, fatal -> severe |

Notes:
* Classes with multiple logger member variables are not migrated
    * e.g. `LogManager.getLogger(getClass())` & `LogManager.getLogger("myLogger")` would not be migrated
* Classes that use custom log levels are not migrated

### Log4J2 (v2.x)
| Log Construct | Migration Notes |
| --- | --- |
| `LogManager.getLogger( Class<?> )` | Migrated to `FluentLogger.forEnclosingClass()`| 
| `LogManager.getLogger( String )` | *Not migrated* |
| Marker parameters | *Marker parameters are silently ignored during migration* |
| Message format | Log4J2 parameter placeholders `{}` are migrated to `%s`, honoring escaping logic from Log4J2 |
| `Throwable` arguments | Trailing arguments of type `java.lang.Throwable` are migrated to `.withCause(t)`
| `is*Enabled` methods | Migrated to `logger.at*().isEnabled()` |
| Log levels | trace -> finest, debug -> fine, info -> info, warn -> warning, error -> severe, fatal -> severe |

Notes:
* Classes with multiple logger member variables are not migrated
    * e.g. `LogManager.getLogger(getClass())` & `LogManager.getLogger("myLogger")` would not be migrated
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
| Methods `entering, exiting` | *Not migrated* |
| Methods `logp, logrb` | *Not migrated* |
| Method `throwing` | *Not migrated* |
| Method `log( LogRecord )` | *Not migrated* |
| Methods taking a `Supplier<String>` parameter | *Not migrated* |

Notes:
* Classes with multiple logger member variables are not migrated


# Configuration

| Command Line Option | Arguments |
| --- | --- |
| LoggerApiRefactoring:SourceApi=<api name> | slf4j,log4j,log4j2,tinylog,tinylog2,commons-logging,jul |

