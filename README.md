[![Java CI with Maven](https://github.com/pfichtner/vaadoo/actions/workflows/maven.yml/badge.svg)](https://github.com/pfichtner/vaadoo/actions/workflows/maven.yml)

# Vaadoo
Validating automatically domain objects: It's magic

## This is a "work in progress" PoC of weaving JSR 380 (Bean Validation 2.0) checks based on annotations into the bytecode. 

## Why? 
When implementing an application using Spring it's very handy to use the JSR 380 annotations. But where to place them? 
- If the code does not have exlicitly DTOs but mapping it's domain objects directly, the annotations have to been placed on the domain objects but then your domain won't be able to validate the classes until it has some dependency to any JSR 380 implementation
- If your code differs between DTOs and domain objects, you have to options: 
  - Place the JSR 380 annotations on the DTO but then your internal valid state would rely on the checks done in a non-domain layer
  - Again make your donain dependant on a JSR 380 implemenation

So if you decide, that none of these possibilites is an option you cannot just declare things like this...

```java
public class MyDomainObject {
    private final String someStringValue;
    public MyDomainObject(@NotEmpty String someStringValue) {
        this.someStringValue = someStringValue;
    }
}
```

...but you would start implementing hand-written code into you domain objects to make them self-validating: 

```java
public class MyDomainObject {
    private final String someStringValue;
    public MyDomainObject(String someStringValue) {
        Preconditions.checkNotNull(someStringValue, "someStringValue must not be null");
        Preconditions.checkArgument(someStringValue.length() > 0, "someStringValue must not be empty");
        this.someStringValue = someStringValue;
    }
}
```

Ough, what a mess and waste of time! 

And this is where vaadoo comes into play. Vaadoo is a compiler plugin that generates this boilerplate code for you. Checks are added to the bytecode so you get rid of a JSR 380 validation library. The generated code does nor depend on JSR 380 API- nor on JSR 380 validation libaries anymore. Another advantage is that these checks don't depend on reflection: What and how to check will be decided during compile- not during runtime. So this could also be helpful if you can't use reflection in your system or if it's hard to use (like e.g. when compiling native images for GraalVM)


PS: This is getting real fun with lombok and records! 
```java
@lombok.Value public class MyDomainObject {
    @NotEmpty String someStringValue;
}
```

```java
public record MyDomainObject(@NotEmpty String someStringValue) {}
```

## Why are only constructors supported? Please add support for methods as well! 

The intention is to support creating domain classes (value types/entities) and get rid of boilerplate code there. 
You don't want to have methods like ...
```java
void sendMail(String from, String to, String subject, String body) {}
```

... but domain classes MailAddress, Subject and Text. Vaadoo helps you to add validation in a declarative way, so you get: 
```java
record MailAddress(@Email String value) {}
record Subject(@NotBlank @Max(256) String value) {}
record Text(@Max(4 * 1024) String value) {}
[...]
void send(MailAddress from, MailAddress to, Subject subject, Text body) {}
```

If vaadoo would support validation on methods we'd still write code like this
```java
void sendMail(@Email String value, @NotBlank @Max(256) String value, @Max(4 * 1024) String value) {}
```

This is not what vaadoo was thought for! 

## Drawbacks
- no runtime internationalization (i18n) since messages are copied during compile-time into the bytecode

## Other projects/approaches
- https://github.com/opensanca/service-validator



