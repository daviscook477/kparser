# kparser

A bidirectional converter between scml projects and klei animation files.

For the direction (klei animation files -> scml project) this tool must be used in tandem with a unity asset extractor. You will have to use the asset extractor to get the game files - specifically you are looking for the
atlas file (a single png that contains image data for an animation) and the corresponding (they have the same name)
`*.build` and `*.anim` file. Then this tool can convert that into an scml project.

For the direction (scml project -> klei animation files) this tool must be used in tandem with an actual full unity
install. Use version 2018 because that is the version that Oxygen not included runs on. This is because after this
tool creates the klei animation files from your scml project you will need to use unity to create an asset bundle
that contains the atlas file (`*.png` file), the build file (`*.build`) and the animation file (`*.anim`).

The conversion from (klei animation files -> scml project) is a lossy conversion because klei animation format is more powerful than spriter (main limitation is skew transforms aren't part of spriter). But the conversion from (scml project -> klei animation file) is correct. It may be running the animation at an incorrect speed though.

Both directions have been tested and confirmed to work. A more clear tutorial on how to use this tool to create asset mods for Oxygen not included will be made.