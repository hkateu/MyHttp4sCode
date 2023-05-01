## Http4s Feature Examples

The main purpose of this project is to help me get a deeper understanding of the Http4s documentation. I structured the project with the same names in the documentation. The hanging code has been filled out, where you would find `???` for unfinished implementations in http4s documentation, i tried my best to implement these methods so that the code compiles.
This whole project will be written entirely in scala 3 simply because its awesome :-) .

## How to follow along

Clone this project into you computer by using this code:

```bash
> git clone https://github.com/hkateu/MyHttp4sCode.git
```

Navigate to the project directory

```bash
> cd MyHttp4sCode
```

Execute the sbt command to make sure it's working, this will take you to the sbt shell

```bash
> sbt
```

Each section is a standalone module, you can list the modules, the following way

```bash
sbt:root> projects
```

The default module is `root` module which is an aggregate of all the rest, once you compile it all the necessary libraries will download for the whole project, you can do this by running the compile command.

```bash
sbt:root> compile
```

Running root should produce the following output:

```bash
Scala3 Http4s code examples
```

To execute code for a particular module, for example `service`, one would run the following sequence of commands

```bash
sbt:root> project service
sbt:service>
```
