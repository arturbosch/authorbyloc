Collects from given project path all contributors and assign them the loc count of
their written files.

As lloc is used for loc calculation only some jvm languages are supported.

If the @author's line tokens are separated with a ',', then we split the line
and all parts are treated as authors. Brackets within an authors name are deleted
with a regex. If the currently scanned sub path (e.g. a file) has not a supported
language ending of lloc, it is skipped. This also happens if the sub path contains
the word resources as this indicates some source code resources for testing.
In the future a concept of name merging is thoughtful.

This little program is though to be used as a groovy script, but can also be build
to a jar file with the shadow task.