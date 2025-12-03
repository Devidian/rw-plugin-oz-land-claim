# Maven Plugin Template for Rising World

Use this repository as template for new Rising World Plugins.

## Files included

- [.github/workflows/ci.yml](.github/workflows/ci.yml)
  - for GitHub Action, you have to change `Devidian/rw-plugin-maven-template` here (2x)
- [src/assembly/rw-plugin-maven-template.xml](src/assembly/rw-plugin-maven-template.xml)
  - this is needed to pack you plugin as zip, change atleast directory/outputDirectory here
  - change name to your `pom.project.artifactId`
  - i use `pom.project.name` for directory/outputDirectory
- [src/resources/plugin.yml](src/resources/plugin.yml)
  - your plugin definition file, change as you need
- [src/de/omegazirkel/risingworld/MavenTemplate.java](src/de/omegazirkel/risingworld/MavenTemplate.java)
  - sample main file for your plugin, change name and path as you need (dont forget to change it in plugin.yml too)
- [pom.xml](pom.xml)
  - maven file, change as you need it
- [HISTORY.md](HISTORY.md)
  - for your changelog
- [README.md](README.md)
  - this file, override it as you like
