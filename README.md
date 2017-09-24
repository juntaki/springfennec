# Springfennec

Springfennec generate spec.json suitable for the creation of client library by swagger-codegen.
it has the following features. (compared to Springfox)

* Predictable OperationId. build will fail if it isn't unique.
* No duplicate of model name
* Output spec.json as a file, bootRun isn't required.
* Use Spring annotation than Swagger annotation,

On the contrary, it cannot have the following featue:

* Hosting swagger-ui

Although it supports major (as I think) annotaion, it still cannot cover all Spring functionality.
Please make a request by Pull request or Issue.

## How to work

### OperaionId

nickname of @ApiOperaiton is OperaitonId if defined. or function name is used.
In order to generate a predicatable OperationId, programmer have the responsibility to maintain its uniqueness. Therefore, suffix are not added by springfennec.
If it doesn't become unique, the build will fail.

### Model name

Model name is Java FQCN.

## How to use

### Get started

Use apt or kapt to work Springfennec at build.
Add the following line to build.Gradle.

~~~

~~~

### Springfennec annotation

You can define sub API gruops in an App, by @ApiGroup annotation. (like Docket)
OperationId must be unique for each API group.

For @SwaggerDefinition annotaion, refer to [Swagger-Core Annotations documentation](https://github.com/swagger-api/swagger-core/wiki/Annotations-1.5.X#swaggerdefinition)

~~~
@ApiGroup(value="^/pet/.*",        // regex for path (not include basePath)
          name = "api_group_name", // output will be spec_${name}.json
          apiInfo = @SwaggerDefinition(...))
~~~

### Swagger annotation

The following swagger annotation is used for spec.json generation.
Parameters determined by Spring may be ignored, even if annotation defines it.

~~~
@ApiOperaiton
@ApiParam
~~~
