# Springfennec

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.juntaki/springfennec/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.juntaki/springfennec)

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

### OperationId

If nickname in @ApiOperation annotation is defined, it will be OperationId. If not, function name will be used.

In order to generate a predictable OperationId, programmer have the responsibility to maintain its uniqueness. Therefore, suffix are not added by springfennec. If it isn't unique in the API group, the build will just fail.

### Model name

Model name is Java FQCN, so it's always unique.

## How to use

### Get started

Please refer to [springfennec-demo](https://github.com/juntaki/springfennec-demo) Project.

Use apt or kapt to work Springfennec at build.
Add the following line to build.Gradle's dependencies.
If you are working with Java only project, add Kotlin dependency also.

~~~
dependencies {
	...
	// Springfennec
	def springfennecVersion = 'x.x.x'
	compile('io.swagger:swagger-core:1.5.16')
	compile("com.juntaki:springfennec:${springfennecVersion}")
	kapt("com.juntaki:springfennec:${springfennecVersion}")
	...
}
~~~

### Springfennec annotation

You can define multiple API groups in an App, by @ApiGroup/@ApiGroups annotations. (like Docket)
OperationId must be unique for each API group.

For @SwaggerDefinition annotation, refer to [Swagger-Core Annotations documentation](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#swaggerdefinition)

~~~
@ApiGroup(value="^/pet/.*",        // regex for path (not include basePath)
          name = "pet_api",        // output will be spec_${name}.json, e.g. spec_pet_api.json
          apiInfo = @SwaggerDefinition(...))
~~~

e.g. ApiGroups example

~~~
@ApiGroups(
        arrayOf(
                ApiGroup(arrayOf("^/demo/user.*"),
                        name = "user",
                        apiInfo = SwaggerDefinition(
                                info = Info(
                                        version = "1.0",
                                        title = "User API"
                                )
                        )
                ),
                ApiGroup(arrayOf("^/demo/admin.*"),
                        name = "admin",
                        apiInfo = SwaggerDefinition(
                                info = Info(
                                        version = "1.0",
                                        title = "Admin API"
                                )
                        )
                )
        )
)
~~~


### Swagger annotation

The following swagger annotation is used for spec.json generation.
Parameters determined by Spring may be ignored, even if the annotation defines it.

~~~
@ApiOperation
@ApiParam
~~~
