[![Java CI with Maven](https://github.com/pfichtner/vaadoo/actions/workflows/maven.yml/badge.svg)](https://github.com/pfichtner/vaadoo/actions/workflows/maven.yml)

# Vaadoo
Validating automatically domain objects: It's magic

## This is a "work in progress" PoC of weaving JSR 380 (Bean Validation 2.0) checks based on annotations into the bytecode. 

## Why? 
When implementing an application using Spring it's very handy to use the JSR 380 annotations. But where to place them? 
- If the code does not have exlicitly DTOs but mapping it's domain objects directly, the annotations have to been placed on the domain objects but then your domain won't be able to validate the classes until it has some dependency to any JSR 380 implementation
- If your code differs between DTOs and domain objects, you have to options: 
  - Place the JSR 380 annotations to the DTO but then your internal vali state would rely on the checks done in a non-domain layer
  - Again make your donain dependant on a JSR 380 implemenation

So if you decide, that none of these possibilites is an option you cannot just declare things like this...

```
public MyDomainObject {
    private final String someStringValue
    public MyDomainObject(@NotEmpty String someStringValue) {
        this.someStringValue = someStringValue;
    }
}
```

...but you would start implementing hand-written code into you domain objects to make them self-validating: 

```
public MyDomainObject {
    private final String someStringValue
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
```
@Value
public MyDomainObject {
    @NotEmpty private final String someStringValue
}
```

```
public record MyDomainObject(@NotEmpty String someStringValue) {}
```



- supporting javax.validation ([ ]) AND jakarta.validation ([X])?
- Thouht to annotate the conctructors so checks are currently only added on constructors
- Support JSR 380 annotations on fields?
- Check if annotation support parameter type (e.g. @NotBlank Integer is not valid) and add tests for it (e.g. @NotBlank with non CharSequence)
- Add "but was %s" to messages? 
- Support for custom message (@Min(value = 18, message = "Age should not be less than 18"), @Email(message = "Email yada yada yada"))
- Feature to turn on to generate for ALL notations by default or only if the constructor/class has a @Vadoo annotation
- Tests with mutliple constructors
- Tests with multiple annotations (NotNull, NotEmpty, ...)
- Test if validate method already is present
- Test with lombok classes (annotations on fields, @AllArgsConstructor) --> https://projectlombok.org/features/constructor --> lombok.copyableAnnotations
- Test with records
- Is/should there be an annotation evaluation order?
- Support type constraints (e.g. List<@NotBlank String>)
- PBTs that generate bytecode, transform it and run it (@NotEmpty and checks with all valid types and some invalid ones)
- Inlining "validate" method(s)?  https://lsieun.github.io/assets/pdf/asm-transformations.pdf https://github.com/xxDark/asm-inline
- Support for custom validators
- Test Oracle comparing vadoo behaviuos against jakarta behaviour
- Use messages like hibernate reference implementation src/main/resources/org/hibernate/validator/ValidationMessages.properties (also I18N)
- Check compatibility to https://github.com/hibernate/hibernate-validator/ (tests)
- Check compatibility to https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#validator-annotation-processor

[X] AssertFalse
[X] AssertTrue
[ ] DecimalMax
[ ] DecimalMin
[ ] Digits
[ ] Email
[ ] Future
[ ] FutureOrPresent
[ ] Max
[X] Min (only int)
[ ] Negative
[ ] NegativeOrZero
[X] NotBlank
[X] NotEmpty
[X] NotNull
[X] Null
[ ] Past
[ ] PastOrPresent
[ ] Pattern
[ ] Positive
[ ] PositiveOrZero
[ ] Size

CodeEmitters are be exchangeable but at the moment only Guava is implemented
[ ] JDK-only
[X] Guava (depends mostly on the class Preconditions and it's methods checkNotNull but also on CharMatcher)

