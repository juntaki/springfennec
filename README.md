# Springfennec

[Swagger (OpenAPI 2.0)](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md) spec.json generator for the API client generation by [swagger-codegen](https://github.com/swagger-api/swagger-codegen).

## Features 

mainly compared with [Springfox](http://springfox.github.io/springfox/).

* Predictable OperationId. build will fail if it isn't unique.
* No duplicate of model name
* Output spec.json as a file at build, server start isn't required.
* Spring annotation than Swagger annotation, focus on the actual behaviour.

You cannot do the following only with springfennec. Use [swagger-ui](https://github.com/swagger-api/swagger-ui).

* Hosting swagger-ui in App

Although Springfennec supports basic (as I think) Spring functionality, but it still cannot cover all of it.
Please make a request by Pull request or Issue.

## How to work

### OperaionId

If nickname in @ApiOperaiton annotaion is defined, it will be OperaitonId. If not, function name will be used.

In order to generate a predicatable OperationId, programmer have the responsibility to maintain its uniqueness. Therefore, suffix are not added by springfennec. If it isn't unique in the API group, the build will just fail.

### Model name

Model name is Java FQCN.

## How to use

### Get started

Use apt or kapt to work Springfennec at build.
Add the following line to build.Gradle.

~~~
compileOnly 'com.juntaki:springfennec:{version}'
kapt 'com.juntaki:springfennec:{version}'
~~~

### Springfennec annotation

You can define sub API gruops in an App, by @ApiGroup annotation. (like Docket)
OperationId must be unique for each API group.

For @SwaggerDefinition annotaion, refer to [Swagger-Core Annotations documentation](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#swaggerdefinition)

~~~
@ApiGroup(value="^/pet/.*",        // regex for path (not include basePath)
          name = "pet_api",        // output will be spec_${name}.json, e.g. spec_pet_api.json
          apiInfo = @SwaggerDefinition(...))
~~~

### Swagger annotation

The following swagger annotation is used for spec.json generation.
Parameters determined by Spring may be ignored, even if annotation defines it.

~~~
@ApiOperaiton
@ApiParam
~~~
