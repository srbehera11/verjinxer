# VerJInxer Package Organization #

The dependencies between packages should be simple.
If at all possible, there should be no circular package dependencies.

  * `verjinxer.util` is the most basic package. It should only import from standard JDK/JRE packages.

  * `verjinxer.sequenceanalysis` builds on `verjinxer.util`.

  * `verjinxer` contains the package modules. It may import from both helper packages.